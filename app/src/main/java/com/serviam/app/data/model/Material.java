package com.serviam.app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "materiales")
public class Material {
    @PrimaryKey
    @NonNull
    private String id; // ID del archivo/material
    private String nombre;
    private String categoria; // mística, charlas, formación, oraciones
    private boolean visibleParaChicos; // visibilidad (true: visible / false: solo capitanes)
    private String urlArchivo; // URL del PDF o archivo en Firebase Storage
    private String contenido; // Texto plano (letras de canciones, oraciones, descripción)
    private String tipoContenido; // TEXTO, PDF, ENLACE
    private long timestamp; // Fecha de subida

    public Material() {}

    @Ignore
    public Material(@NonNull String id, String nombre, String categoria, boolean visibleParaChicos, String urlArchivo, long timestamp) {
        this(id, nombre, categoria, visibleParaChicos, urlArchivo, "", "PDF", timestamp);
    }

    @Ignore
    public Material(@NonNull String id, String nombre, String categoria, boolean visibleParaChicos, String urlArchivo, String contenido, String tipoContenido, long timestamp) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.visibleParaChicos = visibleParaChicos;
        this.urlArchivo = urlArchivo;
        this.contenido = contenido;
        this.tipoContenido = tipoContenido;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public boolean isVisibleParaChicos() { return visibleParaChicos; }
    public void setVisibleParaChicos(boolean visibleParaChicos) { this.visibleParaChicos = visibleParaChicos; }

    public String getUrlArchivo() { return urlArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getTipoContenido() { return tipoContenido != null ? tipoContenido : "TEXTO"; }
    public void setTipoContenido(String tipoContenido) { this.tipoContenido = tipoContenido; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
