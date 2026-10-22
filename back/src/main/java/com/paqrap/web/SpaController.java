package com.paqrap.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Cuando el front compilado se sirve desde este mismo JAR (static/), las rutas del SPA deben
 * devolver index.html al recargar la página o al abrirlas desde otro dispositivo.
 */
@Controller
public class SpaController {

    @GetMapping({"/simulacion", "/simulacion/parametros", "/simulacion/mapa", "/operacion-diaria", "/pedidos", "/dashboard"})
    public String index() {
        return "forward:/index.html";
    }
}
