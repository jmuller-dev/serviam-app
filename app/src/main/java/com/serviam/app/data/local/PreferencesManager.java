package com.serviam.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "ServiamAppPrefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_USER_DNI = "user_dni";
    private static final String KEY_USER_ROL = "user_rol";
    private static final String KEY_SELECTED_AGRUPACION = "selected_agrupacion";
    private static final String KEY_USER_NOMBRE = "user_nombre";
    private static final String KEY_RECORDAR_DNI = "recordar_dni";

    private final SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setSession(String uid, String dni, String rol, String agrupacion, String nombre, boolean recordar) {
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_UID, uid)
                .putString(KEY_USER_DNI, dni)
                .putString(KEY_USER_ROL, rol)
                .putString(KEY_SELECTED_AGRUPACION, agrupacion)
                .putString(KEY_USER_NOMBRE, nombre)
                .putBoolean(KEY_RECORDAR_DNI, recordar)
                .apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUserUid() {
        return sharedPreferences.getString(KEY_USER_UID, null);
    }

    public String getUserDni() {
        return sharedPreferences.getString(KEY_USER_DNI, null);
    }

    public String getUserRol() {
        return sharedPreferences.getString(KEY_USER_ROL, null);
    }

    public String getUserNombre() {
        return sharedPreferences.getString(KEY_USER_NOMBRE, "");
    }

    public String getSelectedAgrupacion() {
        return sharedPreferences.getString(KEY_SELECTED_AGRUPACION, null);
    }

    public void setSelectedAgrupacion(String agrupacion) {
        sharedPreferences.edit().putString(KEY_SELECTED_AGRUPACION, agrupacion).apply();
    }

    public void setUserNombre(String nombre) {
        sharedPreferences.edit().putString(KEY_USER_NOMBRE, nombre).apply();
    }

    public boolean isRecordarDni() {
        return sharedPreferences.getBoolean(KEY_RECORDAR_DNI, false);
    }

    public void clearSession() {
        boolean recordar = isRecordarDni();
        String dni = getUserDni();
        SharedPreferences.Editor editor = sharedPreferences.edit().clear();
        if (recordar) {
            editor.putBoolean(KEY_RECORDAR_DNI, true)
                  .putString(KEY_USER_DNI, dni);
        }
        editor.apply();
    }

    public void clearSessionCompletely() {
        sharedPreferences.edit().clear().apply();
    }
}
