package com.serviam.app;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Campamento;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityDetalleCampamentoBinding;

public class DetalleCampamentoActivity extends AppCompatActivity {

    private ActivityDetalleCampamentoBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String campId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDetalleCampamentoBinding.inflate(getLayoutInflater());
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

        personalizarDiseno(agrupacion);

        // Volver atrás
        binding.btnBack.setOnClickListener(v -> finish());

        if (campId != null) {
            cargarDetallesCampamento();
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

    private void cargarDetallesCampamento() {
        repository.getCampamentosCollection(agrupacion).document(campId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Campamento camp = documentSnapshot.toObject(Campamento.class);
                        if (camp != null) {
                            binding.textCampNombre.setText(camp.getNombre().toUpperCase());
                            binding.textCampFechas.setText(camp.getFechaInicio() + " - " + camp.getFechaFin());

                            // Mayor
                            binding.textMayorEq1.setText(camp.getMayorEquipo1());
                            binding.textMayorEq2.setText(camp.getMayorEquipo2());
                            binding.textMayorScore.setText(camp.getMayorPuntajeEquipo1() + " - " + camp.getMayorPuntajeEquipo2());

                            // Menor
                            binding.textMenorEq1.setText(camp.getMenorEquipo1());
                            binding.textMenorEq2.setText(camp.getMenorEquipo2());
                            binding.textMenorScore.setText(camp.getMenorPuntajeEquipo1() + " - " + camp.getMenorPuntajeEquipo2());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar detalles de campamento", Toast.LENGTH_SHORT).show());
    }
}
