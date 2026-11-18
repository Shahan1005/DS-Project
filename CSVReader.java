package FDS_Project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

    public static List<Transaction> readTransactions(String filePath) {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }
                if (isHeader) {
                    isHeader = false;
                    if (line.toLowerCase().startsWith("transaction_id") || line.toLowerCase().startsWith("id")) {
                        continue;
                    }
                }
                String[] tokens = line.split(",");
                if (tokens.length < 5) {
                    System.err.println("Skipping malformed row: " + line);
                    continue;
                }
                try {
                    long transactionId = Long.parseLong(tokens[0].trim());
                    String senderStr = tokens[1].trim();
                    String receiverStr = tokens[2].trim();
                    double amount = Double.parseDouble(tokens[3].trim());
                    long timestamp = Long.parseLong(tokens[4].trim());

                    Transaction tx = new Transaction(transactionId, senderStr, receiverStr, amount, timestamp);
                    transactions.add(tx);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping row with invalid number format: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return transactions;
    }
}