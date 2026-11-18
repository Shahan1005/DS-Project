package FDS_Project;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BenchmarkRunner {

    private static class BenchmarkResult {
        int transactionCount;
        int uniqueAccounts;
        int edges;
        int ringsDetected;
        long ingestionTimeMs;
        long sortingTimeMs;
        long detectionTimeMs;
        long totalPipelineTimeMs;
        double memoryUsedMb;

        BenchmarkResult(int transactionCount, int uniqueAccounts, int edges, int ringsDetected,
                        long ingestionTimeMs, long sortingTimeMs, long graphBuildTimeMs,
                        long detectionTimeMs, long totalPipelineTimeMs, double memoryUsedMb) {
            this.transactionCount = transactionCount;
            this.uniqueAccounts = uniqueAccounts;
            this.edges = edges;
            this.ringsDetected = ringsDetected;
            this.ingestionTimeMs = ingestionTimeMs;
            this.sortingTimeMs = sortingTimeMs;
            this.detectionTimeMs = detectionTimeMs;
            this.totalPipelineTimeMs = totalPipelineTimeMs;
            this.memoryUsedMb = memoryUsedMb;
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================================================");
        System.out.println("              FINANCIAL FRAUD RING DETECTOR - BENCHMARK SUITE                            ");
        System.out.println("=========================================================================================");

        int[] datasetSizes = {250, 500, 1000};
        int[] ringCounts = {2, 3, 4};

        int maxHops = 5;
        long timeWindowSeconds = 86400L; // 24 hours

        // 1. Warm-up run
        System.out.print("Running JVM warm-up phase... ");
        String warmupFile = "benchmark_warmup.csv";
        generateBenchmarkCSV(warmupFile, 200, 2);
        executeBenchmarkRun(warmupFile, maxHops, timeWindowSeconds);
        deleteFile(warmupFile);
        System.out.println("done.\n");

        BenchmarkResult[] results = new BenchmarkResult[datasetSizes.length];

        // 2. Benchmarks across 250, 500, and 1,000 records
        for (int i = 0; i < datasetSizes.length; i++) {
            int txCount = datasetSizes[i];
            int rings = ringCounts[i];
            String tempFileName = "benchmark_temp_" + txCount + ".csv";

            System.out.println(">>> Benchmarking target: " + txCount + " records (" + rings + " injected rings)...");
            generateBenchmarkCSV(tempFileName, txCount, rings);

            results[i] = executeBenchmarkRun(tempFileName, maxHops, timeWindowSeconds);
            deleteFile(tempFileName);

            System.out.println("Completed run for " + txCount + " records.\n");
        }

        // 3. Output results
        printPerformanceSummaryTable(results);
    }

    private static BenchmarkResult executeBenchmarkRun(String filePath, int maxHops, long timeWindow) {
        System.gc();
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        long startPipeline = System.currentTimeMillis();

        // 1. Ingestion
        long startRead = System.currentTimeMillis();
        List<Transaction> transactions = CSVReader.readTransactions(filePath);
        long endRead = System.currentTimeMillis();

        // 2. Sorting
        long startSort = System.currentTimeMillis();
        TransactionSorter.sortByTimestamp(transactions);
        long endSort = System.currentTimeMillis();

        // 3. Graph Assembly
        long startGraph = System.currentTimeMillis();
        AccountMapper mapper = new AccountMapper();
        FraudGraph graph = new FraudGraph();

        for (Transaction tx : transactions) {
            int srcId = mapper.getOrCreateId(tx.getSenderStr());
            int targetId = mapper.getOrCreateId(tx.getReceiverStr());
            graph.addEdge(srcId, targetId, tx.getAmount(), tx.getTimestamp(), tx.getTransactionId());
        }
        long endGraph = System.currentTimeMillis();

        // 4. Detection Engine
        long startDetect = System.currentTimeMillis();
        FraudDetector detector = new FraudDetector(graph, mapper);
        List<FraudRing> rings = detector.detectRings(maxHops, timeWindow);
        long endDetect = System.currentTimeMillis();

        // 5. Kingpin Analysis
        KingpinAnalyzer analyzer = new KingpinAnalyzer(graph, mapper);
        analyzer.identifyKingpins(rings);

        long endPipeline = System.currentTimeMillis();

        long postMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsedMb = Math.max(0, (postMemory - initialMemory)) / (1024.0 * 1024.0);

        return new BenchmarkResult(
                transactions.size(),
                mapper.size(),
                graph.getEdgeCount(),
                rings.size(),
                (endRead - startRead),
                (endSort - startSort),
                (endGraph - startGraph),
                (endDetect - startDetect),
                (endPipeline - startPipeline),
                memoryUsedMb
        );
    }

    private static void generateBenchmarkCSV(String outputPath, int totalCount, int ringCount) {
        Random rand = new Random(42);
        List<String> rows = new ArrayList<>(totalCount);
        long baseTime = 1700000000L;
        long txId = 50000L;

        // Injected Rings
        for (int r = 0; r < ringCount; r++) {
            int ringSize = 3 + (r % 3); // cycles between 3, 4, and 5 hops
            List<String> ringAccounts = new ArrayList<>();
            for (int i = 0; i < ringSize; i++) {
                ringAccounts.add("BENCH_FRAUD_" + r + "_NODE_" + i);
            }

            double baseAmount = 7000.0 + (rand.nextDouble() * 3000.0);
            long ringStart = baseTime + (r * 10000L);

            for (int i = 0; i < ringSize; i++) {
                String src = ringAccounts.get(i);
                String dst = ringAccounts.get((i + 1) % ringSize);
                double amt = baseAmount * (1.0 - (i * 0.015));
                long time = ringStart + (i * 900L);
                rows.add(String.format("%d,%s,%s,%.2f,%d", txId++, src, dst, amt, time));
            }
        }

        // Background normal transfers
        int remaining = totalCount - rows.size();
        for (int i = 0; i < remaining; i++) {
            int srcIdx = rand.nextInt(150);
            int dstIdx = rand.nextInt(150);
            while (dstIdx == srcIdx) {
                dstIdx = rand.nextInt(150);
            }
            String src = "BENCH_USER_" + srcIdx;
            String dst = "BENCH_USER_" + dstIdx;
            double amt = 20.0 + (rand.nextDouble() * 1200.0);
            long time = baseTime + rand.nextInt(86400);
            rows.add(String.format("%d,%s,%s,%.2f,%d", txId++, src, dst, amt, time));
        }

        Collections.shuffle(rows, rand);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("transaction_id,sender_id,receiver_id,amount,timestamp\n");
            for (String row : rows) {
                writer.write(row);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error generating benchmark file: " + e.getMessage());
        }
    }

    private static void deleteFile(String path) {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
    }

    private static void printPerformanceSummaryTable(BenchmarkResult[] results) {
        System.out.println("=========================================================================================");
        System.out.println("                               BENCHMARK PERFORMANCE SUMMARY                             ");
        System.out.println("=========================================================================================");
        System.out.printf("| %-8s | %-8s | %-8s | %-6s | %-9s | %-9s | %-10s | %-9s | %-10s |\n",
                "Records", "Nodes", "Edges", "Rings", "Read (ms)", "Sort (ms)", "Detect(ms)", "Total(ms)", "Memory(MB)");
        System.out.println("+----------+----------+----------+--------+-----------+-----------+------------+-----------+------------+");

        for (BenchmarkResult r : results) {
            System.out.printf("| %-8d | %-8d | %-8d | %-6d | %-9d | %-9d | %-10d | %-9d | %-10.2f |\n",
                    r.transactionCount,
                    r.uniqueAccounts,
                    r.edges,
                    r.ringsDetected,
                    r.ingestionTimeMs,
                    r.sortingTimeMs,
                    r.detectionTimeMs,
                    r.totalPipelineTimeMs,
                    r.memoryUsedMb);
        }
        System.out.println("=========================================================================================");
    }
}