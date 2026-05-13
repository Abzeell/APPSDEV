package com.example.todolistmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var tvHeading: TextView
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilDescription: TextInputLayout
    private lateinit var etDescription: TextInputEditText
    private lateinit var cbCompleted: CheckBox
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var mode = "add"
    private var position = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_add_edit_task)

        tvHeading = findViewById(R.id.tvHeading)
        tilTitle = findViewById(R.id.tilTitle)
        etTitle = findViewById(R.id.etTitle)
        tilDescription = findViewById(R.id.tilDescription)
        etDescription = findViewById(R.id.etDescription)
        cbCompleted = findViewById(R.id.cbCompleted)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        mode = intent.getStringExtra("mode") ?: "add"
        position = intent.getIntExtra("position", -1)

        if (mode == "edit") {
            tvHeading.text = "Edit Task"
            btnSave.text = "Update Task"
            etTitle.setText(intent.getStringExtra("title"))
            etDescription.setText(intent.getStringExtra("description"))
            cbCompleted.isChecked = intent.getBooleanExtra("completed", false)
            cbCompleted.visibility = View.VISIBLE
        } else {
            tvHeading.text = "Add New Task"
            btnSave.text = "Add Task"
            cbCompleted.visibility = View.GONE
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()

            // Validation
            var valid = true
            if (title.isEmpty()) {
                tilTitle.error = "Title is required"
                valid = false
            } else if (title.length < 3) {
                tilTitle.error = "Title must be at least 3 characters"
                valid = false
            } else {
                tilTitle.error = null
            }

            if (!valid) return@setOnClickListener

            val resultIntent = Intent()
            resultIntent.putExtra("mode", mode)
            resultIntent.putExtra("title", title)
            resultIntent.putExtra("description", description)
            resultIntent.putExtra("position", position)
            resultIntent.putExtra("completed", cbCompleted.isChecked)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}
