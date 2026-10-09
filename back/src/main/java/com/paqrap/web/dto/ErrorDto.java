package com.paqrap.web.dto;

public class ErrorDto {
    private String error;
    private String mensaje;

    public ErrorDto() {}

    public ErrorDto(String error, String mensaje) {
        this.error = error;
        this.mensaje = mensaje;
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
