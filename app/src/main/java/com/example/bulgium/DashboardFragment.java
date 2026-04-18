package com.example.bulgium;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.*;

public class DashboardFragment extends Fragment {

    private TextView tvBalance, tvTotalIncome, tvTotalExpenses, tvFlowStatus, tvEmptyState;
    private TextView tvBalanceLabel, tvAIInsight, tvFlowAnalysis;
    private com.google.android.material.card.MaterialCardView cardFlowStatus;
    private PieChart pieChart;
    private RecyclerView rvTransactions;
    private TableLayout tableSuggestions;
    private TransactionAdapter adapter;
    private TransactionViewModel viewModel;
    private ScrollView scrollView;

    private TextView tvGoalName, tvGoalPercent, tvGoalStatus;
    private LinearProgressIndicator goalProgressBar;
    private EditText etWishlistNote;
    private Button btnSaveNote;
    private MaterialButton btnChangeStrategy;
    
    private SmartBudgetEngine.BudgetStrategy currentStrategy = SmartBudgetEngine.BudgetStrategy.BALANCED_50_30_20;
    
    private Snackbar currentSnackbar;

    private static final String PREFS_SAVINGS = "savings_prefs";
    private static final String PREFS_WISHLIST = "wishlist_prefs";
    private static final String KEY_WISHLIST_NOTE = "wishlist_note";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        scrollView = view.findViewById(R.id.dashboard_scroll_view);
        pieChart = view.findViewById(R.id.pieChart);
        tvBalance     = view.findViewById(R.id.tv_balance);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpenses = view.findViewById(R.id.tv_total_expenses);
        tvBalanceLabel= view.findViewById(R.id.tv_balance_label);
        tvFlowStatus = view.findViewById(R.id.tv_dashboard_flow_status);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        rvTransactions= view.findViewById(R.id.rv_transactions);
        tableSuggestions = view.findViewById(R.id.table_suggestions);
        tvAIInsight = view.findViewById(R.id.tv_ai_insight);
        tvFlowAnalysis = view.findViewById(R.id.tv_flow_analysis);
        cardFlowStatus = view.findViewById(R.id.card_flow_status);

        tvGoalName = view.findViewById(R.id.tv_dashboard_goal_name);
        tvGoalPercent = view.findViewById(R.id.tv_dashboard_goal_percent);
        tvGoalStatus = view.findViewById(R.id.tv_dashboard_goal_status);
        goalProgressBar = view.findViewById(R.id.dashboard_goal_progress);
        etWishlistNote = view.findViewById(R.id.et_wishlist_note);
        btnSaveNote = view.findViewById(R.id.btn_save_note);
        btnChangeStrategy = view.findViewById(R.id.btn_change_strategy);

