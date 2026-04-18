package com.example.bulgium;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "bulgium_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private String[] currencies = {"PHP (₱)", "USD ($)", "EUR (€)", "JPY (¥)", "GBP (£)", "KRW (₩)"};
    private String[] symbols = {"₱", "$", "€", "¥", "£", "₩"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 1. Dark Mode Logic
        Switch darkSwitch = view.findViewById(R.id.switch_dark_mode);
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        darkSwitch.setChecked(isDark);

        darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // 2. Currency Logic
        TextView tvCurrencyLabel = view.findViewById(R.id.tv_current_currency);
        String currentSymbol = CurrencyPrefs.getCurrencySymbol(requireContext());
        
        // Find which currency name matches the symbol
        for (int i = 0; i < symbols.length; i++) {
            if (symbols[i].equals(currentSymbol)) {
                tvCurrencyLabel.setText(currencies[i]);
                break;
            }
        }

        view.findViewById(R.id.layout_currency).setOnClickListener(v -> showCurrencyDialog(tvCurrencyLabel));

        // 3. Clear Data Logic
        view.findViewById(R.id.btn_clear_data).setOnClickListener(v -> showClearDataDialog());

        return view;
    }

    private void showCurrencyDialog(TextView tvCurrencyLabel) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                .setTitle("Select Currency")
                .setItems(currencies, (dialog, which) -> {
                    String selectedName = currencies[which];
                    String selectedSymbol = symbols[which];
                    
                    CurrencyPrefs.setCurrencySymbol(requireContext(), selectedSymbol);
                    tvCurrencyLabel.setText(selectedName);
                    
                    Toast.makeText(getContext(), "Currency updated to " + selectedName, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showClearDataDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                .setTitle("Clear All Data?")
                .setMessage("This will permanently delete all your transactions and savings history. This cannot be undone.")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    db.getQueryExecutor().execute(() -> {
                        db.transactionDao().deleteAll();
                        db.savingsDao().deleteAll();
                        db.savingsGoalDao().deleteAll();

                        SharedPreferences savingsPrefs = requireContext().getSharedPreferences("savings_prefs", Context.MODE_PRIVATE);
                        savingsPrefs.edit().clear().apply();

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), "All history cleared successfully", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
