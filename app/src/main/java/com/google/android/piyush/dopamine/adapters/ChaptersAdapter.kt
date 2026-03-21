package com.google.android.piyush.dopamine.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.piyush.dopamine.R
import com.google.android.piyush.dopamine.utilities.Chapter

class ChaptersAdapter(
    private val context: Context,
    private val chapters: List<Chapter>,
    private val onChapterClick: (Chapter) -> Unit
) : RecyclerView.Adapter<ChaptersAdapter.ChapterViewHolder>() {

    inner class ChapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chapterTime: TextView = view.findViewById(R.id.chapterTime)
        val chapterTitle: TextView = view.findViewById(R.id.chapterTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.chapterTime.text = chapter.formattedTime
        holder.chapterTitle.text = chapter.title
        holder.itemView.setOnClickListener {
            onChapterClick(chapter)
        }
    }

    override fun getItemCount(): Int = chapters.size
}
