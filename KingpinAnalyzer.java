package FDS_Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KingpinAnalyzer {
    private final FraudGraph graph;
    private final AccountMapper mapper;

    public KingpinAnalyzer(FraudGraph graph, AccountMapper mapper) {
        this.graph = graph;
        this.mapper = mapper;
    }

    public void identifyKingpins(List<FraudRing> rings) {
        if (rings == null || rings.isEmpty()) {
            return;
        }

        for (FraudRing ring : rings) {
            analyzeRing(ring);
        }
    }

    private void analyzeRing(FraudRing ring) {
        List<Integer> path = ring.getNodePath();
        // The last node in path is duplicate of first to close the loop; exclude it for candidate evaluation
        int uniqueCount = path.size() - 1;
        if (uniqueCount <= 0) {
            return;
        }

        Map<Integer, Double> candidateScores = new HashMap<>();
        Map<Integer, String> candidateReasons = new HashMap<>();

        // Candidate 1: The Initiator (Earliest outgoing transfer in this ring)
        int initiatorNode = path.get(0);
        candidateScores.put(initiatorNode, candidateScores.getOrDefault(initiatorNode, 0.0) + 40.0);
        candidateReasons.put(initiatorNode, "Initiated the primary transfer sequence");

        // Candidate 2 & 3: Evaluate Max Outbound Volume and Network Out-Degree
        int maxVolumeNode = -1;
        double maxVolume = -1.0;

        for (int i = 0; i < uniqueCount; i++) {
            int u = path.get(i);
            int v = path.get(i + 1);

            // Find the specific edge u -> v
            for (Edge edge : graph.getOutgoingEdges(u)) {
                if (edge.getTargetNode() == v) {
                    if (edge.getAmount() > maxVolume) {
                        maxVolume = edge.getAmount();
                        maxVolumeNode = u;
                    }
                    break;
                }
            }

            // Centrality score: broader network activity
            int outDegree = graph.getOutDegree(u);
            double centralityBonus = Math.min(30.0, outDegree * 5.0);
            candidateScores.put(u, candidateScores.getOrDefault(u, 0.0) + centralityBonus);
        }

        if (maxVolumeNode != -1) {
            candidateScores.put(maxVolumeNode, candidateScores.getOrDefault(maxVolumeNode, 0.0) + 30.0);
        }

        // Pick highest scoring candidate
        int bestCandidate = initiatorNode;
        double bestScore = -1.0;

        for (Map.Entry<Integer, Double> entry : candidateScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCandidate = entry.getKey();
            }
        }

        String reason = candidateReasons.getOrDefault(bestCandidate, "Highest capital injection and out-degree centrality");
        if (bestCandidate == initiatorNode && bestCandidate == maxVolumeNode) {
            reason = "Initiator of ring and injected largest single volume ($" + String.format("%.2f", maxVolume) + ")";
        } else if (bestCandidate == maxVolumeNode) {
            reason = "Injected largest single volume ($" + String.format("%.2f", maxVolume) + ") into the cycle";
        }

        ring.setKingpin(bestCandidate, mapper.getAccountStr(bestCandidate), reason);
    }
}