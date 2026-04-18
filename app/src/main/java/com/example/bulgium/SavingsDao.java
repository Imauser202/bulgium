package com.example.bulgium;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SavingsDao {
    @Insert
    void insert(SavingsRecord record);

    @Query("SELECT * FROM savings_records WHERE goalName = :goalName ORDER BY timestamp DESC")
    LiveData<List<SavingsRecord>> getRecordsForGoal(String goalName);

    @Query("DELETE FROM savings_records WHERE goalName = :goalName")
    void deleteRecordsForGoal(String goalName);

    @Query("DELETE FROM savings_records")
    void deleteAll();
}
