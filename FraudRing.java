package FDS_Project;

import java.util.List;

public class FraudRing {
    private final List<Integer> nodePath;
    private final List<String> accountPath;
    private final double totalVolume;
    private final long durationSeconds;
    private final double riskScore;

    // Kingpin forensic fields
    private String kingpinAccount;
    private int kingpinNodeId;
    private String kingpinReason;

    public FraudRing(List<Integer> nodePath, List<String> accountPath, double totalVolume, long durationSeconds, double riskScore) {
        this.nodePath = nodePath;
        this.accountPath = accountPath;
        this.totalVolume = totalVolume;
        this.durationSeconds = durationSeconds;
        this.riskScore = riskScore;
        this.kingpinAccount = "UNASSIGNED";
        this.kingpinNodeId = -1;
        this.kingpinReason = "Not analyzed";
    }

    public List<Integer> getNodePath() {
        return nodePath;
    }

    public List<String> getAccountPath() {
        return accountPath;
    }

    public double getTotalVolume() {
        return totalVolume;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getKingpinAccount() {
        return kingpinAccount;
    }

    public int getKingpinNodeId() {
        return kingpinNodeId;
    }

    public String getKingpinReason() {
        return kingpinReason;
    }

    public void setKingpin(int nodeId, String accountStr, String reason) {
        this.kingpinNodeId = nodeId;
        this.kingpinAccount = accountStr;
        this.kingpinReason = reason;
    }
}