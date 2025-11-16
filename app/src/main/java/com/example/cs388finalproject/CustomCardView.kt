package com.example.cs388finalproject // Use your actual package name

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

class CustomCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleTextView: TextView
    private val descriptionTextView: TextView

    init {
        // Inflate the layout you created (card_item.xml)
        LayoutInflater.from(context).inflate(R.layout.post_item, this, true)

        // Initialize the views from the inflated layout
        titleTextView = findViewById(R.id.tv_card_title)
        descriptionTextView = findViewById(R.id.tv_card_description)
    }

    /**
     * Public method to set the data, similar to passing props in React.
     * * @param title The text for the card header.
     * @param description The text for the card body.
     */
    fun setCardData(title: String, description: String) {
        titleTextView.text = title
        descriptionTextView.text = description
    }
}