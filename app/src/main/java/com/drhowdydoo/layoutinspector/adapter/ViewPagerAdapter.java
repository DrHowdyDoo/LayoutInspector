package com.drhowdydoo.layoutinspector.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drhowdydoo.layoutinspector.R;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.SimpleViewHolder> {

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.component_view,parent,false);
        }else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hierarchy_view,parent,false);

        }
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
