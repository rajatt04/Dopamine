package com.google.android.piyush.dopamine.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.piyush.dopamine.R

class ChipAdapter(
    private val chips: List<String>,
    private val onChipClicked: (String) -> Unit
) : RecyclerView.Adapter<ChipAdapter.ChipViewHolder>() {

    private var selectedPosition = 0

    class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chip: Chip = itemView.findViewById(R.id.chip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val chipText = chips[position]
        holder.chip.text = chipText
        holder.chip.isChecked = (position == selectedPosition)
        
        holder.chip.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (selectedPosition != currentPosition && currentPosition != RecyclerView.NO_POSITION) {
                val previousPosition = selectedPosition
                selectedPosition = currentPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onChipClicked(chipText)
            } else if (selectedPosition == currentPosition) {
                // Ensure it stays checked if they click the already selected one
                holder.chip.isChecked = true
            }
        }
    }

    override fun getItemCount(): Int = chips.size
}
