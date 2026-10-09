package com.paqrap.web.dto;

import java.util.Map;

public class ParametrosCorridaDto {
    private String algoritmo;
    private String fechaInicio;
    private String archivoPedidosId;
    private Map<String, Integer> flota;
    private Map<String, Double> umbralesSemaforo;

    public ParametrosCorridaDto() {}

    public String getAlgoritmo() { return algoritmo; }
    public void setAlgoritmo(String algoritmo) { this.algoritmo = algoritmo; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getArchivoPedidosId() { return archivoPedidosId; }
    public void setArchivoPedidosId(String archivoPedidosId) { this.archivoPedidosId = archivoPedidosId; }

    public Map<String, Integer> getFlota() { return flota; }
    public void setFlota(Map<String, Integer> flota) { this.flota = flota; }

    public Map<String, Double> getUmbralesSemaforo() { return umbralesSemaforo; }
    public void setUmbralesSemaforo(Map<String, Double> umbralesSemaforo) { this.umbralesSemaforo = umbralesSemaforo; }
}
