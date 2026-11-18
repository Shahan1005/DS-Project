package FDS_Project;

import java.util.Comparator;
import java.util.List;

public class TransactionSorter {

    public static void sortByTimestamp(List<Transaction> transactions) {
        if (transactions == null || transactions.size() <= 1) {
            return;
        }
        transactions.sort(Comparator.comparingLong(Transaction::getTimestamp));
    }
}