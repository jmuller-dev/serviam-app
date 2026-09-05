package com.serviam.app;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.ListenerRegistration;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Campamento;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityPuntosBinding;

public class PuntosActivity extends AppCompatActivity {

    private ActivityPuntosBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String userRol;
    private String campId;
    private Campamento currentCamp;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPuntosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Insets setup
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ServiamRepository(this);
        prefs = new PreferencesManager(this);

        campId = getIntent().getStringExtra("CAMP_ID");
        agrupacion = getIntent().getStringExtra("AGRUPACION");
        if (agrupacion == null) {
            agrupacion = prefs.getSelectedAgrupacion();
            if (agrupacion == null) agrupacion = "halcones";
        }
        userRol = prefs.getUserRol();

        personalizarDiseno(agrupacion);

        // Volver atrás
        binding.btnBack.setOnClickListener(v -> finish());

        // Validar rol para mostrar/ocultar botones de edición
        if ("capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol)) {
            binding.layoutButtonsMayor.setVisibility(View.VISIBLE);
            binding.layoutButtonsMenor.setVisibility(View.VISIBLE);
            binding.btnFinalizarCamp.setVisibility(View.VISIBLE);
            setupScoreAdjustButtons();
            binding.btnFinalizarCamp.setOnClickListener(v -> confirmarFinalizacion());
        } else {
            binding.layoutButtonsMayor.setVisibility(View.GONE);
            binding.layoutButtonsMenor.setVisibility(View.GONE);
            binding.btnFinalizarCamp.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (campId != null) {
            // Escuchar cambios en tiempo real
            firestoreListener = repository.getCampamentosCollection(agrupacion).document(campId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            Toast.makeText(this, "Error al sincronizar marcador", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            currentCamp = snapshot.toObject(Campamento.class);
                            if (currentCamp != null) {
                                currentCamp.setId(snapshot.getId());
                                actualizarMarcadores();
                            }
                        }
                    });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    private void personalizarDiseno(String agrupacion) {
        int primaryColor;
        int darkColor;
        if (agrupacion.equals("juanas")) {
            primaryColor = getResources().getColor(R.color.color_juanas, getTheme());
            darkColor = getResources().getColor(R.color.color_juanas_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        } else {
            primaryColor = getResources().getColor(R.color.color_halcones, getTheme());
            darkColor = getResources().getColor(R.color.color_halcones_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        }
    }

    private void actualizarMarcadores() {
        if (currentCamp == null) return;

        binding.textCampNombre.setText(currentCamp.getNombre().toUpperCase());

        // Mayor
        binding.textMayorEq1.setText(currentCamp.getMayorEquipo1());
        binding.textMayorEq2.setText(currentCamp.getMayorEquipo2());
        binding.textMayorScore.setText(currentCamp.getMayorPuntajeEquipo1() + " - " + currentCamp.getMayorPuntajeEquipo2());

        // Menor
        binding.textMenorEq1.setText(currentCamp.getMenorEquipo1());
        binding.textMenorEq2.setText(currentCamp.getMenorEquipo2());
        binding.textMenorScore.setText(currentCamp.getMenorPuntajeEquipo1() + " - " + currentCamp.getMenorPuntajeEquipo2());

        // Si ya no está activo, cerrar esta pantalla y redirigir
        if (!currentCamp.isActivo()) {
            Toast.makeText(this, "El campamento fue finalizado.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupScoreAdjustButtons() {
        // --- DIVISIÓN MAYOR ---
        binding.btnMayorEq1Add.setOnClickListener(v -> actualizarPuntaje("mayor", 1, 10));
        binding.btnMayorEq1Sub.setOnClickListener(v -> actualizarPuntaje("mayor", 1, -10));
        binding.btnMayorEq2Add.setOnClickListener(v -> actualizarPuntaje("mayor", 2, 10));
        binding.btnMayorEq2Sub.setOnClickListener(v -> actualizarPuntaje("mayor", 2, -10));

        // --- DIVISIÓN MENOR ---
        binding.btnMenorEq1Add.setOnClickListener(v -> actualizarPuntaje("menor", 1, 10));
        binding.btnMenorEq1Sub.setOnClickListener(v -> actualizarPuntaje("menor", 1, -10));
        binding.btnMenorEq2Add.setOnClickListener(v -> actualizarPuntaje("menor", 2, 10));
        binding.btnMenorEq2Sub.setOnClickListener(v -> actualizarPuntaje("menor", 2, -10));
    }

    private void actualizarPuntaje(String division, int equipoNum, int delta) {
        if (currentCamp == null) return;

        if (division.equals("mayor")) {
            if (equipoNum == 1) {
                int nuevo = Math.max(0, currentCamp.getMayorPuntajeEquipo1() + delta);
                currentCamp.setMayorPuntajeEquipo1(nuevo);
            } else {
                int nuevo = Math.max(0, currentCamp.getMayorPuntajeEquipo2() + delta);
                currentCamp.setMayorPuntajeEquipo2(nuevo);
            }
        } else {
            if (equipoNum == 1) {
                int nuevo = Math.max(0, currentCamp.getMenorPuntajeEquipo1() + delta);
                currentCamp.setMenorPuntajeEquipo1(nuevo);
            } else {
                int nuevo = Math.max(0, currentCamp.getMenorPuntajeEquipo2() + delta);
                currentCamp.setMenorPuntajeEquipo2(nuevo);
            }
        }

        // Subir cambio a Firestore (Offline-First guardará localmente si no hay internet)
        repository.getCampamentosCollection(agrupacion).document(campId).set(currentCamp);
    }

    private void confirmarFinalizacion() {
        new AlertDialog.Builder(this)
                .setTitle("Finalizar Campamento")
                .setMessage("¿Estás seguro de que deseas finalizar este campamento? Los resultados se guardarán permanentemente en el historial.")
                .setPositiveButton("Sí, finalizar", (dialog, which) -> {
                    if (currentCamp != null) {
                        currentCamp.setActivo(false);
                        repository.getCampamentosCollection(agrupacion).document(campId).set(currentCamp)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(PuntosActivity.this, "Campamento finalizado con éxito", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
