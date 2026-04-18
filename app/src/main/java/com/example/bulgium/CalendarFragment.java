package com.example.bulgium;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private RecyclerView rvCalendarGrid;
    private RecyclerView rvTransactions;
    private TextView tvMonthYear, tvDateLabel;
    private ImageButton btnPrev, btnNext;

    private TransactionViewModel viewModel;
    private List<Transaction> allTransactions = new ArrayList<>();
    private final List<Transaction> filteredTransactions = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private CalendarAdapter calendarAdapter;

    private Calendar currentMonth = Calendar.getInstance();
    private long selectedTimestamp = System.currentTimeMillis();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        rvCalendarGrid = view.findViewById(R.id.rv_calendar_grid);
        rvTransactions = view.findViewById(R.id.rv_calendar_transactions);
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        tvDateLabel = view.findViewById(R.id.tv_date_label);
        btnPrev = view.findViewById(R.id.btn_prev_month);
        btnNext = view.findViewById(R.id.btn_next_month);

        setupRecyclerViews();
        setupListeners();

        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            allTransactions = transactions;
            updateCalendar();
            filterTransactions(selectedTimestamp);
        });

        return view;
    }

    private void setupRecyclerViews() {
        rvCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarAdapter();
        rvCalendarGrid.setAdapter(calendarAdapter);

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(filteredTransactions, t -> {
            AddTransactionFragment fragment = new AddTransactionFragment();
            Bundle b = new Bundle();
            b.putInt("transaction_id", t.getId());
            fragment.setArguments(b);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvTransactions.setAdapter(transactionAdapter);
    }

    private void setupListeners() {
        btnPrev.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateCalendar();
        });
        btnNext.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateCalendar();
        });
    }

    private void updateCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));

        List<Date> days = new ArrayList<>();
        Calendar calendar = (Calendar) currentMonth.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int monthBeginningCell = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        calendar.add(Calendar.DAY_OF_MONTH, -monthBeginningCell);

        while (days.size() < 42) {
            days.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendarAdapter.setDays(days);
    }

    private void filterTransactions(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvDateLabel.setText("Transactions on " + sdf.format(new Date(timestamp)));

        List<Transaction> newList = new ArrayList<>();
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp);

        for (Transaction t : allTransactions) {
            Calendar cal2 = Calendar.getInstance();
            cal2.setTimeInMillis(t.getTimestamp());
            if (isSameDay(cal1, cal2)) {
                newList.add(t);
            }
        }

        filteredTransactions.clear();
        filteredTransactions.addAll(newList);
        transactionAdapter.notifyDataSetChanged();
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private List<Date> days = new ArrayList<>();
        private Set<String> daysWithTransactions = new HashSet<>();

        public void setDays(List<Date> days) {
            this.days = days;
            updateDaysWithTransactions();
            notifyDataSetChanged();
        }

        private void updateDaysWithTransactions() {
            daysWithTransactions.clear();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            for (Transaction t : allTransactions) {
                daysWithTransactions.add(sdf.format(new Date(t.getTimestamp())));
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Date date = days.get(position);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            
            int day = cal.get(Calendar.DAY_OF_MONTH);
            holder.tvDay.setText(String.valueOf(day));

            // Grey out days from other months
            if (cal.get(Calendar.MONTH) != currentMonth.get(Calendar.MONTH)) {
                holder.tvDay.setAlpha(0.3f);
            } else {
                holder.tvDay.setAlpha(1.0f);
            }

            // Highlight selected day
            Calendar selCal = Calendar.getInstance();
            selCal.setTimeInMillis(selectedTimestamp);
            if (isSameDay(cal, selCal)) {
                holder.viewBackground.setVisibility(View.VISIBLE);
                holder.tvDay.setTextColor(Color.WHITE);
            } else {
                holder.viewBackground.setVisibility(View.GONE);
                holder.tvDay.setTextColor(ContextCompat.getColor(getContext(), R.color.textPrimary));
            }

            // Gold dot for transactions
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            if (daysWithTransactions.contains(sdf.format(date))) {
                holder.viewDot.setVisibility(View.VISIBLE);
            } else {
                holder.viewDot.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                selectedTimestamp = date.getTime();
                notifyDataSetChanged();
                filterTransactions(selectedTimestamp);
            });
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDay;
            View viewBackground, viewDot;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDay = itemView.findViewById(R.id.tv_day_number);
                viewBackground = itemView.findViewById(R.id.view_day_background);
                viewDot = itemView.findViewById(R.id.view_has_transaction);
            }
        }
    }
}
