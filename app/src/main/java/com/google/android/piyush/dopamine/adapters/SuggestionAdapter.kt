package com.google.android.piyush.dopamine.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.piyush.dopamine.R

class SuggestionAdapter(
    private val suggestions: List<String>,
    private val onSuggestionClicked: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = suggestions[position]
        holder.textView.text = suggestion
        holder.itemView.setOnClickListener {
            onSuggestionClicked(suggestion)
        }
    }

    override fun getItemCount(): Int = suggestions.size
}
