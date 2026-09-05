package com.serviam.app;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityTesoroEspiritualBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TesoroEspiritualActivity extends AppCompatActivity {

    private ActivityTesoroEspiritualBinding binding;
    private ServiamRepository repository;
    private String agrupacion;
    private String rol;

    private String currentMes = "";
    private String currentDescripcion = "";
    private String currentUrlImagen = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityTesoroEspiritualBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Insets setup
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ServiamRepository(this);

        PreferencesManager prefs = new PreferencesManager(this);
        agrupacion = prefs.getSelectedAgrupacion();
        if (agrupacion == null) {
            agrupacion = "halcones";
        }
        rol = prefs.getUserRol();

        personalizarDiseno(agrupacion);

        // Volver atrás
        binding.btnBack.setOnClickListener(v -> finish());

        // Cargar Tesoro Espiritual
        cargarTesoroActual();

        // Configuración para editar si es Capitán/Admin
        if ("capitan".equalsIgnoreCase(rol) || "admin".equalsIgnoreCase(rol)) {
            binding.btnSubirImagen.setVisibility(View.VISIBLE);
            binding.btnSubirImagen.setOnClickListener(v -> mostrarDialogoConfigurar());
        }
    }

    private void personalizarDiseno(String agrupacion) {
        if (agrupacion.equals("juanas")) {
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
            binding.btnSubirImagen.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_juanas, getTheme()))
            );
            binding.btnBack.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_juanas_oscuro, getTheme()))
            );
        } else {
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.btnSubirImagen.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_halcones, getTheme()))
            );
        }
    }

    private void cargarTesoroActual() {
        binding.progressImage.setVisibility(View.VISIBLE);

        // Obtenemos los datos del tesoro actual de Firestore
        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection(agrupacion).document("datos").collection("tesoro").document("actual");

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot doc = task.getResult();
                currentMes = doc.getString("mes");
                currentDescripcion = doc.getString("descripcion");
                currentUrlImagen = doc.getString("urlImagen");

                if (currentMes == null || currentMes.trim().isEmpty()) currentMes = obtenerMesActual();
                if (currentDescripcion == null || currentDescripcion.trim().isEmpty()) {
                    currentDescripcion = "Sin descripción disponible para este mes.";
                }

                binding.textMes.setText(currentMes);
                binding.textDescripcion.setText(currentDescripcion);

                descargarImagenUrl(currentUrlImagen);
            } else {
                binding.progressImage.setVisibility(View.GONE);
                currentMes = obtenerMesActual();
                currentDescripcion = "Aún no se ha configurado el Tesoro Espiritual de este mes. Habla con tu Capitán para configurar el material.";
                currentUrlImagen = "";
                binding.textMes.setText(currentMes);
                binding.textDescripcion.setText(currentDescripcion);
                binding.imageTesoro.setImageResource(R.drawable.logo_serviam);
            }
        });
    }

    private void descargarImagenUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            binding.progressImage.setVisibility(View.GONE);
            binding.imageTesoro.setImageResource(R.drawable.logo_serviam);
            return;
        }
        binding.progressImage.setVisibility(View.VISIBLE);
        com.serviam.app.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                java.net.URL imageUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) imageUrl.openConnection();
                conn.setDoInput(true);
                conn.connect();
                try (java.io.InputStream input = conn.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            binding.imageTesoro.setImageBitmap(bitmap);
                        } else {
                            binding.imageTesoro.setImageResource(R.drawable.logo_serviam);
                        }
                        binding.progressImage.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    binding.progressImage.setVisibility(View.GONE);
                    binding.imageTesoro.setImageResource(R.drawable.logo_serviam);
                });
            }
        });
    }

    private void mostrarDialogoConfigurar() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Configurar Tesoro Espiritual");

        // Custom view with three input fields
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final android.widget.EditText inputMes = new android.widget.EditText(this);
        inputMes.setHint("Mes y año (ej. Junio 2026)");
        inputMes.setText(currentMes);
        layout.addView(inputMes);

        final android.widget.EditText inputDesc = new android.widget.EditText(this);
        inputDesc.setHint("Metas y descripción");
        inputDesc.setText(currentUrlImagen.isEmpty() && currentDescripcion.startsWith("Aún no se ha") ? "" : currentDescripcion);
        layout.addView(inputDesc);

        final android.widget.EditText inputUrl = new android.widget.EditText(this);
        inputUrl.setHint("Enlace público de la imagen (ej: imgur)");
        inputUrl.setText(currentUrlImagen);
        layout.addView(inputUrl);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String mes = inputMes.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            String url = inputUrl.getText().toString().trim();

            if (mes.isEmpty() || desc.isEmpty() || url.isEmpty()) {
                Toast.makeText(TesoroEspiritualActivity.this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(TesoroEspiritualActivity.this, "El enlace debe comenzar con http:// o https://", Toast.LENGTH_SHORT).show();
                return;
            }

            guardarDatosTesoro(mes, desc, url);
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void guardarDatosTesoro(String mes, String desc, String url) {
        binding.progressImage.setVisibility(View.VISIBLE);

        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection(agrupacion).document("datos").collection("tesoro").document("actual");

        Map<String, Object> data = new HashMap<>();
        data.put("mes", mes);
        data.put("descripcion", desc);
        data.put("urlImagen", url);

        docRef.set(data).addOnCompleteListener(task -> {
            binding.progressImage.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(TesoroEspiritualActivity.this, "¡Tesoro Espiritual configurado con éxito!", Toast.LENGTH_SHORT).show();
                cargarTesoroActual();
            } else {
                Toast.makeText(TesoroEspiritualActivity.this, "Error al guardar configuración.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String obtenerMesActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        String mes = sdf.format(new Date());
        return mes.substring(0, 1).toUpperCase() + mes.substring(1); // Capitalizar
    }
}
