package FDS_Project;

public class Edge {
    private final int targetNode;
    private final long timestamp;
    private final double amount;
    private final long transactionId;
    public Edge(int targetNode, long timestamp, double amount, long transactionId) {
        this.targetNode = targetNode;
        this.timestamp = timestamp;
        this.amount = amount;
        this.transactionId = transactionId;
    }

    public int getTargetNode() {
        return targetNode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public long getTransactionId() {
        return transactionId;
    }
}