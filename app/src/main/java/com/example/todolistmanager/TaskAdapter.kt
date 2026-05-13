package com.example.todolistmanager

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val taskList: MutableList<Task>,
    private val onItemClick: (Task, Int) -> Unit,
    private val onCheckChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        val tvTaskStatus: TextView = itemView.findViewById(R.id.tvTaskStatus)
        val cbDone: CheckBox = itemView.findViewById(R.id.cbDone)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.tvTaskTitle.text = task.title
        holder.tvTaskDescription.text = if (task.description.isNotEmpty()) task.description else "No description"

        if (task.isCompleted) {
            holder.tvTaskStatus.text = "✔ Completed"
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTaskTitle.setTextColor(Color.GRAY)
            holder.tvTaskStatus.setTextColor(Color.parseColor("#4CAF50"))
            holder.tvTaskDescription.setTextColor(Color.LTGRAY)
        } else {
            holder.tvTaskStatus.text = "⏳ Pending"
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTaskTitle.setTextColor(Color.parseColor("#212121"))
            holder.tvTaskStatus.setTextColor(Color.parseColor("#FF9800"))
            holder.tvTaskDescription.setTextColor(Color.parseColor("#757575"))
        }

        // Prevent recursive triggering
        holder.cbDone.setOnCheckedChangeListener(null)
        holder.cbDone.isChecked = task.isCompleted
        holder.cbDone.setOnCheckedChangeListener { _, isChecked ->
            onCheckChanged(task, isChecked)
        }

        holder.btnEdit.setOnClickListener {
            onItemClick(task, holder.bindingAdapterPosition)
        }

        holder.itemView.setOnClickListener {
            onItemClick(task, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount() = taskList.size
}
