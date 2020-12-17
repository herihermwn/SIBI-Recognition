package org.adarmawan117.recognition.sibi.view.category;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.adarmawan117.recognition.sibi.R;
import org.adarmawan117.recognition.sibi.env.CategoryData;

import java.util.ArrayList;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.ListViewHolder> {
    private ArrayList<CategoryData> listCategory;

    public void setListCategory(ArrayList<CategoryData> listCategory) {
        this.listCategory = listCategory;
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        CategoryData data = listCategory.get(position);

        holder.title.setText(data.getName());
        holder.total.setText("Total : " + data.getTotal() + " Gesture");
        holder.number.setText((position + 1) + ".");
    }

    @Override
    public int getItemCount() {
        return listCategory.size();
    }

    class ListViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView total;
        TextView number;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            number = itemView.findViewById(R.id.number);
            total = itemView.findViewById(R.id.total);
        }
    }
}
