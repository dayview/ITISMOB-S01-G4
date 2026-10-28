package com.itismob.mco.purrfolio;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class RowViewHolder extends RecyclerView.ViewHolder {
    private final TextView titleTv;
    private final TextView subtitleTv;
    private final TextView trailingTv;

    public RowViewHolder(@NonNull View itemView) {
        super(itemView);
        this.titleTv = itemView.findViewById(R.id.rowTitleTv);
        this.subtitleTv = itemView.findViewById(R.id.rowSubtitleTv);
        this.trailingTv = itemView.findViewById(R.id.rowTrailingTv);
    }

    public void bindData(Row row) {
        Context c = itemView.getContext();
        this.titleTv.setText(row.title);
        this.titleTv.setTextColor(ContextCompat.getColor(c, row.danger ? R.color.purr_danger : R.color.purr_text));

        boolean hasSub = row.subtitle != null && !row.subtitle.isEmpty();
        this.subtitleTv.setVisibility(hasSub ? View.VISIBLE : View.GONE);
        this.subtitleTv.setText(row.subtitle);

        boolean hasTrailing = row.trailing != null && !row.trailing.isEmpty();
        this.trailingTv.setVisibility(hasTrailing ? View.VISIBLE : View.GONE);
        this.trailingTv.setText(row.trailing);
        if (row.header) {
            this.itemView.setBackground(null);
            this.titleTv.setTextSize(18);
        } else {
            this.itemView.setBackgroundResource(R.drawable.bg_field);
            this.titleTv.setTextSize(16);
        }
    }
}

