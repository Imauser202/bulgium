package com.example.bulgium;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SavingsGoalDao {
    @Insert
    long insert(SavingsGoal goal);

    @Update
    void update(SavingsGoal goal);

    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, id DESC LIMIT 1")
    LiveData<SavingsGoal> getActiveGoal();

    @Query("SELECT * FROM savings_goals WHERE isCompleted = 1 ORDER BY id DESC")
    LiveData<List<SavingsGoal>> getCompletedGoals();

    @Query("DELETE FROM savings_goals WHERE id = :id")
    void delete(int id);

    @Query("DELETE FROM savings_goals")
    void deleteAll();
}
