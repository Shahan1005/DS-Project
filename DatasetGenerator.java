package FDS_Project;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DatasetGenerator {

    public static void generateDataset(String outputPath, int totalTransactions, int totalAccounts, int ringCount) {
        Random random = new Random(42);
        List<String> records = new ArrayList<>(totalTransactions);

        long baseTime = 1700000000L;
        long txIdCounter = 100001L;

        // Inject Controlled Fraud Rings
        for (int r = 0; r < ringCount; r++) {
            int ringSize = 3 + random.nextInt(3); // 3 to 5 hops
            List<String> ringAccounts = new ArrayList<>();
            for (int i = 0; i < ringSize; i++) {
                ringAccounts.add("FRAUD_RING_" + r + "_ACC_" + i);
            }

            double baseAmount = 8000.0 + (random.nextDouble() * 4000.0);
            long ringStartTime = baseTime + (r * 15000L) + random.nextInt(3600);
            long stepInterval = 600L + random.nextInt(1200);

            for (int i = 0; i < ringSize; i++) {
                String sender = ringAccounts.get(i);
                String receiver = ringAccounts.get((i + 1) % ringSize);
                double amount = baseAmount * (1.0 - (i * 0.01));
                long timestamp = ringStartTime + (i * stepInterval);

                records.add(String.format("%d,%s,%s,%.2f,%d", txIdCounter++, sender, receiver, amount, timestamp));
            }
        }

        // Generate Legitimate Background Transactions
        int remainingTransactions = totalTransactions - records.size();
        for (int i = 0; i < remainingTransactions; i++) {
            int senderIdx = random.nextInt(totalAccounts);
            int receiverIdx = random.nextInt(totalAccounts);

            while (receiverIdx == senderIdx) {
                receiverIdx = random.nextInt(totalAccounts);
            }

            String sender = "LEGIT_ACC_" + senderIdx;
            String receiver = "LEGIT_ACC_" + receiverIdx;
            double amount = 10.0 + (random.nextDouble() * 1500.0);
            long timestamp = baseTime + random.nextInt(172800);

            records.add(String.format("%d,%s,%s,%.2f,%d", txIdCounter++, sender, receiver, amount, timestamp));
        }

        Collections.shuffle(records, random);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("transaction_id,sender_id,receiver_id,amount,timestamp\n");
            for (String record : records) {
                writer.write(record);
                writer.newLine();
            }
            System.out.println("Dataset generated successfully at: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error writing dataset: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        generateDataset("test_transactions.csv", 500, 80, 3);
    }
}