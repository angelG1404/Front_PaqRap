package com.paqrap.web.rest;

import com.paqrap.ingesta.ArchivoPedidosStore;
import com.paqrap.model.Order;
import com.paqrap.simulacion.Corrida;
import com.paqrap.simulacion.ScenarioRunner;
import com.paqrap.simulacion.SnapshotMapper;
import com.paqrap.web.dto.EscenarioActivoDto;
import com.paqrap.web.dto.ErrorDto;
import com.paqrap.web.dto.ParametrosCorridaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/escenarios")
public class EscenarioController {

    @Autowired
    private ScenarioRunner scenarioRunner;

    @Autowired
    private ArchivoPedidosStore archivoStore;

    @PostMapping("/{tipo}/iniciar")
    public ResponseEntity<?> iniciarEscenario(@PathVariable String tipo, @RequestBody ParametrosCorridaDto params) {
        try {
            List<Order> initialOrders = null;
            if (params.getArchivoPedidosId() != null) {
                initialOrders = archivoStore.obtener(params.getArchivoPedidosId());
            }
            Corrida corrida = scenarioRunner.iniciarCorrida(tipo, params, initialOrders);
            Map<String, Object> resp = Map.of(
                    "runId", corrida.getRunId(),
                    "escenario", corrida.getEscenario(),
                    "estado", corrida.getEstado(),
                    "topic", corrida.getTopic()
            );
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorDto("CONFLICT", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto("BAD_REQUEST", e.getMessage()));
        }
    }

    @GetMapping("/activos")
    public ResponseEntity<List<EscenarioActivoDto>> getActivos() {
        return ResponseEntity.ok(scenarioRunner.getActivos());
    }

    @GetMapping("/{runId}/estado")
    public ResponseEntity<?> getEstado(@PathVariable String runId) {
        Corrida c = scenarioRunner.getByRunId(runId);
        if (c == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDto("NOT_FOUND", "Corrida no encontrada"));
        }
        return ResponseEntity.ok(SnapshotMapper.map(c));
    }
}
