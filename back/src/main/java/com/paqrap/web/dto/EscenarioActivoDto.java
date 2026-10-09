package com.paqrap.web.dto;

public class EscenarioActivoDto {
    private String runId;
    private String escenario;
    private String estado;
    private String topic;

    public EscenarioActivoDto() {}

    public EscenarioActivoDto(String runId, String escenario, String estado, String topic) {
        this.runId = runId;
        this.escenario = escenario;
        this.estado = estado;
        this.topic = topic;
    }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getEscenario() { return escenario; }
    public void setEscenario(String escenario) { this.escenario = escenario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
