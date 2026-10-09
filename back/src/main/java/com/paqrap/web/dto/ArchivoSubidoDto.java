package com.paqrap.web.dto;

import java.util.List;

public class ArchivoSubidoDto {
    private String archivoId;
    private int totalPedidos;
    private List<ErrorItem> errores;

    public ArchivoSubidoDto() {}

    public ArchivoSubidoDto(String archivoId, int totalPedidos, List<ErrorItem> errores) {
        this.archivoId = archivoId;
        this.totalPedidos = totalPedidos;
        this.errores = errores;
    }

    public String getArchivoId() { return archivoId; }
    public void setArchivoId(String archivoId) { this.archivoId = archivoId; }

    public int getTotalPedidos() { return totalPedidos; }
    public void setTotalPedidos(int totalPedidos) { this.totalPedidos = totalPedidos; }

    public List<ErrorItem> getErrores() { return errores; }
    public void setErrores(List<ErrorItem> errores) { this.errores = errores; }

    public static class ErrorItem {
        private int linea;
        private String mensaje;

        public ErrorItem() {}

        public ErrorItem(int linea, String mensaje) {
            this.linea = linea;
            this.mensaje = mensaje;
        }

        public int getLinea() { return linea; }
        public void setLinea(int linea) { this.linea = linea; }

        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    }
}
