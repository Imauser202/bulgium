package com.example.bulgium;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bulgium.databinding.FragmentHomeBinding;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TransactionViewModel viewModel;
    private List<Transaction> currentTransactions = new ArrayList<>();

    private NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private ColorDrawable deleteBackground = new ColorDrawable(Color.parseColor("#E74C3C"));
    private ColorDrawable editBackground = new ColorDrawable(Color.parseColor("#21C262"));
    private Drawable deleteIcon, editIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
        editIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_edit);

        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        
        setupSwipeActions();

        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            currentTransactions = transactions;
            TransactionAdapter adapter = new TransactionAdapter(transactions, this::showTransactionOptions);
            binding.rvRecentTransactions.setAdapter(adapter);
            setupChart(transactions);
        });
    }

    public void startLocalTour() {
        if (!isAdded()) return;
        
        SharedPreferences prefs = requireContext().getSharedPreferences("bulgium_prefs", Context.MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);
        boolean isHomeTourDone = prefs.getBoolean("home_tour_done", false);

        // BULLETPROOF LOCK: Never start if main onboarding is active or if it's the very first run
        // (In first run, MainActivity will call this manually later)
        if (MainActivity.isTutorialRunning || isFirstRun || isHomeTourDone) return;

        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(binding.tvPulseDaily, "Daily Allowance", "This calculates exactly how much you can spend today based on your income and expenses.")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(28)
                                .descriptionTextSize(18)
                                .titleTextColor(android.R.color.white)
                                .descriptionTextColor(android.R.color.white)
                                .textTypeface(Typeface.SANS_SERIF)
                                .transparentTarget(true)
                                .cancelable(false)
                                .targetRadius(50),
                        TapTarget.forView(binding.tvPulseHealth, "Savings Health", "Our AI analyzes your spending vs income to tell if your financial status is Stable or Critical.")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(28)
                                .descriptionTextSize(18)
                                .titleTextColor(android.R.color.white)
                                .descriptionTextColor(android.R.color.white)
                                .textTypeface(Typeface.SANS_SERIF)
                                .transparentTarget(true)
                                .cancelable(false)
                                .targetRadius(50),
                        TapTarget.forView(binding.balanceChart, "Wealth Trend", "Watch your net worth grow over time with this smooth interactive graph.")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .titleTextSize(28)
                                .descriptionTextSize(18)
                                .titleTextColor(android.R.color.white)
                                .descriptionTextColor(android.R.color.white)
                                .textTypeface(Typeface.SANS_SERIF)
                                .transparentTarget(true)
                                .cancelable(false)
                                .targetRadius(80)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit().putBoolean("home_tour_done", true).apply();
                    }
                    @Override public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}
                    @Override public void onSequenceCanceled(TapTarget lastTarget) {}
                })
                .start();
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
        Snackbar snackbar = Snackbar.make(binding.rvRecentTransactions, message, Snackbar.LENGTH_LONG)
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

                triggerVibration();

                if (direction == ItemTouchHelper.LEFT) {
                    confirmDeletion(t);
                    binding.rvRecentTransactions.getAdapter().notifyItemChanged(position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    binding.rvRecentTransactions.getAdapter().notifyItemChanged(position);
                    openEditFragment(t);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) { // EDIT
                    int iconMargin = (itemView.getHeight() - editIcon.getIntrinsicHeight()) / 2;
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = itemView.getLeft() + iconMargin + editIcon.getIntrinsicWidth();
                    editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    editBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                    editBackground.draw(c);
                    editIcon.draw(c);
                } else if (dX < 0) { // DELETE
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
        }).attachToRecyclerView(binding.rvRecentTransactions);
    }

    private void setupChart(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            binding.balanceChart.clear();
            binding.tvPulseDaily.setText(nf.format(0));
            binding.tvPulseTopCat.setText("None");
            binding.tvPulseHealth.setText("Stable");
            return;
        }

        binding.balanceChart.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        List<Entry> entries = new ArrayList<>();
        double currentBalance = 0;
        double totalIncome = 0;
        double totalExpense = 0;
        java.util.Map<String, Double> categoryMap = new java.util.HashMap<>();

        for (int i = transactions.size() - 1; i >= 0; i--) {
            Transaction t = transactions.get(i);
            if (t.isIncome()) {
                currentBalance += t.getAmount();
                totalIncome += t.getAmount();
            } else {
                currentBalance -= t.getAmount();
                totalExpense += t.getAmount();
                categoryMap.put(t.getCategory(), categoryMap.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
            entries.add(new Entry(transactions.size() - 1 - i, (float) currentBalance));
        }

        binding.tvPulseDaily.setText(nf.format(Math.max(0, (totalIncome - totalExpense) / 30.0)));
        
        String topCat = "None";
        double maxExp = 0;
        for (java.util.Map.Entry<String, Double> e : categoryMap.entrySet()) {
            if (e.getValue() > maxExp) {
                maxExp = e.getValue();
                topCat = e.getKey();
            }
        }
        binding.tvPulseTopCat.setText(topCat);

        if (totalExpense > totalIncome * 0.8) {
            binding.tvPulseHealth.setText("Critical");
            binding.tvPulseHealth.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
        } else {
            binding.tvPulseHealth.setText("Excellent");
            binding.tvPulseHealth.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        }

        LineDataSet ds = new LineDataSet(entries, "Balance");
        int color = ContextCompat.getColor(requireContext(), R.color.primary);
        ds.setColor(color);
        ds.setLineWidth(3f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        ds.setDrawFilled(true);
        
        int alphaColor = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color));
        ds.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{alphaColor, Color.TRANSPARENT}));

        binding.balanceChart.setData(new LineData(ds));
        binding.balanceChart.getLegend().setEnabled(false);
        binding.balanceChart.getDescription().setEnabled(false);
        binding.balanceChart.getXAxis().setEnabled(false);
        binding.balanceChart.getAxisLeft().setEnabled(false);
        binding.balanceChart.getAxisRight().setEnabled(false);
        binding.balanceChart.invalidate();
    }
}
