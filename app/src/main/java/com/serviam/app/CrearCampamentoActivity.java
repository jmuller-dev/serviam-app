package com.serviam.app;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Campamento;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityCrearCampamentoBinding;

public class CrearCampamentoActivity extends AppCompatActivity {

    private ActivityCrearCampamentoBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCrearCampamentoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Insets setup
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ServiamRepository(this);
        prefs = new PreferencesManager(this);

        agrupacion = getIntent().getStringExtra("AGRUPACION");
        if (agrupacion == null) {
            agrupacion = prefs.getSelectedAgrupacion();
            if (agrupacion == null) agrupacion = "halcones";
        }

        personalizarDiseno(agrupacion);

        // Volver atrás
        binding.btnBack.setOnClickListener(v -> finish());

        // Guardar
        binding.btnGuardar.setOnClickListener(v -> guardarCampamento());
    }

    private void personalizarDiseno(String agrupacion) {
        int primaryColor;
        int darkColor;
        int inputBg;
        if (agrupacion.equals("juanas")) {
            primaryColor = getResources().getColor(R.color.color_juanas, getTheme());
            darkColor = getResources().getColor(R.color.color_juanas_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.btnGuardar.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            inputBg = R.drawable.bg_input_field_juanas;
        } else {
            primaryColor = getResources().getColor(R.color.color_halcones, getTheme());
            darkColor = getResources().getColor(R.color.color_halcones_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.btnGuardar.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            inputBg = R.drawable.bg_input_field;
        }
        binding.editNombre.setBackgroundResource(inputBg);
        binding.editFechaInicio.setBackgroundResource(inputBg);
        binding.editFechaFin.setBackgroundResource(inputBg);
        binding.editMayorEquipo1.setBackgroundResource(inputBg);
        binding.editMayorEquipo2.setBackgroundResource(inputBg);
        binding.editMenorEquipo1.setBackgroundResource(inputBg);
        binding.editMenorEquipo2.setBackgroundResource(inputBg);
    }

    private void guardarCampamento() {
        String nombre = binding.editNombre.getText().toString().trim();
        String inicio = binding.editFechaInicio.getText().toString().trim();
        String fin = binding.editFechaFin.getText().toString().trim();
        
        String mayorEq1 = binding.editMayorEquipo1.getText().toString().trim();
        String mayorEq2 = binding.editMayorEquipo2.getText().toString().trim();
        String menorEq1 = binding.editMenorEquipo1.getText().toString().trim();
        String menorEq2 = binding.editMenorEquipo2.getText().toString().trim();

        if (nombre.isEmpty() || inicio.isEmpty() || fin.isEmpty() || 
                mayorEq1.isEmpty() || mayorEq2.isEmpty() || menorEq1.isEmpty() || menorEq2.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnGuardar.setEnabled(false);

        String id = repository.getCampamentosCollection(agrupacion).document().getId();

        Campamento camp = new Campamento(
                id, nombre, inicio, fin, true,
                mayorEq1, mayorEq2, 0, 0,
                menorEq1, menorEq2, 0, 0
        );

        repository.getCampamentosCollection(agrupacion).document(id).set(camp)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CrearCampamentoActivity.this, "Campamento activo creado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnGuardar.setEnabled(true);
                    Toast.makeText(CrearCampamentoActivity.this, "Error al guardar campamento: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
