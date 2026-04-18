package com.example.bulgium;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalculatorActivity extends AppCompatActivity {

    private TextView tvDisplay;
    private StringBuilder input = new StringBuilder();
    private double firstValue = Double.NaN;
    private String operator = "";
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private List<String> historyList = new ArrayList<>();
    private SharedPreferences historyPrefs;
    private SmartBudgetEngine.BudgetStrategy currentStrategy = SmartBudgetEngine.BudgetStrategy.BALANCED_50_30_20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_calculator);

        tvDisplay = findViewById(R.id.tv_calc_display);
        rvHistory = findViewById(R.id.rv_calc_history);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvDisplay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                View container = findViewById(R.id.smart_budget_container);
                if (container != null && container.getVisibility() == View.VISIBLE) {
                    updateSmartBudget();
                }
            }
        });

        TabLayout modeTabs = findViewById(R.id.calc_mode_tabs);
        View normalCalcContainer = findViewById(R.id.normal_calc_container);
        View smartBudgetContainer = findViewById(R.id.smart_budget_container);

        modeTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    normalCalcContainer.setVisibility(View.VISIBLE);
                    smartBudgetContainer.setVisibility(View.GONE);
                } else {
                    normalCalcContainer.setVisibility(View.GONE);
                    smartBudgetContainer.setVisibility(View.VISIBLE);
                    updateSmartBudget();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        ChipGroup strategyGroup = findViewById(R.id.strategy_chip_group);
        strategyGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_50_30_20) currentStrategy = SmartBudgetEngine.BudgetStrategy.BALANCED_50_30_20;
            else if (id == R.id.chip_aggressive) currentStrategy = SmartBudgetEngine.BudgetStrategy.AGGRESSIVE_SAVER;
            else if (id == R.id.chip_student) currentStrategy = SmartBudgetEngine.BudgetStrategy.STUDENT_PRIORITY;
            else if (id == R.id.chip_minimalist) currentStrategy = SmartBudgetEngine.BudgetStrategy.MINIMALIST;
            updateSmartBudget();
        });

        historyPrefs = getSharedPreferences("calc_history", Context.MODE_PRIVATE);
        loadHistory();

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(historyAdapter);

        int[] numberButtons = {
                R.id.btn_calc_0, R.id.btn_calc_1, R.id.btn_calc_2, R.id.btn_calc_3,
                R.id.btn_calc_4, R.id.btn_calc_5, R.id.btn_calc_6, R.id.btn_calc_7,
                R.id.btn_calc_8, R.id.btn_calc_9
        };

        View.OnClickListener numberListener = v -> {
            MaterialButton button = (MaterialButton) v;
            input.append(button.getText().toString());
            tvDisplay.setText(input.toString());
        };

        for (int id : numberButtons) {
            findViewById(id).setOnClickListener(numberListener);
        }

        findViewById(R.id.btn_calc_dot).setOnClickListener(v -> {
            if (!input.toString().contains(".")) {
                if (input.length() == 0) input.append("0");
                input.append(".");
                tvDisplay.setText(input.toString());
            }
        });

        findViewById(R.id.btn_calc_ac).setOnClickListener(v -> {
            input.setLength(0);
            firstValue = Double.NaN;
            operator = "";
            tvDisplay.setText("0");
        });

        View.OnClickListener opListener = v -> {
            if (input.length() > 0 || !Double.isNaN(firstValue)) {
                if (input.length() > 0) {
                    calculate(false);
                }
                operator = ((MaterialButton) v).getText().toString();
                input.setLength(0);
            }
        };

        findViewById(R.id.btn_calc_plus).setOnClickListener(opListener);
        findViewById(R.id.btn_calc_minus).setOnClickListener(opListener);
        findViewById(R.id.btn_calc_multiply).setOnClickListener(opListener);
        findViewById(R.id.btn_calc_divide).setOnClickListener(opListener);

        findViewById(R.id.btn_calc_equal).setOnClickListener(v -> {
            calculate(true);
            operator = "";
        });

        findViewById(R.id.btn_calc_percent).setOnClickListener(v -> {
            if (input.length() > 0) {
                double val = Double.parseDouble(input.toString()) / 100;
                input.setLength(0);
                input.append(val);
                tvDisplay.setText(input.toString());
            }
        });

        findViewById(R.id.btn_calc_plus_minus).setOnClickListener(v -> {
            if (input.length() > 0) {
                if (input.charAt(0) == '-') {
                    input.deleteCharAt(0);
                } else {
                    input.insert(0, '-');
                }
                tvDisplay.setText(input.toString());
            }
        });
        
        findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            historyList.clear();
            historyAdapter.notifyDataSetChanged();
            saveHistory();
        });
    }

    private void updateSmartBudget() {
        double currentAmount = 0;
        try {
            currentAmount = Double.parseDouble(tvDisplay.getText().toString());
        } catch (Exception ignored) {}

        if (currentAmount <= 0) {
            // Show prompt to enter income
            ViewGroup resultsLayout = findViewById(R.id.budget_results_layout);
            resultsLayout.removeAllViews();
            TextView prompt = new TextView(this);
            prompt.setText("Enter an income amount on the calculator keypad to see budget suggestions!");
            prompt.setTextColor(getResources().getColor(R.color.textSecondary));
            prompt.setGravity(android.view.Gravity.CENTER);
            resultsLayout.addView(prompt);
            return;
        }

        Map<String, Double> suggestions = SmartBudgetEngine.getBudgetSuggestions(
                currentAmount, 
                TransactionManager.getInstance().getTransactions(),
                currentStrategy
        );

        ViewGroup resultsLayout = findViewById(R.id.budget_results_layout);
        resultsLayout.removeAllViews();

        // Add Header
        TextView header = new TextView(this);
        header.setText(currentStrategy.name + " Plan");
        header.setTextSize(16);
        header.setTextColor(getResources().getColor(R.color.primary));
        header.setPadding(0, 0, 0, 16);
        resultsLayout.addView(header);

        for (Map.Entry<String, Double> entry : suggestions.entrySet()) {
            View itemView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, resultsLayout, false);
            TextView title = itemView.findViewById(android.R.id.text1);
            TextView value = itemView.findViewById(android.R.id.text2);
            
            title.setText(entry.getKey());
            title.setTextColor(getResources().getColor(R.color.textPrimary));
            
            value.setText(String.format(Locale.getDefault(), "₱%,.2f", entry.getValue()));
            value.setTextColor(getResources().getColor(R.color.primary));
            value.setTextSize(18);
            value.setTypeface(null, android.graphics.Typeface.BOLD);
            
            resultsLayout.addView(itemView);
        }
    }

    private void calculate(boolean fromEqual) {
        if (input.length() > 0) {
            double secondValue = Double.parseDouble(input.toString());
            String operation = "";
            if (Double.isNaN(firstValue)) {
                firstValue = secondValue;
            } else {
                operation = formatResult(firstValue) + " " + operator + " " + formatResult(secondValue);
                switch (operator) {
                    case "+": firstValue += secondValue; break;
                    case "−": firstValue -= secondValue; break;
                    case "×": firstValue *= secondValue; break;
                    case "÷": 
                        if (secondValue != 0) firstValue /= secondValue;
                        else {
                            tvDisplay.setText("Error");
                            firstValue = Double.NaN;
                            input.setLength(0);
                            return;
                        }
                        break;
                }
                if (fromEqual) {
                    addToHistory(operation + " = " + formatResult(firstValue));
                }
            }
            tvDisplay.setText(formatResult(firstValue));
            input.setLength(0);
        }
    }

    private void addToHistory(String item) {
        historyList.add(0, item);
        if (historyList.size() > 20) historyList.remove(historyList.size() - 1);
        historyAdapter.notifyItemInserted(0);
        rvHistory.scrollToPosition(0);
        saveHistory();
    }

    private void saveHistory() {
        SharedPreferences.Editor editor = historyPrefs.edit();
        Set<String> set = new HashSet<>(historyList);
        editor.putStringSet("history_set", set);
        editor.apply();
    }

    private void loadHistory() {
        Set<String> set = historyPrefs.getStringSet("history_set", new HashSet<>());
        historyList.clear();
        historyList.addAll(set);
        // Set doesn't maintain order, but this is a simple implementation
    }

    private String formatResult(double d) {
        if (d == (long) d) return String.format(Locale.getDefault(), "%d", (long) d);
        else return String.format(Locale.getDefault(), "%.2f", d);
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<String> history;

        HistoryAdapter(List<String> history) {
            this.history = history;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(history.get(position));
            holder.textView.setTextSize(14);
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
