package com.example.asistencia_comida;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Inicio_activity extends MainActivity{

    private Button btninicio;

    @SuppressLint({"ClickableViewAccessibility", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inicio_activity);

        btninicio = findViewById(R.id.btnIniciar);


        btninicio.setOnClickListener(v ->{
            Intent intent = new Intent(Inicio_activity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
