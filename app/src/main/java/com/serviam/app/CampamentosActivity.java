package com.serviam.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Campamento;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityCampamentosBinding;
import java.util.ArrayList;
import java.util.List;

public class CampamentosActivity extends AppCompatActivity {

    private ActivityCampamentosBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String userRol;
    private List<Campamento> finalizedCamps = new ArrayList<>();
    private Campamento activeCamp = null;
    private CampamentoAdapter adapter;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCampamentosBinding.inflate(getLayoutInflater());
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
        userRol = prefs.getUserRol();

        personalizarDiseno(agrupacion);

        // Volver atrás
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        binding.recyclerCampamentos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CampamentoAdapter();
        binding.recyclerCampamentos.setAdapter(adapter);

        // Setup button clicks
        binding.btnGestionarPuntos.setOnClickListener(v -> {
            if (activeCamp != null) {
                Intent intent = new Intent(CampamentosActivity.this, PuntosActivity.class);
                intent.putExtra("CAMP_ID", activeCamp.getId());
                intent.putExtra("AGRUPACION", agrupacion);
                startActivity(intent);
            }
        });

        // Setup FAB
        if ("capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol)) {
            binding.fabCreateCamp.setVisibility(View.VISIBLE);
            binding.fabCreateCamp.setOnClickListener(v -> {
                if (activeCamp != null) {
                    Toast.makeText(this, "Ya hay un campamento activo. Finalícelo primero.", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(CampamentosActivity.this, CrearCampamentoActivity.class);
                    intent.putExtra("AGRUPACION", agrupacion);
                    startActivity(intent);
                }
            });
        } else {
            binding.fabCreateCamp.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Escuchar en tiempo real campamentos (Offline-First)
        firestoreListener = repository.getCampamentosCollection(agrupacion)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al sincronizar campamentos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        finalizedCamps.clear();
                        activeCamp = null;

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Campamento camp = doc.toObject(Campamento.class);
                            if (camp != null) {
                                camp.setId(doc.getId());
                                if (camp.isActivo()) {
                                    activeCamp = camp;
                                } else {
                                    finalizedCamps.add(camp);
                                }
                            }
                        }

                        actualizarUIActiveCamp();
                        adapter.notifyDataSetChanged();
                    }
                });
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
            binding.btnGestionarPuntos.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            binding.labelHistorial.setTextColor(primaryColor);
        } else {
            primaryColor = getResources().getColor(R.color.color_halcones, getTheme());
            darkColor = getResources().getColor(R.color.color_halcones_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.btnGestionarPuntos.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            binding.labelHistorial.setTextColor(primaryColor);
        }
        binding.fabCreateCamp.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
    }

    private void actualizarUIActiveCamp() {
        if (activeCamp != null) {
            binding.textActiveCampName.setText(activeCamp.getNombre());
            binding.cardCampamentoActivo.setVisibility(View.VISIBLE);
            binding.cardNoActiveCamp.setVisibility(View.GONE);
        } else {
            binding.cardCampamentoActivo.setVisibility(View.GONE);
            binding.cardNoActiveCamp.setVisibility(View.VISIBLE);
        }
    }

    // --- ADAPTER & VIEWHOLDER ---

    private class CampamentoAdapter extends RecyclerView.Adapter<CampamentoAdapter.CampViewHolder> {

        @NonNull
        @Override
        public CampViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campamento, parent, false);
            return new CampViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CampViewHolder holder, int position) {
            Campamento camp = finalizedCamps.get(position);
            holder.bind(camp);
        }

        @Override
        public int getItemCount() {
            return finalizedCamps.size();
        }

        class CampViewHolder extends RecyclerView.ViewHolder {
            TextView textNombre;
            TextView textDates;
            View iconContainer;
            ImageView imageIcon;

            CampViewHolder(View itemView) {
                super(itemView);
                textNombre = itemView.findViewById(R.id.textCampName);
                textDates = itemView.findViewById(R.id.textCampDates);
                iconContainer = itemView.findViewById(R.id.iconContainer);
                imageIcon = itemView.findViewById(R.id.imageIcon);
            }

            void bind(Campamento camp) {
                textNombre.setText(camp.getNombre());
                textDates.setText(camp.getFechaInicio() + " - " + camp.getFechaFin());

                int containerColor = agrupacion.equals("juanas") ? 
                        getResources().getColor(R.color.color_juanas_mas_claro, getTheme()) : 
                        getResources().getColor(R.color.color_halcones_mas_claro, getTheme());
                iconContainer.setBackgroundTintList(ColorStateList.valueOf(containerColor));

                if (agrupacion.equals("juanas")) {
                    imageIcon.setImageResource(R.drawable.ic_calendar_juanas);
                    imageIcon.setImageTintList(null);
                } else {
                    imageIcon.setImageResource(R.drawable.ic_calendar_halcones);
                    imageIcon.setImageTintList(null);
                }

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(CampamentosActivity.this, DetalleCampamentoActivity.class);
                    intent.putExtra("CAMP_ID", camp.getId());
                    intent.putExtra("AGRUPACION", agrupacion);
                    startActivity(intent);
                });
            }
        }
    }
}
