package com.paqrap.ingesta;

import com.paqrap.model.Order;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ArchivoPedidosStore {
    private final Map<String, List<Order>> storage = new ConcurrentHashMap<>();

    public void guardar(String archivoId, List<Order> pedidos) {
        storage.put(archivoId, pedidos);
    }

    public List<Order> obtener(String archivoId) {
        return storage.get(archivoId);
    }
}
