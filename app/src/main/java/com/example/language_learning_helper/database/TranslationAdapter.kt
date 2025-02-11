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

class TranslationAdapter(
    private var translations: List<TranslationEntry>,
    private val viewModel: TranslationViewModel,
    private val loadPage: () -> Unit
) : RecyclerView.Adapter<TranslationAdapter.TranslationViewHolder>() {

    class TranslationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val originalText: TextView = view.findViewById(R.id.original_text)
        val translatedText: TextView = view.findViewById(R.id.translated_text)
        val language: TextView = view.findViewById(R.id.language_text)
        val saveIcon: ImageView = view.findViewById(R.id.save_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_translation, parent, false)
        return TranslationViewHolder(view)
    }

    override fun onBindViewHolder(holder: TranslationViewHolder, position: Int) {
        val translation = translations[position]
        holder.originalText.text = translation.originalText
        holder.translatedText.text = translation.translatedText
        holder.language.text = translation.sourceLanguage

        // Update the icon based on save status
        updateSaveIcon(holder.saveIcon, translation.isSaved, holder.itemView.context)

        holder.saveIcon.setOnClickListener {
            val newSaveStatus = !translation.isSaved
            translation.isSaved = newSaveStatus
            updateSaveIcon(holder.saveIcon, newSaveStatus, holder.itemView.context)

            viewModel.toggleSaveStatus(
                translation.sourceLanguage,
                translation.targetLanguage,
                translation.originalText
            )

            loadPage()
        }
    }

    override fun getItemCount(): Int = translations.size

    fun updateData(newTranslations: List<TranslationEntry>) {
        translations = newTranslations
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
