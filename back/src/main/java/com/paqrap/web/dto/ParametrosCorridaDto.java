package com.paqrap.web.dto;

import java.util.Map;

public class ParametrosCorridaDto {
    private String algoritmo;
    private String fechaInicio;
    private String archivoPedidosId;
    private String archivoBloqueosId;
    private Map<String, Integer> flota;
    /** Reglas de avería, almacenes y capacidades de unidad; opcional y parcial (lo omitido usa los valores por defecto). */
    private com.paqrap.config.ConfiguracionCorrida configuracion;
    private Map<String, Double> umbralesSemaforo;

    public ParametrosCorridaDto() {}

    public String getAlgoritmo() { return algoritmo; }
    public void setAlgoritmo(String algoritmo) { this.algoritmo = algoritmo; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getArchivoPedidosId() { return archivoPedidosId; }
    public void setArchivoPedidosId(String archivoPedidosId) { this.archivoPedidosId = archivoPedidosId; }

    public String getArchivoBloqueosId() { return archivoBloqueosId; }
    public void setArchivoBloqueosId(String archivoBloqueosId) { this.archivoBloqueosId = archivoBloqueosId; }

    public com.paqrap.config.ConfiguracionCorrida getConfiguracion() { return configuracion; }
    public void setConfiguracion(com.paqrap.config.ConfiguracionCorrida configuracion) { this.configuracion = configuracion; }

    public Map<String, Integer> getFlota() { return flota; }
    public void setFlota(Map<String, Integer> flota) { this.flota = flota; }

    public Map<String, Double> getUmbralesSemaforo() { return umbralesSemaforo; }
    public void setUmbralesSemaforo(Map<String, Double> umbralesSemaforo) { this.umbralesSemaforo = umbralesSemaforo; }
}
