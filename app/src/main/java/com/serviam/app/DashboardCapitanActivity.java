package com.serviam.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
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
import com.serviam.app.databinding.ActivityDashboardCapitanBinding;

public class DashboardCapitanActivity extends AppCompatActivity {

    private ActivityDashboardCapitanBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        binding = ActivityDashboardCapitanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Manejo de gesto Volver atrás (minimizar app en vez de regresar a selección)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
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

        prefs = new PreferencesManager(this);
        repository = new ServiamRepository(this);
        
        agrupacion = getIntent().getStringExtra("AGRUPACION");
        if (agrupacion == null) {
            agrupacion = prefs.getSelectedAgrupacion();
            if (agrupacion == null) agrupacion = "halcones";
        }

        // Formatear saludo según rango y nombre del usuario (corto con rango)
        String userRol = prefs.getUserRol();
        String userNombre = prefs.getUserNombre();
        
        String welcomeName = ChicosActivity.formatUserWelcome(userNombre, userRol, agrupacion);
        binding.textCapitanName.setText(welcomeName);

        personalizarDashboard(agrupacion);

        // Configurar menú desplegable para Admin / Padre / botón de logout para Capitanes
        if ("admin".equalsIgnoreCase(userRol) || "padre".equalsIgnoreCase(userRol)) {
            binding.btnMenuAdmin.setVisibility(View.VISIBLE);
            binding.btnResetSession.setVisibility(View.GONE);

            binding.btnMenuAdmin.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(DashboardCapitanActivity.this, binding.btnMenuAdmin);
                popup.getMenu().add(0, 1, 0, "Cambiar de Agrupación");

                if ("admin".equalsIgnoreCase(userRol)) {
                    popup.getMenu().add(0, 2, 1, "Ver Vista de Chico");
                    popup.getMenu().add(0, 8, 2, "Gestionar Roles / Usuarios");
                    popup.getMenu().add(0, 4, 3, "Resumen Estadístico");
                    popup.getMenu().add(0, 5, 4, "Resincronizar Datos");
                    popup.getMenu().add(0, 6, 5, "Probar Notificación");
                    popup.getMenu().add(0, 7, 6, "Diagnóstico del Sistema (Debug)");
                    popup.getMenu().add(0, 3, 7, "Cerrar Sesión");
                } else { // "padre"
                    popup.getMenu().add(0, 3, 1, "Cerrar Sesión");
                }

                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == 1) {
                        String newAgrup = agrupacion.equals("juanas") ? "halcones" : "juanas";
                        prefs.setSelectedAgrupacion(newAgrup);
                        Intent intent = getIntent();
                        intent.putExtra("AGRUPACION", newAgrup);
                        finish();
                        overridePendingTransition(0, 0);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        return true;
                    } else if (itemId == 2) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(DashboardCapitanActivity.this, DashboardChicoActivity.class);
                            intent.putExtra("AGRUPACION", agrupacion);
                            intent.putExtra("IS_ADMIN_PREVIEW", true);
                            startActivity(intent);
                        }, 150);
                        return true;
                    } else if (itemId == 8) {
                        mostrarGestionarRoles();
                        return true;
                    } else if (itemId == 4) {
                        mostrarResumenEstadistico();
                        return true;
                    } else if (itemId == 5) {
                        resincronizarDatosLocales();
                        return true;
                    } else if (itemId == 6) {
                        probarNotificacion();
                        return true;
                    } else if (itemId == 7) {
                        mostrarDiagnosticoSistema();
                        return true;
                    } else if (itemId == 3) {
                        repository.logout();
                        prefs.clearSessionCompletely();
                        Intent intent = new Intent(DashboardCapitanActivity.this, SelectionActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            binding.btnMenuAdmin.setVisibility(View.GONE);
            binding.btnResetSession.setVisibility(View.VISIBLE);
            binding.btnResetSession.setOnClickListener(v -> {
                repository.logout();
                prefs.clearSessionCompletely();
                Intent intent = new Intent(DashboardCapitanActivity.this, SelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Clic en la foto de perfil (Abre el perfil del Admin/Capitán)
        binding.imageProfileButton.setOnClickListener(v -> {
            String myDni = prefs.getUserDni();
            if (myDni != null && !myDni.isEmpty()) {
                Intent intent = new Intent(DashboardCapitanActivity.this, PerfilChicoActivity.class);
                intent.putExtra("CHICO_DNI", myDni);
                intent.putExtra("AGRUPACION", agrupacion);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Perfil no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        // Buscar campamento activo en Firestore
        buscarCampamentoActivo();
        cargarMesTesoro();
        cargarCumpleanosYSantoral();

        // Configurar clics de las tarjetas de administración
        binding.cardChicos.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChicosActivity.class);
            intent.putExtra("AGRUPACION", agrupacion);
            startActivity(intent);
        });

        binding.cardMaterial.setOnClickListener(v -> {
            Intent intent = new Intent(this, MaterialesActivity.class);
            intent.putExtra("AGRUPACION", agrupacion);
            startActivity(intent);
        });

        binding.cardTesoro.setOnClickListener(v -> {
            Intent intent = new Intent(this, TesoroEspiritualActivity.class);
            intent.putExtra("AGRUPACION", agrupacion);
            startActivity(intent);
        });

        binding.cardCalendario.setOnClickListener(v -> abrirCalendario());
        binding.cardCumpleanos.setOnClickListener(v -> abrirCalendario());
        binding.cardSantoral.setOnClickListener(v -> abrirCalendario());
    }

    private void abrirCalendario() {
        Intent intent = new Intent(this, CalendarioActivity.class);
        intent.putExtra("AGRUPACION", agrupacion);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buscarCampamentoActivo();
        cargarMesTesoro();
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

    private void buscarCampamentoActivo() {
        repository.getCampamentosCollection(agrupacion)
                .whereEqualTo("activo", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                        binding.cardCrearCampamento.setVisibility(View.GONE);
                        binding.cardCampamentoActivoInfo.setVisibility(View.VISIBLE);

                        binding.textCampActiveName.setText(doc.getString("nombre"));
                        String fechaInicio = doc.getString("fechaInicio");
                        int dia = calcularDiaCampamento(fechaInicio);
                        binding.textCampActiveDay.setText("Día " + dia + " · Tocá para ver puntos");

                        // Mayor
                        binding.textTeamsMayor.setText(doc.getString("mayorEquipo1") + " vs " + doc.getString("mayorEquipo2"));
                        Long scoreMayor1 = doc.getLong("mayorPuntajeEquipo1");
                        Long scoreMayor2 = doc.getLong("mayorPuntajeEquipo2");
                        binding.textScoreMayor1.setText(String.valueOf(scoreMayor1 != null ? scoreMayor1 : 0));
                        binding.textScoreMayor2.setText(String.valueOf(scoreMayor2 != null ? scoreMayor2 : 0));

                        // Menor
                        binding.textTeamsMenor.setText(doc.getString("menorEquipo1") + " vs " + doc.getString("menorEquipo2"));
                        Long scoreMenor1 = doc.getLong("menorPuntajeEquipo1");
                        Long scoreMenor2 = doc.getLong("menorPuntajeEquipo2");
                        binding.textScoreMenor1.setText(String.valueOf(scoreMenor1 != null ? scoreMenor1 : 0));
                        binding.textScoreMenor2.setText(String.valueOf(scoreMenor2 != null ? scoreMenor2 : 0));

                        binding.cardCampamentoActivoInfo.setOnClickListener(v -> {
                            Intent intent = new Intent(DashboardCapitanActivity.this, PuntosActivity.class);
                            intent.putExtra("CAMP_ID", doc.getId());
                            intent.putExtra("AGRUPACION", agrupacion);
                            startActivity(intent);
                        });
                    } else {
                        binding.cardCampamentoActivoInfo.setVisibility(View.GONE);
                        binding.cardCrearCampamento.setVisibility(View.VISIBLE);

                        binding.cardCrearCampamento.setOnClickListener(v -> {
                            Intent intent = new Intent(DashboardCapitanActivity.this, CrearCampamentoActivity.class);
                            intent.putExtra("AGRUPACION", agrupacion);
                            startActivity(intent);
                        });
                    }
                });
    }

    private void cargarMesTesoro() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(agrupacion).document("datos").collection("tesoro").document("actual")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String mes = task.getResult().getString("mes");
                        if (mes != null && !mes.trim().isEmpty()) {
                            binding.textTesoroMes.setText(mes);
                        } else {
                            binding.textTesoroMes.setText(obtenerMesActual());
                        }
                    } else {
                        binding.textTesoroMes.setText(obtenerMesActual());
                    }
                });
    }

    private String obtenerMesActual() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", new java.util.Locale("es", "ES"));
        String mes = sdf.format(new java.util.Date());
        return mes.substring(0, 1).toUpperCase() + mes.substring(1);
    }

    private void personalizarDashboard(String agrupacion) {
        int primaryDarkColor = agrupacion.equals("juanas") ?
                getResources().getColor(R.color.color_juanas_oscuro, getTheme()) :
                getResources().getColor(R.color.color_halcones_oscuro, getTheme());

        binding.textSantoTitle.setTextColor(primaryDarkColor);
        binding.textSantoNombre.setTextColor(primaryDarkColor);
        binding.textCumpleanosInfo.setTextColor(primaryDarkColor);
        binding.textTitleCalendario.setTextColor(primaryDarkColor);
        binding.imageArrowCal.setColorFilter(primaryDarkColor);

        if (agrupacion.equals("juanas")) {
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
            binding.cardTesoro.setCardBackgroundColor(getResources().getColor(R.color.color_juanas_oscuro, getTheme()));
            binding.starIconContainer.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_juanas_claro, getTheme()))
            );

            binding.textTitleChicos.setTextColor(primaryDarkColor);
            binding.textTitleMaterial.setTextColor(primaryDarkColor);

            binding.textTitleChicos.setText("Chicas");
            binding.textSubTitleChicos.setText("Por Compañía");

            // Cargar iconos de juanas
            binding.imageIconChicos.setImageResource(R.drawable.ic_chicos_juanas);
            binding.imageIconMaterial.setImageResource(R.drawable.ic_book_juanas);

            // Limpiar filtros para ver los colores del PNG nativo
            binding.imageIconChicos.clearColorFilter();
            binding.imageIconMaterial.clearColorFilter();

            ColorStateList lightJ = ColorStateList.valueOf(getResources().getColor(R.color.color_juanas_mas_claro, getTheme()));
            binding.containerIconChicos.setBackgroundTintList(lightJ);
            binding.containerIconMaterial.setBackgroundTintList(lightJ);
            binding.containerIconCal.setBackgroundTintList(lightJ);
            binding.imageIconCalendario.setColorFilter(primaryDarkColor);

            ColorStateList darkJ = ColorStateList.valueOf(getResources().getColor(R.color.color_juanas_oscuro, getTheme()));
            binding.btnMenuAdmin.setBackgroundTintList(darkJ);
            binding.btnResetSession.setBackgroundTintList(darkJ);
            binding.cardCrearCampamento.setCardBackgroundColor(getResources().getColor(R.color.color_juanas_oscuro, getTheme()));
        } else {
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.cardTesoro.setCardBackgroundColor(getResources().getColor(R.color.color_halcones_oscuro, getTheme()));
            binding.starIconContainer.setBackgroundTintList(
                ColorStateList.valueOf(getResources().getColor(R.color.color_halcones_claro, getTheme()))
            );

            binding.textTitleChicos.setTextColor(primaryDarkColor);
            binding.textTitleMaterial.setTextColor(primaryDarkColor);

            binding.textTitleChicos.setText("Chicos");
            binding.textSubTitleChicos.setText("Por Brigada");

            // Cargar iconos de halcones
            binding.imageIconChicos.setImageResource(R.drawable.ic_chicos_halcones);
            binding.imageIconMaterial.setImageResource(R.drawable.ic_book_halcones);

            // Limpiar filtros
            binding.imageIconChicos.clearColorFilter();
            binding.imageIconMaterial.clearColorFilter();

            ColorStateList lightH = ColorStateList.valueOf(getResources().getColor(R.color.color_halcones_mas_claro, getTheme()));
            binding.containerIconChicos.setBackgroundTintList(lightH);
            binding.containerIconMaterial.setBackgroundTintList(lightH);
            binding.containerIconCal.setBackgroundTintList(lightH);
            binding.imageIconCalendario.setColorFilter(primaryDarkColor);

            ColorStateList darkH = ColorStateList.valueOf(getResources().getColor(R.color.color_halcones_oscuro, getTheme()));
            binding.btnMenuAdmin.setBackgroundTintList(darkH);
            binding.btnResetSession.setBackgroundTintList(darkH);
            binding.cardCrearCampamento.setCardBackgroundColor(getResources().getColor(R.color.color_halcones_oscuro, getTheme()));
        }
    }

    private void cargarCumpleanosYSantoral() {
        String userRol = prefs.getUserRol();
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
                    java.util.List<String> cumpleaneros = new java.util.ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Boolean activoBool = doc.getBoolean("activo");
                            boolean activo = activoBool != null ? activoBool : true;
                            if (!activo) continue;

                            String fechaNac = doc.getString("fechaNacimiento");
                            String nombre = doc.getString("nombre");
                            String brigada = doc.getString("brigada");
                            if (fechaNac != null && fechaNac.contains(hoyMmDd)) {
                                String display = nombre + (brigada != null ? " (" + brigada + ")" : "");
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
                })
                .addOnFailureListener(e -> {
                    binding.cardCumpleanos.setVisibility(View.GONE);
                });

        com.serviam.app.util.DailyNotificationWorker.scheduleDailyWork(this);
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

    private void mostrarResumenEstadistico() {
        repository.getChicosCollection(agrupacion).get()
                .addOnSuccessListener(snapshots -> {
                    int totalChicos = 0;
                    java.util.Map<String, Integer> brigadaCount = new java.util.HashMap<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (ServiamRepository.isDocumentActivo(doc)) {
                                totalChicos++;
                                String brigada = doc.getString("brigada");
                                if (brigada == null || brigada.isEmpty()) brigada = "Sin asignación";
                                brigadaCount.put(brigada, brigadaCount.getOrDefault(brigada, 0) + 1);
                            }
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("📊 Resumen de Agrupación (").append(agrupacion.toUpperCase()).append(")\n\n");
                    sb.append("• Total Integrantes Activos: ").append(totalChicos).append("\n\n");
                    sb.append("• Desglose por Brigada/Compañía:\n");
                    for (java.util.Map.Entry<String, Integer> entry : brigadaCount.entrySet()) {
                        sb.append("  - ").append(ChicosActivity.getBrigadaDisplay(entry.getKey(), agrupacion))
                                .append(": ").append(entry.getValue()).append("\n");
                    }

                    new androidx.appcompat.app.AlertDialog.Builder(DashboardCapitanActivity.this)
                            .setTitle("Estadísticas de Agrupación")
                            .setMessage(sb.toString())
                            .setPositiveButton("Aceptar", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(DashboardCapitanActivity.this, "Error al cargar estadísticas", Toast.LENGTH_SHORT).show());
    }

    private void resincronizarDatosLocales() {
        Toast.makeText(this, "Resincronizando datos con el servidor...", Toast.LENGTH_SHORT).show();
        repository.syncChicosFromFirestore(agrupacion);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(DashboardCapitanActivity.this, "¡Datos resincronizados correctamente!", Toast.LENGTH_SHORT).show();
        }, 1200);
    }

    private void probarNotificacion() {
        com.serviam.app.util.DailyNotificationWorker.scheduleDailyWork(this);
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String channelId = "serviam_daily_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "Notificaciones Serviam", android.app.NotificationManager.IMPORTANCE_DEFAULT);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🔔 Prueba de Notificación Admin")
                .setContentText("Las notificaciones diarias y recordatorios están funcionando correctamente.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(999, builder.build());
        }
        Toast.makeText(this, "Notificación de prueba enviada", Toast.LENGTH_SHORT).show();
    }

    private void mostrarDiagnosticoSistema() {
        String dni = prefs.getUserDni();
        String userNombre = prefs.getUserNombre();
        String userRol = prefs.getUserRol();
        String uid = prefs.getUserUid();

        StringBuilder sb = new StringBuilder();
        sb.append("🛠️ DIAGNÓSTICO DE SISTEMA (DEBUG)\n\n");
        sb.append("• Agrupación Activa: ").append(agrupacion.toUpperCase()).append("\n");
        sb.append("• Rol Actual: ").append(userRol != null ? userRol : "admin").append("\n");
        sb.append("• Administrador: ").append(userNombre != null ? userNombre : "Admin").append("\n");
        sb.append("• DNI Sesión: ").append(dni != null ? dni : "N/A").append("\n");
        sb.append("• UID Firebase: ").append(uid != null ? uid : "No vinculado").append("\n");
        sb.append("• Versión Android SDK: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("• Permiso Notificaciones: ").append(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ? "Permitido" : "Denegado")
                        : "Habilitado"
        ).append("\n");

        new androidx.appcompat.app.AlertDialog.Builder(DashboardCapitanActivity.this)
                .setTitle("Información de Debug")
                .setMessage(sb.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void mostrarGestionarRoles() {
        repository.getUsuariosCollection(agrupacion).get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null || snapshots.isEmpty()) {
                        Toast.makeText(this, "No hay usuarios registrados.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs = snapshots.getDocuments();
                    String[] userNames = new String[docs.size()];
                    for (int i = 0; i < docs.size(); i++) {
                        com.google.firebase.firestore.DocumentSnapshot d = docs.get(i);
                        String name = d.getString("nombre");
                        String rol = d.getString("rol");
                        String dni = d.getString("dni");
                        userNames[i] = (name != null ? name : "Sin nombre") + " (" + (rol != null ? rol.toUpperCase() : "CHICO") + " - DNI: " + (dni != null ? dni : "N/A") + ")";
                    }

                    new androidx.appcompat.app.AlertDialog.Builder(DashboardCapitanActivity.this)
                            .setTitle("Gestionar Roles (Seleccionar Usuario)")
                            .setItems(userNames, (dialog, which) -> {
                                com.google.firebase.firestore.DocumentSnapshot selectedUserDoc = docs.get(which);
                                String selectedName = selectedUserDoc.getString("nombre");
                                String currentRol = selectedUserDoc.getString("rol");

                                String[] rolesDisplay = new String[]{"Admin (Administrador General)", "Padre (Asesor de Agrupación)", "Capitán (Capitán / Dirigente)", "Chico (Integrante)"};
                                String[] rolesKeys = new String[]{"admin", "padre", "capitan", "chico"};

                                int selectedIndex = -1;
                                if (currentRol != null) {
                                    for (int r = 0; r < rolesKeys.length; r++) {
                                        if (rolesKeys[r].equalsIgnoreCase(currentRol)) {
                                            selectedIndex = r;
                                            break;
                                        }
                                    }
                                }

                                final int[] chosenIndex = {selectedIndex};
                                new androidx.appcompat.app.AlertDialog.Builder(DashboardCapitanActivity.this)
                                        .setTitle("Asignar rol a " + selectedName)
                                        .setSingleChoiceItems(rolesDisplay, selectedIndex, (dialogRol, whichRol) -> {
                                            chosenIndex[0] = whichRol;
                                        })
                                        .setPositiveButton("Guardar Rol", (dialogRol, whichBtn) -> {
                                            if (chosenIndex[0] >= 0 && chosenIndex[0] < rolesKeys.length) {
                                                String newRol = rolesKeys[chosenIndex[0]];
                                                repository.getUsuariosCollection(agrupacion).document(selectedUserDoc.getId())
                                                        .update("rol", newRol)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(DashboardCapitanActivity.this, "¡Rol de " + selectedName + " actualizado a " + newRol.toUpperCase() + "!", Toast.LENGTH_LONG).show();
                                                        })
                                                        .addOnFailureListener(e -> Toast.makeText(DashboardCapitanActivity.this, "Error al actualizar rol", Toast.LENGTH_SHORT).show());
                                            }
                                        })
                                        .setNegativeButton("Cancelar", null)
                                        .show();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al obtener usuarios", Toast.LENGTH_SHORT).show());
    }
}
