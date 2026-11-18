package FDS_Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountMapper {
    private final Map<String, Integer> stringToId;
    private final List<String> idToString;

    public AccountMapper() {
        this.stringToId = new HashMap<>();
        this.idToString = new ArrayList<>();
    }

    public synchronized int getOrCreateId(String accountStr) {
        if (accountStr == null || accountStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Account identifier cannot be null or empty.");
        }

        Integer existingId = stringToId.get(accountStr);
        if (existingId != null) {
            return existingId;
        }

        int newId = idToString.size();
        stringToId.put(accountStr, newId);
        idToString.add(accountStr);
        return newId;
    }

    public int getId(String accountStr) {
        if (accountStr == null) {
            return -1;
        }
        Integer id = stringToId.get(accountStr);
        return (id != null) ? id : -1;
    }

    public String getAccountStr(int id) {
        if (id < 0 || id >= idToString.size()) {
            throw new IndexOutOfBoundsException("Invalid account ID: " + id);
        }
        return idToString.get(id);
    }

    public int size() {
        return idToString.size();
    }

    public boolean contains(String accountStr) {
        return stringToId.containsKey(accountStr);
    }
}