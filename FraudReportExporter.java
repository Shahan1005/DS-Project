package FDS_Project;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FraudReportExporter {

    public static void exportToCSV(String outputPath, List<FraudRing> rings) {
        if (rings == null) {
            System.err.println("No rings provided for CSV export.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("ring_id,kingpin_account,risk_score,total_volume,duration_seconds,hop_count,account_chain,flag_reason");
            writer.newLine();

            for (int i = 0; i < rings.size(); i++) {
                FraudRing ring = rings.get(i);
                int ringId = i + 1;
                String kingpin = escapeCsv(ring.getKingpinAccount());
                String riskScore = String.format("%.2f", ring.getRiskScore());
                String volume = String.format("%.2f", ring.getTotalVolume());
                long duration = ring.getDurationSeconds();
                int hopCount = ring.getNodePath().size() - 1;
                String chain = escapeCsv(String.join(" -> ", ring.getAccountPath()));
                String reason = escapeCsv(ring.getKingpinReason());

                String row = String.format("%d,%s,%s,%s,%d,%d,%s,%s",
                        ringId, kingpin, riskScore, volume, duration, hopCount, chain, reason);
                writer.write(row);
                writer.newLine();
            }

            System.out.println("Forensic CSV report exported successfully to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error exporting to CSV: " + e.getMessage());
        }
    }

    public static void exportToJSON(String outputPath, List<FraudRing> rings, String inputDatasetName, long executionTimeMs) {
        if (rings == null) {
            System.err.println("No rings provided for JSON export.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"audit_metadata\": {\n");
            json.append("    \"dataset\": \"").append(escapeJson(inputDatasetName)).append("\",\n");
            json.append("    \"total_rings_detected\": ").append(rings.size()).append(",\n");
            json.append("    \"execution_time_ms\": ").append(executionTimeMs).append("\n");
            json.append("  },\n");
            json.append("  \"flagged_rings\": [\n");

            for (int i = 0; i < rings.size(); i++) {
                FraudRing ring = rings.get(i);
                json.append("    {\n");
                json.append("      \"ring_id\": ").append(i + 1).append(",\n");
                json.append("      \"kingpin_account\": \"").append(escapeJson(ring.getKingpinAccount())).append("\",\n");
                json.append("      \"kingpin_reason\": \"").append(escapeJson(ring.getKingpinReason())).append("\",\n");
                json.append("      \"risk_score\": ").append(String.format("%.2f", ring.getRiskScore())).append(",\n");
                json.append("      \"total_volume\": ").append(String.format("%.2f", ring.getTotalVolume())).append(",\n");
                json.append("      \"duration_seconds\": ").append(ring.getDurationSeconds()).append(",\n");
                json.append("      \"hop_count\": ").append(ring.getNodePath().size() - 1).append(",\n");
                json.append("      \"account_chain\": [\n");

                List<String> path = ring.getAccountPath();
                for (int j = 0; j < path.size(); j++) {
                    json.append("        \"").append(escapeJson(path.get(j))).append("\"");
                    if (j < path.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }

                json.append("      ]\n");
                json.append("    }");
                if (i < rings.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}\n");

            writer.write(json.toString());
            System.out.println("Forensic JSON audit log exported successfully to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error exporting to JSON: " + e.getMessage());
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}