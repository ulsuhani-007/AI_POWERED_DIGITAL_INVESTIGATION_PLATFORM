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

        // Cache for de-duplicating entity nodes (users, devices, IPs, locations)
        Map<String, GraphNode> userNodes = new HashMap<>();
        Map<String, GraphNode> deviceNodes = new HashMap<>();
        Map<String, GraphNode> ipNodes = new HashMap<>();
        Map<String, GraphNode> evidenceNodes = new HashMap<>();
        Map<String, GraphNode> locationNodes = new HashMap<>();

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

                // IP Address Entity Node
                if (evt.getIpAddress() != null && !evt.getIpAddress().isBlank()) {
                    String ipKey = evt.getIpAddress();
                    GraphNode ipNode = ipNodes.computeIfAbsent(ipKey, k -> {
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

                    // Add simulated geo location for external suspicious IPs
                    if (ipKey.startsWith("198.51.")) {
                        GraphNode locNode = locationNodes.computeIfAbsent("loc_tor", k -> {
                            GraphNode l = new GraphNode("loc_tor", "Tor Exit Relay", NodeType.LOCATION, "Anonymized");
                            l.addProperty("Country", "Netherlands (Simulated)");
                            l.addProperty("Threat Level", "HIGH / Tor Node");
                            graph.addNode(l);
                            return l;
                        });
                        graph.addEdge(new GraphEdge("e_ip_loc_" + ipKey, ipNode, locNode, "GEO_RESOLVED"));
                    } else if (ipKey.startsWith("203.0.113.")) {
                        GraphNode locNode = locationNodes.computeIfAbsent("loc_bucharest", k -> {
                            GraphNode l = new GraphNode("loc_bucharest", "Bucharest, RO", NodeType.LOCATION, "WAN Geolocation");
                            l.addProperty("Country", "Romania");
                            l.addProperty("Confidence", "85%");
                            graph.addNode(l);
                            return l;
                        });
                        graph.addEdge(new GraphEdge("e_ip_loc_" + ipKey, ipNode, locNode, "GEO_RESOLVED"));
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
