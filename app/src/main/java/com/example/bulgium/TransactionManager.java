package com.example.bulgium;

import java.util.ArrayList;
import java.util.List;

public class TransactionManager {
    private static TransactionManager instance;
    private final List<Transaction> list = new ArrayList<>();

    private TransactionManager() {}

    public static TransactionManager getInstance() {
        if (instance == null) instance = new TransactionManager();
        return instance;
    }

    public void addTransaction(Transaction t) {
        list.add(0, t); // newest first
    }

    public List<Transaction> getTransactions() {
        return list;
    }
}
