package FDS_Project;

public class Transaction {
    private final long transactionId;
    private final String senderStr;
    private final String receiverStr;
    private final double amount;
    private final long timestamp;

    public Transaction(long transactionId, String senderStr, String receiverStr, double amount, long timestamp) {
        this.transactionId = transactionId;
        this.senderStr = senderStr;
        this.receiverStr = receiverStr;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public long getTransactionId() { return transactionId; }
    public String getSenderStr() { return senderStr; }
    public String getReceiverStr() { return receiverStr; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + transactionId +
                ", from='" + senderStr + '\'' +
                ", to='" + receiverStr + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}