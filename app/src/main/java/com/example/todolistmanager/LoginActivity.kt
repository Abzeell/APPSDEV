package com.example.todolistmanager

import android.content.Context
import android.content.Intent
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

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // 1. CHECK FOR EXISTING SESSION FIRST
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val savedUsername = sharedPref.getString("USERNAME", null)

        if (savedUsername != null) {
            // User is already logged in, jump straight to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etUsername = findViewById<TextInputEditText>(R.id.etLoginUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        val pbLogin = findViewById<ProgressBar>(R.id.pbLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pbLogin.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch {
                try {
                    val users = MockApiClient.apiService.login(username)

                    if (users.isNotEmpty() && users[0].password == password) {

                        // 2. SAVE SESSION ON SUCCESSFUL LOGIN
                        val editor = sharedPref.edit()
                        editor.putString("USER_ID", users[0].id)
                        editor.putString("USERNAME", users[0].username)
                        editor.putString("NAME", users[0].name)
                        editor.apply()

                        Toast.makeText(this@LoginActivity, "Welcome back, ${users[0].name}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    pbLogin.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}