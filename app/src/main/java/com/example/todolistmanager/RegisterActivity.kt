package com.example.todolistmanager

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)

        val etName = findViewById<TextInputEditText>(R.id.etRegisterName)
        val etUsername = findViewById<TextInputEditText>(R.id.etRegisterUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etRegisterPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)
        val pbRegister = findViewById<ProgressBar>(R.id.pbRegister)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pbRegister.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Check if username already exists
                    val existingUsers = MockApiClient.apiService.login(username)
                    if (existingUsers.isNotEmpty()) {
                        Toast.makeText(this@RegisterActivity, "Username already taken", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Register new user
                    val newUser = User(name = name, username = username, password = password)
                    MockApiClient.apiService.register(newUser)

                    Toast.makeText(this@RegisterActivity, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    finish() // Return to LoginActivity
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    pbRegister.visibility = View.GONE
                    btnRegister.isEnabled = true
                }
            }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}