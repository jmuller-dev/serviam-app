package com.serviam.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.google.android.material.button.MaterialButton;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Chico;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityChicosBinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChicosActivity extends AppCompatActivity {

    private ActivityChicosBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String userRol;
    private List<Chico> allChicosList = new ArrayList<>();
    private List<Chico> dbChicosList = new ArrayList<>();
    private List<Chico> dbUsuariosList = new ArrayList<>();
    private List<Chico> filteredChicosList = new ArrayList<>();
    private ChicosAdapter adapter;
    private String currentBrigadaFilter = "Todos";
    private String currentSearchQuery = "";
    private final Set<String> inactiveChicosDnis = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityChicosBinding.inflate(getLayoutInflater());
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
        binding.recyclerChicos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChicosAdapter();
        binding.recyclerChicos.setAdapter(adapter);

        // Configurar Filtros
        setupFilterButtons();

        // Configurar Barra de Búsqueda
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configurar FAB para agregar chicos
        if ("capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol)) {
            binding.fabAddChico.setVisibility(View.VISIBLE);
            binding.fabAddChico.setOnClickListener(v -> mostrarDialogoAgregarChico());
        } else {
            binding.fabAddChico.setVisibility(View.GONE);
        }
    }

    private com.google.firebase.firestore.ListenerRegistration firestoreListenerChicos;
    private com.google.firebase.firestore.ListenerRegistration firestoreListenerUsuarios;

    @Override
    protected void onStart() {
        super.onStart();

        firestoreListenerChicos = repository.getChicosCollection(agrupacion)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        runOnUiThread(() -> {
                            dbChicosList.clear();
                            inactiveChicosDnis.clear();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                                try {
                                    Object rawDni = doc.get("dni");
                                    String dni = rawDni != null ? String.valueOf(rawDni).trim() : doc.getId();
                                    if (dni.isEmpty()) dni = doc.getId();

                                    boolean activo = ServiamRepository.isDocumentActivo(doc);
                                    if (!activo) {
                                        inactiveChicosDnis.add(dni);
                                        continue;
                                    }

                                    Object rawNombre = doc.get("nombre");
                                    String nombre = rawNombre != null ? String.valueOf(rawNombre) : "Sin Nombre";

                                    Object rawBrigada = doc.get("brigada");
                                    String brigada = rawBrigada != null ? String.valueOf(rawBrigada) : "Halcones";

                                    Long edadLong = doc.getLong("edad");
                                    int edad = edadLong != null ? edadLong.intValue() : 0;

                                    Object rawFechaNac = doc.get("fechaNacimiento");
                                    String fechaNac = rawFechaNac != null ? String.valueOf(rawFechaNac) : "";

                                    Object rawTelChico = doc.get("telefonoChico");
                                    String telChico = rawTelChico != null ? String.valueOf(rawTelChico) : "";

                                    Object rawTelPadres = doc.get("telefonoPadres");
                                    String telPadres = rawTelPadres != null ? String.valueOf(rawTelPadres) : "";

                                    Object rawAsistencia = doc.get("asistencia");
                                    String asistencia = rawAsistencia != null ? String.valueOf(rawAsistencia) : "";

                                    Chico c = new Chico(dni, nombre, brigada, edad, fechaNac, telChico, telPadres, asistencia, true);
                                    c.setAgrupacion(agrupacion);
                                    dbChicosList.add(c);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            combineLists();
                        });
                    }
                });

        firestoreListenerUsuarios = repository.getUsuariosCollection(agrupacion)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        runOnUiThread(() -> {
                            dbUsuariosList.clear();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                                try {
                                    if (!ServiamRepository.isDocumentActivo(doc)) continue;

                                    Object rawDni = doc.get("dni");
                                    String dni = rawDni != null ? String.valueOf(rawDni).trim() : null;
                                    Object rawNombre = doc.get("nombre");
                                    String nombre = rawNombre != null ? String.valueOf(rawNombre) : null;
                                    Object rawRol = doc.get("rol");
                                    String rol = rawRol != null ? String.valueOf(rawRol) : null;
                                    Object rawEmail = doc.get("email");
                                    String email = rawEmail != null ? String.valueOf(rawEmail) : "";

                                    if (dni != null && !dni.isEmpty() && nombre != null) {
                                        String brigadaLabel = "capitan".equalsIgnoreCase(rol) ? "Capitanes" : "Admins";
                                        Chico usuarioVirtual = new Chico(dni, nombre, brigadaLabel, 0, "N/A", email, "N/A", "", true);
                                        usuarioVirtual.setAgrupacion(agrupacion);
                                        dbUsuariosList.add(usuarioVirtual);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            combineLists();
                        });
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firestoreListenerChicos != null) {
            firestoreListenerChicos.remove();
        }
        if (firestoreListenerUsuarios != null) {
            firestoreListenerUsuarios.remove();
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
            
            // Juanas specific filter names
            binding.btnFilterHalcones.setText("Juana");
            binding.btnFilterConquistadores.setText("Goretti");
            binding.btnFilterPioneros.setText("Inés");
            binding.btnFilterLenadores.setText("Jacinta");
            binding.btnFilterCapitanes.setText("Dirigentes");
        } else {
            primaryColor = getResources().getColor(R.color.color_halcones, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.btnBack.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_halcones_oscuro, getTheme()))
            );
            binding.editSearch.setBackgroundResource(R.drawable.bg_search_bar_halcones);
        }
        binding.fabAddChico.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        binding.btnFilterAll.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
    }

    private void combineLists() {
        allChicosList.clear();
        java.util.Map<String, Chico> uniqueChicos = new java.util.LinkedHashMap<>();
        
        for (Chico c : dbChicosList) {
            if (c.getDni() != null && c.isActivo()) {
                uniqueChicos.put(c.getDni(), c);
            }
        }
        
        for (Chico u : dbUsuariosList) {
            if (u.getDni() == null || !u.isActivo()) continue;
            if (inactiveChicosDnis.contains(u.getDni())) continue;

            if (uniqueChicos.containsKey(u.getDni())) {
                Chico existing = uniqueChicos.get(u.getDni());
                if (existing != null && existing.isActivo()) {
                    uniqueChicos.put(u.getDni(), u);
                }
            } else {
                uniqueChicos.put(u.getDni(), u);
            }
        }
        
        allChicosList.addAll(uniqueChicos.values());
        applyFilters();
    }

    private void setupFilterButtons() {
        MaterialButton[] buttons = new MaterialButton[]{
                binding.btnFilterAll,
                binding.btnFilterHalcones,
                binding.btnFilterConquistadores,
                binding.btnFilterPioneros,
                binding.btnFilterLenadores,
                binding.btnFilterCapitanes
        };

        String[] filters = new String[]{"Todos", "Halcones", "Conquistadores", "Pioneros", "Leñadores", "Capitanes"};

        for (int i = 0; i < buttons.length; i++) {
            final String filterName = filters[i];
            final MaterialButton btn = buttons[i];

            btn.setOnClickListener(v -> {
                currentBrigadaFilter = filterName;
                
                // Actualizar UI de botones
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
                
                applyFilters();
            });
        }
    }

    private boolean matchesFilter(String chicoBrigada, String filter) {
        if (filter.equals("Todos")) return true;
        if (chicoBrigada == null) return false;
        if ("juanas".equalsIgnoreCase(agrupacion)) {
            if (filter.equalsIgnoreCase("Halcones")) {
                return chicoBrigada.equalsIgnoreCase("Halcones") || chicoBrigada.equalsIgnoreCase("Juana") || chicoBrigada.equalsIgnoreCase("santa juana");
            }
            if (filter.equalsIgnoreCase("Conquistadores")) {
                return chicoBrigada.equalsIgnoreCase("Conquistadores") || chicoBrigada.equalsIgnoreCase("Goretti") || chicoBrigada.equalsIgnoreCase("santa maria goretti");
            }
            if (filter.equalsIgnoreCase("Pioneros")) {
                return chicoBrigada.equalsIgnoreCase("Pioneros") || chicoBrigada.equalsIgnoreCase("Inés") || chicoBrigada.equalsIgnoreCase("ines") || chicoBrigada.equalsIgnoreCase("santa ines") || chicoBrigada.equalsIgnoreCase("santa inés");
            }
            if (filter.equalsIgnoreCase("Leñadores")) {
                return chicoBrigada.equalsIgnoreCase("Leñadores") || chicoBrigada.equalsIgnoreCase("lenadores") || chicoBrigada.equalsIgnoreCase("Jacinta") || chicoBrigada.equalsIgnoreCase("santa jacinta");
            }
            if (filter.equalsIgnoreCase("Capitanes")) {
                return chicoBrigada.equalsIgnoreCase("Capitanes") || chicoBrigada.equalsIgnoreCase("Dirigentes") || chicoBrigada.equalsIgnoreCase("capitan") || chicoBrigada.equalsIgnoreCase("dirigente");
            }
        }
        return chicoBrigada.equalsIgnoreCase(filter);
    }

    private void applyFilters() {
        filteredChicosList.clear();
        for (Chico c : allChicosList) {
            boolean matchesBrigada = matchesFilter(c.getBrigada(), currentBrigadaFilter);
            
            String nombreChico = c.getNombre() != null ? c.getNombre().toLowerCase() : "";
            String dniChico = c.getDni() != null ? c.getDni() : "";
            String queryLower = currentSearchQuery.toLowerCase();

            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                    nombreChico.contains(queryLower) ||
                    dniChico.contains(currentSearchQuery);

            if (matchesBrigada && matchesSearch) {
                filteredChicosList.add(c);
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredChicosList.isEmpty()) {
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.recyclerChicos.setVisibility(View.GONE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
            binding.recyclerChicos.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarDialogoAgregarChico() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_chico, null);
        builder.setView(dialogView);

        EditText editDni = dialogView.findViewById(R.id.editDni);
        EditText editNombre = dialogView.findViewById(R.id.editNombre);
        Spinner spinnerBrigada = dialogView.findViewById(R.id.spinnerBrigada);
        EditText editEdad = dialogView.findViewById(R.id.editEdad);
        EditText editFechaNac = dialogView.findViewById(R.id.editFechaNac);
        EditText editTelChico = dialogView.findViewById(R.id.editTelChico);
        EditText editTelPadres = dialogView.findViewById(R.id.editTelPadres);

        // Adaptar fondos de cajas de texto del diálogo al color del grupo
        int inputBg = agrupacion.equals("juanas") ? R.drawable.bg_input_field_juanas : R.drawable.bg_input_field;
        editDni.setBackgroundResource(inputBg);
        editNombre.setBackgroundResource(inputBg);
        editEdad.setBackgroundResource(inputBg);
        editFechaNac.setBackgroundResource(inputBg);
        editTelChico.setBackgroundResource(inputBg);
        editTelPadres.setBackgroundResource(inputBg);

        // Auto-formateador de fecha DD/MM/AAAA al escribir
        aplicarFormatoFechaAuto(editFechaNac);

        // Configurar Spinner
        String[] brigadas;
        if (agrupacion.equals("juanas")) {
            brigadas = new String[]{"Juana", "Goretti", "Inés", "Jacinta", "Dirigentes"};
        } else {
            brigadas = new String[]{"Halcones", "Conquistadores", "Pioneros", "Leñadores", "Capitanes"};
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, brigadas);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBrigada.setAdapter(spinnerAdapter);

        builder.setTitle(agrupacion.equals("juanas") ? "Agregar Integrante" : "Agregar Integrante");
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String dni = editDni.getText().toString().trim();
            String nombre = editNombre.getText().toString().trim();
            String brigada = spinnerBrigada.getSelectedItem().toString();
            String fechaNac = editFechaNac.getText().toString().trim();
            String telChico = editTelChico.getText().toString().trim();
            String telPadres = editTelPadres.getText().toString().trim();

            if (dni.isEmpty() || nombre.isEmpty()) {
                Toast.makeText(ChicosActivity.this, "DNI y Nombre son requeridos", Toast.LENGTH_SHORT).show();
                return;
            }

            Chico nuevoChico = new Chico(dni, nombre, brigada, 0, fechaNac, telChico, telPadres, "", true);
            nuevoChico.setAgrupacion(agrupacion);
            int edadCalculada = nuevoChico.getEdad();
            nuevoChico.setEdad(edadCalculada);

            // Guardar en Firestore
            repository.getChicosCollection(agrupacion).document(dni).set(nuevoChico)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ChicosActivity.this, "Integrante agregado correctamente", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ChicosActivity.this, "Error al sincronizar con la nube", Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancelar", null);
        builder.create().show();
    }

    // --- RECYCLER VIEW ADAPTER & HOLDER ---

    private class ChicosAdapter extends RecyclerView.Adapter<ChicosAdapter.ChicoViewHolder> {

        @NonNull
        @Override
        public ChicoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chico, parent, false);
            return new ChicoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChicoViewHolder holder, int position) {
            Chico chico = filteredChicosList.get(position);
            holder.bind(chico);
        }

        @Override
        public int getItemCount() {
            return filteredChicosList.size();
        }

        class ChicoViewHolder extends RecyclerView.ViewHolder {
            TextView textNombre;
            TextView textDetail;
            View viewIndicator;

            ChicoViewHolder(View itemView) {
                super(itemView);
                textNombre = itemView.findViewById(R.id.textNombre);
                textDetail = itemView.findViewById(R.id.textDetail);
                viewIndicator = itemView.findViewById(R.id.viewBrigadaIndicator);
            }

            void bind(Chico chico) {
                textNombre.setText(chico.getNombre() != null ? chico.getNombre() : "");
                
                String label = "juanas".equalsIgnoreCase(agrupacion) ? "Compañía" : "Brigada";
                String brigadaVal = chico.getBrigada() != null ? chico.getBrigada() : "";
                String displayBrigada = getBrigadaDisplay(brigadaVal, agrupacion);
                textDetail.setText(label + ": " + displayBrigada + " | DNI: " + (chico.getDni() != null ? chico.getDni() : ""));

                // Configurar color del bólster (indicador)
                int colorRes;
                switch (brigadaVal.toLowerCase()) {
                    case "conquistadores":
                    case "goretti":
                        colorRes = R.color.brigada_conquistadores;
                        break;
                    case "pioneros":
                    case "inés":
                    case "ines":
                        colorRes = R.color.brigada_pioneros;
                        break;
                    case "leñadores":
                    case "lenadores":
                    case "jacinta":
                        colorRes = R.color.brigada_lenadores;
                        break;
                    case "capitanes":
                    case "dirigentes":
                    case "admins":
                        colorRes = "juanas".equalsIgnoreCase(agrupacion) ? R.color.color_juanas_oscuro : R.color.color_halcones_oscuro;
                        break;
                    case "halcones":
                    case "juana":
                    default:
                        colorRes = R.color.brigada_halcones;
                        break;
                }
                viewIndicator.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(colorRes, getTheme())));

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(ChicosActivity.this, PerfilChicoActivity.class);
                    intent.putExtra("CHICO_DNI", chico.getDni());
                    intent.putExtra("AGRUPACION", agrupacion);
                    startActivity(intent);
                });
            }
        }
    }

    public static String getBrigadaDisplay(String brigada, String agrupacion) {
        if (brigada == null) return "";
        if ("juanas".equalsIgnoreCase(agrupacion)) {
            switch (brigada.toLowerCase()) {
                case "halcones":
                case "juana":
                case "santa juana":
                    return "Juana";
                case "conquistadores":
                case "goretti":
                case "santa maria goretti":
                    return "Goretti";
                case "pioneros":
                case "inés":
                case "ines":
                case "santa ines":
                case "santa inés":
                    return "Inés";
                case "leñadores":
                case "lenadores":
                case "jacinta":
                case "santa jacinta":
                    return "Jacinta";
                case "capitanes":
                case "dirigentes":
                case "capitan":
                case "dirigente":
                    return "Dirigentes";
                default:
                    return brigada;
            }
        } else {
            switch (brigada.toLowerCase()) {
                case "juana":
                case "halcones":
                case "santa juana":
                    return "Halcones";
                case "goretti":
                case "conquistadores":
                case "santa maria goretti":
                    return "Conquistadores";
                case "inés":
                case "ines":
                case "santa ines":
                case "santa inés":
                case "pioneros":
                    return "Pioneros";
                case "jacinta":
                case "leñadores":
                case "lenadores":
                case "santa jacinta":
                    return "Leñadores";
                case "dirigentes":
                case "capitanes":
                case "dirigente":
                case "capitan":
                    return "Capitanes";
                default:
                    return brigada;
            }
        }
    }

    public static String formatUserWelcome(String nombre, String rol, String agrupacion) {
        if (nombre == null || nombre.isEmpty()) return "";
        String firstWord = nombre.split("\\s+")[0];
        
        if (firstWord.length() > 0) {
            firstWord = firstWord.substring(0, 1).toUpperCase() + firstWord.substring(1).toLowerCase();
        }
        
        String rank = "";
        if ("admin".equalsIgnoreCase(rol)) {
            rank = "Admin";
        } else if ("capitan".equalsIgnoreCase(rol)) {
            if ("juanas".equalsIgnoreCase(agrupacion)) {
                rank = "Dirigente";
            } else {
                rank = "Capitán";
            }
        } else { // chico
            if ("juanas".equalsIgnoreCase(agrupacion)) {
                rank = "Juana";
            } else {
                rank = "Halcón";
            }
        }
        return rank + " " + firstWord;
    }

    public static void aplicarFormatoFechaAuto(EditText editText) {
        if (editText == null) return;
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                String str = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                if (str.length() > 0) {
                    if (str.length() <= 2) {
                        formatted.append(str);
                    } else if (str.length() <= 4) {
                        formatted.append(str.substring(0, 2)).append("/").append(str.substring(2));
                    } else {
                        formatted.append(str.substring(0, 2)).append("/")
                                 .append(str.substring(2, 4)).append("/");
                        if (str.length() <= 8) {
                            formatted.append(str.substring(4));
                        } else {
                            formatted.append(str.substring(4, 8));
                        }
                    }
                }

                s.replace(0, s.length(), formatted.toString());
                isUpdating = false;
            }
        });
    }
}
