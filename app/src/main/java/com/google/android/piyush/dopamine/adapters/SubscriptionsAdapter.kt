package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.piyush.database.entities.SubscriptionEntity
import com.google.android.piyush.dopamine.R

class SubscriptionsAdapter(
    private val context: Context,
    private val subscriptions: List<SubscriptionEntity>,
    private val onItemClick: (SubscriptionEntity) -> Unit
) : RecyclerView.Adapter<SubscriptionsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val channelImage: ImageView = itemView.findViewById(R.id.channelImage)
        val channelName: TextView = itemView.findViewById(R.id.channelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_subscription, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subscription = subscriptions[position]
        holder.channelName.text = subscription.channelTitle ?: subscription.title
        
        if (!subscription.thumbnail.isNullOrEmpty()) {
            Glide.with(context)
                .load(subscription.thumbnail)
                .circleCrop()
                .placeholder(R.mipmap.ic_launcher_round)
                .into(holder.channelImage)
        } else {
            holder.channelImage.setImageResource(R.mipmap.ic_launcher_round)
        }

        holder.itemView.setOnClickListener {
            onItemClick(subscription)
        }
    }

    override fun getItemCount(): Int = subscriptions.size
}
