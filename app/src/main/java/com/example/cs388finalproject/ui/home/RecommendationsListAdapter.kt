package com.example.cs388finalproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cs388finalproject.databinding.ItemRecommendationRowBinding

data class RecommendationRow(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val rank: Int
)

class RecommendationsListAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<RecommendationsListAdapter.ViewHolder>() {

    private val items = mutableListOf<RecommendationRow>()

    fun submitList(list: List<RecommendationRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemRecommendationRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecommendationRow) {
            binding.tvRank.text = "${item.rank}."
            binding.imgCover.load(item.coverUrl)
            binding.tvTitle.text = item.title
            binding.tvArtist.text = item.artist

            itemView.setOnClickListener {
                onClick(item.title)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRecommendationRowBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}