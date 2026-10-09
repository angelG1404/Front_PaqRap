package com.paqrap.web.rest;

import com.paqrap.ingesta.ArchivoPedidosStore;
import com.paqrap.ingesta.OrderRecordParser;
import com.paqrap.model.Order;
import com.paqrap.web.dto.ArchivoSubidoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

    @Autowired
    private ArchivoPedidosStore archivoStore;

    @PostMapping("/pedidos")
    public ResponseEntity<ArchivoSubidoDto> subirArchivoPedidos(@RequestParam("file") MultipartFile file) {
        String archivoId = "arch-" + System.currentTimeMillis();
        List<Order> pedidos = new ArrayList<>();
        List<ArchivoSubidoDto.ErrorItem> errores = new ArrayList<>();

        OrderRecordParser parser = new OrderRecordParser();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank() || line.startsWith("#")) continue;
                try {
                    Order order = parser.parse(line.trim());
                    pedidos.add(order);
                } catch (Exception e) {
                    errores.add(new ArchivoSubidoDto.ErrorItem(lineNum, e.getMessage()));
                }
            }
        } catch (Exception e) {
            errores.add(new ArchivoSubidoDto.ErrorItem(0, "Error al leer el archivo: " + e.getMessage()));
        }

        archivoStore.guardar(archivoId, pedidos);
        ArchivoSubidoDto response = new ArchivoSubidoDto(archivoId, pedidos.size(), errores);
        return ResponseEntity.ok(response);
    }
}
