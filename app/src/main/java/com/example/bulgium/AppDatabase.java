package com.example.bulgium;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Transaction.class, Reminder.class, SavingsRecord.class, SavingsGoal.class}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract TransactionDao transactionDao();
    public abstract ReminderDao reminderDao();
    public abstract SavingsDao savingsDao();
    public abstract SavingsGoalDao savingsGoalDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "bulgium_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
