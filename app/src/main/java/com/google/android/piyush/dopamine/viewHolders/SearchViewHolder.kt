package com.google.android.piyush.dopamine.viewHolders

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.piyush.dopamine.R

class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var image: ShapeableImageView = itemView.findViewById(R.id.image)
    var text1: TextView = itemView.findViewById(R.id.text1)
    var text2: TextView = itemView.findViewById(R.id.text2)
    var video: View = itemView.findViewById(R.id.video)
    var channelview: ImageButton = itemView.findViewById(R.id.ChannelView)

}
