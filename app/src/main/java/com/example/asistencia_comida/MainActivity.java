package com.example.asistencia_comida;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.asistencia_comida.R;

public class MainActivity extends AppCompatActivity {

    private Button btnAlmuerzo, btnDesayuno ,btnSalir;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnAlmuerzo = findViewById(R.id.btnAlmuerzo);
        btnDesayuno = findViewById(R.id.btnDesayuno);
        btnSalir = findViewById(R.id.btnSalir);

        btnDesayuno.setOnClickListener(v ->{
            Intent intent = new Intent(MainActivity.this, Almuerzo_activity.class);
            startActivity(intent);
        });

        btnAlmuerzo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Desayuno.class);
            startActivity(intent);
        });

        btnSalir.setOnClickListener(v ->{
            finish();
        });
    }
}