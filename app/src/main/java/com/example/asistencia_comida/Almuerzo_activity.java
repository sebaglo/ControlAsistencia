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

public class Almuerzo_activity extends AppCompatActivity {

    private Button btnEscanear, btnRegresar, btnSalida;
    private EditText txtResultado;
    private TextView txtNombreAlumno;
    private ListView listViewAlumnos;
    private ArrayAdapter<String> alumnosAdapter;
    private ArrayList<String> listaAlumnos;

    private RequestQueue requestQueue;

    private static final String API_URL = "http://172.100.8.99/servicio.php";
    private static final String API_BUSCAR_ALUMNO_URL = "http://172.100.8.99/Conexion.php";

    private boolean escaneandoSalida = false;

    private static final int ID_TSERVICIO = 2;
    private boolean registroEnProceso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.almuerzo_activity);

        requestQueue = Volley.newRequestQueue(this);

        listViewAlumnos = findViewById(R.id.listViewAlumnos);
        listaAlumnos = new ArrayList<>();
        alumnosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaAlumnos);
        listViewAlumnos.setAdapter(alumnosAdapter);

        btnSalida = findViewById(R.id.btnEscanearSalida);
        btnEscanear = findViewById(R.id.btnEscanearAlmuerzo);
        btnRegresar = findViewById(R.id.btnRegresarAlmuerzo);
        txtResultado = findViewById(R.id.txtResultadoAlmuerzo);
        txtNombreAlumno = findViewById(R.id.txtNombreAlumnoAlmuerzo);

        btnRegresar.setOnClickListener(v -> onBackPressed());

        btnSalida.setOnClickListener(v -> {
            if (registroEnProceso) {
                Toast.makeText(this, "Procesando asistencia, espera un momento...", Toast.LENGTH_SHORT).show();
                return;
            }

            txtResultado.setText("");
            txtNombreAlumno.setText("Nombre del alumno aparecerá aquí");

            escaneandoSalida = true;

            IntentIntegrator integrador = new IntentIntegrator(Almuerzo_activity.this);
            integrador.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrador.setPrompt("Escanea el RUT para registrar SALIDA");
            integrador.setCameraId(0);
            integrador.setBeepEnabled(true);
            integrador.setBarcodeImageEnabled(false);
            integrador.initiateScan();
        });

        btnEscanear.setOnClickListener(v -> {
            if (registroEnProceso) {
                Toast.makeText(this, "Procesando asistencia, espera un momento...", Toast.LENGTH_SHORT).show();
                return;
            }

            txtResultado.setText("");
            txtNombreAlumno.setText("Nombre del alumno aparecerá aquí");

            IntentIntegrator integrador = new IntentIntegrator(Almuerzo_activity.this);
            integrador.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrador.setPrompt("Escanea el RUT del alumno");
            integrador.setCameraId(0);
            integrador.setBeepEnabled(true);
            integrador.setBarcodeImageEnabled(false);
            integrador.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Escaneo cancelado.", Toast.LENGTH_LONG).show();
                txtResultado.setText("");
                txtNombreAlumno.setText("Nombre del alumno aparecerá aquí");
            } else {
                final String scannedCode = result.getContents().trim();
                txtResultado.setText(scannedCode);
                registroEnProceso = true;
                if (escaneandoSalida) {
                    registrarSalida(scannedCode);
                    escaneandoSalida = false;
                } else {
                    hacerRegistroAsistencia(scannedCode, ID_TSERVICIO);
                }
            }
        }
    }

    private void registrarSalida(final String rut) {
        @SuppressLint("SimpleDateFormat")
        final String fechaActual = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    registroEnProceso = false;
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        if (success) {
                            String horaSalida = json.getString("hora_salida");
                            String nombre = json.getString("nombre_alumno");

                            // Mostrar los datos en un dialogo
                            new AlertDialog.Builder(this)
                                    .setTitle("Salida registrada")
                                    .setMessage("Alumno: " + nombre + "\nHora de salida: " + horaSalida)
                                    .setPositiveButton("OK", null)
                                    .show();

                            // Opcional: actualizar UI
                            txtNombreAlumno.setText(nombre);
                            txtResultado.setText("Salida: " + horaSalida);
                        } else {
                            Toast.makeText(this, "Error al registrar salida: " + message, Toast.LENGTH_LONG).show();
                            txtNombreAlumno.setText("Error al registrar salida");
                            txtResultado.setText("");
                            showErrorDialog("Error al registrar salida", message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al procesar salida", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    registroEnProceso = false;
                    error.printStackTrace();
                    Toast.makeText(this, "Error de conexión al registrar salida", Toast.LENGTH_LONG).show();
                    showErrorDialog("Error de conexión", "No se pudo registrar la salida. Verifica tu conexión.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "registrar_salida");
                params.put("rut", rut);
                params.put("fecha_asistencia", fechaActual);
                return params; // ⬅️ Ya no se envía hora_salida
            }
        };

        requestQueue.add(request);
    }

    private void hacerRegistroAsistencia(final String rut, final int idTServicio) {
        @SuppressLint("SimpleDateFormat")
        final String fechaActual = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        @SuppressLint("SimpleDateFormat")
        final String horaActual = new SimpleDateFormat("HH:mm:ss").format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d("RESPUESTA", response);
                    registroEnProceso = false;

                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            buscarAlumnoPorRut(rut, fechaActual, horaActual);
                        } else {
                            txtNombreAlumno.setText("Error al registrar asistencia");
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            showErrorDialog("Error al registrar", message);
                            txtResultado.setText("");
                        }
                    } catch (JSONException e) {
                        registroEnProceso = false;
                        e.printStackTrace();
                        txtNombreAlumno.setText("Error en la respuesta del servidor");
                        Toast.makeText(this, "Error al procesar datos del servidor", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    registroEnProceso = false;
                    error.printStackTrace();
                    txtNombreAlumno.setText("Error de conexión al registrar");
                    Toast.makeText(this, "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
                    showErrorDialog("Error de conexión", "No se pudo conectar con el servidor. Verifica tu conexión.");
                    txtResultado.setText("");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "registrar_asistencia");
                params.put("rut", rut);
                params.put("id_servicio", String.valueOf(idTServicio));
                params.put("fecha_asistencia", fechaActual);
                params.put("hora_entrada", horaActual);
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void buscarAlumnoParaSalida(final String rut, final String fechaRegistro) {
        StringRequest request = new StringRequest(Request.Method.POST, API_BUSCAR_ALUMNO_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");

                        if (success) {
                            JSONObject alumno = json.getJSONObject("alumno");

                            String nombreCompleto = alumno.optString("NOMBRE_COMPLETO_ALUMNO", "");
                            String apPaterno = alumno.optString("AP_PATERNO_ALUMNO", "");
                            String apMaterno = alumno.optString("AP_MATERNO_ALUMNO", "");
                            String idCurso = alumno.optString("ID_CURSO", "");

                            String nombreFinal = (nombreCompleto + " " + apPaterno + " " + apMaterno).trim();
                            txtNombreAlumno.setText(nombreFinal.isEmpty() ? "Nombre no disponible" : nombreFinal);

                            String horaEntrada = alumno.optString("hora_entrada", "");
                            String horaSalida = alumno.optString("hora_salida", "");

                            String textoResultado = "Entrada: " + (horaEntrada.isEmpty() ? "No registrada" : horaEntrada) + "\n" +
                                    "Salida: " + (horaSalida.isEmpty() ? "Sin registrar" : horaSalida);
                            txtResultado.setText(textoResultado);

                            showRegistroDialog(nombreFinal, rut, fechaRegistro, horaEntrada, horaSalida, idCurso);

                            String itemLista = nombreFinal + " (" + rut + ")";
                            if (!listaAlumnos.contains(itemLista)) {
                                listaAlumnos.add(itemLista);
                                alumnosAdapter.notifyDataSetChanged();
                            }

                        } else {
                            txtNombreAlumno.setText("Alumno no encontrado");
                            txtResultado.setText("");
                            Toast.makeText(this, "Alumno no encontrado", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        txtResultado.setText("");
                        Toast.makeText(this, "Error al procesar datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    txtResultado.setText("");
                    Toast.makeText(this, "Error de conexión al buscar alumno", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "buscar_alumno");
                params.put("rut", rut);
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void buscarAlumnoPorRut(final String rut, final String fechaRegistro, final String horaEntrada) {
        StringRequest request = new StringRequest(Request.Method.POST, API_BUSCAR_ALUMNO_URL,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");

                        if (success) {
                            JSONObject alumno = json.getJSONObject("alumno");

                            String nombreCompleto = alumno.optString("NOMBRE_COMPLETO_ALUMNO", "");
                            String apPaterno = alumno.optString("AP_PATERNO_ALUMNO", "");
                            String apMaterno = alumno.optString("AP_MATERNO_ALUMNO", "");
                            String idCurso = alumno.optString("ID_CURSO", "");

                            String nombreFinal = (nombreCompleto + " " + apPaterno + " " + apMaterno).trim();
                            txtNombreAlumno.setText(nombreFinal.isEmpty() ? "Nombre no disponible" : nombreFinal);

                            String horaSalida = alumno.optString("hora_salida", "");

                            String textoResultado = "Entrada: " + horaEntrada + "\n" +
                                    "Salida: " + (horaSalida.isEmpty() ? "Sin registrar" : horaSalida);
                            txtResultado.setText(textoResultado);

                            showRegistroDialog(nombreFinal, rut, fechaRegistro, horaEntrada, horaSalida, idCurso);

                            String itemLista = nombreFinal + " (" + rut + ")";
                            if (!listaAlumnos.contains(itemLista)) {
                                listaAlumnos.add(itemLista);
                                alumnosAdapter.notifyDataSetChanged();
                            }

                        } else {
                            txtNombreAlumno.setText("Alumno no encontrado");
                            txtResultado.setText("");
                            Toast.makeText(this, "Alumno no encontrado", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        txtResultado.setText("");
                        Toast.makeText(this, "Error al procesar datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    txtResultado.setText("");
                    Toast.makeText(this, "Error de conexión al buscar alumno", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "buscar_alumno");
                params.put("rut", rut);
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void showRegistroDialog(String nombre, String rut, String fecha, String horaEntrada, String horaSalida, String idCurso) {
        String mensaje = "Nombre: " + nombre + "\n" +
                "RUT: " + rut + "\n" +
                "Fecha: " + fecha + "\n" +
                "Hora entrada: " + horaEntrada + "\n" +
                (horaSalida != null && !horaSalida.isEmpty() ? "Hora salida: " + horaSalida + "\n" : "") +
                "Curso ID: " + idCurso;

        new AlertDialog.Builder(this)
                .setTitle("Registro de Asistencia")
                .setMessage(mensaje)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void showErrorDialog(String titulo, String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Aceptar", null)
                .show();
    }
}
