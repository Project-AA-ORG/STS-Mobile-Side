package com.example.birdaha.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.birdaha.General.HwModel;
import com.example.birdaha.R;

import java.util.ArrayList;

public class HomeworkAdapter extends RecyclerView.Adapter<HomeworkAdapter.HomeworkViewHolder>{

    Context context;
    ArrayList<HwModel> hwModels;

    public HomeworkAdapter(Context context, ArrayList<HwModel> hwModels){
        this.context = context;
        this.hwModels = hwModels;

    }

    @NonNull
    @Override
    public HomeworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recycler_view_row2,parent,false);



        return new HomeworkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeworkViewHolder holder, int position) {

        holder.textViewTitle.setText(hwModels.get(position).getTitle());

    }

    @Override
    public int getItemCount() {

        return hwModels.size();
    }

    public static class HomeworkViewHolder extends RecyclerView.ViewHolder{


        TextView textViewTitle;
        CardView cardView;
        Context context;



        public HomeworkViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize the textViewTitle variable with the view from the layout with id textView
            textViewTitle = itemView.findViewById(R.id.textView);

            // Initialize the cardView variable with the view from the layout with id cardView
            cardView = itemView.findViewById(R.id.cardView);

            // Set a click listener on the cardView
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create an AlertDialog.Builder object with the context of the itemView
                    AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());

                    // Create a LayoutInflater object from the itemView's context
                    LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

                    // Inflate the overlay_layout.xml file into a View object
                    View overlayView = inflater.inflate(R.layout.homework_overlay_layout, null);

                    // Set the inflated view as the custom view for the AlertDialog
                    builder.setView(overlayView);

                    // Create an AlertDialog object from the builder
                    AlertDialog dialog = builder.create();

                    // Show the AlertDialog
                    dialog.show();
                }
            });
        }
    }
}