package FDS_Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class FraudDetector {
    private final FraudGraph graph;
    private final AccountMapper mapper;
    private final Set<String> detectedRingSignatures;

    public FraudDetector(FraudGraph graph, AccountMapper mapper) {
        this.graph = graph;
        this.mapper = mapper;
        // Concurrent, thread-safe set backed by ConcurrentHashMap
        this.detectedRingSignatures = ConcurrentHashMap.newKeySet();
    }

    public List<FraudRing> detectRings(int maxDepth, long maxWindowSeconds) {
        int nodeCount = graph.getNodeCount();
        ConcurrentLinkedQueue<FraudRing> threadSafeRingQueue = new ConcurrentLinkedQueue<>();

        // Parallelize across available CPU cores via Java parallel stream
        IntStream.range(0, nodeCount)
                .parallel()
                .forEach(startNode -> {
                    // Thread-local state allocation: isolated per thread/task
                    boolean[] localInStack = new boolean[nodeCount];
                    List<Integer> currentPath = new ArrayList<>();
                    List<Edge> currentEdges = new ArrayList<>();

                    currentPath.add(startNode);
                    localInStack[startNode] = true;

                    dfs(startNode, startNode, maxDepth, maxWindowSeconds, -1L, -1L,
                            currentPath, currentEdges, localInStack, threadSafeRingQueue);

                    localInStack[startNode] = false;
                });

        return new ArrayList<>(threadSafeRingQueue);
    }

    private void dfs(int startNode,
                     int currentNode,
                     int maxDepth,
                     long maxWindowSeconds,
                     long cycleStartTime,
                     long lastTxTime,
                     List<Integer> currentPath,
                     List<Edge> currentEdges,
                     boolean[] localInStack,
                     ConcurrentLinkedQueue<FraudRing> detectedRings) {

        if (currentPath.size() > maxDepth) {
            return;
        }

        List<Edge> outgoingEdges = graph.getOutgoingEdges(currentNode);

        for (Edge edge : outgoingEdges) {
            int neighbor = edge.getTargetNode();
            long edgeTime = edge.getTimestamp();

            // 1. Causality check
            if (lastTxTime != -1L && edgeTime < lastTxTime) {
                continue;
            }

            long newStartTime = (cycleStartTime == -1L) ? edgeTime : cycleStartTime;

            // 2. Time window pruning
            if (edgeTime - newStartTime > maxWindowSeconds) {
                break;
            }

            // 3. Cycle Completion
            if (neighbor == startNode) {
                if (currentPath.size() >= 3) {
                    currentEdges.add(edge);
                    registerFraudRing(currentPath, currentEdges, detectedRings);
                    currentEdges.remove(currentEdges.size() - 1);
                }
                continue;
            }

            // 4. Avoid nested sub-cycles
            if (localInStack[neighbor]) {
                continue;
            }

            // 5. Recurse forward
            localInStack[neighbor] = true;
            currentPath.add(neighbor);
            currentEdges.add(edge);

            dfs(startNode, neighbor, maxDepth, maxWindowSeconds, newStartTime, edgeTime,
                    currentPath, currentEdges, localInStack, detectedRings);

            // 6. Backtrack
            currentEdges.remove(currentEdges.size() - 1);
            currentPath.remove(currentPath.size() - 1);
            localInStack[neighbor] = false;
        }
    }

    private void registerFraudRing(List<Integer> nodePath, List<Edge> edgePath,
                                   ConcurrentLinkedQueue<FraudRing> detectedRings) {
        String signature = generateCanonicalSignature(nodePath);

        // Atomic conditional insert: returns true only if the key was newly added
        if (!detectedRingSignatures.add(signature)) {
            return;
        }

        double totalVolume = 0.0;
        double minAmount = Double.MAX_VALUE;
        double maxAmount = Double.MIN_VALUE;

        for (Edge e : edgePath) {
            double amt = e.getAmount();
            totalVolume += amt;
            if (amt < minAmount) minAmount = amt;
            if (amt > maxAmount) maxAmount = amt;
        }

        long duration = edgePath.get(edgePath.size() - 1).getTimestamp() - edgePath.get(0).getTimestamp();

        double amountConsistencyRatio = (maxAmount > 0) ? (minAmount / maxAmount) : 0.0;
        double durationFactor = Math.max(0.1, 1.0 - ((double) duration / 86400.0));
        double riskScore = Math.min(100.0, (amountConsistencyRatio * 50.0) + (durationFactor * 30.0) + (nodePath.size() * 4.0));

        List<String> accountNames = new ArrayList<>();
        for (int nodeId : nodePath) {
            accountNames.add(mapper.getAccountStr(nodeId));
        }
        accountNames.add(mapper.getAccountStr(nodePath.get(0)));

        List<Integer> closedNodePath = new ArrayList<>(nodePath);
        closedNodePath.add(nodePath.get(0));

        FraudRing ring = new FraudRing(closedNodePath, accountNames, totalVolume, duration, riskScore);
        detectedRings.add(ring);
    }

    private String generateCanonicalSignature(List<Integer> nodePath) {
        int minIndex = 0;
        int minVal = nodePath.get(0);
        for (int i = 1; i < nodePath.size(); i++) {
            if (nodePath.get(i) < minVal) {
                minVal = nodePath.get(i);
                minIndex = i;
            }
        }

        StringBuilder sb = new StringBuilder();
        int n = nodePath.size();
        for (int i = 0; i < n; i++) {
            sb.append(nodePath.get((minIndex + i) % n)).append("-");
        }
        return sb.toString();
    }
}