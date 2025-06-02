package com.example.asistencia_comida;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class ServicioRequest extends StringRequest {
    private static final String URL = "https://tusitio.com/servicio.php";
    private final Map<String, String> params;

    public ServicioRequest(String fecha, String idTipoServicio, Response.Listener<String> listener) {
        super(Method.POST, URL, listener, null);
        params = new HashMap<>();
        params.put("accion", "obtener_servicio");
        params.put("fecha", fecha);
        params.put("id_tservicio", idTipoServicio); // "1" para desayuno, "2" para almuerzo
    }

    @Override
    protected Map<String, String> getParams() {
        return params;
    }
}
