package com.serviam.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Importamos la clase de Binding auto-generada para activity_selection.xml
import com.serviam.app.databinding.ActivitySelectionBinding;

public class SelectionActivity extends AppCompatActivity {

    // Variable para acceder a los componentes del diseño sin usar findViewById
    private ActivitySelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilita el diseño de borde a borde (pantalla completa detrás de las barras de estado/navegación)
        EdgeToEdge.enable(this);
        
        // Inflamos el XML de la actividad usando View Binding
        binding = ActivitySelectionBinding.inflate(getLayoutInflater());
        
        // Establecemos la vista raíz del Binding como el contenido de la pantalla
        setContentView(binding.getRoot());
        
        // Ajustamos los márgenes internos (padding) para evitar que el diseño se superponga con las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Evento de clic para la tarjeta "Halcones"
        binding.cardHalcones.setOnClickListener(v -> {
            irAlLogin("halcones");
        });

        // Evento de clic para la tarjeta "Santa Juana"
        binding.cardJuanas.setOnClickListener(v -> {
            irAlLogin("juanas");
        });
    }

    /**
     * Navega a LoginActivity pasando la agrupación seleccionada como parámetro extra.
     * @param agrupacion El nombre de la agrupación ("halcones" o "juanas")
     */
    private void irAlLogin(String agrupacion) {
        // Creamos un Intent para definir que queremos pasar de esta actividad a LoginActivity
        Intent intent = new Intent(SelectionActivity.this, LoginActivity.class);
        
        // Pasamos el valor de la agrupación seleccionada como un parámetro extra
        intent.putExtra("agrupacion", agrupacion);
        
        // Iniciamos la nueva actividad
        startActivity(intent);
    }
}