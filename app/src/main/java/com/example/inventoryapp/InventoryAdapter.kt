package com.example.inventoryapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.model.InventoryItem

class InventoryAdapter(
    private val items: MutableList<InventoryItem>,
    private val onItemClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    private val LOW_STOCK_COLOR = Color.parseColor("#FFEBEE")
    private val NORMAL_COLOR = Color.WHITE

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_item_name)
        val tvBarcode: TextView = view.findViewById(R.id.tv_item_barcode)
        val tvQuantity: TextView = view.findViewById(R.id.tv_item_quantity)
        val tvLocation: TextView = view.findViewById(R.id.tv_item_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvBarcode.text = item.barcode
        holder.tvQuantity.text = "Qty: ${item.quantity}"
        holder.tvLocation.text = item.location.ifBlank { "No location" }

        holder.itemView.setBackgroundColor(
            if (item.isLowStock()) LOW_STOCK_COLOR else NORMAL_COLOR
        )

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<InventoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getPositionForId(id: Long): Int {
        return items.indexOfFirst { it.id == id }
    }
}