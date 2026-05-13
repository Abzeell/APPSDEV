package com.example.todolistmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    private val taskList = mutableListOf<Task>()
    private var nextLocalId = 1

    private lateinit var addEditLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        fabAddTask = findViewById(R.id.fabAddTask)

        loadLocalTasks()
        setupRecyclerView()
        setupSwipeToDelete()
        setupLauncher()

        // Only fetch from API if no local tasks exist
        if (taskList.isEmpty()) {
            fetchTasksFromApi()
        } else {
            updateEmptyState()
        }

        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddEditTaskActivity::class.java)
            intent.putExtra("mode", "add")
            addEditLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            taskList,
            onItemClick = { task, position ->
                val intent = Intent(this, AddEditTaskActivity::class.java)
                intent.putExtra("mode", "edit")
                intent.putExtra("position", position)
                intent.putExtra("id", task.id)
                intent.putExtra("apiId", task.apiId)
                intent.putExtra("title", task.title)
                intent.putExtra("description", task.description)
                intent.putExtra("completed", task.isCompleted)
                addEditLauncher.launch(intent)
            },
            onCheckChanged = { task, isChecked ->
                task.isCompleted = isChecked
                saveLocalTasks()
                syncToggleWithApi(task)
            }
        )
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        recyclerViewTasks.adapter = taskAdapter
        updateEmptyState()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val task = taskList[position]

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete Task")
                    .setMessage("Delete \"${task.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteTask(task, position)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        taskAdapter.notifyItemChanged(position)
                    }
                    .show()
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerViewTasks)
    }

    private fun setupLauncher() {
        addEditLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val mode = data.getStringExtra("mode") ?: "add"
                val title = data.getStringExtra("title") ?: return@registerForActivityResult
                val description = data.getStringExtra("description") ?: ""

                if (mode == "add") {
                    createTask(title, description)
                } else {
                    val position = data.getIntExtra("position", -1)
                    val completed = data.getBooleanExtra("completed", false)
                    if (position != -1) updateTask(position, title, description, completed)
                }
            }
        }
    }

    // ── REST API: READ ─────────────────────────────────────────────────────────
    private fun fetchTasksFromApi() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val apiTasks = RetrofitClient.apiService.getTodos(10)
                val newTasks = apiTasks.map { todo ->
                    Task(
                        id = nextLocalId++,
                        apiId = todo.id,
                        title = todo.title.replaceFirstChar { it.uppercase() },
                        description = "Fetched from JSONPlaceholder API",
                        isCompleted = todo.completed
                    )
                }
                taskList.addAll(newTasks)
                taskAdapter.notifyDataSetChanged()
                saveLocalTasks()
                Toast.makeText(this@MainActivity, "✅ Loaded ${newTasks.size} tasks from API", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "⚠️ Could not reach API: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
                updateEmptyState()
            }
        }
    }

    // ── REST API: CREATE ───────────────────────────────────────────────────────
    private fun createTask(title: String, description: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.createTodo(
                    TodoResponse(title = title, completed = false)
                )
                val task = Task(
                    id = nextLocalId++,
                    apiId = response.id,
                    title = title,
                    description = description,
                    isCompleted = false
                )
                taskList.add(0, task)
                taskAdapter.notifyItemInserted(0)
                recyclerViewTasks.scrollToPosition(0)
                saveLocalTasks()
                Toast.makeText(this@MainActivity, "✅ Task created (API ID: ${response.id})", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Graceful fallback: save locally even if API fails
                val task = Task(id = nextLocalId++, title = title, description = description)
                taskList.add(0, task)
                taskAdapter.notifyItemInserted(0)
                saveLocalTasks()
                Toast.makeText(this@MainActivity, "⚠️ Saved locally (API unavailable)", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
                updateEmptyState()
            }
        }
    }

    // ── REST API: UPDATE ───────────────────────────────────────────────────────
    private fun updateTask(position: Int, title: String, description: String, completed: Boolean) {
        val task = taskList[position]
        task.title = title
        task.description = description
        task.isCompleted = completed

        showLoading(true)
        lifecycleScope.launch {
            try {
                val apiId = if (task.apiId > 0) task.apiId else 1
                RetrofitClient.apiService.updateTodo(
                    apiId,
                    TodoResponse(id = apiId, title = title, completed = completed)
                )
                Toast.makeText(this@MainActivity, "✅ Task updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "⚠️ Updated locally", Toast.LENGTH_SHORT).show()
            } finally {
                taskAdapter.notifyItemChanged(position)
                saveLocalTasks()
                showLoading(false)
            }
        }
    }

    // ── REST API: DELETE ───────────────────────────────────────────────────────
    private fun deleteTask(task: Task, position: Int) {
        taskList.removeAt(position)
        taskAdapter.notifyItemRemoved(position)
        saveLocalTasks()
        updateEmptyState()

        lifecycleScope.launch {
            try {
                val apiId = if (task.apiId > 0) task.apiId else 1
                RetrofitClient.apiService.deleteTodo(apiId)
                Toast.makeText(this@MainActivity, "🗑️ Task deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "🗑️ Deleted locally", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── REST API: TOGGLE COMPLETE (partial update) ─────────────────────────────
    private fun syncToggleWithApi(task: Task) {
        lifecycleScope.launch {
            try {
                val apiId = if (task.apiId > 0) task.apiId else 1
                RetrofitClient.apiService.updateTodo(
                    apiId,
                    TodoResponse(id = apiId, title = task.title, completed = task.isCompleted)
                )
            } catch (_: Exception) {
                // Silent — local state is already updated
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState() {
        tvEmptyState.visibility = if (taskList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveLocalTasks() {
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
        val taskStrings = taskList.map { "${it.id}||${it.apiId}||${it.title}||${it.description}||${it.isCompleted}" }
        prefs.edit()
            .putString("tasks_v2", taskStrings.joinToString("###"))
            .putInt("next_id", nextLocalId)
            .apply()
    }

    private fun loadLocalTasks() {
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
        nextLocalId = prefs.getInt("next_id", 1)
        val saved = prefs.getString("tasks_v2", null) ?: return
        if (saved.isBlank()) return
        taskList.clear()
        saved.split("###").forEach { entry ->
            val parts = entry.split("||")
            if (parts.size == 5) {
                taskList.add(Task(
                    id = parts[0].toIntOrNull() ?: nextLocalId++,
                    apiId = parts[1].toIntOrNull() ?: 0,
                    title = parts[2],
                    description = parts[3],
                    isCompleted = parts[4].toBoolean()
                ))
            }
        }
    }
}
