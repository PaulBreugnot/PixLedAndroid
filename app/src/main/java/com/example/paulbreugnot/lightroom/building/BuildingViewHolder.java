package com.example.paulbreugnot.lightroom.building;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.paulbreugnot.lightroom.R;
import com.example.paulbreugnot.lightroom.room.RoomSelectionActivity;

public class BuildingViewHolder extends RecyclerView.ViewHolder {

    private TextView textViewView;
    private Building building;
    private BuildingSelectionActivity buildingSelectionActivity;
    // private ImageView imageView;

    //itemView est la vue correspondante à 1 cellule
    public BuildingViewHolder(final View itemView) {
        super(itemView);

        //c'est ici que l'on fait nos findView

        textViewView = itemView.findViewById(R.id.text);
        // imageView = (ImageView) itemView.findViewById(R.id.image);

        final LinearLayout button = itemView.findViewById(R.id.layout);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("BUILDING SELECT", building.getName());
                buildingSelectionActivity.launchRoomSelectionActivity(building);
            }
        });
    }

    //puis ajouter une fonction pour remplir la cellule en fonction d'un MyObject
    public void bind(Building building, BuildingSelectionActivity buildingSelectionActivity){
        this.building = building;
        this.buildingSelectionActivity = buildingSelectionActivity;
        textViewView.setText(building.getName());
        // Picasso.with(imageView.getContext()).load(myObject.getImageUrl()).centerCrop().fit().into(imageView);
    }
}