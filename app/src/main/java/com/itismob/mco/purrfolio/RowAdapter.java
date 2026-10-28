package com.itismob.mco.purrfolio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class RowAdapter extends RecyclerView.Adapter<RowViewHolder> {
    public interface OnRowClick { void onClick(Row row); }
    public interface OnRowLongClick { void onLongClick(Row row); }
    private final ArrayList<Row> data;
    private final OnRowClick click;
    private final OnRowLongClick longClick;

    public RowAdapter(ArrayList<Row> data, OnRowClick click, OnRowLongClick longClick) {
        this.data = data;
        this.click = click;
        this.longClick = longClick;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_row, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        Row row = this.data.get(position);
        holder.bindData(row);
        if (row.header) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setClickable(false);
            return;
        }
        holder.itemView.setOnClickListener(v -> {
            if (click != null) click.onClick(row);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClick == null) return false;
            longClick.onLongClick(row);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }
}


