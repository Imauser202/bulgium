package com.example.bulgium;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavingsAdapter extends RecyclerView.Adapter<SavingsAdapter.ViewHolder> {
    private List<SavingsRecord> records = new ArrayList<>();
    private String symbol;

    public SavingsAdapter(String symbol) {
        this.symbol = symbol;
    }

    public void setRecords(List<SavingsRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavingsRecord record = records.get(position);
        SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        
        holder.tvDate.setText(sdfDate.format(new Date(record.getTimestamp())));
        holder.tvTime.setText(sdfTime.format(new Date(record.getTimestamp())));
        
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        holder.tvAmount.setText("+ " + nf.format(record.getAmount()).replace("₱", symbol));
    }

    @Override
    public int getItemCount() { return records.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvAmount;
        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }
    }
}
