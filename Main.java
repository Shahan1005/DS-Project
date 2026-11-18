package FDS_Project;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String inputFilePath = args[0];
        File file = new File(inputFilePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("Error: File not found at path: " + inputFilePath);
            return;
        }

        int maxHops = 5;
        long timeWindowSeconds = 86400L; // 24 hours
        String jsonOutputPath = "output/fraud_alerts.json";
        String csvOutputPath = "output/fraud_summary.csv";

        if (args.length >= 2) {
            try {
                maxHops = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid max hops provided. Falling back to default: " + maxHops);
            }
        }

        if (args.length >= 3) {
            try {
                timeWindowSeconds = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid time window provided. Falling back to default: " + timeWindowSeconds + "s");
            }
        }

        System.out.println("==================================================");
        System.out.println("      FINANCIAL FRAUD RING DETECTION ENGINE       ");
        System.out.println("==================================================");
        System.out.println("Input Dataset       : " + inputFilePath);
        System.out.println("Max Path Hops       : " + maxHops);
        System.out.println("Time Window Limit   : " + timeWindowSeconds + " seconds (" + (timeWindowSeconds / 3600.0) + " hours)");
        System.out.println("--------------------------------------------------");

        long pipelineStartTime = System.nanoTime();

        // 1. Ingest records
        System.out.print("[1/6] Ingesting CSV records... ");
        long readStart = System.nanoTime();
        List<Transaction> transactions = CSVReader.readTransactions(inputFilePath);
        long readEnd = System.nanoTime();
        System.out.printf("%d records loaded (%.2f ms)%n", transactions.size(), (readEnd - readStart) / 1_000_000.0);

        if (transactions.isEmpty()) {
            System.out.println("No valid transaction records available to process. Exiting.");
            return;
        }

        // 2. Sort chronologically
        System.out.print("[2/6] Sorting transactions chronologically... ");
        long sortStart = System.nanoTime();
        TransactionSorter.sortByTimestamp(transactions);
        long sortEnd = System.nanoTime();
        System.out.printf("done (%.2f ms)%n", (sortEnd - sortStart) / 1_000_000.0);

        // 3. Map accounts and assemble graph
        System.out.print("[3/6] Mapping accounts and assembling graph... ");
        long graphStart = System.nanoTime();
        AccountMapper mapper = new AccountMapper();
        FraudGraph graph = new FraudGraph();

        for (Transaction tx : transactions) {
            int srcId = mapper.getOrCreateId(tx.getSenderStr());
            int targetId = mapper.getOrCreateId(tx.getReceiverStr());
            graph.addEdge(srcId, targetId, tx.getAmount(), tx.getTimestamp(), tx.getTransactionId());
        }
        long graphEnd = System.nanoTime();
        System.out.printf("done (%.2f ms)%n", (graphEnd - graphStart) / 1_000_000.0);

        // 4. Run Depth-Bounded DFS Detection
        System.out.print("[4/6] Executing DFS cycle detection with temporal pruning... ");
        long detectStart = System.nanoTime();
        FraudDetector detector = new FraudDetector(graph, mapper);
        List<FraudRing> detectedRings = detector.detectRings(maxHops, timeWindowSeconds);
        long detectEnd = System.nanoTime();
        System.out.printf("done (%.2f ms)%n", (detectEnd - detectStart) / 1_000_000.0);

        // 5. Identify Ringleaders / Kingpins
        System.out.print("[5/6] Analyzing syndicate centrality and kingpins... ");
        long analyzeStart = System.nanoTime();
        KingpinAnalyzer analyzer = new KingpinAnalyzer(graph, mapper);
        analyzer.identifyKingpins(detectedRings);
        long analyzeEnd = System.nanoTime();
        System.out.printf("done (%.2f ms)%n", (analyzeEnd - analyzeStart) / 1_000_000.0);

        long pipelineEndTime = System.nanoTime();
        long totalPipelineMs = Math.round((pipelineEndTime - pipelineStartTime) / 1_000_000.0);

        // 6. Export Reports for Web Dashboard & Audit
        System.out.println("[6/6] Exporting forensic reports (JSON & CSV)...");
        new File("output").mkdirs();
        FraudReportExporter.exportToCSV(csvOutputPath, detectedRings);
        FraudReportExporter.exportToJSON(jsonOutputPath, detectedRings, file.getName(), totalPipelineMs);

        // Calculate JVM Memory Footprint
        Runtime runtime = Runtime.getRuntime();
        double memoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        System.out.printf("JVM Heap Memory Utilized: %.2f MB%n", memoryUsedMb);

        // Print Formatted Console Tables using ForensicTablePrinter
        ForensicTablePrinter.printSummaryMetrics(
                transactions.size(),
                mapper.size(),
                graph.getEdgeCount(),
                detectedRings.size(),
                totalPipelineMs
        );
        ForensicTablePrinter.printDetectedRingsTable(detectedRings, graph);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java FDS_Project.Main <path_to_csv> [maxHops] [timeWindowSeconds]");
    }
}
