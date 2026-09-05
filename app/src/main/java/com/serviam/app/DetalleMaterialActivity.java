package com.serviam.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Material;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityDetalleMaterialBinding;

public class DetalleMaterialActivity extends AppCompatActivity {

    private ActivityDetalleMaterialBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String materialId;
    private Material currentMaterial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDetalleMaterialBinding.inflate(getLayoutInflater());
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

        materialId = getIntent().getStringExtra("MATERIAL_ID");

        personalizarDiseno(agrupacion);

        // Volver atrás
        binding.btnBack.setOnClickListener(v -> finish());

        // Cargar detalles de material
        if (materialId != null) {
            cargarDetalleMaterial();
        } else {
            binding.progressPdf.setVisibility(View.GONE);
            binding.textError.setVisibility(View.VISIBLE);
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
            binding.btnOpenExternal.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.textMaterialCategory.setTextColor(primaryColor);
            binding.textMaterialCategory.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_juanas_mas_claro, getTheme())));
        } else {
            primaryColor = getResources().getColor(R.color.color_halcones, getTheme());
            darkColor = getResources().getColor(R.color.color_halcones_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.btnOpenExternal.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.textMaterialCategory.setTextColor(primaryColor);
            binding.textMaterialCategory.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_halcones_mas_claro, getTheme())));
        }
    }

    private void cargarDetalleMaterial() {
        repository.getMaterialesCollection(agrupacion).document(materialId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMaterial = documentSnapshot.toObject(Material.class);
                        if (currentMaterial != null) {
                            binding.textMaterialName.setText(currentMaterial.getNombre());
                            binding.textMaterialCategory.setText(currentMaterial.getCategoria().toUpperCase());

                            String tipo = currentMaterial.getTipoContenido();

                            if ("TEXTO".equalsIgnoreCase(tipo) && currentMaterial.getContenido() != null && !currentMaterial.getContenido().isEmpty()) {
                                binding.progressPdf.setVisibility(View.GONE);
                                binding.webViewPdf.setVisibility(View.GONE);
                                binding.scrollTextoNativo.setVisibility(View.VISIBLE);
                                binding.textContenidoNativo.setText(currentMaterial.getContenido());
                                binding.btnOpenExternal.setVisibility(View.GONE);
                            } else if (currentMaterial.getUrlArchivo() != null && !currentMaterial.getUrlArchivo().isEmpty()) {
                                binding.scrollTextoNativo.setVisibility(View.GONE);
                                binding.webViewPdf.setVisibility(View.VISIBLE);
                                binding.btnOpenExternal.setVisibility(View.VISIBLE);
                                setupWebView(currentMaterial.getUrlArchivo());
                            } else {
                                binding.progressPdf.setVisibility(View.GONE);
                                binding.webViewPdf.setVisibility(View.GONE);
                                binding.scrollTextoNativo.setVisibility(View.GONE);
                                binding.textError.setVisibility(View.VISIBLE);
                                binding.btnOpenExternal.setVisibility(View.GONE);
                            }

                            // Botón abrir externo
                            binding.btnOpenExternal.setOnClickListener(v -> {
                                if (currentMaterial.getUrlArchivo() != null && !currentMaterial.getUrlArchivo().isEmpty()) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentMaterial.getUrlArchivo()));
                                    startActivity(intent);
                                }
                            });
                        }
                    } else {
                        binding.progressPdf.setVisibility(View.GONE);
                        binding.textError.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressPdf.setVisibility(View.GONE);
                    binding.textError.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error al cargar material", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupWebView(String url) {
        WebSettings settings = binding.webViewPdf.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        binding.webViewPdf.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                binding.progressPdf.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                binding.progressPdf.setVisibility(View.GONE);
                Toast.makeText(DetalleMaterialActivity.this, "Error al cargar PDF: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        // Cargar usando Google Docs Viewer
        String docUrl = "https://docs.google.com/gview?embedded=true&url=" + Uri.encode(url);
        binding.webViewPdf.loadUrl(docUrl);
    }
}
