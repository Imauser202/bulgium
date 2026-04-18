package com.example.bulgium;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class CustomMarkerView extends MarkerView {

    private final TextView tvValue;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvValue = findViewById(R.id.tv_marker_value);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e.getData() instanceof Transaction) {
            Transaction t = (Transaction) e.getData();
            String info = String.format("%s\n₱%,.2f", t.getCategory(), t.getAmount());
            tvValue.setText(info);
        } else {
            tvValue.setText(String.format("₱%,.2f", e.getY()));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
