package com.paqrap.web.rest;

import com.paqrap.config.ScenarioDefaults;
import com.paqrap.ingesta.ArchivoBloqueosStore;
import com.paqrap.ingesta.ArchivoPedidosStore;
import com.paqrap.ingesta.OrderRecordParser;
import com.paqrap.model.Order;
import com.paqrap.simulacion.BloqueoSim;
import com.paqrap.web.dto.ArchivoSubidoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

    @Autowired
    private ArchivoPedidosStore archivoStore;

    @Autowired
    private ArchivoBloqueosStore bloqueosStore;

    @PostMapping("/pedidos")
    public ResponseEntity<ArchivoSubidoDto> subirArchivoPedidos(@RequestParam("file") MultipartFile file) {
        String archivoId = "arch-" + System.currentTimeMillis();
        List<Order> pedidos = new ArrayList<>();
        List<ArchivoSubidoDto.ErrorItem> errores = new ArrayList<>();

        OrderRecordParser parser = new OrderRecordParser();
        leerLineas(file, errores, (linea, numero) -> {
            try {
                pedidos.add(parser.parse(linea));
            } catch (Exception e) {
                errores.add(new ArchivoSubidoDto.ErrorItem(numero, e.getMessage()));
            }
        });

        archivoStore.guardar(archivoId, pedidos);
        return ResponseEntity.ok(new ArchivoSubidoDto(archivoId, pedidos.size(), errores));
    }

    /** Archivo opcional de bloqueos: DDdHHhMMm-DDdHHhMMm:x1,y1,x2,y2,... con tramos horizontales o verticales. */
    @PostMapping("/bloqueos")
    public ResponseEntity<ArchivoSubidoDto> subirArchivoBloqueos(@RequestParam("file") MultipartFile file) {
        String archivoId = "blq-" + System.currentTimeMillis();
        List<String> validas = new ArrayList<>();
        List<ArchivoSubidoDto.ErrorItem> errores = new ArrayList<>();
        leerLineas(file, errores, (linea, numero) -> {
            String error = validarBloqueo(linea);
            if (error == null) validas.add(linea);
            else errores.add(new ArchivoSubidoDto.ErrorItem(numero, error));
        });
        bloqueosStore.guardar(archivoId, validas);
        return ResponseEntity.ok(new ArchivoSubidoDto(archivoId, validas.size(), errores));
    }

    static String validarBloqueo(String linea) {
        try {
            BloqueoSim b = BloqueoSim.desdeLinea("tmp", linea);
            for (int[] p : b.vertices()) {
                if (p[0] < 0 || p[0] > ScenarioDefaults.GRID_ANCHO || p[1] < 0 || p[1] > ScenarioDefaults.GRID_ALTO) {
                    return "Coordenadas fuera de la grilla (" + p[0] + "," + p[1] + ").";
                }
            }
            if (b.finHoras() <= b.inicioHoras()) return "El fin del bloqueo debe ser posterior a su inicio.";
            return null;
        } catch (Exception e) {
            return "Formato inválido. Usa DDdHHhMMm-DDdHHhMMm:x1,y1,x2,y2,... (" + e.getMessage() + ")";
        }
    }

    interface LineaConsumer {
        void aceptar(String linea, int numero);
    }

    static void leerLineas(MultipartFile file, List<ArchivoSubidoDto.ErrorItem> errores, LineaConsumer consumidor) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String limpia = line.replace("﻿", "").trim();
                if (limpia.isEmpty() || limpia.startsWith("#")) continue;
                consumidor.aceptar(limpia, lineNum);
            }
        } catch (IOException e) {
            errores.add(new ArchivoSubidoDto.ErrorItem(0, "Error al leer el archivo: " + e.getMessage()));
        }
    }
}
