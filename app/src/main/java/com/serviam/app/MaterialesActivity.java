package com.serviam.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Material;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityMaterialesBinding;
import java.util.ArrayList;
import java.util.List;

public class MaterialesActivity extends AppCompatActivity {

    private ActivityMaterialesBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String userRol;
    private List<Material> allMaterialsList = new ArrayList<>();
    private List<Material> filteredMaterialsList = new ArrayList<>();
    private MaterialAdapter adapter;
    private String currentCategoryFilter = "Todos";
    private String currentSearchQuery = "";
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMaterialesBinding.inflate(getLayoutInflater());
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
        binding.recyclerMateriales.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MaterialAdapter();
        binding.recyclerMateriales.setAdapter(adapter);

        // Configurar Barra de Búsqueda
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFiltersAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar Filtros
        setupCategoryFilters();

        // Configurar FAB
        if ("capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol)) {
            binding.fabAddMaterial.setVisibility(View.VISIBLE);
            binding.fabAddMaterial.setOnClickListener(v -> {
                Intent intent = new Intent(MaterialesActivity.this, AgregarMaterialActivity.class);
                intent.putExtra("AGRUPACION", agrupacion);
                startActivity(intent);
            });
        } else {
            binding.fabAddMaterial.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        // Armar consulta según rol
        Query query = repository.getMaterialesCollection(agrupacion)
                .orderBy("timestamp", Query.Direction.DESCENDING);
        
        if ("chico".equalsIgnoreCase(userRol)) {
            query = query.whereEqualTo("visibleParaChicos", true);
        }

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error al sincronizar materiales", Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                allMaterialsList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Material mat = doc.toObject(Material.class);
                    if (mat != null) {
                        mat.setId(doc.getId());
                        allMaterialsList.add(mat);
                    }
                }
                applyFiltersAndSearch();
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
        if (agrupacion.equals("juanas")) {
            primaryColor = getResources().getColor(R.color.color_juanas, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
            binding.btnBack.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_juanas_oscuro, getTheme()))
            );
            binding.editSearch.setBackgroundResource(R.drawable.bg_search_bar_juanas);
        } else {
            primaryColor = getResources().getColor(R.color.color_halcones, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.btnBack.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_halcones_oscuro, getTheme()))
            );
            binding.editSearch.setBackgroundResource(R.drawable.bg_search_bar_halcones);
        }
        binding.fabAddMaterial.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        binding.btnFilterAll.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
    }

    private void setupCategoryFilters() {
        MaterialButton[] buttons = new MaterialButton[]{
                binding.btnFilterAll,
                binding.btnFilterMistica,
                binding.btnFilterCharlas,
                binding.btnFilterFormacion,
                binding.btnFilterOraciones
        };

        String[] filters = new String[]{"Todos", "Mística", "Charlas", "Formación", "Oraciones"};

        for (int i = 0; i < buttons.length; i++) {
            final String filterName = filters[i];
            final MaterialButton btn = buttons[i];

            btn.setOnClickListener(v -> {
                currentCategoryFilter = filterName;

                // Actualizar UI
                for (MaterialButton otherBtn : buttons) {
                    if (otherBtn == btn) {
                        int activeColor = agrupacion.equals("juanas") ? 
                                getResources().getColor(R.color.color_juanas, getTheme()) :
                                getResources().getColor(R.color.color_halcones, getTheme());
                        otherBtn.setBackgroundTintList(ColorStateList.valueOf(activeColor));
                        otherBtn.setTextColor(getResources().getColor(R.color.blanco, getTheme()));
                    } else {
                        otherBtn.setBackgroundTintList(ColorStateList.valueOf(0xFFF2F2F2));
                        otherBtn.setTextColor(getResources().getColor(R.color.gris, getTheme()));
                    }
                }

                applyFiltersAndSearch();
            });
        }
    }

    private void applyFiltersAndSearch() {
        filteredMaterialsList.clear();
        for (Material mat : allMaterialsList) {
            boolean matchesCategory = currentCategoryFilter.equals("Todos") || 
                    mat.getCategoria().equalsIgnoreCase(currentCategoryFilter);
            
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                    mat.getNombre().toLowerCase().contains(currentSearchQuery.toLowerCase()) ||
                    (mat.getContenido() != null && mat.getContenido().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            if (matchesCategory && matchesSearch) {
                filteredMaterialsList.add(mat);
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredMaterialsList.isEmpty()) {
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.recyclerMateriales.setVisibility(View.GONE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
            binding.recyclerMateriales.setVisibility(View.VISIBLE);
        }
    }

    // --- ADAPTER & VIEWHOLDER ---

    private class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {

        @NonNull
        @Override
        public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_material, parent, false);
            return new MaterialViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
            Material mat = filteredMaterialsList.get(position);
            holder.bind(mat);
        }

        @Override
        public int getItemCount() {
            return filteredMaterialsList.size();
        }

        class MaterialViewHolder extends RecyclerView.ViewHolder {
            TextView textNombre;
            TextView textCategoria;
            TextView textVisibility;
            CardView cardVisibility;
            View iconContainer;
            ImageView imageIcon;

            MaterialViewHolder(View itemView) {
                super(itemView);
                textNombre = itemView.findViewById(R.id.textMaterialNombre);
                textCategoria = itemView.findViewById(R.id.textMaterialCategoria);
                textVisibility = itemView.findViewById(R.id.textVisibility);
                cardVisibility = itemView.findViewById(R.id.cardVisibility);
                iconContainer = itemView.findViewById(R.id.iconContainer);
                imageIcon = itemView.findViewById(R.id.imageIcon);
            }

            void bind(Material mat) {
                textNombre.setText(mat.getNombre());
                textCategoria.setText("Categoría: " + mat.getCategoria());

                // Visibilidad (solo capitanes/admin ven este badge)
                if ("capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol)) {
                    cardVisibility.setVisibility(View.VISIBLE);
                    if (mat.isVisibleParaChicos()) {
                        textVisibility.setText("Visible");
                        cardVisibility.setCardBackgroundColor(getResources().getColor(R.color.verde_punto, getTheme()));
                    } else {
                        textVisibility.setText("Solo Capitanes");
                        cardVisibility.setCardBackgroundColor(getResources().getColor(R.color.rojo_punto, getTheme()));
                    }
                } else {
                    cardVisibility.setVisibility(View.GONE);
                }

                // Personalizar icono de libro
                int containerColor = agrupacion.equals("juanas") ? 
                        getResources().getColor(R.color.color_juanas_mas_claro, getTheme()) : 
                        getResources().getColor(R.color.color_halcones_mas_claro, getTheme());
                iconContainer.setBackgroundTintList(ColorStateList.valueOf(containerColor));

                if (agrupacion.equals("juanas")) {
                    imageIcon.setImageResource(R.drawable.ic_book_juanas);
                    imageIcon.setImageTintList(null);
                } else {
                    imageIcon.setImageResource(R.drawable.ic_book_halcones);
                    imageIcon.setImageTintList(null);
                }

                // Clic para abrir el material
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(MaterialesActivity.this, DetalleMaterialActivity.class);
                    intent.putExtra("MATERIAL_ID", mat.getId());
                    intent.putExtra("AGRUPACION", agrupacion);
                    startActivity(intent);
                });
            }
        }
    }
}