        btnChangeStrategy.setOnClickListener(v -> showStrategyPicker());

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
                adapter = new TransactionAdapter(transactions, this::openEditFragment);
                rvTransactions.setAdapter(adapter);
            }
            updateDashboard(transactions);
            updateAIAdvice(transactions);
        });

        SharedPreferences wishlistPrefs = requireContext().getSharedPreferences(PREFS_WISHLIST, Context.MODE_PRIVATE);
        etWishlistNote.setText(wishlistPrefs.getString(KEY_WISHLIST_NOTE, ""));

        btnSaveNote.setOnClickListener(v -> {
            String note = etWishlistNote.getText().toString();
            wishlistPrefs.edit().putString(KEY_WISHLIST_NOTE, note).apply();
            Toast.makeText(getContext(), "Wishlist updated!", Toast.LENGTH_SHORT).show();
        });

        tvBalance.setOnClickListener(v -> {
            dismissSnackbar();
            currentSnackbar = Snackbar.make(view, "This is your Net Balance (Income - Expenses)", Snackbar.LENGTH_INDEFINITE)
                    .setAnchorView(tvBalance)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.primary))
                    .setTextColor(Color.WHITE)
                    .setAction("OK", v1 -> dismissSnackbar());
            currentSnackbar.show();
        });
        
        TooltipCompat.setTooltipText(tvBalance, "Income - Expenses");

        if (scrollView != null) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (Math.abs(scrollY - oldScrollY) > 10) {
                    dismissSnackbar();
                }
            });
        }

        updateSavingsCard();

        return view;
    }

    private void updateAIAdvice(List<Transaction> transactions) {
        SmartBudgetEngine.getAdvice(transactions, advice -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvAIInsight.setText(advice));
            }
        });
    }

    private void dismissSnackbar() {
        if (currentSnackbar != null && currentSnackbar.isShown()) {
            currentSnackbar.dismiss();
        }
    }

    private void updateSavingsCard() {
        Context context = getContext();
        if (context == null) return;
        AppDatabase.getInstance(context).savingsGoalDao().getActiveGoal().observe(getViewLifecycleOwner(), goal -> {
            if (goal != null) {
                tvGoalName.setText(goal.getName());
                int percent = (int) Math.min(100, (goal.getCurrentAmount() / goal.getTargetAmount()) * 100);
                tvGoalPercent.setText(percent + "%");
                goalProgressBar.setProgress(percent);
                String symbol = CurrencyPrefs.getCurrencySymbol(context);
                tvGoalStatus.setText(symbol + String.format(Locale.getDefault(), "%.0f", goal.getCurrentAmount()) + " / " + symbol + String.format(Locale.getDefault(), "%.0f", goal.getTargetAmount()));
            } else {
                tvGoalName.setText("No active goal");
                tvGoalPercent.setText("0%");
                goalProgressBar.setProgress(0);
                tvGoalStatus.setText("Set a goal in Savings");
            }
        });
    }

    private void openEditFragment(Transaction t) {
        AddTransactionFragment fragment = new AddTransactionFragment();
        Bundle b = new Bundle();
        b.putInt("transaction_id", t.getId());
        fragment.setArguments(b);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void updateDashboard(List<Transaction> transactions) {
        Context context = getContext();
        if (context == null) return;

        double income = 0;
        double expenses = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.isIncome()) income += t.getAmount();
            else {
                expenses += t.getAmount();
                double current = expenseByCategory.getOrDefault(t.getCategory(), 0.0);
                expenseByCategory.put(t.getCategory(), current + t.getAmount());
            }
        }

        double balance = income - expenses;
        double totalVolume = income + expenses;
        String symbol = CurrencyPrefs.getCurrencySymbol(context);
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        
        tvBalance.setText(nf.format(balance).replace("₱", symbol));
        tvTotalIncome.setText(nf.format(income).replace("₱", symbol));
        tvTotalExpenses.setText(nf.format(expenses).replace("₱", symbol));

        if (totalVolume > 0) {
            int incomePercent = (int) ((income / totalVolume) * 100);
            int expensePercent = (int) ((expenses / totalVolume) * 100);
            tvFlowAnalysis.setText(String.format(Locale.getDefault(), "Cash In: %d%% | Cash Out: %d%%", incomePercent, expensePercent));
        }

        if (income > expenses) {
            tvFlowStatus.setText("Positive Cash Flow 🎉");
            tvFlowStatus.setTextColor(ContextCompat.getColor(context, R.color.income_green));
            cardFlowStatus.setStrokeColor(ContextCompat.getColor(context, R.color.income_green));
        } else if (expenses > income) {
            tvFlowStatus.setText("Negative Cash Flow ⚠️");
            tvFlowStatus.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
            cardFlowStatus.setStrokeColor(ContextCompat.getColor(context, R.color.expense_red));
        }

        setupPieChart(expenseByCategory, expenses);
        updateSuggestionsTable(balance, transactions);
    }

    private void updateSuggestionsTable(double balance, List<Transaction> transactions) {
        Context context = getContext();
        if (context == null) return;
        tableSuggestions.removeAllViews();
        if (balance <= 0) {
            TextView tv = new TextView(context);
            tv.setText("Add income to see smart suggestions.");
            tv.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
            tableSuggestions.addView(tv);
            return;
        }

        Map<String, Double> suggestions = SmartBudgetEngine.getBudgetSuggestions(balance, transactions, currentStrategy);
        String symbol = CurrencyPrefs.getCurrencySymbol(context);
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        for (Map.Entry<String, Double> entry : suggestions.entrySet()) {
            TableRow row = new TableRow(context);
            row.setPadding(0, 16, 0, 16);
            TextView tvCategory = new TextView(context);
            tvCategory.setText("• " + entry.getKey());
            tvCategory.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
            TextView tvAmount = new TextView(context);
            tvAmount.setText(nf.format(entry.getValue()).replace("₱", symbol));
            tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green));
            tvAmount.setGravity(Gravity.END);
            row.addView(tvCategory);
            row.addView(tvAmount);
            tableSuggestions.addView(row);
            View divider = new View(context);
            divider.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(ContextCompat.getColor(context, R.color.light_gray));
            tableSuggestions.addView(divider);
        }
    }

    private void setupPieChart(Map<String, Double> data, double totalExpenses) {
        Context context = getContext();
        if (context == null) return;
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No expenses yet.");
            pieChart.setNoDataTextColor(ContextCompat.getColor(context, R.color.textSecondary));
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#00BFA5")); 
        colors.add(Color.parseColor("#78909C")); 
        colors.add(Color.parseColor("#546E7A")); 
        colors.add(Color.parseColor("#26A69A")); 
        dataSet.setColors(colors);
        dataSet.setValueTextSize(0f); 
        dataSet.setSliceSpace(0f); 

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT); 
        String symbol = CurrencyPrefs.getCurrencySymbol(context);
        pieChart.setCenterText("All\n" + symbol + String.format(Locale.getDefault(), "%.0f", totalExpenses));
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(ContextCompat.getColor(context, R.color.textSecondary));
        pieChart.setHoleRadius(70f); 
        pieChart.setDrawEntryLabels(false); 

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
        pieChart.animateY(1000);
        pieChart.invalidate(); 
    }

    private void showStrategyPicker() {
        Context context = getContext();
        if (context == null) return;

        SmartBudgetEngine.BudgetStrategy[] strategies = SmartBudgetEngine.BudgetStrategy.values();
        
        // Create custom view for dialog
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(0, 24, 0, 24);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                .setTitle("Select Strategy")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .create();

        for (SmartBudgetEngine.BudgetStrategy strategy : strategies) {
            View itemView = LayoutInflater.from(context).inflate(R.layout.item_strategy, container, false);
            TextView tvName = itemView.findViewById(R.id.tv_strategy_name);
            TextView tvDesc = itemView.findViewById(R.id.tv_strategy_desc);
            
            tvName.setText(strategy.name);
            tvDesc.setText(strategy.description);
            
            // Highlight current strategy
            if (strategy == currentStrategy) {
                ((com.google.android.material.card.MaterialCardView)itemView).setStrokeColor(ContextCompat.getColor(context, R.color.primary));
                ((com.google.android.material.card.MaterialCardView)itemView).setStrokeWidth(4);
            }

            itemView.setOnClickListener(v -> {
                currentStrategy = strategy;
                btnChangeStrategy.setText(strategy.name);
                List<Transaction> transactions = viewModel.getAllTransactions().getValue();
                if (transactions != null) updateDashboard(transactions);
                dialog.dismiss();
            });
            
            container.addView(itemView);
        }

        dialog.show();
    }
}
