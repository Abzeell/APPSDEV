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

    // Store the logged in user's ID
    private var currentUserId: String = ""

    private lateinit var addEditLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        // --- SESSION MANAGEMENT ---
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        currentUserId = sharedPref.getString("USER_ID", "") ?: ""
        val name = sharedPref.getString("NAME", "User")

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome.text = "$name's Tasks"

        val btnLogout = findViewById<View>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        // --------------------------

        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        fabAddTask = findViewById(R.id.fabAddTask)

        loadLocalTasks()
        setupRecyclerView()
        setupSwipeToDelete()
        setupLauncher()

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

    // ── mockAPI: READ ─────────────────────────────────────────────────────────
    private fun fetchTasksFromApi() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // Fetch ONLY the tasks belonging to this user
                val apiTasks = MockApiClient.apiService.getTasks(currentUserId)
                val newTasks = apiTasks.map { apiTask ->
                    Task(
                        id = nextLocalId++,
                        apiId = apiTask.id,
                        title = apiTask.title,
                        description = apiTask.description,
                        isCompleted = apiTask.completed
                    )
                }
                taskList.clear()
                taskList.addAll(newTasks)
                taskAdapter.notifyDataSetChanged()
                saveLocalTasks()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "⚠️ Could not reach mockAPI", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
                updateEmptyState()
            }
        }
    }

    // ── mockAPI: CREATE ───────────────────────────────────────────────────────
    private fun createTask(title: String, description: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val newApiTask = ApiTask(
                    userId = currentUserId,
                    title = title,
                    description = description,
                    completed = false
                )
                val response = MockApiClient.apiService.createTask(newApiTask)

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
            } catch (e: Exception) {
                // Graceful fallback
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

    // ── mockAPI: UPDATE ───────────────────────────────────────────────────────
    private fun updateTask(position: Int, title: String, description: String, completed: Boolean) {
        val task = taskList[position]
        task.title = title
        task.description = description
        task.isCompleted = completed

        showLoading(true)
        lifecycleScope.launch {
            try {
                if (task.apiId.isNotEmpty()) {
                    val updateApiTask = ApiTask(
                        id = task.apiId,
                        userId = currentUserId,
                        title = title,
                        description = description,
                        completed = completed
                    )
                    MockApiClient.apiService.updateTask(task.apiId, updateApiTask)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "⚠️ Updated locally only", Toast.LENGTH_SHORT).show()
            } finally {
                taskAdapter.notifyItemChanged(position)
                saveLocalTasks()
                showLoading(false)
            }
        }
    }

    // ── mockAPI: DELETE ───────────────────────────────────────────────────────
    private fun deleteTask(task: Task, position: Int) {
        taskList.removeAt(position)
        taskAdapter.notifyItemRemoved(position)
        saveLocalTasks()
        updateEmptyState()

        lifecycleScope.launch {
            try {
                if (task.apiId.isNotEmpty()) {
                    MockApiClient.apiService.deleteTask(task.apiId)
                }
            } catch (e: Exception) {
                // Failed to delete from cloud, but already deleted locally
            }
        }
    }

    // ── mockAPI: TOGGLE COMPLETE ──────────────────────────────────────────────
    private fun syncToggleWithApi(task: Task) {
        lifecycleScope.launch {
            try {
                if (task.apiId.isNotEmpty()) {
                    val updateApiTask = ApiTask(
                        id = task.apiId,
                        userId = currentUserId,
                        title = task.title,
                        description = task.description,
                        completed = task.isCompleted
                    )
                    MockApiClient.apiService.updateTask(task.apiId, updateApiTask)
                }
            } catch (_: Exception) {}
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
            // We save local tasks grouped by currentUserId so user data doesn't mix!
            .putString("tasks_$currentUserId", taskStrings.joinToString("###"))
            .putInt("next_id", nextLocalId)
            .apply()
    }

    private fun loadLocalTasks() {
        val prefs = getSharedPreferences("todo_prefs", MODE_PRIVATE)
        nextLocalId = prefs.getInt("next_id", 1)
        val saved = prefs.getString("tasks_$currentUserId", null) ?: return
        if (saved.isBlank()) return
        taskList.clear()
        saved.split("###").forEach { entry ->
            val parts = entry.split("||")
            if (parts.size == 5) {
                taskList.add(Task(
                    id = parts[0].toIntOrNull() ?: nextLocalId++,
                    apiId = parts[1],
                    title = parts[2],
                    description = parts[3],
                    isCompleted = parts[4].toBoolean()
                ))
            }
        }
    }
}