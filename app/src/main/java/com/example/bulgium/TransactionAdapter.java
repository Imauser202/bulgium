package com.example.bulgium;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
    private final List<Transaction> list;
    private OnTransactionLongClickListener longClickListener;

    public interface OnTransactionLongClickListener {
        void onTransactionLongClick(Transaction t);
    }

    public TransactionAdapter(List<Transaction> list, OnTransactionLongClickListener longClickListener) {
        this.list = list;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Transaction t = list.get(position);
        holder.tvCategory.setText(t.getCategory());
        
        // 1. Categorical Icons/Emojis
        holder.tvIcon.setText(getCategoryEmoji(t.getCategory()));
        
        if (t.getNote() != null && !t.getNote().trim().isEmpty()) {
            holder.tvNote.setText(t.getNote());
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }
        
        String symbol = CurrencyPrefs.getCurrencySymbol(holder.itemView.getContext());
        holder.tvAmount.setText(symbol + String.format("%.2f", t.getAmount()));
        
        int colorRes = t.isIncome() ? R.color.income_green : R.color.expense_red;
        holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onTransactionLongClick(t);
                return true;
            }
            return false;
        });
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "💰";
        switch (category.toLowerCase()) {
            case "food": return "🍔";
            case "transport": return "🚗";
            case "school": return "📚";
            case "leisure": return "🎮";
            case "work": return "💼";
            case "allowance": return "💸";
            case "scholarship": return "🎓";
            case "others": return "📦";
            default: return "💰";
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvNote, tvIcon;
        VH(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvNote = itemView.findViewById(R.id.tv_note);
            tvIcon = itemView.findViewById(R.id.tv_category_icon);
        }
    }
}
