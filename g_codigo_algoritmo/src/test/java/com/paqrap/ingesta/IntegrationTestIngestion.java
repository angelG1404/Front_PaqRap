package com.paqrap.ingesta;

import com.paqrap.simulator.ScenarioGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IntegrationTestIngestion {

    @Test
    public void testLoadData() {
        ScenarioDataLoader loader = new ScenarioDataLoader();
        
        System.out.println("Iniciando carga de archivos...");
        
        // Mapeamos los archivos reales a los parámetros del loader
        ScenarioGenerator.Scenario scenario = loader.loadScenario(
            "data/ventas.txt",      // Pedidos
            "data/bloqueo.txt",     // Bloqueos
            null,                   // Mantenimientos
            null                    // Averías
        );
        
        assertNotNull(scenario);
        System.out.println("--- RESULTADO DE INGESTA ---");
        System.out.println("Pedidos cargados: " + scenario.orders.size());
        System.out.println("Bloqueos cargados: " + scenario.roadblocks.size());
    }
}
