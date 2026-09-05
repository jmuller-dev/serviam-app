package com.serviam.app.data.model;

public class Evento {
    private String id;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String hora;
    private String lugar;
    private String agrupacion;
    private String creador;

    public Evento() {
        // Constructor vacío requerido por Firestore
    }

    public Evento(String id, String titulo, String descripcion, String fecha, String hora, String lugar, String agrupacion, String creador) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.hora = hora;
        this.lugar = lugar;
        this.agrupacion = agrupacion;
        this.creador = creador;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public String getAgrupacion() { return agrupacion; }
    public void setAgrupacion(String agrupacion) { this.agrupacion = agrupacion; }

    public String getCreador() { return creador; }
    public void setCreador(String creador) { this.creador = creador; }
}
