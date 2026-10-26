package com.example.campusschedule.ui.classroom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusschedule.R
import com.example.campusschedule.data.model.Classroom
import com.example.campusschedule.databinding.ItemClassroomBinding

class ClassroomAdapter(
    private val onBorrow: (Classroom) -> Unit
) : RecyclerView.Adapter<ClassroomAdapter.VH>() {

    private val items = mutableListOf<Classroom>()

    fun submitList(list: List<Classroom>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemClassroomBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemClassroomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvRoom.text = "${item.building} ${item.room}"
        holder.binding.tvCapacity.text =
            holder.itemView.context.getString(R.string.classroom_capacity, item.capacity)
        holder.binding.btnBorrow.setOnClickListener { onBorrow(item) }
    }
}