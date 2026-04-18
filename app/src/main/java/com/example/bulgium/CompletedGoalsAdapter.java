package com.example.bulgium;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CompletedGoalsAdapter extends RecyclerView.Adapter<CompletedGoalsAdapter.ViewHolder> {
    private List<SavingsGoal> goals = new ArrayList<>();
    private String symbol;

    public CompletedGoalsAdapter(String symbol) {
        this.symbol = symbol;
    }

    public void setGoals(List<SavingsGoal> goals) {
        this.goals = goals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavingsGoal goal = goals.get(position);
        holder.tvName.setText(goal.getName());
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        holder.tvAmount.setText(nf.format(goal.getTargetAmount()).replace("₱", symbol));
        holder.tvStatus.setText("Completed");
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAmount, tvStatus;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_date); // Reusing layout IDs for simplicity
            tvStatus = v.findViewById(R.id.tv_time);
            tvAmount = v.findViewById(R.id.tv_amount);
        }
    }
}
