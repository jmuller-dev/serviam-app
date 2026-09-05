package com.serviam.app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "chicos")
public class Chico {
    @PrimaryKey
    @NonNull
    private String dni; // El DNI es único y sirve como clave primaria
    private String nombre;
    private String brigada; // halcones, conquistadores, pioneros, lenadores
    private int edad;
    private String fechaNacimiento;
    private String telefonoChico;
    private String telefonoPadres;
    private String asistencia; // Representación de asistencias (guardado como texto serializado)
    private boolean activo; // Estado de alta/baja
    private String agrupacion; // Grupo: halcones, juanas

    public Chico() {}

    @Ignore
    public Chico(@NonNull String dni, String nombre, String brigada, int edad, String fechaNacimiento, String telefonoChico, String telefonoPadres, String asistencia, boolean activo) {
        this.dni = dni;
        this.nombre = nombre;
        this.brigada = brigada;
        this.edad = edad;
        this.fechaNacimiento = fechaNacimiento;
        this.telefonoChico = telefonoChico;
        this.telefonoPadres = telefonoPadres;
        this.asistencia = asistencia;
        this.activo = activo;
    }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getBrigada() { return brigada; }
    public void setBrigada(String brigada) { this.brigada = brigada; }

    public int getEdad() {
        if (fechaNacimiento != null && !fechaNacimiento.trim().isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf;
                if (fechaNacimiento.contains("/")) {
                    sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US);
                } else if (fechaNacimiento.contains("-")) {
                    sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                } else {
                    return edad;
                }
                java.util.Date birthDate = sdf.parse(fechaNacimiento.trim());
                if (birthDate != null) {
                    java.util.Calendar birthCal = java.util.Calendar.getInstance();
                    birthCal.setTime(birthDate);
                    java.util.Calendar today = java.util.Calendar.getInstance();
                    int calcAge = today.get(java.util.Calendar.YEAR) - birthCal.get(java.util.Calendar.YEAR);
                    if (today.get(java.util.Calendar.DAY_OF_YEAR) < birthCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                        calcAge--;
                    }
                    if (calcAge >= 0) return calcAge;
                }
            } catch (Exception ignored) {}
        }
        return edad;
    }
    public void setEdad(int edad) { this.edad = edad; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getTelefonoChico() { return telefonoChico; }
    public void setTelefonoChico(String telefonoChico) { this.telefonoChico = telefonoChico; }

    public String getTelefonoPadres() { return telefonoPadres; }
    public void setTelefonoPadres(String telefonoPadres) { this.telefonoPadres = telefonoPadres; }

    public String getAsistencia() { return asistencia; }
    public void setAsistencia(String asistencia) { this.asistencia = asistencia; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getAgrupacion() { return agrupacion; }
    public void setAgrupacion(String agrupacion) { this.agrupacion = agrupacion; }
}
