package com.example.asistencia_comida;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

public class Desayuno extends AppCompatActivity {

    private Button btnEscanear, btnRegresar;
    private ArrayList<Almuerzo_activity.AlumnoAsistencia> listaAlumnosAsistencia;

    private RequestQueue requestQueue;

    private static final String API_URL = "http://172.100.8.99/AppColación/conexion.php"; // CONEXIÓN BASE DE DATOS.
    private static final int ID_TSERVICIO = 2; // ID TIPO DE SERVICIO (DESAYUNO).
    private boolean registroEnProceso = false;

    public static class AlumnoAsistencias {
        private String rut;
        private String nombreCompleto;
        private String horaEntrada;
        private String horaSalida;
        private String nombreCurso;

        public AlumnoAsistencias(String rut, String nombreCompleto, String horaEntrada, String horaSalida, String nombreCurso) {
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
        setContentView(R.layout.almuerzo_activity);

        requestQueue = Volley.newRequestQueue(this);

        btnEscanear = findViewById(R.id.btnEscanearDesayuno);
        btnRegresar = findViewById(R.id.btnRegresarDesayuno);


        listaAlumnosAsistencia = new ArrayList<>();
        btnRegresar.setOnClickListener(v -> onBackPressed());

        btnEscanear.setOnClickListener(v -> {
            if (registroEnProceso) {
                Toast.makeText(this, "Procesando asistencia, espera un momento...", Toast.LENGTH_SHORT).show();
                return;
            }
            limpiarCamposEscaneo();
            iniciarEscaneo("Escanea el RUT del alumno");
        });

        cargarAlumnosDelDia();
    }

    //INICIO DE LA CAMARA MEDIANTE ESACNEO DE BARRA.
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

    //CARGAR DATOS DE LOS ALUMNOS DESDE LA BASE DE DATOS.
    private void obtenerDatosAlumno(final String rut) {
        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d("API_GET_ALUMNO_DATA", response);
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");

                        if (success) {
                            String nombreCompleto = json.optString("nombre_completo_alumno", "Nombre no disponible");
                            String nombreCurso = json.optString("nombre_curso", "Curso no disponible");

                            @SuppressLint("SimpleDateFormat") final String horaActual = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                            @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                            obtenerEstadoYDecidirAccion(rut, fechaActual, horaActual, nombreCompleto, nombreCurso);

                        } else {
                            String message = json.getString("message");
                            Toast.makeText(this, "Error al obtener datos del alumno: " + message, Toast.LENGTH_LONG).show();
                            limpiarCamposEscaneo();
                            registroEnProceso = false;
                            showErrorDialog("Alumno no encontrado", message);
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar JSON obtener_alumno_data: " + response, e);
                        Toast.makeText(this, "Error al procesar respuesta del servidor para datos de alumno.", Toast.LENGTH_LONG).show();
                        limpiarCamposEscaneo();
                        registroEnProceso = false;
                        showErrorDialog("Error de JSON", "Respuesta del servidor inválida al obtener datos de alumno.");
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR_GET_ALUMNO_DATA", "Error de red al obtener datos de alumno: " + error.toString(), error);
                    Toast.makeText(this, "Error de conexión al obtener datos de alumno. Verifica tu conexión.", Toast.LENGTH_LONG).show();
                    limpiarCamposEscaneo();
                    registroEnProceso = false;
                    showErrorDialog("Error de Conexión", "No se pudo conectar con el servidor para obtener datos de alumno.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "obtener_alumno_data");
                params.put("rut_alumno", rut);
                return params;
            }
        };
        requestQueue.add(request);
    }

