package com.investigation.graph.adapter;

import com.investigation.graph.model.GraphData;
import com.investigation.graph.model.GraphEdge;
import com.investigation.graph.model.GraphNode;
import com.investigation.graph.model.NodeType;
import com.investigation.model.CaseModel;
import com.investigation.model.EventModel;
import com.investigation.model.EvidenceModel;
import com.investigation.model.FindingModel;
import com.investigation.util.DateTimeUtils;

import java.util.*;

/**
 * Adapter that converts standard Case, Evidence, Event, and Finding domain models
 * into an interactive GraphData topology without requiring a dedicated backend graph endpoint.
 */
public class CaseGraphAdapter {

    public GraphData buildGraph(CaseModel caseModel,
                                List<EvidenceModel> evidenceList,
                                List<EventModel> eventList,
                                List<FindingModel> findingsList) {

        GraphData graph = new GraphData();
        if (caseModel == null) return graph;

        // Cache for de-duplicating entity nodes (users, devices, IPs, files, URLs)
        Map<String, GraphNode> userNodes = new HashMap<>();
        Map<String, GraphNode> deviceNodes = new HashMap<>();
        Map<String, GraphNode> ipNodes = new HashMap<>();
        Map<String, GraphNode> fileNodes = new HashMap<>();
        Map<String, GraphNode> urlNodes = new HashMap<>();
        Map<String, GraphNode> evidenceNodes = new HashMap<>();

        // 1. Evidence Nodes
        if (evidenceList != null) {
            for (EvidenceModel ev : evidenceList) {
                GraphNode evNode = new GraphNode(
                        "ev_" + ev.getId(),
                        ev.getFileName(),
                        NodeType.EVIDENCE,
                        ev.getFileType()
                );
                evNode.addProperty("Evidence ID", ev.getId());
                evNode.addProperty("Status", ev.getStatus() != null ? ev.getStatus().getDisplayName() : "Unknown");
                evNode.addProperty("Size", ev.getFormattedSize());
                evNode.addProperty("SHA-256", ev.getHashSha256() != null ? ev.getHashSha256() : "N/A");
                evNode.addProperty("Source", ev.getSource() != null ? ev.getSource() : "Unknown");

                evidenceNodes.put(ev.getId(), evNode);
                graph.addNode(evNode);
            }
        }

        // 2. Events, Entities, and Relationships
        if (eventList != null) {
            for (EventModel evt : eventList) {
                // Event Node
                String evtLabel = evt.getEventType() != null ? evt.getEventType() : "EVENT";
                GraphNode eventNode = new GraphNode(
                        "evt_" + evt.getId(),
                        evtLabel,
                        NodeType.EVENT,
                        DateTimeUtils.formatTimeOnly(evt.getTimestamp())
                );
                eventNode.addProperty("Event ID", evt.getId());
                eventNode.addProperty("Timestamp", DateTimeUtils.formatUtc(evt.getTimestamp()));
                eventNode.addProperty("Type", evt.getEventType());
                eventNode.addProperty("Description", evt.getDescription());
                eventNode.addProperty("Success", evt.getSuccess() != null ? evt.getSuccess().toString() : "N/A");
                if (evt.getBytesTransferred() != null && evt.getBytesTransferred() > 0) {
                    eventNode.addProperty("Bytes", evt.getBytesTransferred() + " B");
                }
                graph.addNode(eventNode);

                // Link to source Evidence
                if (evt.getEvidenceId() != null && evidenceNodes.containsKey(evt.getEvidenceId())) {
                    GraphNode evNode = evidenceNodes.get(evt.getEvidenceId());
                    graph.addEdge(new GraphEdge(
                            "e_ev_" + evt.getId(),
                            eventNode,
                            evNode,
                            "EXTRACTED_FROM"
                    ));
                }

                // User Entity Node
                if (evt.getUser() != null && !evt.getUser().isBlank()) {
                    String userKey = evt.getUser().toLowerCase();
                    GraphNode uNode = userNodes.computeIfAbsent(userKey, k -> {
                        GraphNode n = new GraphNode(
                                "user_" + userKey,
                                evt.getUser(),
                                NodeType.PERSON,
                                "Account"
                        );
                        n.addProperty("Username", evt.getUser());
                        n.addProperty("Entity Type", "User / Principal");
                        graph.addNode(n);
                        return n;
                    });

                    graph.addEdge(new GraphEdge(
                            "e_u_" + evt.getId(),
                            uNode,
                            eventNode,
                            "PERFORMED"
                    ));
                }

                // Device Entity Node
                if (evt.getDevice() != null && !evt.getDevice().isBlank()) {
                    String devKey = evt.getDevice().toUpperCase();
                    GraphNode dNode = deviceNodes.computeIfAbsent(devKey, k -> {
                        GraphNode n = new GraphNode(
                                "dev_" + devKey,
                                evt.getDevice(),
                                NodeType.DEVICE,
                                "Endpoint"
                        );
                        n.addProperty("Host / Device", evt.getDevice());
                        n.addProperty("Entity Type", "System Endpoint");
                        graph.addNode(n);
                        return n;
                    });

                    graph.addEdge(new GraphEdge(
                            "e_d_" + evt.getId(),
                            eventNode,
                            dNode,
                            "HOSTED_ON"
                    ));

                    // If user is also present, connect user to device
                    if (evt.getUser() != null && !evt.getUser().isBlank()) {
                        GraphNode uNode = userNodes.get(evt.getUser().toLowerCase());
                        if (uNode != null) {
                            graph.addEdge(new GraphEdge(
                                    "e_ud_" + uNode.getId() + "_" + dNode.getId(),
                                    uNode,
                                    dNode,
                                    "LOGGED_INTO"
                            ));
                        }
                    }
                }

                // IP Address Entity Node (Source IP)
                GraphNode ipNode = null;
                if (evt.getIpAddress() != null && !evt.getIpAddress().isBlank()) {
                    String ipKey = evt.getIpAddress();
                    ipNode = ipNodes.computeIfAbsent(ipKey, k -> {
                        boolean isExternal = !ipKey.startsWith("192.168.") && !ipKey.startsWith("10.") && !ipKey.startsWith("127.");
                        GraphNode n = new GraphNode(
                                "ip_" + ipKey.replace(".", "_"),
                                ipKey,
                                NodeType.IP_ADDRESS,
                                isExternal ? "External IP" : "Internal Subnet"
                        );
                        n.addProperty("IP Address", ipKey);
                        n.addProperty("Routing", isExternal ? "Public / WAN" : "Private / RFC1918");
                        graph.addNode(n);
                        return n;
                    });

                    graph.addEdge(new GraphEdge(
                            "e_ip_" + evt.getId(),
                            eventNode,
                            ipNode,
                            "COMMUNICATED_WITH"
                    ));

                    // USER -> IP
                    if (evt.getUser() != null && !evt.getUser().isBlank()) {
                        GraphNode uNode = userNodes.get(evt.getUser().toLowerCase());
                        if (uNode != null) {
                            graph.addEdge(new GraphEdge(
                                    "e_uip_" + uNode.getId() + "_" + ipNode.getId(),
                                    uNode,
                                    ipNode,
                                    "ORIGINATED_FROM"
                            ));
                        }
                    }
                }

                // Destination IP Entity Node
                if (evt.getDestinationIp() != null && !evt.getDestinationIp().isBlank()) {
                    String destIpKey = evt.getDestinationIp();
                    GraphNode destIpNode = ipNodes.computeIfAbsent(destIpKey, k -> {
                        boolean isExternal = !destIpKey.startsWith("192.168.") && !destIpKey.startsWith("10.") && !destIpKey.startsWith("127.");
                        GraphNode n = new GraphNode(
                                "ip_" + destIpKey.replace(".", "_"),
                                destIpKey,
                                NodeType.IP_ADDRESS,
                                isExternal ? "Destination WAN" : "Destination LAN"
                        );
                        n.addProperty("IP Address", destIpKey);
                        n.addProperty("Type", "Destination IP");
                        graph.addNode(n);
                        return n;
                    });

                    // EVENT -> DESTINATION IP
                    graph.addEdge(new GraphEdge(
                            "e_destip_" + evt.getId(),
                            eventNode,
                            destIpNode,
                            "TARGET_IP"
                    ));

                    // IP -> DESTINATION IP
                    if (ipNode != null && !ipNode.equals(destIpNode)) {
                        graph.addEdge(new GraphEdge(
                                "e_iproute_" + ipNode.getId() + "_" + destIpNode.getId(),
                                ipNode,
                                destIpNode,
                                "CONNECTED_TO"
                        ));
                    }
                }

                // Target File Entity Node
                if (evt.getFileName() != null && !evt.getFileName().isBlank()) {
                    String fileKey = evt.getFileName().toLowerCase();
                    GraphNode fNode = fileNodes.computeIfAbsent(fileKey, k -> {
                        GraphNode n = new GraphNode(
                                "file_" + Math.abs(fileKey.hashCode()),
                                evt.getFileName(),
                                NodeType.FILE,
                                "File Resource"
                        );
                        n.addProperty("File Name", evt.getFileName());
                        n.addProperty("Entity Type", "Target File");
                        graph.addNode(n);
                        return n;
                    });

                    // EVENT -> FILE
                    graph.addEdge(new GraphEdge(
                            "e_file_" + evt.getId(),
                            eventNode,
                            fNode,
                            "TARGET_FILE"
                    ));

                    // USER -> FILE
                    if (evt.getUser() != null && !evt.getUser().isBlank()) {
                        GraphNode uNode = userNodes.get(evt.getUser().toLowerCase());
                        if (uNode != null) {
                            graph.addEdge(new GraphEdge(
                                    "e_ufile_" + uNode.getId() + "_" + fNode.getId(),
                                    uNode,
                                    fNode,
                                    "ACCESSED_FILE"
                            ));
                        }
                    }
                }

                // URL Resource Entity Node
                if (evt.getUrl() != null && !evt.getUrl().isBlank()) {
                    String urlKey = evt.getUrl().toLowerCase();
                    GraphNode urlNode = urlNodes.computeIfAbsent(urlKey, k -> {
                        GraphNode n = new GraphNode(
                                "url_" + Math.abs(urlKey.hashCode()),
                                evt.getUrl(),
                                NodeType.URL,
                                "Web Endpoint"
                        );
                        n.addProperty("URL", evt.getUrl());
                        n.addProperty("Entity Type", "Requested URL");
                        graph.addNode(n);
                        return n;
                    });

                    // EVENT -> URL
                    graph.addEdge(new GraphEdge(
                            "e_url_" + evt.getId(),
                            eventNode,
                            urlNode,
                            "REQUESTED_URL"
                    ));

                    // USER -> URL
                    if (evt.getUser() != null && !evt.getUser().isBlank()) {
                        GraphNode uNode = userNodes.get(evt.getUser().toLowerCase());
                        if (uNode != null) {
                            graph.addEdge(new GraphEdge(
                                    "e_uurl_" + uNode.getId() + "_" + urlNode.getId(),
                                    uNode,
                                    urlNode,
                                    "VISITED_URL"
                            ));
                        }
                    }
                }
            }
        }

        // 3. Findings correlation
        if (findingsList != null) {
            for (FindingModel finding : findingsList) {
                if (finding.getEventId() != null) {
                    GraphNode evNode = graph.getNodeById("evt_" + finding.getEventId());
                    if (evNode != null) {
                        evNode.addProperty("AI Finding", finding.getReason());
                        evNode.addProperty("Severity", finding.getSeverity().getDisplayName());
                        evNode.addProperty("Anomaly Score", String.format("%.2f", finding.getScore()));
                        evNode.addProperty("Detection Method", finding.getDetectionMethod());
                        // Mark high severity event nodes with a special indicator
                        evNode.setSubtitle("ANOMALY: " + finding.getSeverity().getDisplayName());
                    }
                }
            }
        }

        return graph;
    }
}
