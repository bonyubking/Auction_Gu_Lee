package com.example.auction_gu_lee.Board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Post

// PostAdapter.kt
class PostAdapter(
    private val postList: List<Post>,
    private val onItemClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemTextView: TextView = itemView.findViewById(R.id.textView_item)
        val desiredPriceTextView: TextView = itemView.findViewById(R.id.textView_desired_price)
        val quantityTextView: TextView = itemView.findViewById(R.id.textView_quantity)
        val detailTextView: TextView = itemView.findViewById(R.id.textView_detail)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(postList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        holder.itemTextView.text = post.item
        holder.desiredPriceTextView.text = "구매 희망 가격: ${String.format("%,d원", post.desiredPrice)}"
        holder.quantityTextView.text = "수량: ${post.quantity}"
        holder.detailTextView.text = post.detail
    }

    override fun getItemCount(): Int = postList.size
}