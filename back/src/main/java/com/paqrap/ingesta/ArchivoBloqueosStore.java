package com.paqrap.ingesta;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Líneas válidas de archivos de bloqueos subidos; cada corrida las convierte con sus propios ids. */
@Component
public class ArchivoBloqueosStore {
    private final Map<String, List<String>> storage = new ConcurrentHashMap<>();

    public void guardar(String archivoId, List<String> lineas) {
        storage.put(archivoId, List.copyOf(lineas));
    }

    public List<String> obtener(String archivoId) {
        return storage.get(archivoId);
    }
}
