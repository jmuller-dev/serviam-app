package com.serviam.app.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "usuarios")
public class Usuario {
    @PrimaryKey
    @NonNull
    private String uid; // Firebase Auth UID
    private String nombre;
    private String dni;
    private String email;
    private String rol; // admin, capitan, chico
    private String agrupacion; // halcones, juanas, ambas

    public Usuario() {}

    @Ignore
    public Usuario(@NonNull String uid, String nombre, String dni, String email, String rol, String agrupacion) {
        this.uid = uid;
        this.nombre = nombre;
        this.dni = dni;
        this.email = email;
        this.rol = rol;
        this.agrupacion = agrupacion;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getAgrupacion() { return agrupacion; }
    public void setAgrupacion(String agrupacion) { this.agrupacion = agrupacion; }
}
