package com.google.android.piyush.dopamine.viewHolders

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.piyush.dopamine.R

class SearchHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val searchHistoryItem: TextView = itemView.findViewById(R.id.textView)
    val deleteButton: ImageButton? = itemView.findViewById(R.id.deleteButton)
}
