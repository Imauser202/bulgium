package com.example.bulgium;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "savings_records")
public class SavingsRecord {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private double amount;
    private long timestamp;
    private String goalName;

    public SavingsRecord(double amount, long timestamp, String goalName) {
        this.amount = amount;
        this.timestamp = timestamp;
        this.goalName = goalName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }
}
