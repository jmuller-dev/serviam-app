package com.serviam.app;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Material;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityAgregarMaterialBinding;

public class AgregarMaterialActivity extends AppCompatActivity {

    private ActivityAgregarMaterialBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;

    private final androidx.activity.result.ActivityResultLauncher<String> pickPdfLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    subirPdfFirebase(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAgregarMaterialBinding.inflate(getLayoutInflater());
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

        // Setup Spinner Categoría (Mística, Formación, Cursos, Oraciones)
        String[] categorias = new String[]{"Mística", "Formación", "Cursos", "Oraciones"};
        ArrayAdapter<String> spinnerAdapterCat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categorias);
        spinnerAdapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategoria.setAdapter(spinnerAdapterCat);

        // Setup Spinner Tipo Formato
        String[] tiposFormato = new String[]{"📄 Texto Nativo / Guía", "📎 Archivo PDF", "🔗 Enlace Web / Externo"};
        ArrayAdapter<String> spinnerAdapterTipo = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tiposFormato);
        spinnerAdapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTipoFormato.setAdapter(spinnerAdapterTipo);

        binding.spinnerTipoFormato.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Texto
                    binding.containerTexto.setVisibility(View.VISIBLE);
                    binding.containerUrl.setVisibility(View.GONE);
                } else {
                    binding.containerTexto.setVisibility(View.GONE);
                    binding.containerUrl.setVisibility(View.VISIBLE);
                    if (position == 1) {
                        binding.labelUrlHeader.setText("Enlace del Archivo PDF");
                        binding.editUrlArchivo.setHint("Pegá el enlace directo al PDF aquí o seleccionalo de tu dispositivo");
                    } else {
                        binding.labelUrlHeader.setText("Enlace Web / Externo");
                        binding.editUrlArchivo.setHint("Pegá el enlace web aquí (https://...)");
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.btnSeleccionarPdfLocal.setOnClickListener(v -> pickPdfLauncher.launch("application/pdf"));

        // Botón guardar
        binding.btnGuardar.setOnClickListener(v -> guardarMaterial());
    }

    private void subirPdfFirebase(android.net.Uri uri) {
        binding.progressUpload.setVisibility(View.VISIBLE);
        binding.btnGuardar.setEnabled(false);
        Toast.makeText(this, "Subiendo PDF...", Toast.LENGTH_SHORT).show();

        String fileName = "pdf_" + System.currentTimeMillis() + ".pdf";
        com.google.firebase.storage.StorageReference storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                .getReference().child(agrupacion).child("materiales").child(fileName);

        storageRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                    binding.editUrlArchivo.setText(downloadUri.toString());
                    Toast.makeText(AgregarMaterialActivity.this, "PDF cargado correctamente", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                    Toast.makeText(AgregarMaterialActivity.this, "Error al obtener URL del PDF", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                    Toast.makeText(AgregarMaterialActivity.this, "Error al subir PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        binding.editContenidoTexto.setBackgroundResource(inputBg);
        binding.editUrlArchivo.setBackgroundResource(inputBg);
        binding.spinnerCategoria.setBackgroundResource(inputBg);
        binding.spinnerTipoFormato.setBackgroundResource(inputBg);
    }

    private void guardarMaterial() {
        String nombre = binding.editNombre.getText().toString().trim();
        String categoria = binding.spinnerCategoria.getSelectedItem().toString();
        int tipoPos = binding.spinnerTipoFormato.getSelectedItemPosition();
        boolean visible = binding.switchVisible.isChecked();
        
        String tipoContenido = (tipoPos == 0) ? "TEXTO" : (tipoPos == 1 ? "PDF" : "ENLACE");
        String contenidoTexto = binding.editContenidoTexto.getText().toString().trim();
        String urlArchivo = binding.editUrlArchivo.getText().toString().trim();

        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre del material es requerido", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("TEXTO".equalsIgnoreCase(tipoContenido) && contenidoTexto.isEmpty()) {
            Toast.makeText(this, "Por favor escriba el contenido del material", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!"TEXTO".equalsIgnoreCase(tipoContenido)) {
            if (urlArchivo.isEmpty()) {
                Toast.makeText(this, "El enlace del archivo/web es requerido", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!urlArchivo.startsWith("http://") && !urlArchivo.startsWith("https://")) {
                Toast.makeText(this, "El enlace debe empezar con http:// o https://", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        binding.progressUpload.setVisibility(View.VISIBLE);
        binding.btnGuardar.setEnabled(false);

        String fileId = repository.getMaterialesCollection(agrupacion).document().getId();

        Material material = new Material(
                fileId,
                nombre,
                categoria,
                visible,
                urlArchivo,
                contenidoTexto,
                tipoContenido,
                System.currentTimeMillis()
        );

        // Guardar en Firestore
        repository.getMaterialesCollection(agrupacion).document(fileId).set(material)
                .addOnSuccessListener(aVoid -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    Toast.makeText(AgregarMaterialActivity.this, "Material guardado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressUpload.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                    Toast.makeText(AgregarMaterialActivity.this, "Error al guardar material: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