    //SE CREA LA DECISIÓN DE QUE SI SE REGISTRA LA ASISTENCIA O SE ELIMINA (SALIDA)
    private void obtenerEstadoYDecidirAccion(String rut, String fechaActual, String horaActual, String nombreAlumno, String nombreCurso) {
        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    registroEnProceso = false;
                    Log.d("API_RESPONSE_ESTADO", response);

                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        String horaEntradaExistente = json.optString("hora_entrada", null);
                        String horaSalidaExistente = json.optString("hora_salida", null);
                        String finalNombreAlumno = json.optString("nombre_completo_alumno", nombreAlumno);
                        String finalNombreCurso = json.optString("nombre_curso", nombreCurso);

                        Almuerzo_activity.AlumnoAsistencia currentAlumno = new Almuerzo_activity.AlumnoAsistencia(rut, finalNombreAlumno, horaEntradaExistente, horaSalidaExistente, finalNombreCurso);
                        if (success) {
                            if (currentAlumno.tieneEntradaRegistrada() && !currentAlumno.tieneSalidaRegistrada()) {
                                Toast.makeText(this, "Registrando salida para " + currentAlumno.getNombreCompleto() + "...", Toast.LENGTH_LONG).show();
                                registrarSalida(rut, horaActual);
                            } else if (currentAlumno.tieneEntradaRegistrada() && currentAlumno.tieneSalidaRegistrada()) {
                                Toast.makeText(this, "Asistencia de " + currentAlumno.getNombreCompleto() + " (" + rut + ") ya completa hoy.", Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                                showRegistroDialog(currentAlumno.getNombreCompleto(), currentAlumno.getRut(), fechaActual, currentAlumno.getHoraEntrada(), currentAlumno.getHoraSalida(), currentAlumno.getNombreCurso());
                            } else {
                                Toast.makeText(this, "Estado inesperado: " + message, Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                            }
                        } else {
                            if (message.contains("Ya existe un registro de entrada para este alumno en esta fecha.")) {
                                Toast.makeText(this, "Entrada ya registrada para " + currentAlumno.getNombreCompleto() + " (" + rut + "). Hora: " + currentAlumno.getHoraEntrada(), Toast.LENGTH_LONG).show();
                                limpiarCamposEscaneo();
                                showRegistroDialog(currentAlumno.getNombreCompleto(), currentAlumno.getRut(), fechaActual, currentAlumno.getHoraEntrada(), currentAlumno.getHoraSalida(), currentAlumno.getNombreCurso());
                            } else if (message.contains("Alumno encontrado, pero sin asistencia para hoy.")) {
                                Toast.makeText(this, "Registrando entrada para " + currentAlumno.getNombreCompleto() + "...", Toast.LENGTH_LONG).show();
                                hacerRegistroAsistencia(rut, ID_TSERVICIO, horaActual);
                            } else if (message.contains("Error: El RUT del alumno no existe en la base de datos.")) {
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
                    registroEnProceso = false;
                    Log.e("VOLLEY_ERROR_ESTADO", "Error de red al obtener estado: " + error.toString(), error);
                    Toast.makeText(Desayuno.this, "Error de red al obtener estado. Verifica tu conexión.", Toast.LENGTH_SHORT).show();
                    limpiarCamposEscaneo();
                    showErrorDialog("Error de Conexión", "No se pudo conectar con el servidor para obtener el estado.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("accion", "obtener_asistencia");
                params.put("rut_alumno", rut);
                params.put("fecha_servicio", fechaActual);
                params.put("tipo_servicio", String.valueOf(ID_TSERVICIO));
                return params;
            }
        };
        requestQueue.add(request);
    }

    //SE REGISTRA LA SALIDA EN LA BASE DE DATOS.
    private void registrarSalida(final String rut, final String horaSalida) {
        @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    registroEnProceso = false;
                    Log.d("API_RESPONSE_SALIDA", response);
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String mensaje = json.getString("message");

                        String horaEntradaConfirmada = json.optString("hora_entrada", null);
                        String horaSalidaConfirmada = json.optString("hora_salida", horaSalida);
                        String nombreCompleto = json.optString("nombre_completo_alumno", "Alumno");
                        String nombreCurso = json.optString("nombre_curso", "N/A");

                        if (success) {
                            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
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
                    registroEnProceso = false;
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

    //SE REALIZA LA INSERSION DE LOS DATOS EN LA TABLA ASISTENCIA.
    private void hacerRegistroAsistencia(final String rut, final int idTServicio, final String horaEntrada) {
        @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    registroEnProceso = false;
                    Log.d("API_RESPONSE_ENTRADA", response);

                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        String horaEntradaConfirmada = json.optString("hora_entrada", horaEntrada);
                        String horaSalidaConfirmada = json.optString("hora_salida", null);
                        String nombreAlumnoResponse = json.optString("nombre_completo_alumno", "Alumno");
                        String nombreCurso = json.optString("nombre_curso", "N/A");

                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            showRegistroDialog(nombreAlumnoResponse, rut, fechaActual, horaEntradaConfirmada, horaSalidaConfirmada, nombreCurso);
                            limpiarCamposEscaneo();

                        } else {
                            Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
                            showErrorDialog("Error al registrar", message);
                            limpiarCamposEscaneo();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar respuesta JSON al registrar entrada: " + response, e);
                        Toast.makeText(this, "Error al procesar datos del servidor", Toast.LENGTH_LONG).show();
                        limpiarCamposEscaneo();
                        showErrorDialog("Error de JSON", "Respuesta del servidor inválida al registrar entrada.");
                    }
                },
                error -> {
                    registroEnProceso = false;
                    Log.e("VOLLEY_ERROR_ENTRADA", "Error de red al registrar entrada: " + error.toString(), error);
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

    //MUESTRA EL DIALOGO CON LOS DATOS DE LOS ALUMNOS.
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

    //EN CASO DE ALGUN ERROR MOSTRARA ESTE DIALOGO.
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error: " + title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // El método limpiarCamposEscaneo() ya no necesita resetear ningún TextView/EditText.
    private void limpiarCamposEscaneo() {
    }

    // Carga inicial de alumnos del día (MANTENIDA)
    private void cargarAlumnosDelDia() {
        @SuppressLint("SimpleDateFormat") final String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        StringRequest request = new StringRequest(Request.Method.POST, API_URL,
                response -> {
                    Log.d("API_CARGAR_ALUMNOS_DIA", response);
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String message = json.getString("message");

                        if (success) {
                            JSONArray jsonAlumnos = json.getJSONArray("alumnos");
                            listaAlumnosAsistencia.clear(); // Limpia la lista antes de llenarla
                            for (int i = 0; i < jsonAlumnos.length(); i++) {
                                JSONObject alumnoJson = jsonAlumnos.getJSONObject(i);
                                String rut = alumnoJson.getString("rut_alumno");
                                String nombreCompleto = alumnoJson.getString("nombre_completo_alumno");
                                String horaEntrada = alumnoJson.optString("hora_entrada", null);
                                String horaSalida = alumnoJson.optString("hora_salida", null);
                                String nombreCurso = alumnoJson.optString("nombre_curso", "N/A");

                                listaAlumnosAsistencia.add(new Almuerzo_activity.AlumnoAsistencia(rut, nombreCompleto, horaEntrada, horaSalida, nombreCurso));
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            // Aquí podrías agregar un Log para ver cuántos alumnos se cargaron
                            Log.d("CARGA_ALUMNOS", "Alumnos cargados para el día: " + listaAlumnosAsistencia.size());
                        } else {
                            Toast.makeText(this, "Error al cargar alumnos: " + message, Toast.LENGTH_LONG).show();
                            listaAlumnosAsistencia.clear(); 
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "Error al procesar JSON cargarAlumnosDelDia: " + response, e);
                        Toast.makeText(this, "Error al procesar respuesta del servidor al cargar alumnos.", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR_CARGAR_ALUMNOS_DIA", "Error de red al cargar alumnos del día: " + error.toString(), error);
                    Toast.makeText(this, "Error de conexión al cargar alumnos del día. Verifica tu conexión.", Toast.LENGTH_LONG).show();
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
