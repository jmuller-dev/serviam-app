package com.serviam.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityDashboardChicoBinding;

public class DashboardChicoActivity extends AppCompatActivity {

    private ActivityDashboardChicoBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private com.google.firebase.firestore.ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDashboardChicoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean isAdminPreview = getIntent().getBooleanExtra("IS_ADMIN_PREVIEW", false);
        if (isAdminPreview) {
            binding.bannerAdminPreview.setVisibility(View.VISIBLE);
            binding.btnResetSession.setVisibility(View.GONE);
            binding.btnBack.setVisibility(View.GONE);
            binding.btnReturnToAdmin.setOnClickListener(v -> finish());
        } else {
            binding.bannerAdminPreview.setVisibility(View.GONE);
            binding.btnBack.setVisibility(View.VISIBLE);
        }

        // Manejo de gesto Volver atrás
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isAdminPreview) {
                    finish();
                } else {
                    moveTaskToBack(true);
                }
            }
        });

        // Solicitar permiso de notificaciones en Android 13+ (API 33)
        solicitarPermisoNotificaciones();

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
            agrupacion = "halcones";
        }

        // Configurar saludo dinámico (corto con rango)
        String userNombre = prefs.getUserNombre();
        String userRol = prefs.getUserRol();
        String dni = prefs.getUserDni();

        if (isAdminPreview) {
            String firstWord = (userNombre != null && userNombre.contains(" ")) ? userNombre.split(" ")[0] : (userNombre != null ? userNombre : "");
            String rank = agrupacion.equals("juanas") ? "Juana " : "Halcón ";
            binding.textChicoName.setText(rank + firstWord);
            String labelPreview = agrupacion.equals("juanas") ? "Compañía (Vista Previa)" : "Brigada (Vista Previa)";
            binding.textBrigadaLabel.setText(labelPreview);
        } else {
            String welcomeName = ChicosActivity.formatUserWelcome(userNombre, userRol, agrupacion);
            binding.textChicoName.setText(welcomeName);

            // Cargar Brigada / Compañía desde la base de datos local
            if (dni != null) {
                repository.getLocalChicoByDni(dni).observe(this, chico -> {
                    if (chico != null) {
                        String label = agrupacion.equals("juanas") ? "Compañía" : "Brigada";
                        String displayBrigada = ChicosActivity.getBrigadaDisplay(chico.getBrigada(), agrupacion);
                        binding.textBrigadaLabel.setText(label + " " + displayBrigada);
                    }
                });
            }
        }

        personalizarDashboard(agrupacion);

        // Foto de perfil click
        binding.imageProfileButton.setOnClickListener(v -> {
            if (dni != null && !dni.isEmpty()) {
                Intent intent = new Intent(DashboardChicoActivity.this, PerfilChicoActivity.class);
                intent.putExtra("CHICO_DNI", dni);
                intent.putExtra("AGRUPACION", agrupacion);
                startActivity(intent);
            }
        });

        // Volver abajo (atrás)
        binding.btnBack.setOnClickListener(v -> {
            if (isAdminPreview) {
                finish();
            } else {
                moveTaskToBack(true);
            }
        });

        // Cerrar sesión
        binding.btnResetSession.setOnClickListener(v -> {
            repository.logout();
            prefs.clearSessionCompletely();
            Intent intent = new Intent(DashboardChicoActivity.this, SelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Cargar marcador en tiempo real si hay campamento activo
        cargarMarcadorCampamento();
        cargarCumpleanosYSantoral();

        binding.cardMaterialesEmpty.setOnClickListener(v -> {
            Intent intent = new Intent(this, MaterialesActivity.class);
            intent.putExtra("AGRUPACION", agrupacion);
            startActivity(intent);
        });
        binding.labelMateriales.setOnClickListener(v -> {
            Intent intent = new Intent(this, MaterialesActivity.class);
            intent.putExtra("AGRUPACION", agrupacion);
            startActivity(intent);
        });

        binding.cardCumpleanos.setOnClickListener(v -> abrirCalendario());
        binding.cardSantoral.setOnClickListener(v -> abrirCalendario());
        binding.cardActividades.setOnClickListener(v -> abrirCalendario());
        binding.labelActividades.setOnClickListener(v -> abrirCalendario());
    }

    private void abrirCalendario() {
        Intent intent = new Intent(this, CalendarioActivity.class);
        intent.putExtra("AGRUPACION", agrupacion);
        startActivity(intent);
    }

    private void confirmarSalidaAdminPreview() {
        new androidx.appcompat.app.AlertDialog.Builder(DashboardChicoActivity.this)
                .setTitle("Vista de Chico")
                .setMessage("¿Deseas salir del modo vista de chico y volver al panel de administración?")
                .setPositiveButton("Volver a Admin", (dialog, which) -> finish())
                .setNegativeButton("Continuar viendo", null)
                .show();
    }

    private int calcularDiaCampamento(String startCampDateStr) {
        if (startCampDateStr == null || startCampDateStr.isEmpty()) return 1;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date startDate = sdf.parse(startCampDateStr);
            if (startDate != null) {
                java.util.Date today = new java.util.Date();
                java.util.Calendar calStart = java.util.Calendar.getInstance();
                calStart.setTime(startDate);
                calStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
                calStart.set(java.util.Calendar.MINUTE, 0);
                calStart.set(java.util.Calendar.SECOND, 0);
                calStart.set(java.util.Calendar.MILLISECOND, 0);

                java.util.Calendar calToday = java.util.Calendar.getInstance();
                calToday.setTime(today);
                calToday.set(java.util.Calendar.HOUR_OF_DAY, 0);
                calToday.set(java.util.Calendar.MINUTE, 0);
                calToday.set(java.util.Calendar.SECOND, 0);
                calToday.set(java.util.Calendar.MILLISECOND, 0);

                long diffInMillis = calToday.getTimeInMillis() - calStart.getTimeInMillis();
                long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                return (int) (diffInDays + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private String getStringSafe(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) return "";
        Object raw = doc.get(field);
        return raw != null ? String.valueOf(raw) : "";
    }

    private long getLongSafe(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) return 0L;
        Object raw = doc.get(field);
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong(((String) raw).trim());
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private void cargarMarcadorCampamento() {
        firestoreListener = repository.getCampamentosCollection(agrupacion)
                .whereEqualTo("activo", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    try {
                        if (value != null && !value.isEmpty()) {
                            DocumentSnapshot doc = value.getDocuments().get(0);
                            binding.cardCrearCampamento.setVisibility(View.GONE);
                            binding.cardCampamentoActivoInfo.setVisibility(View.VISIBLE);

                            String nombreCamp = getStringSafe(doc, "nombre");
                            binding.textCampActiveName.setText(nombreCamp.isEmpty() ? "Campamento" : nombreCamp);

                            String fechaInicio = getStringSafe(doc, "fechaInicio");
                            int dia = calcularDiaCampamento(fechaInicio);
                            binding.textCampActiveDay.setText("Día " + dia + " · Tocá para ver puntos");

                            // Mayor
                            String m1 = getStringSafe(doc, "mayorEquipo1");
                            String m2 = getStringSafe(doc, "mayorEquipo2");
                            binding.textTeamsMayor.setText((m1.isEmpty() ? "Equipo 1" : m1) + " vs " + (m2.isEmpty() ? "Equipo 2" : m2));
                            long scoreMayor1 = getLongSafe(doc, "mayorPuntajeEquipo1");
                            long scoreMayor2 = getLongSafe(doc, "mayorPuntajeEquipo2");
                            binding.textScoreMayor1.setText(String.valueOf(scoreMayor1));
                            binding.textScoreMayor2.setText(String.valueOf(scoreMayor2));

                            // Menor
                            String me1 = getStringSafe(doc, "menorEquipo1");
                            String me2 = getStringSafe(doc, "menorEquipo2");
                            binding.textTeamsMenor.setText((me1.isEmpty() ? "Equipo 1" : me1) + " vs " + (me2.isEmpty() ? "Equipo 2" : me2));
                            long scoreMenor1 = getLongSafe(doc, "menorPuntajeEquipo1");
                            long scoreMenor2 = getLongSafe(doc, "menorPuntajeEquipo2");
                            binding.textScoreMenor1.setText(String.valueOf(scoreMenor1));
                            binding.textScoreMenor2.setText(String.valueOf(scoreMenor2));

                            binding.cardCampamentoActivoInfo.setOnClickListener(v -> {
                                Intent intent = new Intent(DashboardChicoActivity.this, PuntosActivity.class);
                                intent.putExtra("CAMP_ID", doc.getId());
                                intent.putExtra("AGRUPACION", agrupacion);
                                startActivity(intent);
                            });
                        } else {
                            binding.cardCampamentoActivoInfo.setVisibility(View.GONE);
                            binding.cardCrearCampamento.setVisibility(View.VISIBLE);

                            binding.textTitleCrearCamp.setText("Sin campamento activo");
                            binding.textSubTitleCrearCamp.setText("No hay ninguno activo por ahora");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        binding.cardCampamentoActivoInfo.setVisibility(View.GONE);
                        binding.cardCrearCampamento.setVisibility(View.VISIBLE);
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

    private void personalizarDashboard(String agrupacion) {
        int primaryDarkColor = agrupacion.equals("juanas") ?
                getResources().getColor(R.color.color_juanas_oscuro, getTheme()) :
                getResources().getColor(R.color.color_halcones_oscuro, getTheme());

        binding.textSantoTitle.setTextColor(primaryDarkColor);
        binding.textSantoNombre.setTextColor(primaryDarkColor);
        binding.textCumpleanosInfo.setTextColor(primaryDarkColor);
        binding.labelMateriales.setTextColor(primaryDarkColor);
        binding.labelActividades.setTextColor(primaryDarkColor);

        if (agrupacion.equals("juanas")) {
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
            binding.textWelcomeLabel.setText("BIENVENIDA");
            binding.cardCrearCampamento.setCardBackgroundColor(getResources().getColor(R.color.color_juanas_oscuro, getTheme()));
        } else {
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.textWelcomeLabel.setText("BIENVENIDO");
            binding.cardCrearCampamento.setCardBackgroundColor(getResources().getColor(R.color.color_halcones_oscuro, getTheme()));
        }
    }

    private void cargarCumpleanosYSantoral() {
        try {
            com.serviam.app.util.SantoralUtils.SantoInfo santoInfo = com.serviam.app.util.SantoralUtils.getSantoDelDia();
            if (santoInfo != null && santoInfo.getNombre() != null) {
                binding.textSantoTitle.setText("¡Día de " + santoInfo.getNombre() + "!");
                String subtituloPatrono = santoInfo.getPatronoSubtitulo(agrupacion);
                if (subtituloPatrono != null && !subtituloPatrono.trim().isEmpty()) {
                    binding.textSantoNombre.setText(subtituloPatrono);
                    binding.textSantoNombre.setVisibility(View.VISIBLE);
                } else {
                    binding.textSantoNombre.setVisibility(View.GONE);
                }
                binding.cardSantoral.setVisibility(View.VISIBLE);
            } else {
                binding.cardSantoral.setVisibility(View.GONE);
            }

            String hoyMmDd = com.serviam.app.util.SantoralUtils.getDiaMesHoy();
            repository.getChicosCollection(agrupacion).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        try {
                            java.util.List<String> cumpleaneros = new java.util.ArrayList<>();
                            if (queryDocumentSnapshots != null) {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                    if (!ServiamRepository.isDocumentActivo(doc)) continue;

                                    String fechaNac = getStringSafe(doc, "fechaNacimiento");
                                    String nombre = getStringSafe(doc, "nombre");
                                    String brigada = getStringSafe(doc, "brigada");
                                    if (!fechaNac.isEmpty() && fechaNac.contains(hoyMmDd)) {
                                        String display = nombre + (!brigada.isEmpty() ? " (" + brigada + ")" : "");
                                        cumpleaneros.add(display);
                                    }
                                }
                            }

                            if (!cumpleaneros.isEmpty()) {
                                binding.cardCumpleanos.setVisibility(View.VISIBLE);
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < cumpleaneros.size(); i++) {
                                    if (i > 0) sb.append(", ");
                                    sb.append(cumpleaneros.get(i));
                                }
                                binding.textCumpleanosInfo.setText("🎂 ¡Hoy cumple años " + sb.toString() + "!");
                            } else {
                                binding.cardCumpleanos.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            binding.cardCumpleanos.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.cardCumpleanos.setVisibility(View.GONE);
                    });

            com.serviam.app.util.DailyNotificationWorker.scheduleDailyWork(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}
