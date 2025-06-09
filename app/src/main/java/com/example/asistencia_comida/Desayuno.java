package com.example.asistencia_comida;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Desayuno extends AppCompatActivity {

    private Button btnEscanear, btnRegresar;
    private EditText txtResultado, etBuscarRut;
    private TextView txtNombreAlumno;
    private ListView listViewAlumnos;
    private ArrayAdapter<Almuerzo_activity.AlumnoAsistencia> alumnosAdapter;
    private ArrayList<Almuerzo_activity.AlumnoAsistencia> listaAlumnosAsistencia;
    private ArrayList<Almuerzo_activity.AlumnoAsistencia> listaAlumnosFiltrada;

    private RequestQueue requestQueue;

    private static final String API_URL = "http://172.100.8.99/conexion.php"; // <--- ¡Asegúrate de que esta URL sea correcta!
    private static final int ID_TSERVICIO = 1; // Colación/Almuerzo
    private boolean registroEnProceso = false;

    public static class AlumnoAsistencia {
        private String rut;
        private String nombreCompleto;
        private String horaEntrada;
        private String horaSalida;
        private String nombreCurso;

        public AlumnoAsistencia(String rut, String nombreCompleto, String horaEntrada, String horaSalida, String nombreCurso) {
            this.rut = rut;
            this.nombreCompleto = nombreCompleto;
            this.horaEntrada = horaEntrada;
            this.horaSalida = horaSalida;
            this.nombreCurso = nombreCurso;
        }

        // Getters
        public String getRut() { return rut; }
        public String getNombreCompleto() { return nombreCompleto; }
        public String getHoraEntrada() { return horaEntrada; }
        public String getHoraSalida() { return horaSalida; }
        public String getNombreCurso() { return nombreCurso; }

        // Setters (útiles para actualizar el estado de un alumno en la lista)
        public void setRut(String rut) { this.rut = rut; }
        public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
        public void setHoraEntrada(String horaEntrada) { this.horaEntrada = horaEntrada; }
        public void setHoraSalida(String horaSalida) { this.horaSalida = horaSalida; }
        public void setNombreCurso(String nombreCurso) { this.nombreCurso = nombreCurso; }


        public boolean tieneSalidaRegistrada() {
            return horaSalida != null && !horaSalida.isEmpty() && !horaSalida.equalsIgnoreCase("null");
        }

        public boolean tieneEntradaRegistrada() {
            return horaEntrada != null && !horaEntrada.isEmpty() && !horaEntrada.equalsIgnoreCase("null");
        }

        @Override
        public String toString() {
            String entradaDisplay = (tieneEntradaRegistrada()) ? horaEntrada : "N/A";
            String salidaDisplay = (tieneSalidaRegistrada()) ? horaSalida : "N/A";
            String cursoDisplay = (nombreCurso != null && !nombreCurso.isEmpty() && !nombreCurso.equalsIgnoreCase("null") && !nombreCurso.equalsIgnoreCase("N/A")) ? " - " + nombreCurso : "";

            String estado = " (Pendiente)"; // Estado por defecto si no hay entrada
            if (tieneEntradaRegistrada()) {
                estado = " (ENTRADA: " + entradaDisplay;
                if (tieneSalidaRegistrada()) {
                    estado += " - SALIDA: " + salidaDisplay + ")";
                } else {
                    estado += " - Salida Pendiente)";
                }
            } else if (tieneSalidaRegistrada()) { // Solo si tiene salida pero no entrada (caso raro, pero manejado)
                estado = " (SALIDA: " + salidaDisplay + ") - SIN ENTRADA";
            }

            return nombreCompleto + " - " + rut + cursoDisplay + estado;
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desayuno_activity);

        requestQueue = Volley.newRequestQueue(this);

        listViewAlumnos = findViewById(R.id.listViewAlumnosDesayuno);
        etBuscarRut = findViewById(R.id.etBuscarRutDesayuno);
        btnEscanear = findViewById(R.id.btnEscanearDesayuno);
        btnRegresar = findViewById(R.id.btnRegresarDesayuno);
        txtResultado = findViewById(R.id.txtResultadoDesayuno);
        txtNombreAlumno = findViewById(R.id.txtNombreAlumnoDesayuno); // Asegúrate que este ID sea correcto, lo cambie por uno más genérico

        listaAlumnosAsistencia = new ArrayList<>();
        listaAlumnosFiltrada = new ArrayList<>();
        alumnosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaAlumnosFiltrada);
        listViewAlumnos.setAdapter(alumnosAdapter);

        btnRegresar.setOnClickListener(v -> onBackPressed());

        btnEscanear.setOnClickListener(v -> {
            if (registroEnProceso) {
                Toast.makeText(this, "Procesando asistencia, espera un momento...", Toast.LENGTH_SHORT).show();
                return;
            }
            limpiarCamposEscaneo();
            iniciarEscaneo("Escanea el RUT del alumno");
        });

        listViewAlumnos.setOnItemLongClickListener((parent, view, position, id) -> {
            Almuerzo_activity.AlumnoAsistencia alumnoSeleccionado = listaAlumnosFiltrada.get(position);
            mostrarDialogoAccionesAlumno(alumnoSeleccionado);
            return true;
        });

        etBuscarRut.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarAlumnos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cargarAlumnosDelDia();
    }

    private void iniciarEscaneo(String promptMessage) {
        IntentIntegrator integrator = new IntentIntegrator(Desayuno.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt(promptMessage);
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Escaneo cancelado.", Toast.LENGTH_LONG).show();
                limpiarCamposEscaneo();
            } else {
                final String scannedCode = result.getContents().trim();
                txtResultado.setText(scannedCode);
                String rutNormalizado = scannedCode.replace(".", "").replace("-", "").toUpperCase(Locale.getDefault());
                if (rutNormalizado.length() > 1 && rutNormalizado.endsWith("K")) {
                    rutNormalizado = rutNormalizado.substring(0, rutNormalizado.length() - 1) + "K";
                }

                registroEnProceso = true; // Activa la bandera de proceso

                // PRIMER PASO: Obtener datos del alumno
                obtenerDatosAlumno(rutNormalizado);
            }
        }
    }

    private void obtenerDatosAlumno(final String rut) {
        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d("API_GET_ALUMNO_DATA", response); // Log de la respuesta completa
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");

                        if (success) {
                            String nombreCompleto = json.optString("nombre_completo_alumno", "Nombre no disponible");
                            String nombreCurso = json.optString("nombre_curso", "Curso no disponible");

                            txtNombreAlumno.setText(nombreCompleto); // Mostrar el nombre inmediatamente

                            // SEGUNDO PASO: Si el alumno existe, ahora obtener su estado de asistencia
                            @SuppressLint("SimpleDateFormat") final String horaActual = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                            @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                            // Pasar el nombre y curso obtenidos para que obtenerEstadoYDecidirAccion no tenga que volver a buscarlos
                            obtenerEstadoYDecidirAccion(rut, fechaActual, horaActual, nombreCompleto, nombreCurso);

                        } else {
                            String message = json.getString("message");
                            Toast.makeText(this, "Error al obtener datos del alumno: " + message, Toast.LENGTH_LONG).show();
                            txtNombreAlumno.setText("Alumno no encontrado");
                            limpiarCamposEscaneo();
                            registroEnProceso = false; // Desactiva la bandera en caso de error
                            showErrorDialog("Alumno no encontrado", message);
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar JSON obtener_alumno_data: " + response, e);
                        Toast.makeText(this, "Error al procesar respuesta del servidor para datos de alumno.", Toast.LENGTH_LONG).show();
                        txtNombreAlumno.setText("Error en la respuesta del servidor");
                        limpiarCamposEscaneo();
                        registroEnProceso = false; // Desactiva la bandera en caso de error
                        showErrorDialog("Error de JSON", "Respuesta del servidor inválida al obtener datos de alumno.");
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR_GET_ALUMNO_DATA", "Error de red al obtener datos de alumno: " + error.toString(), error);
                    Toast.makeText(this, "Error de conexión al obtener datos de alumno. Verifica tu conexión.", Toast.LENGTH_LONG).show();
                    txtNombreAlumno.setText("Error de conexión");
                    limpiarCamposEscaneo();
                    registroEnProceso = false; // Desactiva la bandera en caso de error
                    showErrorDialog("Error de Conexión", "No se pudo conectar con el servidor para obtener datos de alumno.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "obtener_alumno_data"); // Nueva acción
                params.put("rut_alumno", rut);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void obtenerEstadoYDecidirAccion(String rut, String fechaActual, String horaActual, String nombreAlumno, String nombreCurso) {
        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    registroEnProceso = false; // Desactiva la bandera de proceso
                    Log.d("API_RESPONSE_ESTADO", response); // Log de la respuesta completa

                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        String horaEntradaExistente = json.optString("hora_entrada", null);
                        String horaSalidaExistente = json.optString("hora_salida", null);
                        // Usar el nombre_completo_alumno y nombre_curso pasados por parámetro, o desde la respuesta si viene
                        String finalNombreAlumno = json.optString("nombre_completo_alumno", nombreAlumno);
                        String finalNombreCurso = json.optString("nombre_curso", nombreCurso);

                        // Crear o actualizar la instancia de AlumnoAsistencia
                        Almuerzo_activity.AlumnoAsistencia currentAlumno = new Almuerzo_activity.AlumnoAsistencia(rut, finalNombreAlumno, horaEntradaExistente, horaSalidaExistente, finalNombreCurso);
                        actualizarAlumnoEnLista(currentAlumno); // Intenta actualizar o añadir a la lista principal
                        filtrarAlumnos(etBuscarRut.getText().toString()); // Refresca la vista

                        if (success) {
                            // PHP reporta éxito (normalmente, se encontró asistencia de entrada)
                            if (currentAlumno.tieneEntradaRegistrada() && !currentAlumno.tieneSalidaRegistrada()) {
                                // Alumno con entrada pero sin salida -> Registrar salida
                                Toast.makeText(this, "Registrando salida para " + currentAlumno.getNombreCompleto() + "...", Toast.LENGTH_LONG).show();
                                registrarSalida(rut, horaActual);
                            } else if (currentAlumno.tieneEntradaRegistrada() && currentAlumno.tieneSalidaRegistrada()) {
                                // Alumno con entrada y salida -> Asistencia completa
                                Toast.makeText(this, "Asistencia de " + currentAlumno.getNombreCompleto() + " (" + rut + ") ya completa hoy.", Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                                showRegistroDialog(currentAlumno.getNombreCompleto(), currentAlumno.getRut(), fechaActual, currentAlumno.getHoraEntrada(), currentAlumno.getHoraSalida(), currentAlumno.getNombreCurso());
                            } else {
                                Toast.makeText(this, "Estado inesperado: " + message, Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                            }
                        } else {
                            // Caso success: false, verificar el mensaje para decidir la acción
                            if (message.contains("Ya existe un registro de entrada para este alumno en esta fecha.")) {
                                // PHP nos dice que ya hay entrada (y nos da las horas existentes)
                                Toast.makeText(this, "Entrada ya registrada para " + currentAlumno.getNombreCompleto() + " (" + rut + "). Hora: " + currentAlumno.getHoraEntrada(), Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                                showRegistroDialog(currentAlumno.getNombreCompleto(), currentAlumno.getRut(), fechaActual, currentAlumno.getHoraEntrada(), currentAlumno.getHoraSalida(), currentAlumno.getNombreCurso());
                            } else if (message.contains("Alumno encontrado, pero sin asistencia para hoy.")) {
                                // Alumno existe pero no tiene ninguna asistencia para hoy (ni entrada, ni salida) -> Registrar entrada
                                Toast.makeText(this, "Registrando entrada para " + currentAlumno.getNombreCompleto() + "...", Toast.LENGTH_LONG).show();
                                hacerRegistroAsistencia(rut, ID_TSERVICIO, horaActual);
                            } else if (message.contains("Error: El RUT del alumno no existe en la base de datos.")) {
                                // Esto no debería ocurrir si obtenerDatosAlumno fue exitoso, pero se mantiene como fallback
                                Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                                showErrorDialog("Alumno no encontrado (2)", message);
                            } else {
                                Toast.makeText(this, "Error en el servidor: " + message, Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                                showErrorDialog("Error en el servidor", message);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar respuesta JSON: " + response, e);
                        Toast.makeText(Desayuno.this, "Error al procesar respuesta del servidor. Formato inválido.", Toast.LENGTH_LONG).show();
                        limpiarCamposEscaneo();
                        showErrorDialog("Error de JSON", "Respuesta del servidor inválida.");
                    }
                },
                error -> {
                    registroEnProceso = false; // Desactiva la bandera de proceso
                    Log.e("VOLLEY_ERROR_ESTADO", "Error de red al obtener estado: " + error.toString(), error);
                    Toast.makeText(Desayuno.this, "Error de red al obtener estado. Verifica tu conexión.", Toast.LENGTH_SHORT).show();
                    limpiarCamposEscaneo();
                    showErrorDialog("Error de Conexión", "No se pudo conectar con el servidor para obtener el estado.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "obtener_asistencia"); // Acción para obtener el estado de asistencia
                params.put("rut_alumno", rut);
                params.put("fecha_servicio", fechaActual);
                params.put("tipo_servicio", String.valueOf(ID_TSERVICIO));
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void registrarSalida(final String rut, final String horaSalida) {
        @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    registroEnProceso = false; // Desactiva la bandera
                    Log.d("API_RESPONSE_SALIDA", response); // Log de la respuesta completa
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String mensaje = json.getString("message");

                        // Extraer los datos de asistencia confirmados
                        String horaEntradaConfirmada = json.optString("hora_entrada", null);
                        String horaSalidaConfirmada = json.optString("hora_salida", horaSalida);
                        String nombreCompleto = json.optString("nombre_completo_alumno", txtNombreAlumno.getText().toString());
                        String nombreCurso = json.optString("nombre_curso", "N/A");

                        if (success) {
                            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
                            Almuerzo_activity.AlumnoAsistencia updatedAlumno = new Almuerzo_activity.AlumnoAsistencia(rut, nombreCompleto, horaEntradaConfirmada, horaSalidaConfirmada, nombreCurso);
                            actualizarAlumnoEnLista(updatedAlumno);
                            filtrarAlumnos(etBuscarRut.getText().toString());
                            showRegistroDialog(nombreCompleto, rut, fechaActual, horaEntradaConfirmada, horaSalidaConfirmada, nombreCurso);
                            limpiarCamposEscaneo();
                        } else {
                            mostrarDialogo("Error al registrar salida", mensaje);
                            limpiarCamposEscaneo();
                            showErrorDialog("Error al registrar salida", mensaje);
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar respuesta JSON al registrar salida: " + response, e);
                        mostrarDialogo("Error", "Respuesta inválida del servidor al registrar salida.");
                        limpiarCamposEscaneo();
                        showErrorDialog("Error de JSON", "Respuesta del servidor inválida al registrar salida.");
                    }
                },
                error -> {
                    registroEnProceso = false; // Desactiva la bandera
                    Log.e("VOLLEY_ERROR_SALIDA", "Error de red al registrar salida: " + error.toString(), error);
                    mostrarDialogo("Error", "Error en la conexión con el servidor al registrar salida. Verifica tu conexión.");
                    limpiarCamposEscaneo();
                    showErrorDialog("Error de Conexión", "No se pudo conectar con el servidor al registrar salida.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "registrar_salida");
                params.put("rut_alumno", rut);
                params.put("fecha_servicio", fechaActual);
                params.put("hora_salida_servicio", horaSalida);
                params.put("tipo_servicio", String.valueOf(ID_TSERVICIO));
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void hacerRegistroAsistencia(final String rut, final int idTServicio, final String horaEntrada) {
        @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    registroEnProceso = false; // Desactiva la bandera
                    Log.d("API_RESPONSE_ENTRADA", response); // Log de la respuesta completa

                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        String horaEntradaConfirmada = json.optString("hora_entrada", horaEntrada);
                        String horaSalidaConfirmada = json.optString("hora_salida", null);
                        String nombreAlumnoResponse = json.optString("nombre_completo_alumno", txtNombreAlumno.getText().toString());
                        String nombreCurso = json.optString("nombre_curso", "N/A");


                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            Almuerzo_activity.AlumnoAsistencia newAlumno = new Almuerzo_activity.AlumnoAsistencia(rut, nombreAlumnoResponse, horaEntradaConfirmada, horaSalidaConfirmada, nombreCurso);
                            actualizarAlumnoEnLista(newAlumno);
                            filtrarAlumnos(etBuscarRut.getText().toString());
                            showRegistroDialog(nombreAlumnoResponse, rut, fechaActual, horaEntradaConfirmada, horaSalidaConfirmada, nombreCurso);
                            limpiarCamposEscaneo();

                        } else {
                            txtNombreAlumno.setText("Error al registrar asistencia");
                            Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
                            showErrorDialog("Error al registrar", message);
                            limpiarCamposEscaneo();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar respuesta JSON al registrar entrada: " + response, e);
                        txtNombreAlumno.setText("Error en la respuesta del servidor");
                        Toast.makeText(this, "Error al procesar datos del servidor", Toast.LENGTH_LONG).show();
                        limpiarCamposEscaneo();
                        showErrorDialog("Error de JSON", "Respuesta del servidor inválida al registrar entrada.");
                    }
                },
                error -> {
                    registroEnProceso = false; // Desactiva la bandera
                    Log.e("VOLLEY_ERROR_ENTRADA", "Error de red al registrar entrada: " + error.toString(), error);
                    txtNombreAlumno.setText("Error de conexión al registrar");
                    Toast.makeText(this, "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
                    showErrorDialog("Error de conexión", "No se pudo conectar con el servidor. Verifica tu conexión.");
                    limpiarCamposEscaneo();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "registrar_asistencia");
                params.put("rut_alumno", rut);
                params.put("tipo_servicio", String.valueOf(idTServicio));
                params.put("fecha_servicio", fechaActual);
                params.put("hora_entrada_servicio", horaEntrada);
                return params;
            }
        };
        requestQueue.add(request);
    }

    private void showRegistroDialog(String nombreCompleto, String rut, String fecha, String horaEntrada, String horaSalida, String nombreCurso) {
        String mensaje = "Alumno: " + (nombreCompleto != null ? nombreCompleto : "N/A") + "\n" +
                "RUT: " + rut + "\n" +
                "Curso: " + (nombreCurso != null && !nombreCurso.equalsIgnoreCase("null") ? nombreCurso : "N/A") + "\n" +
                "Fecha: " + fecha + "\n" +
                "Hora Entrada: " + (horaEntrada != null && !horaEntrada.isEmpty() && !horaEntrada.equalsIgnoreCase("null") ? horaEntrada : "Sin registrar") + "\n" +
                "Hora Salida: " + (horaSalida != null && !horaSalida.isEmpty() && !horaSalida.equalsIgnoreCase("null") ? horaSalida : "Sin registrar");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Estado de Asistencia")
                .setMessage(mensaje)
                .setPositiveButton("Aceptar", null);
        builder.show();
    }

    private void mostrarDialogo(String titulo, String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error: " + title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void mostrarDialogoAccionesAlumno(Almuerzo_activity.AlumnoAsistencia alumno) {
        String[] opciones = new String[]{"Ver Detalles"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Acciones para " + alumno.getNombreCompleto())
                .setItems(opciones, (dialog, which) -> {
                    if (opciones[which].equals("Ver Detalles")) {
                        showRegistroDialog(alumno.getNombreCompleto(), alumno.getRut(),
                                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()), // Fecha actual
                                alumno.getHoraEntrada(), alumno.getHoraSalida(), alumno.getNombreCurso());
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarAlumnoEnLista(Almuerzo_activity.AlumnoAsistencia updatedAlumno) {
        boolean encontrado = false;
        for (int i = 0; i < listaAlumnosAsistencia.size(); i++) {
            if (Objects.equals(listaAlumnosAsistencia.get(i).getRut(), updatedAlumno.getRut())) {
                listaAlumnosAsistencia.set(i, updatedAlumno);
                encontrado = true;
                break;
            }
        }
        if (!encontrado) {
            listaAlumnosAsistencia.add(updatedAlumno);
        }
    }

    private void filtrarAlumnos(String texto) {
        listaAlumnosFiltrada.clear();
        if (texto.isEmpty()) {
            listaAlumnosFiltrada.addAll(listaAlumnosAsistencia);
        } else {
            String lowerCaseText = texto.toLowerCase(Locale.getDefault());
            for (Almuerzo_activity.AlumnoAsistencia alumno : listaAlumnosAsistencia) {
                if (alumno.getRut().toLowerCase(Locale.getDefault()).contains(lowerCaseText) ||
                        alumno.getNombreCompleto().toLowerCase(Locale.getDefault()).contains(lowerCaseText)) {
                    listaAlumnosFiltrada.add(alumno);
                }
            }
        }
        listaAlumnosFiltrada.sort((a1, a2) -> a1.getNombreCompleto().compareToIgnoreCase(a2.getNombreCompleto()));
        alumnosAdapter.notifyDataSetChanged();
    }

    private void limpiarCamposEscaneo() {
        txtResultado.setText("");
        txtNombreAlumno.setText("Nombre del alumno aparecerá aquí");
    }

    private void cargarAlumnosDelDia() {
        @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d("API_CARGAR_DIA", response);
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        if (success) {
                            JSONArray alumnosArray = json.getJSONArray("alumnos");
                            listaAlumnosAsistencia.clear();
                            for (int i = 0; i < alumnosArray.length(); i++) {
                                JSONObject alumnoJson = alumnosArray.getJSONObject(i);
                                String rut = alumnoJson.optString("rut_alumno", "N/A");
                                String nombre = alumnoJson.optString("nombre_completo_alumno", "N/A");
                                String horaEntrada = alumnoJson.optString("hora_entrada", null);
                                String horaSalida = alumnoJson.optString("hora_salida", null);
                                String nombreCurso = alumnoJson.optString("nombre_curso", "N/A");

                                Almuerzo_activity.AlumnoAsistencia alumno = new Almuerzo_activity.AlumnoAsistencia(rut, nombre, horaEntrada, horaSalida, nombreCurso);
                                listaAlumnosAsistencia.add(alumno);
                            }
                            filtrarAlumnos(etBuscarRut.getText().toString());
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        } else {
                            listaAlumnosAsistencia.clear();
                            filtrarAlumnos(etBuscarRut.getText().toString());
                            Toast.makeText(this, "No se encontraron registros de asistencia para hoy: " + message, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar JSON cargarAlumnosDelDia: " + response, e);
                        Toast.makeText(this, "Error al procesar datos de alumnos del día. Formato inválido.", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR_CARGAR_DIA", "Error de conexión al cargar alumnos del día: " + error.toString(), error);
                    Toast.makeText(this, "Error de conexión al cargar alumnos del día. Verifica tu conexión.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "obtener_alumnos_dia");
                params.put("fecha_servicio", fechaActual);
                params.put("tipo_servicio", String.valueOf(ID_TSERVICIO));
                return params;
            }
        };
        requestQueue.add(request);
    }
}
