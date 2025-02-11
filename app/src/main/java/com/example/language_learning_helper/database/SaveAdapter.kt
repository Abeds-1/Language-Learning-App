package com.example.language_learning_helper.database

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.language_learning_helper.R

class SaveAdapter(
    private var saveWords: List<TranslationEntry>,
    private val viewModel: TranslationViewModel,
    private val loadPage: () -> Unit
) : RecyclerView.Adapter<SaveAdapter.SaveViewHolder>() {

    class SaveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val language: TextView = view.findViewById(R.id.language_text)
        val wordsList: TextView = view.findViewById(R.id.words_list)
        val saveIcon: ImageView = view.findViewById(R.id.save_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return SaveViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaveViewHolder, position: Int) {
        val saveWord = saveWords[position]
        holder.language.text = saveWord.sourceLanguage
        holder.wordsList.text = saveWord.originalText

        // Update save icon based on status
        updateSaveIcon(holder.saveIcon, saveWord.isSaved, holder.itemView.context)

        holder.saveIcon.setOnClickListener {
            val newSaveStatus = !saveWord.isSaved
            saveWord.isSaved = newSaveStatus
            updateSaveIcon(holder.saveIcon, newSaveStatus, holder.itemView.context)

            viewModel.toggleSaveStatus(
                saveWord.sourceLanguage,
                saveWord.targetLanguage,
                saveWord.originalText
            )

            loadPage()
        }
    }

    override fun getItemCount(): Int = saveWords.size

    fun updateData(newSaved: List<TranslationEntry>) {
        saveWords = newSaved
        notifyDataSetChanged()
    }

    private fun updateSaveIcon(saveIcon: ImageView, isSaved: Boolean, context: Context) {
        if (isSaved) {
            saveIcon.setImageResource(R.drawable.save)
            saveIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary_color))
        } else {
            saveIcon.setImageResource(R.drawable.save)
            saveIcon.setColorFilter(ContextCompat.getColor(context, R.color.save))
        }
    }
}
