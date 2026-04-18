package com.example.bulgium;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bulgium.databinding.FragmentCashFlowBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CashFlowFragment extends Fragment {

    private FragmentCashFlowBinding binding;
    private TransactionViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCashFlowBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), this::calculateFlow);
    }

    private void calculateFlow(List<Transaction> transactions) {
        if (transactions == null) return;

        double income = 0;
        double expenses = 0;
        
        for (Transaction t : transactions) {
            if (t.isIncome()) income += t.getAmount();
            else expenses += t.getAmount();
        }

        double net = income - expenses;
        String symbol = CurrencyPrefs.getCurrencySymbol(requireContext());
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        if (net >= 0) {
            binding.tvNetFlow.setText("+ " + nf.format(net).replace("₱", symbol));
            binding.tvFlowStatus.setText("You are saving more than you spend! 🎉");
            int green = Color.parseColor("#21C262");
            binding.tvFlowStatus.setTextColor(green);
            binding.tvNetFlow.setTextColor(green);
            binding.cardNetFlow.setStrokeColor(green);
        } else {
            binding.tvNetFlow.setText("- " + nf.format(Math.abs(net)).replace("₱", symbol));
            binding.tvFlowStatus.setText("Careful! You are overspending. ⚠️");
            int red = Color.parseColor("#E74C3C");
            binding.tvFlowStatus.setTextColor(red);
            binding.tvNetFlow.setTextColor(red);
            binding.cardNetFlow.setStrokeColor(red);
        }

        // Efficiency (Savings Rate)
        int rate = (income > 0 && net > 0) ? (int) ((net / income) * 100) : 0;
        binding.tvSavingsRate.setText(rate + "%");

        // Avg Daily (Simplified)
        double avg = expenses / 30.0;
        binding.tvAvgDaily.setText(nf.format(avg).replace("₱", symbol));

        setupBarChart(income, expenses);
    }

    private void setupBarChart(double income, double expenses) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) income));
        entries.add(new BarEntry(1, (float) expenses));

        BarDataSet dataSet = new BarDataSet(entries, "Cash Flow");
        dataSet.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.income_green),
                ContextCompat.getColor(requireContext(), R.color.expense_red)
        });
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        binding.barChart.setData(data);
        
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getLegend().setEnabled(false);
        binding.barChart.getAxisRight().setEnabled(false);
        
        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Income", "Expenses"}));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.GRAY);

        binding.barChart.getAxisLeft().setTextColor(Color.GRAY);
        binding.barChart.getAxisLeft().setDrawGridLines(true);
        binding.barChart.getAxisLeft().setGridColor(Color.parseColor("#1AFFFFFF"));

        // Anti-flicker for Infinix/Samsung/Oppo (Disable HW Acceleration for chart)
        binding.barChart.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        binding.barChart.animateY(1000);
        binding.barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
