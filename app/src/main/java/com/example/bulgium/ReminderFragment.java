package com.example.bulgium;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ReminderFragment extends Fragment {

    private TextInputEditText etNote;
    private AutoCompleteTextView spInterval;
    private MaterialButton btnSet;
    private RecyclerView rvReminders;
    private ReminderAdapter adapter;
    private List<Reminder> reminderList = new ArrayList<>();

    private final String[] intervals = {"5 Minutes", "30 Minutes", "1 Hour", "6 Hours", "Daily"};
    private final long[] intervalMillis = {
            5 * 60 * 1000L,
            30 * 60 * 1000L,
            60 * 60 * 1000L,
            6 * 60 * 60 * 1000L,
            24 * 60 * 60 * 1000L
    };

    // Permission launcher for Android 13+
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Notification permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Notifications are disabled. You won't see reminders.", Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);

        etNote = view.findViewById(R.id.et_reminder_note);
        spInterval = view.findViewById(R.id.sp_interval);
        btnSet = view.findViewById(R.id.btn_set_reminder);
        rvReminders = view.findViewById(R.id.rv_reminders);

        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, intervals);
        spInterval.setAdapter(intervalAdapter);

        rvReminders.setLayoutManager(new LinearLayoutManager(getContext()));
        
        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.reminderDao().getAllReminders().observe(getViewLifecycleOwner(), reminders -> {
            reminderList = reminders;
            adapter = new ReminderAdapter(reminders, this::cancelReminder);
            rvReminders.setAdapter(adapter);
        });

        // Request permission on load for Android 13+
        checkNotificationPermission();

        btnSet.setOnClickListener(v -> scheduleReminder());

        return view;
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void scheduleReminder() {
        String note = etNote.getText().toString().trim();
        String selected = spInterval.getText().toString();
        int index = -1;
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i].equals(selected)) {
                index = i;
                break;
            }
        }

        if (index == -1) return;

        long millis = intervalMillis[index];

        // Check for duplicates
        for (Reminder r : reminderList) {
            if (r.getIntervalMillis() == millis) {
                new MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                        .setTitle("Reminder Already Exists")
                        .setMessage("You already have an active reminder for " + selected + ". You can't set another one for the same interval.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
        }

        Reminder reminder = new Reminder(note, millis, true);

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.reminderDao().insert(reminder);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    startAlarm(reminder);
                    Toast.makeText(getContext(), "Reminder set for every " + selected, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void startAlarm(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        intent.putExtra("note", reminder.getNote());
        
        int requestCode = (int) (reminder.getIntervalMillis() / 1000); 
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + reminder.getIntervalMillis();

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP, 
                    triggerTime, 
                    reminder.getIntervalMillis(), 
                    pendingIntent
            );
        }
    }

    private void cancelReminder(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 
                (int) (reminder.getIntervalMillis() / 1000),
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        new Thread(() -> {
            AppDatabase.getInstance(requireContext()).reminderDao().delete(reminder);
        }).start();
        
        Toast.makeText(getContext(), "Reminder cancelled", Toast.LENGTH_SHORT).show();
    }
}
