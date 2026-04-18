package com.example.bulgium;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private LineChart balanceChart;
    private RecyclerView rvRecentTransactions;
    private TextView tvPulseDaily, tvPulseTopCat, tvPulseHealth;
    private TransactionViewModel viewModel;
    private List<Transaction> currentTransactions = new ArrayList<>();

    private NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private ColorDrawable deleteBackground = new ColorDrawable(Color.parseColor("#E74C3C"));
    private ColorDrawable editBackground = new ColorDrawable(Color.parseColor("#21C262"));
    private Drawable deleteIcon, editIcon;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        balanceChart = view.findViewById(R.id.balance_chart);
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions);
        tvPulseDaily = view.findViewById(R.id.tv_pulse_daily);
        tvPulseTopCat = view.findViewById(R.id.tv_pulse_top_cat);
        tvPulseHealth = view.findViewById(R.id.tv_pulse_health);

        deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
        editIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_edit);

        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        
        setupSwipeActions();

        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            currentTransactions = transactions;
            TransactionAdapter adapter = new TransactionAdapter(transactions, this::showTransactionOptions);
            rvRecentTransactions.setAdapter(adapter);
            setupChart(transactions);
        });

        return view;
    }

    private void triggerVibration() {
        Context context = getContext();
        if (context == null) return;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void showTransactionOptions(Transaction t) {
        Context context = getContext();
        if (context == null) return;
        triggerVibration(); // Haptic feedback on long press
        String[] options = {"Edit Transaction", "Delete Transaction"};
        new MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                .setTitle(t.getCategory() + " - " + nf.format(t.getAmount()))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openEditFragment(t);
                    } else if (which == 1) {
                        confirmDeletion(t);
                    }
                })
                .show();
    }

    private void confirmDeletion(Transaction t) {
        Context context = getContext();
        if (context == null) return;
        new MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                .setTitle("Delete Transaction?")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Delete", (d, w) -> {
                    viewModel.delete(t);
                    showAestheticSnackbar("Deleted " + t.getCategory(), t);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAestheticSnackbar(String message, Transaction t) {
        Context context = getContext();
        if (context == null) return;
        View bottomNav = getActivity() != null ? getActivity().findViewById(R.id.bottom_navigation) : null;
        Snackbar snackbar = Snackbar.make(rvRecentTransactions, message, Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> viewModel.insert(t))
                .setActionTextColor(Color.YELLOW);

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.primary));
        TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);

        if (bottomNav != null) snackbar.setAnchorView(bottomNav);
        snackbar.show();
    }

    private void openEditFragment(Transaction t) {
        AddTransactionFragment fragment = new AddTransactionFragment();
        Bundle b = new Bundle();
        b.putInt("transaction_id", t.getId());
        fragment.setArguments(b);
        
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupSwipeActions() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Transaction t = currentTransactions.get(position);

                triggerVibration(); // Feedback when action is confirmed

                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.delete(t);
                    showAestheticSnackbar("Transaction deleted", t);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    rvRecentTransactions.getAdapter().notifyItemChanged(position);
                    openEditFragment(t);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) { // EDIT (Green)
                    int iconMargin = (itemView.getHeight() - editIcon.getIntrinsicHeight()) / 2;
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = itemView.getLeft() + iconMargin + editIcon.getIntrinsicWidth();
                    editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    editBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                    editBackground.draw(c);
                    editIcon.draw(c);
                } else if (dX < 0) { // DELETE (Red)
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    deleteBackground.draw(c);
                    deleteIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            
            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.7f;
            }
        }).attachToRecyclerView(rvRecentTransactions);
    }

    private void setupChart(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            balanceChart.clear();
            balanceChart.setNoDataText("No data yet");
            balanceChart.setNoDataTextColor(Color.GRAY);
            tvPulseDaily.setText(nf.format(0));
            tvPulseTopCat.setText("None");
            tvPulseHealth.setText("New");
            return;
        }

        List<Entry> entries = new ArrayList<>();
        double currentBalance = 0;
        double totalIncome = 0;
        double totalExpense = 0;
        java.util.Map<String, Double> categoryMap = new java.util.HashMap<>();
        
        int count = 0;
        // Iterate oldest to newest
        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction t = transactions.get(i);
            if (t.isIncome()) {
                totalIncome += t.getAmount();
                currentBalance += t.getAmount();
            } else {
                totalExpense += t.getAmount();
                currentBalance -= t.getAmount();
                categoryMap.put(t.getCategory(), categoryMap.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
            entries.add(new Entry(count, (float) currentBalance));
            count++;
        }

        // Calculate Pulse Data
        double dailyAllowance = (totalIncome - totalExpense) / 30.0;
        tvPulseDaily.setText(nf.format(Math.max(0, dailyAllowance)));

        String topCat = "None";
        double maxExp = 0;
        for (java.util.Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            if (entry.getValue() > maxExp) {
                maxExp = entry.getValue();
                topCat = entry.getKey();
            }
        }
        tvPulseTopCat.setText(topCat);

        if (totalIncome > totalExpense * 1.5) {
            tvPulseHealth.setText("Excellent");
            tvPulseHealth.setTextColor(Color.parseColor("#21C262"));
        } else if (totalIncome >= totalExpense) {
            tvPulseHealth.setText("Stable");
            tvPulseHealth.setTextColor(Color.parseColor("#F1C40F"));
        } else {
            tvPulseHealth.setText("Critical");
            tvPulseHealth.setTextColor(Color.parseColor("#E74C3C"));
        }
        
        LineDataSet ds = new LineDataSet(entries, "Balance");
        
        // Fix: Use Primary color instead of hardcoded WHITE so it's visible in Light Mode
        int chartColor = ContextCompat.getColor(requireContext(), R.color.primary);
        
        ds.setColor(chartColor);
        ds.setLineWidth(3.0f); // Slightly thinner for better performance
        ds.setDrawCircles(false); 
        ds.setDrawValues(false); 
        
        // Infinix/Performance Optimization: Use HORIZONTAL_BEZIER instead of CUBIC_BEZIER
        ds.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER); 
        
        ds.setDrawFilled(true);
        ds.setHighlightEnabled(false); 
        
        // Soft primary color glow fill instead of white
        int alphaColor = Color.argb(40, Color.red(chartColor), Color.green(chartColor), Color.blue(chartColor));
        Drawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, 
                new int[] { alphaColor, Color.TRANSPARENT });
        ds.setFillDrawable(drawable);

        balanceChart.setData(new LineData(ds));
        
        // Disable Hardware Acceleration for the chart to stop flickering on Infinix/Mali GPUs
        balanceChart.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        balanceChart.setExtraOffsets(0f, 10f, 0f, 0f); // Top offset for the line width
        balanceChart.setViewPortOffsets(0f, 40f, 0f, 0f); // More breathing room at top
        
        balanceChart.getXAxis().setEnabled(false);
        balanceChart.getAxisLeft().setEnabled(false);
        balanceChart.getAxisRight().setEnabled(false);
        balanceChart.getLegend().setEnabled(false);
        balanceChart.getDescription().setEnabled(false);
        balanceChart.setTouchEnabled(false); 

        // Ensure the chart scales to its content
        balanceChart.getAxisLeft().setSpaceTop(20f);
        balanceChart.getAxisLeft().setSpaceBottom(20f);

        balanceChart.invalidate();
    }
}
