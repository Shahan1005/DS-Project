package FDS_Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FraudGraph {
    private final List<List<Edge>> adjList;
    private int edgeCount;

    public FraudGraph() {
        this.adjList = new ArrayList<>();
        this.edgeCount = 0;
    }

    public FraudGraph(int initialCapacity) {
        this.adjList = new ArrayList<>(initialCapacity);
        for (int i = 0; i < initialCapacity; i++) {
            this.adjList.add(new ArrayList<>());
        }
        this.edgeCount = 0;
    }

    private void ensureCapacity(int nodeId) {
        while (adjList.size() <= nodeId) {
            adjList.add(new ArrayList<>());
        }
    }

    public void addEdge(int sourceNode, int targetNode, double amount, long timestamp, long transactionId) {
        if (sourceNode < 0 || targetNode < 0) {
            throw new IllegalArgumentException("Node IDs cannot be negative.");
        }

        int maxNode = Math.max(sourceNode, targetNode);
        ensureCapacity(maxNode);

        Edge edge = new Edge(targetNode, timestamp, amount, transactionId);
        adjList.get(sourceNode).add(edge);
        edgeCount++;
    }

    public List<Edge> getOutgoingEdges(int nodeId) {
        if (nodeId < 0 || nodeId >= adjList.size()) {
            return Collections.emptyList();
        }
        return adjList.get(nodeId);
    }

    public int getNodeCount() {
        return adjList.size();
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int getOutDegree(int nodeId) {
        if (nodeId < 0 || nodeId >= adjList.size()) {
            return 0;
        }
        return adjList.get(nodeId).size();
    }
}