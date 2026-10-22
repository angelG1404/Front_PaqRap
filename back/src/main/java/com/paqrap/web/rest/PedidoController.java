package com.paqrap.web.rest;

import com.paqrap.config.ScenarioDefaults;
import com.paqrap.simulacion.Corrida;
import com.paqrap.simulacion.ScenarioRunner;
import com.paqrap.web.dto.ErrorDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final Set<Integer> HORIZONTES_VALIDOS = Set.of(4, 8, 12, 18, 36);

    @Autowired
    private ScenarioRunner scenarioRunner;

    /** Registra un pedido en la operación diaria; el plazo se cuenta desde el registro. */
    @PostMapping
    public ResponseEntity<?> registrarPedido(@RequestBody Map<String, Object> body) {
        Corrida diario = scenarioRunner.getByType("DIARIO");
        if (diario == null || !diario.getMotor().enCurso()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorDto("CONFLICT", "La operación diaria no está activa."));
        }
        int x, y, cantidad, horizonte;
        String clienteId;
        try {
            x = Integer.parseInt(String.valueOf(body.get("x")));
            y = Integer.parseInt(String.valueOf(body.get("y")));
            cantidad = Integer.parseInt(String.valueOf(body.get("cantidad")));
            horizonte = (int) Double.parseDouble(String.valueOf(body.get("horizonteHoras")));
            clienteId = String.valueOf(body.get("clienteId")).trim();
        } catch (Exception e) {
            return invalido("Datos de pedido inválidos: " + e.getMessage());
        }
        if (x < 0 || x > ScenarioDefaults.GRID_ANCHO || y < 0 || y > ScenarioDefaults.GRID_ALTO) {
            return invalido("Las coordenadas deben estar dentro de la grilla (X 0–70, Y 0–50).");
        }
        if (clienteId.isEmpty() || "null".equals(clienteId)) return invalido("El ID de cliente es obligatorio.");
        if (cantidad <= 0) return invalido("La cantidad debe ser mayor que cero.");
        if (!HORIZONTES_VALIDOS.contains(horizonte)) return invalido("El plazo debe ser 4, 8, 12, 18 o 36 horas.");
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(diario.getMotor().registrarPedido(clienteId, x, y, cantidad, horizonte));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto("CONFLICT", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable String id) {
        for (Corrida c : scenarioRunner.todas()) {
            Map<String, Object> pedido = c.getMotor().buscarPedido(id);
            if (pedido != null) return ResponseEntity.ok(pedido);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto("NOT_FOUND", "Pedido no encontrado"));
    }

    private static ResponseEntity<ErrorDto> invalido(String mensaje) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("BAD_REQUEST", mensaje));
    }
}
