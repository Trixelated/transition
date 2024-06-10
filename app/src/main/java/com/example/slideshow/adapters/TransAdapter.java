package com.example.slideshow.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slideshow.databinding.ThemeItemBinding;
import com.example.slideshow.newslideshow.glstuff.FilterGLProgram.Filters;


public class TransAdapter extends RecyclerView.Adapter<TransAdapter.MyViewHolder> {

    public static int selected_animation = 0;
    private final Context context;
    private final OnItemClicked itemClicked;
    private Filters currentFilter;
    private final Filters[] filters;

  /*  public TransAdapter(Context cn, HashMap<Integer, String> ad, OnItemClicked onItemClicked) {
        context = cn;
        al = ad;
        oic = onItemClicked;

        alkeys = new ArrayList<>(al.keySet());
        Collections.sort(alkeys);

        for (Integer i : alkeys) {
            effectnames.add(al.get(i));
        }

        width = Helper.getwidth(context);
        height = Helper.getHeight(context);

    }*/

    public TransAdapter(Context context, @NonNull Filters[] filters, OnItemClicked itemClicked) {
        this.context = context;
        this.itemClicked = itemClicked;
        this.filters = filters;
        currentFilter = filters[0];
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Filters filter = filters[holder.getAdapterPosition()];

        holder.binding.themeItem.setText(filter.name());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();

                if (pos != RecyclerView.NO_POSITION) {
                    Filters selectedFilter = filters[pos];
                    if (!currentFilter.equals(selectedFilter)) {
                        itemClicked.onClicked(selectedFilter);
                        currentFilter = selectedFilter;
                        notifyDataSetChanged();
                    }
                /* selected_animation = position;
                oic.onClicked(alkeys.get(position), position);*/
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return filters.length;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(ThemeItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),parent,false
        ));
    }

    public void setCurrentFilter(Filters currentFilter) {
        this.currentFilter = currentFilter;
    }

    @Nullable
    public Filters getCurrentFilter() {
        return currentFilter;
    }

    @Deprecated
    public void setSelected(int selected) {
        this.selected_animation = selected;
        notifyDataSetChanged();
    }

    public interface OnItemClicked {
        void onClicked(Filters filters);

    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

    private final   ThemeItemBinding binding;

        MyViewHolder(@NonNull ThemeItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
