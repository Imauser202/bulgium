package com.example.bulgium;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.VH> {

    private final List<Reminder> list;
    private final OnReminderCancelListener listener;

    public interface OnReminderCancelListener {
        void onCancel(Reminder r);
    }

    public ReminderAdapter(List<Reminder> list, OnReminderCancelListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Reminder r = list.get(position);
        holder.tvNote.setText(r.getNote());
        
        String intervalText = "Every " + (r.getIntervalMillis() / (60 * 1000L)) + " mins";
        if (r.getIntervalMillis() >= 60 * 60 * 1000L) {
             intervalText = "Every " + (r.getIntervalMillis() / (60 * 60 * 1000L)) + " hours";
        }
        holder.tvInterval.setText(intervalText);

        holder.btnCancel.setOnClickListener(v -> listener.onCancel(r));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNote, tvInterval;
        ImageButton btnCancel;
        VH(@NonNull View itemView) {
            super(itemView);
            tvNote = itemView.findViewById(R.id.tv_reminder_note);
            tvInterval = itemView.findViewById(R.id.tv_reminder_interval);
            btnCancel = itemView.findViewById(R.id.btn_cancel_reminder);
        }
    }
}
