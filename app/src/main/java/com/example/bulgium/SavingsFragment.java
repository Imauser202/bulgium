package com.example.bulgium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import nl.dionsegijn.konfetti.core.*;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class SavingsFragment extends Fragment {

    private TextView tvTitle, tvSavedAmount, tvRemaining, tvGoalAmount, tvPercent, tvTargetDate;
    private CircularProgressIndicator progressBar;
    private ShapeableImageView ivGoalImage;
    private MaterialButton btnAddSavings, btnViewCompleted, btnCreateFirstGoal;
    private ImageView btnEditGoal, btnBack;
    private KonfettiView konfettiView;

    private View layoutActiveGoal, layoutEmptyState, layoutCompletedView, layoutGoalView, layoutRecordsView;
    private TextView tvTabGoal, tvTabRecords, tvEmptyRecords;
    private View tabIndicatorGoal, tabIndicatorRecords;
    private RecyclerView rvSavingsHistory, rvCompletedGoalsList;
    private SavingsAdapter savingsAdapter;
    private CompletedGoalsAdapter completedGoalsAdapter;
    private MaterialButton btnCreateNewGoalFromCompleted;

    private SavingsGoal currentGoal;
    private String tempImageUri = "";

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Context context = getContext();
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && context != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            context.getContentResolver().takePersistableUriPermission(
                                    selectedImageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                            if (currentGoal != null) {
                                currentGoal.setImageUri(selectedImageUri.toString());
                                updateGoalInDb(currentGoal);
                            } else {
                                tempImageUri = selectedImageUri.toString();
                            }
                        } catch (SecurityException ignored) {}
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_savings, container, false);

        // Bind views
        layoutActiveGoal = v.findViewById(R.id.layout_active_goal);
        layoutEmptyState = v.findViewById(R.id.layout_empty_state);
        layoutCompletedView = v.findViewById(R.id.layout_completed_view);
        btnCreateFirstGoal = v.findViewById(R.id.btn_create_first_goal);
        btnCreateNewGoalFromCompleted = v.findViewById(R.id.btn_create_new_goal_from_completed);

        tvTitle = v.findViewById(R.id.tv_title);
        tvSavedAmount = v.findViewById(R.id.tv_saved_amount);
        tvRemaining = v.findViewById(R.id.tv_remaining);
        tvGoalAmount = v.findViewById(R.id.tv_goal_amount);
        tvPercent = v.findViewById(R.id.tv_percent);
        tvTargetDate = v.findViewById(R.id.tv_target_date);
        progressBar = v.findViewById(R.id.progressBar);
        ivGoalImage = v.findViewById(R.id.iv_goal_image);
        btnAddSavings = v.findViewById(R.id.btnAddSavings);
        btnViewCompleted = v.findViewById(R.id.btn_view_completed);
        btnEditGoal = v.findViewById(R.id.btn_edit_goal);
        btnBack = v.findViewById(R.id.btn_back);
        konfettiView = v.findViewById(R.id.konfettiView);

        layoutGoalView = v.findViewById(R.id.layout_goal_view);
        layoutRecordsView = v.findViewById(R.id.layout_records_view);
        tvTabGoal = v.findViewById(R.id.tv_tab_goal);
        tvTabRecords = v.findViewById(R.id.tv_tab_records);
        tvEmptyRecords = v.findViewById(R.id.tv_empty_records);
        tabIndicatorGoal = v.findViewById(R.id.tab_indicator_goal);
        tabIndicatorRecords = v.findViewById(R.id.tab_indicator_records);
        rvSavingsHistory = v.findViewById(R.id.rvSavingsHistory);
        rvCompletedGoalsList = v.findViewById(R.id.rv_completed_goals_list);

        setupRecyclerView();
        setupTabs();
        observeGoal();
        observeCompletedGoals();

        btnEditGoal.setOnClickListener(x -> showEditGoalDialog(currentGoal));
        btnAddSavings.setOnClickListener(x -> showAddSavingsDialog());
        btnBack.setOnClickListener(x -> getParentFragmentManager().popBackStack());
        ivGoalImage.setOnClickListener(x -> pickImage());
        btnCreateFirstGoal.setOnClickListener(x -> showEditGoalDialog(null));
        btnCreateNewGoalFromCompleted.setOnClickListener(x -> showEditGoalDialog(null));
        btnViewCompleted.setOnClickListener(x -> showHallOfFame());

        return v;
    }

    private void observeCompletedGoals() {
        Context context = getContext();
        if (context == null) return;
        AppDatabase.getInstance(context).savingsGoalDao().getCompletedGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null && completedGoalsAdapter != null) {
                completedGoalsAdapter.setGoals(goals);
            }
        });
    }

    private void showHallOfFame() {
        layoutActiveGoal.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutCompletedView.setVisibility(View.VISIBLE);
    }

    private void observeGoal() {
        Context context = getContext();
        if (context == null) return;
        AppDatabase.getInstance(context).savingsGoalDao().getActiveGoal().observe(getViewLifecycleOwner(), goal -> {
            if (goal == null) {
                layoutActiveGoal.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                layoutCompletedView.setVisibility(View.GONE);
                currentGoal = null;
            } else if (goal.isCompleted()) {
                showHallOfFame();
                currentGoal = goal;
            } else {
                layoutActiveGoal.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
                layoutCompletedView.setVisibility(View.GONE);
                currentGoal = goal;
                updateUI(goal);
                loadSavingsRecords(goal.getName());
            }
        });
    }

    private void setupRecyclerView() {
        rvSavingsHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        savingsAdapter = new SavingsAdapter(CurrencyPrefs.getCurrencySymbol(requireContext()));
        rvSavingsHistory.setAdapter(savingsAdapter);

        rvCompletedGoalsList.setLayoutManager(new LinearLayoutManager(getContext()));
        completedGoalsAdapter = new CompletedGoalsAdapter(CurrencyPrefs.getCurrencySymbol(requireContext()));
        rvCompletedGoalsList.setAdapter(completedGoalsAdapter);
    }

    private void setupTabs() {
        tvTabGoal.setOnClickListener(v -> showGoalTab());
        tvTabRecords.setOnClickListener(v -> showRecordsTab());
    }

    private void showGoalTab() {
        Context context = getContext();
        if (context == null) return;
        layoutGoalView.setVisibility(View.VISIBLE);
        layoutRecordsView.setVisibility(View.GONE);
        tvTabGoal.setTextColor(ContextCompat.getColor(context, R.color.primary));
        tvTabRecords.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
        tabIndicatorGoal.setVisibility(View.VISIBLE);
        tabIndicatorRecords.setVisibility(View.INVISIBLE);
    }

    private void showRecordsTab() {
        Context context = getContext();
        if (context == null) return;
        layoutGoalView.setVisibility(View.GONE);
        layoutRecordsView.setVisibility(View.VISIBLE);
        tvTabGoal.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
        tvTabRecords.setTextColor(ContextCompat.getColor(context, R.color.primary));
        tabIndicatorGoal.setVisibility(View.INVISIBLE);
        tabIndicatorRecords.setVisibility(View.VISIBLE);
    }

    private void loadSavingsRecords(String goalName) {
        Context context = getContext();
        if (context == null) return;
        AppDatabase.getInstance(context).savingsDao().getRecordsForGoal(goalName)
                .observe(getViewLifecycleOwner(), records -> {
                    if (records == null || records.isEmpty()) {
                        tvEmptyRecords.setVisibility(View.VISIBLE);
                        rvSavingsHistory.setVisibility(View.GONE);
                    } else {
                        tvEmptyRecords.setVisibility(View.GONE);
                        rvSavingsHistory.setVisibility(View.VISIBLE);
                        savingsAdapter.setRecords(records);
                    }
                });
    }

    private void updateUI(SavingsGoal goal) {
        Context context = getContext();
        if (context == null) return;
        String symbol = CurrencyPrefs.getCurrencySymbol(context);
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        
        tvTitle.setText(goal.getName().toLowerCase());
        tvSavedAmount.setText(nf.format(goal.getCurrentAmount()).replace("₱", symbol));
        tvGoalAmount.setText(nf.format(goal.getTargetAmount()).replace("₱", symbol));
        
        double remaining = goal.getTargetAmount() - goal.getCurrentAmount();
        tvRemaining.setText(nf.format(Math.max(0, remaining)).replace("₱", symbol));

        int progress = 0;
        if (goal.getTargetAmount() > 0) {
            progress = (int) Math.min(100, (goal.getCurrentAmount() / goal.getTargetAmount()) * 100);
        }
        progressBar.setProgress(progress);
        tvPercent.setText(progress + "%");

        if (!TextUtils.isEmpty(goal.getImageUri())) {
            Glide.with(this)
                    .load(Uri.parse(goal.getImageUri()))
                    .placeholder(R.mipmap.ic_logo)
                    .error(R.mipmap.ic_logo)
                    .into(ivGoalImage);
        } else {
            ivGoalImage.setImageResource(R.mipmap.ic_logo);
        }

        if (goal.getTargetDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvTargetDate.setText("Target: " + sdf.format(new Date(goal.getTargetDate())));
        } else {
            tvTargetDate.setText("No target date set");
        }

        if (goal.isCompleted()) {
            btnAddSavings.setEnabled(false);
            btnAddSavings.setText("Goal Completed!");
            btnAddSavings.setAlpha(0.5f);
        } else {
            btnAddSavings.setEnabled(true);
            btnAddSavings.setText("Add saving");
            btnAddSavings.setAlpha(1.0f);
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void showEditGoalDialog(@Nullable SavingsGoal goal) {
        Context context = getContext();
        if (context == null) return;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_goal, null);
        TextInputEditText etName = view.findViewById(R.id.et_dialog_goal_name);
        TextInputEditText etAmount = view.findViewById(R.id.et_dialog_goal_amount);
        Button btnPickDate = view.findViewById(R.id.btn_dialog_pick_date);

        if (goal != null) {
            etName.setText(goal.getName());
            etAmount.setText(String.valueOf((long)goal.getTargetAmount()));
        }
        
        final long[] tempDate = {goal != null ? goal.getTargetDate() : System.currentTimeMillis()};
        btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Target Date")
                    .setSelection(tempDate[0])
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> tempDate[0] = selection);
            picker.show(getParentFragmentManager(), "TARGET_DATE");
        });

        new MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                .setTitle(goal == null ? "Set Savings Goal" : "Update Savings Goal")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amtStr)) {
                        double target = Double.parseDouble(amtStr);
                        if (goal == null) {
                            SavingsGoal newGoal = new SavingsGoal(name, target, 0, tempDate[0], tempImageUri);
                            insertGoalInDb(newGoal);
                        } else {
                            goal.setName(name);
                            goal.setTargetAmount(target);
                            goal.setTargetDate(tempDate[0]);
                            updateGoalInDb(goal);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddSavingsDialog() {
        Context context = getContext();
        if (context == null || currentGoal == null) return;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_savings, null);
        TextInputEditText etAmount = view.findViewById(R.id.et_dialog_add_amount);

        new MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                .setTitle("Add Savings")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String amtStr = etAmount.getText().toString().trim();
                    if (!TextUtils.isEmpty(amtStr)) {
                        double added = Double.parseDouble(amtStr);
                        currentGoal.setCurrentAmount(currentGoal.getCurrentAmount() + added);
                        
                        SavingsRecord record = new SavingsRecord(added, System.currentTimeMillis(), currentGoal.getName());
                        AppDatabase.getInstance(context).getQueryExecutor().execute(() -> {
                            AppDatabase.getInstance(context).savingsDao().insert(record);
                            if (currentGoal.getCurrentAmount() >= currentGoal.getTargetAmount()) {
                                currentGoal.setCompleted(true);
                                updateGoalInDb(currentGoal);
                                getActivity().runOnUiThread(this::showGoalReachedDialog);
                            } else {
                                updateGoalInDb(currentGoal);
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGoalReachedDialog() {
        Context context = getContext();
        if (context == null) return;
        
        // Celebrate!
        EmitterConfig config = new Emitter(100L, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);
        Party party = new PartyFactory(config)
                .spread(360)
                .shapes(Arrays.asList(Shape.Square.INSTANCE, Shape.Circle.INSTANCE))
                .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                .setSpeedBetween(0f, 30f)
                .position(new Position.Relative(0.5, 0.3))
                .build();
        konfettiView.start(party);

        // Show Summary Dialog
        showCompletedSummaryDialog();
    }

    private void showCompletedSummaryDialog() {
        Context context = getContext();
        if (context == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_completed_summary, null);
        RecyclerView rv = view.findViewById(R.id.rv_completed_goals);
        Button btnNewGoal = view.findViewById(R.id.btn_set_new_goal_summary);
        Button btnClose = view.findViewById(R.id.btn_close_summary);

        rv.setLayoutManager(new LinearLayoutManager(context));
        CompletedGoalsAdapter adapter = new CompletedGoalsAdapter(CurrencyPrefs.getCurrencySymbol(context));
        rv.setAdapter(adapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                .setView(view)
                .setCancelable(false)
                .create();

        AppDatabase.getInstance(context).savingsGoalDao().getCompletedGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null) adapter.setGoals(goals);
        });

        btnNewGoal.setOnClickListener(v -> {
            dialog.dismiss();
            showEditGoalDialog(null);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showCompletedGoalsDialog() {
        Context context = getContext();
        if (context == null) return;
        
        AppDatabase.getInstance(context).savingsGoalDao().getCompletedGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals == null || goals.isEmpty()) {
                Toast.makeText(context, "No completed goals yet!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String[] goalNames = new String[goals.size()];
            for (int i = 0; i < goals.size(); i++) {
                goalNames[i] = goals.get(i).getName() + " (₱" + goals.get(i).getTargetAmount() + ")";
            }

            new MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                    .setTitle("Completed Goals")
                    .setItems(goalNames, null)
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    private void insertGoalInDb(SavingsGoal goal) {
        AppDatabase.getInstance(requireContext()).getQueryExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).savingsGoalDao().insert(goal);
        });
    }

    private void updateGoalInDb(SavingsGoal goal) {
        AppDatabase.getInstance(requireContext()).getQueryExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).savingsGoalDao().update(goal);
        });
    }
}
