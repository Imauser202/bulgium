package com.example.bulgium;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String category;
    private double amount;
    private boolean income;
    private long timestamp;
    private String note;

    public Transaction() {
        // TEST
    }

    @Ignore
    public Transaction(String category, double amount, boolean income, String note, long timestamp) {
        this.category = category;
        this.amount = amount;
        this.income = income;
        this.timestamp = timestamp;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public boolean isIncome() { return income; }
    public void setIncome(boolean income) { this.income = income; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
