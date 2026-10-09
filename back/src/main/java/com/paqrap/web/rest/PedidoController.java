package com.paqrap.web.rest;

import com.paqrap.model.Location;
import com.paqrap.model.Order;
import com.paqrap.simulacion.Corrida;
import com.paqrap.simulacion.ScenarioRunner;
import com.paqrap.web.dto.ErrorDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private ScenarioRunner scenarioRunner;

    @PostMapping
    public ResponseEntity<?> registrarPedido(@RequestBody Map<String, Object> body) {
        Corrida diario = scenarioRunner.getByType("DIARIO");
        if (diario == null || !"EN_CURSO".equals(diario.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorDto("CONFLICT", "La operación diaria no está activa."));
        }

        try {
            double x = Double.parseDouble(body.get("x").toString());
            double y = Double.parseDouble(body.get("y").toString());
            String clienteId = body.get("clienteId").toString();
            int cantidad = Integer.parseInt(body.get("cantidad").toString());
            double horizonte = Double.parseDouble(body.get("horizonteHoras").toString());

            double currentSimTimeHours = diario.getClock().getCurrentMinutes() / 60.0;
            Location dest = new Location(clienteId, clienteId, x, y, Location.LocationType.CUSTOMER);
            String orderId = "ORD-" + System.currentTimeMillis();

            Order order = new Order(orderId, dest, cantidad, currentSimTimeHours, horizonte);
            scenarioRunner.registrarPedidoDiario(order);

            LocalDateTime regTime = diario.getBaseRealTime() != null ? diario.getBaseRealTime().plusMinutes((long) (currentSimTimeHours * 60)) : LocalDateTime.now();
            LocalDateTime deadlineTime = regTime.plusHours((long) horizonte);

            Map<String, Object> resp = Map.of(
                    "id", orderId,
                    "clienteId", clienteId,
                    "x", x,
                    "y", y,
                    "cantidad", cantidad,
                    "registradoEn", regTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "deadline", deadlineTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "estado", "PENDIENTE"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto("BAD_REQUEST", "Datos de pedido inválidos: " + e.getMessage()));
        }
    }
}
