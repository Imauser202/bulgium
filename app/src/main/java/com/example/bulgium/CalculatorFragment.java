package com.example.bulgium;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class CalculatorFragment extends DialogFragment {

    private TextView tvDisplay;
    private StringBuilder input = new StringBuilder();
    private double firstValue = Double.NaN;
    private String operator = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_calculator, container, false);

        tvDisplay = view.findViewById(R.id.tv_calc_display);

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
            view.findViewById(id).setOnClickListener(numberListener);
        }

        view.findViewById(R.id.btn_calc_dot).setOnClickListener(v -> {
            if (!input.toString().contains(".")) {
                if (input.length() == 0) input.append("0");
                input.append(".");
                tvDisplay.setText(input.toString());
            }
        });

        view.findViewById(R.id.btn_calc_ac).setOnClickListener(v -> {
            input.setLength(0);
            firstValue = Double.NaN;
            operator = "";
            tvDisplay.setText("0");
        });

        View.OnClickListener opListener = v -> {
            if (input.length() > 0 || !Double.isNaN(firstValue)) {
                if (input.length() > 0) {
                    calculate();
                }
                operator = ((MaterialButton) v).getText().toString();
                input.setLength(0);
            }
        };

        view.findViewById(R.id.btn_calc_plus).setOnClickListener(opListener);
        view.findViewById(R.id.btn_calc_minus).setOnClickListener(opListener);
        view.findViewById(R.id.btn_calc_multiply).setOnClickListener(opListener);
        view.findViewById(R.id.btn_calc_divide).setOnClickListener(opListener);

        view.findViewById(R.id.btn_calc_equal).setOnClickListener(v -> {
            calculate();
            operator = "";
        });

        view.findViewById(R.id.btn_calc_percent).setOnClickListener(v -> {
            if (input.length() > 0) {
                double val = Double.parseDouble(input.toString()) / 100;
                input.setLength(0);
                input.append(val);
                tvDisplay.setText(input.toString());
            }
        });

        view.findViewById(R.id.btn_calc_plus_minus).setOnClickListener(v -> {
            if (input.length() > 0) {
                if (input.charAt(0) == '-') {
                    input.deleteCharAt(0);
                } else {
                    input.insert(0, '-');
                }
                tvDisplay.setText(input.toString());
            }
        });

        return view;
    }

    private void calculate() {
        if (input.length() > 0) {
            double secondValue = Double.parseDouble(input.toString());
            if (Double.isNaN(firstValue)) {
                firstValue = secondValue;
            } else {
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
            }
            tvDisplay.setText(formatResult(firstValue));
            input.setLength(0);
        }
    }

    private String formatResult(double d) {
        if (d == (long) d) return String.format(Locale.getDefault(), "%d", (long) d);
        else return String.format(Locale.getDefault(), "%s", d);
    }
}
