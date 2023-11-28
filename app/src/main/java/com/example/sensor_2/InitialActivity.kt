package com.example.sensor_2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button


class InitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)

        val iniciarButton: Button = findViewById(R.id.iniciarButton)
        val configuracionButton: Button = findViewById(R.id.configuracionButton)

        iniciarButton.setOnClickListener {
            val intent = Intent(this, SensorActivity::class.java)
            startActivity(intent)
        }

        configuracionButton.setOnClickListener {
            // Aquí puedes iniciar la actividad de configuración
        }
    }
}
