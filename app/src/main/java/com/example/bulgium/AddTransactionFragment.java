package com.example.bulgium;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddTransactionFragment extends Fragment {

    private EditText etAmount, etNote, etDate;
    private TextInputLayout tilAmount, tilDate, tilCategory, tilNote;
    private MaterialButtonToggleGroup transactionTypeGroup;
    private MaterialAutoCompleteTextView spCategory;
    private MaterialButton btnSave;
    private MaterialButton rbIncome, rbExpense;
    private TransactionViewModel viewModel;
    private TextView tvTitle, tvCurrentBalance;
    private com.google.android.material.chip.Chip chipBalance;

    private long selectedTimestamp = System.currentTimeMillis();
    private int editingTransactionId = -1;

    private final String[] incomeCategories = {
            "Work", "Allowance", "Scholarship", "Other Income"
    };
    private final String[] expenseCategories = {
            "Food", "Transport", "School", "Leisure", "Others"
    };

    private boolean isFormatting = false;

    // Calculator State NEW VER
    private double firstValue = Double.NaN;
    private char currentOperation = ' ';
    private boolean isNewOp = true;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##########");

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_transaction, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        tvTitle = view.findViewById(R.id.tv_title);
        etAmount = view.findViewById(R.id.et_amount);
        etNote = view.findViewById(R.id.et_note);
        etDate = view.findViewById(R.id.et_date);
        tilAmount = view.findViewById(R.id.til_amount);
        tilDate = view.findViewById(R.id.til_date);
        tilCategory = view.findViewById(R.id.til_category);
        tilNote = view.findViewById(R.id.til_note);
        transactionTypeGroup = view.findViewById(R.id.transactionTypeGroup);
        spCategory = view.findViewById(R.id.sp_category);
        btnSave = view.findViewById(R.id.btn_save);
        rbIncome = view.findViewById(R.id.rb_income);
        rbExpense = view.findViewById(R.id.rb_expense);
        chipBalance = view.findViewById(R.id.chip_current_balance);

        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
        
        observeBalance();
        updateDateLabel();
        etDate.setOnClickListener(v -> showDatePicker());

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilAmount.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String original = s.toString().replace(",", "");
                if (!TextUtils.isEmpty(original)) {
                    try {
                        double parsed = Double.parseDouble(original);
                        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
                        nf.setMaximumFractionDigits(2);
                        String formatted = nf.format(parsed);
                        
                        if (!formatted.equals(s.toString())) {
                            etAmount.setText(formatted);
                            etAmount.setSelection(formatted.length());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                isFormatting = false;
            }
        });

        tilAmount.setEndIconOnClickListener(v -> showCalculatorDialog());

        transactionTypeGroup.check(R.id.rb_expense);
        updateCategories(expenseCategories);
        updateButtonColors(false);

        transactionTypeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean isIncome = (checkedId == R.id.rb_income);
                updateCategories(isIncome ? incomeCategories : expenseCategories);
                updateButtonColors(isIncome);
            }
        });

        if (getArguments() != null && getArguments().containsKey("transaction_id")) {
            editingTransactionId = getArguments().getInt("transaction_id");
            tvTitle.setText(R.string.edit_transaction);
            btnSave.setText(R.string.update_transaction);
            observeTransaction();
            viewModel.loadTransactionById(editingTransactionId);
        } else {
            tvTitle.setText(R.string.new_transaction);
        }

        btnSave.setOnClickListener(v -> saveTransaction());

        return view;
    }

    private void observeBalance() {
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            double balance = 0;
            for (Transaction t : transactions) {
                if (t.isIncome()) balance += t.getAmount();
                else balance -= t.getAmount();
            }
            String symbol = CurrencyPrefs.getCurrencySymbol(requireContext());
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            chipBalance.setText("Balance: " + nf.format(balance).replace("₱", symbol));
            
            if (balance < 0) {
                chipBalance.setTextColor(Color.parseColor("#E74C3C"));
                chipBalance.setChipStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#E74C3C")));
            } else {
                chipBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                chipBalance.setChipStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            }
        });
    }

    private void showCalculatorDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_calculator, null);
        TextView tvDisplay = dialogView.findViewById(R.id.tv_calc_display);
        
        firstValue = Double.NaN;
        currentOperation = ' ';
        isNewOp = true;

        View.OnClickListener numberListener = v -> {
            String digit = ((MaterialButton) v).getText().toString();
            if (tvDisplay.getText().toString().equals("0") || isNewOp) {
                tvDisplay.setText(digit);
                isNewOp = false;
            } else {
                tvDisplay.append(digit);
            }
        };

        int[] numIds = {R.id.btn_calc_0, R.id.btn_calc_1, R.id.btn_calc_2, R.id.btn_calc_3, R.id.btn_calc_4, 
                        R.id.btn_calc_5, R.id.btn_calc_6, R.id.btn_calc_7, R.id.btn_calc_8, R.id.btn_calc_9};
        for (int id : numIds) dialogView.findViewById(id).setOnClickListener(numberListener);

        dialogView.findViewById(R.id.btn_calc_dot).setOnClickListener(v -> {
            if (isNewOp) {
                tvDisplay.setText("0.");
                isNewOp = false;
            } else if (!tvDisplay.getText().toString().contains(".")) {
                tvDisplay.append(".");
            }
        });

        View.OnClickListener opListener = v -> {
            String opStr = ((MaterialButton) v).getText().toString();
            char nextOp = ' ';
            if (opStr.equals("+")) nextOp = '+';
            else if (opStr.equals("−")) nextOp = '-';
            else if (opStr.equals("×")) nextOp = '*';
            else if (opStr.equals("÷")) nextOp = '/';

            try {
                double val = Double.parseDouble(tvDisplay.getText().toString());
                if (!Double.isNaN(firstValue)) {
                    calculateResult(val);
                    tvDisplay.setText(decimalFormat.format(firstValue));
                } else {
                    firstValue = val;
                }
                currentOperation = nextOp;
                isNewOp = true;
            } catch (Exception e) {}
        };

        int[] opIds = {R.id.btn_calc_plus, R.id.btn_calc_minus, R.id.btn_calc_multiply, R.id.btn_calc_divide};
        for (int id : opIds) dialogView.findViewById(id).setOnClickListener(opListener);

        dialogView.findViewById(R.id.btn_calc_equal).setOnClickListener(v -> {
            if (!Double.isNaN(firstValue)) {
                try {
                    double secondValue = Double.parseDouble(tvDisplay.getText().toString());
                    calculateResult(secondValue);
                    tvDisplay.setText(decimalFormat.format(firstValue));
                    firstValue = Double.NaN;
                    currentOperation = ' ';
                    isNewOp = true;
                } catch (Exception e) {}
            }
        });

        dialogView.findViewById(R.id.btn_calc_ac).setOnClickListener(v -> {
            tvDisplay.setText("0");
            firstValue = Double.NaN;
            currentOperation = ' ';
            isNewOp = true;
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.calc_title)
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> etAmount.setText(tvDisplay.getText()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void calculateResult(double secondValue) {
        switch (currentOperation) {
            case '+': firstValue += secondValue; break;
            case '-': firstValue -= secondValue; break;
            case '*': firstValue *= secondValue; break;
            case '/': firstValue = (secondValue == 0) ? 0 : firstValue / secondValue; break;
        }
    }

    private void observeTransaction() {
        viewModel.getSelectedTransaction().observe(getViewLifecycleOwner(), transaction -> {
            if (transaction != null && transaction.getId() == editingTransactionId) {
                etAmount.setText(String.valueOf(transaction.getAmount()));
                etNote.setText(transaction.getNote());
                selectedTimestamp = transaction.getTimestamp();
                updateDateLabel();
                
                if (transaction.isIncome()) {
                    transactionTypeGroup.check(R.id.rb_income);
                    updateCategories(incomeCategories);
                } else {
                    transactionTypeGroup.check(R.id.rb_expense);
                    updateCategories(expenseCategories);
                }
                spCategory.setText(transaction.getCategory(), false);
            }
        });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date)
                .setSelection(selectedTimestamp)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedTimestamp = selection;
            updateDateLabel();
        });
        picker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        etDate.setText(sdf.format(new Date(selectedTimestamp)));
    }

    private void updateCategories(String[] categories) {
        spCategory.setSimpleItems(categories);
    }

    private void updateButtonColors(boolean isIncome) {
        int green = ContextCompat.getColor(requireContext(), R.color.income_green);
        int red = ContextCompat.getColor(requireContext(), R.color.expense_red);
        int grey = Color.parseColor("#757575");
        if (isIncome) {
            rbIncome.setTextColor(green);
            rbIncome.setStrokeColor(android.content.res.ColorStateList.valueOf(green));
            rbExpense.setTextColor(grey);
            rbExpense.setStrokeColor(android.content.res.ColorStateList.valueOf(grey));
        } else {
            rbIncome.setTextColor(grey);
            rbIncome.setStrokeColor(android.content.res.ColorStateList.valueOf(grey));
            rbExpense.setTextColor(red);
            rbExpense.setStrokeColor(android.content.res.ColorStateList.valueOf(red));
        }
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().replace(",", "");
        String category = spCategory.getText().toString();
        String note = etNote.getText().toString();
        boolean isIncome = transactionTypeGroup.getCheckedButtonId() == R.id.rb_income;

        if (TextUtils.isEmpty(amountStr)) {
            tilAmount.setError(getString(R.string.error_amount_required));
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            Transaction transaction = new Transaction(category, amount, isIncome, note, selectedTimestamp);
            
            if (editingTransactionId != -1) {
                transaction.setId(editingTransactionId);
                viewModel.update(transaction);
                Toast.makeText(getContext(), R.string.transaction_updated, Toast.LENGTH_SHORT).show();
            } else {
                viewModel.insert(transaction);
                Toast.makeText(getContext(), "Transaction saved!", Toast.LENGTH_SHORT).show();
            }
            
            requireActivity().getSupportFragmentManager().popBackStack();
        } catch (NumberFormatException e) {
            tilAmount.setError(getString(R.string.error_invalid_amount));
        }
    }
}
