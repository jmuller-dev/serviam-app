package com.serviam.app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "campamentos")
public class Campamento {
    @PrimaryKey
    @NonNull
    private String id; // ID único del campamento
    private String nombre;
    private String fechaInicio;
    private String fechaFin;
    private boolean activo; // true si está en curso

    // División Mayor
    private String mayorEquipo1;
    private String mayorEquipo2;
    private int mayorPuntajeEquipo1;
    private int mayorPuntajeEquipo2;

    // División Menor
    private String menorEquipo1;
    private String menorEquipo2;
    private int menorPuntajeEquipo1;
    private int menorPuntajeEquipo2;

    public Campamento() {}

    @Ignore
    public Campamento(@NonNull String id, String nombre, String fechaInicio, String fechaFin, boolean activo,
                      String mayorEquipo1, String mayorEquipo2, int mayorPuntajeEquipo1, int mayorPuntajeEquipo2,
                      String menorEquipo1, String menorEquipo2, int menorPuntajeEquipo1, int menorPuntajeEquipo2) {
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.activo = activo;
        this.mayorEquipo1 = mayorEquipo1;
        this.mayorEquipo2 = mayorEquipo2;
        this.mayorPuntajeEquipo1 = mayorPuntajeEquipo1;
        this.mayorPuntajeEquipo2 = mayorPuntajeEquipo2;
        this.menorEquipo1 = menorEquipo1;
        this.menorEquipo2 = menorEquipo2;
        this.menorPuntajeEquipo1 = menorPuntajeEquipo1;
        this.menorPuntajeEquipo2 = menorPuntajeEquipo2;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getMayorEquipo1() { return mayorEquipo1; }
    public void setMayorEquipo1(String mayorEquipo1) { this.mayorEquipo1 = mayorEquipo1; }

    public String getMayorEquipo2() { return mayorEquipo2; }
    public void setMayorEquipo2(String mayorEquipo2) { this.mayorEquipo2 = mayorEquipo2; }

    public int getMayorPuntajeEquipo1() { return mayorPuntajeEquipo1; }
    public void setMayorPuntajeEquipo1(int mayorPuntajeEquipo1) { this.mayorPuntajeEquipo1 = mayorPuntajeEquipo1; }

    public int getMayorPuntajeEquipo2() { return mayorPuntajeEquipo2; }
    public void setMayorPuntajeEquipo2(int mayorPuntajeEquipo2) { this.mayorPuntajeEquipo2 = mayorPuntajeEquipo2; }

    public String getMenorEquipo1() { return menorEquipo1; }
    public void setMenorEquipo1(String menorEquipo1) { this.menorEquipo1 = menorEquipo1; }

    public String getMenorEquipo2() { return menorEquipo2; }
    public void setMenorEquipo2(String menorEquipo2) { this.menorEquipo2 = menorEquipo2; }

    public int getMenorPuntajeEquipo1() { return menorPuntajeEquipo1; }
    public void setMenorPuntajeEquipo1(int menorPuntajeEquipo1) { this.menorPuntajeEquipo1 = menorPuntajeEquipo1; }

    public int getMenorPuntajeEquipo2() { return menorPuntajeEquipo2; }
    public void setMenorPuntajeEquipo2(int menorPuntajeEquipo2) { this.menorPuntajeEquipo2 = menorPuntajeEquipo2; }
}
