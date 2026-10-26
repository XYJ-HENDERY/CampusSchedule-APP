package com.example.campusschedule.ui.classroom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campusschedule.data.model.BorrowRecord
import com.example.campusschedule.databinding.ItemBorrowRecordBinding

class BorrowRecordAdapter : RecyclerView.Adapter<BorrowRecordAdapter.VH>() {

    private val items = mutableListOf<BorrowRecord>()

    fun submitList(list: List<BorrowRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemBorrowRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBorrowRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.binding.tvRecordRoom.text = "${r.building}${r.room}"
        holder.binding.tvRecordDetail.text = buildString {
            appendLine("借用周次：第${r.week}周")
            appendLine("借用星期：${r.dayOfWeek}")
            appendLine("教室容量：${r.capacity}")
            appendLine("起止节次：%02d-%02d".format(r.startSection, r.endSection))
            if (r.successor.isNotEmpty()) appendLine("借教人：${r.successor}")
            append("教学楼：${r.building}")
        }
        holder.binding.tvRecordStatus.text = r.status
    }
}