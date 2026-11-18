package FDS_Project;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ForensicTablePrinter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void printSummaryMetrics(int totalTx, int uniqueNodes, int edges, int ringsFound, long pipelineMs) {
        System.out.println("\n+-------------------------------------------------------------------------+");
        System.out.println("|                     PIPELINE AUDIT METRICS SUMMARY                      |");
        System.out.println("+-----------------------------+-------------------------------------------+");
        System.out.printf("| %-27s | %-41d |\n", "Total Records Processed", totalTx);
        System.out.printf("| %-27s | %-41d |\n", "Unique Accounts Mapped", uniqueNodes);
        System.out.printf("| %-27s | %-41d |\n", "Directed Graph Edges", edges);
        System.out.printf("| %-27s | %-41d |\n", "Confirmed Fraud Rings", ringsFound);
        System.out.printf("| %-27s | %-41s |\n", "Total Processing Latency", pipelineMs + " ms");
        System.out.println("+-----------------------------+-------------------------------------------+");
    }

    public static void printDetectedRingsTable(List<FraudRing> rings, FraudGraph graph) {
        if (rings == null || rings.isEmpty()) {
            System.out.println("\n[!] No suspicious circular activity detected.");
            return;
        }

        System.out.println("\n========================================================================================================");
        System.out.println("                                 CONFIRMED FRAUD RING DOSSIER TABLE                                     ");
        System.out.println("========================================================================================================");
        System.out.printf("| %-4s | %-24s | %-6s | %-12s | %-10s | %-30s |\n",
                "ID", "Identified Kingpin", "Hops", "Total Vol", "Duration", "Flag Reason");
        System.out.println("+------+--------------------------+--------+--------------+------------+--------------------------------+");

        for (int i = 0; i < rings.size(); i++) {
            FraudRing ring = rings.get(i);
            int hops = ring.getNodePath().size() - 1;
            String kingpin = truncate(ring.getKingpinAccount(), 24);
            String volume = String.format("$%.2f", ring.getTotalVolume());
            String duration = ring.getDurationSeconds() + "s";
            String reason = truncate(ring.getKingpinReason(), 30);

            System.out.printf("| %-4d | %-24s | %-6d | %-12s | %-10s | %-30s |\n",
                    (i + 1), kingpin, hops, volume, duration, reason);
        }
        System.out.println("+------+--------------------------+--------+--------------+------------+--------------------------------+");

        // Detailed Hop-by-Hop Breakdown for each ring
        for (int i = 0; i < rings.size(); i++) {
            FraudRing ring = rings.get(i);
            printRingHopsDetail(i + 1, ring, graph);
        }
    }

    private static void printRingHopsDetail(int ringIndex, FraudRing ring, FraudGraph graph) {
        System.out.println("\n>>> Forensic Trace for Ring #" + ringIndex + " [Risk Score: " + String.format("%.2f", ring.getRiskScore()) + " / 100.0]");
        System.out.println("+-----+--------------------------+--------------------------+--------------+---------------------+");
        System.out.printf("| %-3s | %-24s | %-24s | %-12s | %-19s |\n", "Hop", "Source Account", "Destination Account", "Amount", "Timestamp (UTC)");
        System.out.println("+-----+--------------------------+--------------------------+--------------+---------------------+");

        List<Integer> nodePath = ring.getNodePath();
        List<String> accountPath = ring.getAccountPath();

        for (int i = 0; i < nodePath.size() - 1; i++) {
            int u = nodePath.get(i);
            int v = nodePath.get(i + 1);
            String srcName = truncate(accountPath.get(i), 24);
            String destName = truncate(accountPath.get(i + 1), 24);

            double amount = 0.0;
            long timestamp = 0L;

            for (Edge edge : graph.getOutgoingEdges(u)) {
                if (edge.getTargetNode() == v) {
                    amount = edge.getAmount();
                    timestamp = edge.getTimestamp();
                    break;
                }
            }

            String dateStr = DATE_FORMAT.format(new Date(timestamp * 1000L));
            System.out.printf("| %-3d | %-24s | %-24s | $%-11.2f | %-19s |\n",
                    (i + 1), srcName, destName, amount, dateStr);
        }
        System.out.println("+-----+--------------------------+--------------------------+--------------+---------------------+");
    }

    private static String truncate(String val, int maxLen) {
        if (val == null) return "";
        if (val.length() <= maxLen) return val;
        return val.substring(0, maxLen - 3) + "...";
    }
}