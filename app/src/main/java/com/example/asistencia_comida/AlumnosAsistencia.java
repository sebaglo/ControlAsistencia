package com.example.asistencia_comida;// AlumnoAsistencia.java

public class AlumnosAsistencia {
    private String rut;
    private String nombreCompleto;
    private String horaEntrada;
    private String horaSalida;
    private String idCurso; // Si lo tienes y lo necesitas, si no, puedes eliminarlo

    // Constructor
    public AlumnosAsistencia(String rut, String nombreCompleto, String horaEntrada, String horaSalida, String idCurso) {
        this.rut = rut;
        this.nombreCompleto = nombreCompleto;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.idCurso = idCurso;
    }

    // Getters (métodos para obtener los valores de los campos)
    public String getRut() {
        return rut;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getHoraEntrada() {
        return horaEntrada;
    }

    public String getHoraSalida() {
        return horaSalida;
    }

    public String getIdCurso() {
        return idCurso;
    }

    // Setters (métodos para cambiar los valores de los campos)
    public void setRut(String rut) {
        this.rut = rut;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void setHoraEntrada(String horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public void setHoraSalida(String horaSalida) {
        this.horaSalida = horaSalida;
    }

    public void setIdCurso(String idCurso) {
        this.idCurso = idCurso;
    }

    // Método toString()
    // Este método es crucial para que el ArrayAdapter simple_list_item_1
    // sepa cómo mostrar tu objeto AlumnoAsistencia como un String en el ListView.
    @Override
    public String toString() {
        String estado = "Sin registro hoy";

        // Verifica si hay hora de entrada y formatéala
        if (horaEntrada != null && !horaEntrada.isEmpty() && !horaEntrada.equalsIgnoreCase("null")) {
            estado = "Entrada: " + horaEntrada;
            // Si también hay hora de salida, añádela
            if (horaSalida != null && !horaSalida.isEmpty() && !horaSalida.equalsIgnoreCase("null")) {
                estado += " | Salida: " + horaSalida;
            } else {
                estado += " (Salida Pendiente)";
            }
        }
        // Puedes añadir el curso si lo necesitas para mostrarlo en el ListView
        String cursoInfo = (idCurso != null && !idCurso.isEmpty() && !idCurso.equalsIgnoreCase("null")) ? " - Curso: " + idCurso : "";

        return nombreCompleto + " (" + rut + ") " + cursoInfo + " - " + estado;
    }
}