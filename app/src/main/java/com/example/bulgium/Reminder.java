package com.example.bulgium;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String note;
    private long intervalMillis;
    private boolean active;

    public Reminder() {
        // Required empty constructor for Room
    }

    @Ignore
    public Reminder(String note, long intervalMillis, boolean active) {
        this.note = note;
        this.intervalMillis = intervalMillis;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getIntervalMillis() { return intervalMillis; }
    public void setIntervalMillis(long intervalMillis) { this.intervalMillis = intervalMillis; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
