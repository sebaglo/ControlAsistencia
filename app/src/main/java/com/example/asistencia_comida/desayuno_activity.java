package com.example.asistencia_comida;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class desayuno_activity extends AppCompatActivity {

    // Variables UI
    private Button btnEscanear, btnRegresar;
    private EditText txtResultado;
    private TextView txtNombreAlumno;
    private ListView listViewAlumnos;
    private ArrayAdapter<String> alumnosAdapter;
    private ArrayList<String> listaAlumnos;

    // Constantes de conexión
    private static final String API_URL = "http://172.100.8.99/Conexion.php";
    private static final int ID_TSERVICIO = 1; // Asumiendo que el ID para desayuno es 1

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desayuno_activity);

        // Inicialización de vistas
        listViewAlumnos = findViewById(R.id.listViewAlumnosDesayuno);
        listaAlumnos = new ArrayList<>();
        alumnosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaAlumnos);
        listViewAlumnos.setAdapter(alumnosAdapter);

        btnEscanear = findViewById(R.id.btnEscanearDesayuno);
        btnRegresar = findViewById(R.id.btnRegresarDesayuno);
        txtResultado = findViewById(R.id.txtResultadoDesayuno);
        txtNombreAlumno = findViewById(R.id.txtNombreAlumnoDesayuno);

        // Botón regresar
        btnRegresar.setOnClickListener(v -> onBackPressed());

        // Botón escanear
        btnEscanear.setOnClickListener(v -> {
            txtResultado.setText("");
            txtNombreAlumno.setText("Nombre del alumno aparecerá aquí");

            IntentIntegrator integrador = new IntentIntegrator(this);
            integrador.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrador.setPrompt("Escanea el RUT del alumno");
            integrador.setCameraId(0);
            integrador.setBeepEnabled(true);
            integrador.setBarcodeImageEnabled(true);
            integrador.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Escaneo cancelado.", Toast.LENGTH_SHORT).show();
            } else {
                String rutEscaneado = result.getContents();
                txtResultado.setText(rutEscaneado);
                hacerRegistroAsistencia(rutEscaneado, ID_TSERVICIO);
            }
        }
    }

    private void hacerRegistroAsistencia(final String rut, final int idTServicio) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        if (success) {
                            String fecha = json.optString("fecha");
                            String hora = json.optString("hora_entrada");

                            Toast.makeText(this, "Asistencia registrada", Toast.LENGTH_SHORT).show();
                            buscarAlumnoPorRut(rut, fecha, hora);
                        } else {
                            txtNombreAlumno.setText("Error al registrar asistencia");
                            showErrorDialog("Registro fallido", message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showErrorDialog("Error", "Respuesta inválida del servidor.");
                    }
                },
                error -> {
                    error.printStackTrace();
                    showErrorDialog("Conexión fallida", "No se pudo conectar al servidor.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "registrar_asistencia");
                params.put("RUT_ALUMNO", rut);
                params.put("ID_TSERVICIO", String.valueOf(idTServicio));

                @SuppressLint("SimpleDateFormat")
                String fecha = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());

                params.put("FECHA_ASISTENCIA", fecha);
                params.put("HORA_ENTRADA", hora);
                return params;
            }
        };

        queue.add(request);
    }

    private void buscarAlumnoPorRut(final String rut, final String fechaRegistro, final String horaRegistro) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            JSONObject alumno = json.getJSONObject("alumno");
                            String nombre = alumno.getString("NOMBRE_COMPLETO_ALUMNO");
                            String apPaterno = alumno.getString("AP_PATERNO_ALUMNO");
                            String apMaterno = alumno.getString("AP_MATERNO_ALUMNO");
                            String idCurso = alumno.getString("ID_CURSO");

                            String nombreCompleto = String.format("%s %s %s", nombre, apPaterno, apMaterno);
                            txtNombreAlumno.setText(nombreCompleto);
                            showRegistroDialog(nombreCompleto, rut, fechaRegistro, horaRegistro, idCurso);
                        } else {
                            txtNombreAlumno.setText("Alumno no encontrado");
                            Toast.makeText(this, "Alumno no encontrado", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Error al buscar alumno", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "buscar_alumno");
                params.put("rut", rut);
                return params;
            }
        };

        queue.add(request);
    }

    private void showRegistroDialog(String nombreCompleto, String rut, String fecha, String hora, String idCurso) {
        String mensaje = String.format(
                "Nombre: %s\nRUT: %s\nFecha: %s\nHora: %s\nCurso ID: %s",
                nombreCompleto, rut, fecha, hora, idCurso
        );

        new AlertDialog.Builder(this)
                .setTitle("Registro de Asistencia")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showErrorDialog(String titulo, String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show();
    }
}
