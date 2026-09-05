package com.serviam.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.serviam.app.data.local.PreferencesManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Ajustamos márgenes de insets para el logo central
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.logoSplash), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Retardo de 2 segundos antes de comprobar la sesión y navegar
        new Handler(Looper.getMainLooper()).postDelayed(this::verificarSesion, SPLASH_DELAY);
    }

    private void verificarSesion() {
        PreferencesManager prefs = new PreferencesManager(this);

        if (prefs.isLoggedIn()) {
            String rol = prefs.getUserRol();
            String agrupacion = prefs.getSelectedAgrupacion();
            
            Intent intent;
            // Si el usuario es capitán o admin, entra al Dashboard del Capitán
            if ("capitan".equalsIgnoreCase(rol) || "admin".equalsIgnoreCase(rol)) {
                intent = new Intent(SplashActivity.this, DashboardCapitanActivity.class);
            } else {
                // Si es chico, va a DashboardChicoActivity
                intent = new Intent(SplashActivity.this, DashboardChicoActivity.class);
            }
            intent.putExtra("AGRUPACION", agrupacion);
            startActivity(intent);
        } else {
            // Si no está registrado o logueado, va a la selección de agrupación
            Intent intent = new Intent(SplashActivity.this, SelectionActivity.class);
            startActivity(intent);
        }
        finish(); // Cierra el SplashActivity para que no quede en la pila
    }
}
