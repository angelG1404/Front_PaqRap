package com.paqrap.web.rest;

import com.paqrap.ingesta.ArchivoBloqueosStore;
import com.paqrap.ingesta.ArchivoPedidosStore;
import com.paqrap.model.Order;
import com.paqrap.simulacion.Corrida;
import com.paqrap.simulacion.ScenarioRunner;
import com.paqrap.simulacion.SnapshotMapper;
import com.paqrap.web.dto.ArchivoSubidoDto;
import com.paqrap.web.dto.EscenarioActivoDto;
import com.paqrap.web.dto.ErrorDto;
import com.paqrap.web.dto.ParametrosCorridaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/escenarios")
public class EscenarioController {

    @Autowired
    private ScenarioRunner scenarioRunner;

    @Autowired
    private ArchivoPedidosStore archivoStore;

    @Autowired
    private ArchivoBloqueosStore bloqueosStore;

    @PostMapping("/{tipo}/iniciar")
    public ResponseEntity<?> iniciarEscenario(@PathVariable String tipo, @RequestBody ParametrosCorridaDto params) {
        try {
            List<Order> pedidos = List.of();
            if (params.getArchivoPedidosId() != null) {
                pedidos = archivoStore.obtener(params.getArchivoPedidosId());
                if (pedidos == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "El archivo de pedidos ya no está disponible; súbelo de nuevo.");
            }
            List<String> bloqueos = List.of();
            if (params.getArchivoBloqueosId() != null) {
                bloqueos = bloqueosStore.obtener(params.getArchivoBloqueosId());
                if (bloqueos == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "El archivo de bloqueos ya no está disponible; súbelo de nuevo.");
            }
            Corrida corrida = scenarioRunner.iniciarCorrida(tipo, params, pedidos, bloqueos);
            return ResponseEntity.ok(new EscenarioActivoDto(corrida.getRunId(), corrida.getEscenario(), corrida.getEstado(), corrida.getTopic()));
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
        }
    }

    @GetMapping("/activos")
    public ResponseEntity<List<EscenarioActivoDto>> getActivos() {
        return ResponseEntity.ok(scenarioRunner.getActivos());
    }

    @GetMapping("/{runId}/estado")
    public ResponseEntity<?> getEstado(@PathVariable String runId) {
        Corrida c = scenarioRunner.getByRunId(runId);
        if (c == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Corrida no encontrada");
        return ResponseEntity.ok(SnapshotMapper.map(c));
    }

    @PostMapping("/{runId}/detener")
    public ResponseEntity<?> detener(@PathVariable String runId) {
        Corrida c = scenarioRunner.detener(runId);
        if (c == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Corrida no encontrada");
        return ResponseEntity.ok(new EscenarioActivoDto(c.getRunId(), c.getEscenario(), c.getEstado(), c.getTopic()));
    }

    /** Avería en caliente: { "vehiculoId": "TA01", "tipo": 1 | 2 | 3 }. Sus pedidos pendientes se reasignan. */
    @PostMapping("/{runId}/averias")
    public ResponseEntity<?> registrarAveria(@PathVariable String runId, @RequestBody Map<String, Object> body) {
        Corrida c = scenarioRunner.getByRunId(runId);
        if (c == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Corrida no encontrada");
        try {
            String vehiculoId = String.valueOf(body.get("vehiculoId")).trim();
            int tipo = Integer.parseInt(String.valueOf(body.get("tipo")));
            return ResponseEntity.status(HttpStatus.CREATED).body(c.getMotor().registrarAveria(vehiculoId, tipo));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Avería inválida: " + e.getMessage());
        }
    }

    /** Carga un archivo de bloqueos en una corrida en curso (tiempos relativos al día 01 de la corrida). */
    @PostMapping("/{runId}/bloqueos")
    public ResponseEntity<?> cargarBloqueos(@PathVariable String runId, @RequestParam("file") MultipartFile file) {
        Corrida c = scenarioRunner.getByRunId(runId);
        if (c == null) return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Corrida no encontrada");
        if (!c.getMotor().enCurso()) return error(HttpStatus.CONFLICT, "CONFLICT", "La corrida ya terminó.");
        List<String> validas = new ArrayList<>();
        List<ArchivoSubidoDto.ErrorItem> errores = new ArrayList<>();
        ArchivoController.leerLineas(file, errores, (linea, numero) -> {
            String problema = ArchivoController.validarBloqueo(linea);
            if (problema == null) validas.add(linea);
            else errores.add(new ArchivoSubidoDto.ErrorItem(numero, problema));
        });
        int agregados = validas.isEmpty() ? 0 : scenarioRunner.agregarBloqueos(c.getMotor(), validas);
        return ResponseEntity.ok(new ArchivoSubidoDto(runId, agregados, errores));
    }

    private static ResponseEntity<ErrorDto> error(HttpStatus status, String codigo, String mensaje) {
        return ResponseEntity.status(status).body(new ErrorDto(codigo, mensaje));
    }
}
