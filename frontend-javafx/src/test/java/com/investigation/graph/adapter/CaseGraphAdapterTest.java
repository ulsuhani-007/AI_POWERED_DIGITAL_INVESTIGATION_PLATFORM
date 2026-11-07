package com.investigation.graph.adapter;

import com.investigation.graph.model.GraphData;
import com.investigation.graph.model.GraphNode;
import com.investigation.graph.model.NodeType;
import com.investigation.model.*;
import com.investigation.model.enums.CasePriority;
import com.investigation.model.enums.CaseStatus;
import com.investigation.model.enums.EvidenceStatus;
import com.investigation.model.enums.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaseGraphAdapterTest {

    @Test
    void testBuildGraphFromModels() {
        CaseGraphAdapter adapter = new CaseGraphAdapter();

        CaseModel cm = new CaseModel("case-1", "CASE-2026-0001", "Test Investigation", "Desc",
                CaseStatus.OPEN, CasePriority.HIGH, "Investigator", Instant.now(), Instant.now(), 1, 2, 1);

        EvidenceModel ev = new EvidenceModel("ev-1", "case-1", "auth.log.csv", "CSV Log", 1024L,
                Instant.now(), EvidenceStatus.ANALYZED, "SIEM", "sha256hash", 2, "Auth log");

        EventModel evt1 = new EventModel("evt-1", "case-1", "ev-1", Instant.now(),
                "auth", "AUTHENTICATION", "jdoe", "192.168.1.50", "DESKTOP-01", true, 0L, "Login");

        EventModel evt2 = new EventModel("evt-2", "case-1", "ev-1", Instant.now(),
                "firewall", "DATA_EXFILTRATION", "jdoe", "198.51.100.42", "DESKTOP-01", true, 5000000L, "Egress");

        FindingModel finding = new FindingModel("fnd-1", "evt-2", Instant.now(),
                "DATA_EXFILTRATION", "High egress burst", Severity.CRITICAL, 0.95, "Z-Score", "jdoe", "Isolate");

        GraphData graph = adapter.buildGraph(cm, List.of(ev), List.of(evt1, evt2), List.of(finding));

        assertNotNull(graph);
        assertTrue(graph.getNodeCount() >= 6, "Expected at least 6 nodes (evidence, events, user, device, IPs, location)");
        assertTrue(graph.getEdgeCount() >= 5, "Expected at least 5 connected relationships");

        // Verify node types exist
        boolean hasUser = false;
        boolean hasDevice = false;
        boolean hasIp = false;
        boolean hasEvidence = false;
        boolean hasEvent = false;

        for (GraphNode node : graph.getNodes()) {
            if (node.getType() == NodeType.PERSON) hasUser = true;
            if (node.getType() == NodeType.DEVICE) hasDevice = true;
            if (node.getType() == NodeType.IP_ADDRESS) hasIp = true;
            if (node.getType() == NodeType.EVIDENCE) hasEvidence = true;
            if (node.getType() == NodeType.EVENT) hasEvent = true;
        }

        assertTrue(hasUser, "Graph should contain Person node");
        assertTrue(hasDevice, "Graph should contain Device node");
        assertTrue(hasIp, "Graph should contain IP Address node");
        assertTrue(hasEvidence, "Graph should contain Evidence node");
        assertTrue(hasEvent, "Graph should contain Event node");
    }

    @Test
    void testRealForensicAttributesAndZeroFakeNodes() {
        CaseGraphAdapter adapter = new CaseGraphAdapter();

        CaseModel cm = new CaseModel("case-real", "CASE-2026-REAL", "Real Forensics Case", "Desc",
                CaseStatus.OPEN, CasePriority.HIGH, "investigator", Instant.now(), Instant.now(), 1, 1, 0);

        EventModel evt = new EventModel(
                "evt-real-1", "case-real", "ev-real", Instant.now(),
                "network", "FILE_EXFILTRATION", "attacker", "192.168.1.100", "SERVER-01",
                true, 1048576L, "Exfiltration of secret.docx",
                "198.51.100.99", "secret.docx", "UPLOAD", "https://c2.evil.com/upload", 443
        );

        GraphData graph = adapter.buildGraph(cm, List.of(), List.of(evt), List.of());

        assertNotNull(graph);
        // Verify File and URL and Destination IP nodes exist
        boolean hasFile = false;
        boolean hasUrl = false;
        boolean hasDestIp = false;
        boolean hasFakeLocation = false;

        for (GraphNode node : graph.getNodes()) {
            if (node.getType() == NodeType.FILE) hasFile = true;
            if (node.getType() == NodeType.URL) hasUrl = true;
            if (node.getType() == NodeType.IP_ADDRESS && node.getLabel().equals("198.51.100.99")) hasDestIp = true;
            if (node.getType() == NodeType.LOCATION) hasFakeLocation = true;
        }

        assertTrue(hasFile, "Graph should contain FILE node for secret.docx");
        assertTrue(hasUrl, "Graph should contain URL node for c2 url");
        assertTrue(hasDestIp, "Graph should contain destination IP node");
        assertFalse(hasFakeLocation, "Graph must NOT contain fake/simulated LOCATION nodes");
    }
}
