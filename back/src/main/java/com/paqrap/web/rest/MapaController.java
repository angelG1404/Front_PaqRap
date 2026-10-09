package com.paqrap.web.rest;

import com.paqrap.config.ScenarioDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mapa")
public class MapaController {

    @GetMapping("/base")
    public ResponseEntity<Map<String, Object>> getMapaBase() {
        return ResponseEntity.ok(ScenarioDefaults.MAPA_BASE);
    }
}
