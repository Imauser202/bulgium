package com.example.bulgium;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int sessionId;
    private String text;
    private boolean isUser;
    private long timestamp;

    public ChatMessageEntity(int sessionId, String text, boolean isUser, long timestamp) {
        this.sessionId = sessionId;
        this.text = text;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
