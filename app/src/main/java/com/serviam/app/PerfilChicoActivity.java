package com.serviam.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Chico;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityPerfilChicoBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PerfilChicoActivity extends AppCompatActivity {

    private ActivityPerfilChicoBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String userRol;
    private String chicoDni;
    private Chico currentChico;
    private String targetUserUid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPerfilChicoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Insets setup
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ServiamRepository(this);
        prefs = new PreferencesManager(this);

        chicoDni = getIntent().getStringExtra("CHICO_DNI");
        agrupacion = getIntent().getStringExtra("AGRUPACION");
        if (agrupacion == null) {
            agrupacion = prefs.getSelectedAgrupacion();
            if (agrupacion == null) agrupacion = "halcones";
        }
        userRol = prefs.getUserRol();

        personalizarDiseno(agrupacion);

        // Volver atrás
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnEdit.setOnClickListener(v -> mostrarDialogoEditarChico());

        if (chicoDni != null) {
            repository.getChicosCollection(agrupacion).document(chicoDni).get()
                    .addOnSuccessListener(doc -> {
                        runOnUiThread(() -> {
                            if (doc != null && doc.exists()) {
                                try {
                                    Object rawDni = doc.get("dni");
                                    String dni = rawDni != null ? String.valueOf(rawDni).trim() : doc.getId();
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
                                    boolean activo = ServiamRepository.isDocumentActivo(doc);

                                    Chico chico = new Chico(dni, nombre, brigada, edad, fechaNac, telChico, telPadres, asistencia, activo);
                                    chico.setAgrupacion(agrupacion);

                                    if (chico.isActivo()) {
                                        targetUserUid = null;
                                        currentChico = chico;
                                        cargarDatosChico(chico);

                                        if ("capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol)) {
                                            binding.btnEdit.setVisibility(View.VISIBLE);
                                        } else {
                                            binding.btnEdit.setVisibility(View.GONE);
                                        }
                                    } else {
                                        Toast.makeText(this, "El integrante fue dado de baja.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                // Si no está en chicos, buscar en usuarios
                                repository.getUsuariosCollection(agrupacion)
                                        .whereEqualTo("dni", chicoDni)
                                        .get()
                                        .addOnCompleteListener(task -> {
                                            runOnUiThread(() -> {
                                                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                                                    com.google.firebase.firestore.DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                                                    targetUserUid = userDoc.getId();
                                                    Object rawNombre = userDoc.get("nombre");
                                                    String nombre = rawNombre != null ? String.valueOf(rawNombre) : null;
                                                    Object rawRol = userDoc.get("rol");
                                                    String rol = rawRol != null ? String.valueOf(rawRol) : null;
                                                    Object rawEmail = userDoc.get("email");
                                                    String email = rawEmail != null ? String.valueOf(rawEmail) : "";
                                                    Object rawFechaNac = userDoc.get("fechaNacimiento");
                                                    String fechaNac = rawFechaNac != null ? String.valueOf(rawFechaNac) : "N/A";
                                                    Object rawTelChico = userDoc.get("telefonoChico");
                                                    String telChico = rawTelChico != null ? String.valueOf(rawTelChico) : email;
                                                    Object rawTelPadres = userDoc.get("telefonoPadres");
                                                    String telPadres = rawTelPadres != null ? String.valueOf(rawTelPadres) : "N/A";

                                                    if (nombre != null) {
                                                        String brigadaLabel = (rol != null && "capitan".equalsIgnoreCase(rol)) ? "Capitanes" : "Admins";
                                                        currentChico = new Chico(
                                                                chicoDni,
                                                                nombre,
                                                                brigadaLabel,
                                                                0,
                                                                fechaNac,
                                                                telChico,
                                                                telPadres,
                                                                "",
                                                                true
                                                        );
                                                        currentChico.setAgrupacion(agrupacion);
                                                        cargarDatosChico(currentChico);
                                                    } else {
                                                        cargarPerfilDesdePrefs();
                                                    }
                                                } else {
                                                    cargarPerfilDesdePrefs();
                                                }
                                            });
                                        });
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> cargarPerfilDesdePrefs());
                    });
        }

        // Configurar botones de llamada
        binding.btnCallChico.setOnClickListener(v -> {
            if (currentChico != null && currentChico.getTelefonoChico() != null && !currentChico.getTelefonoChico().isEmpty()) {
                llamarTelefono(currentChico.getTelefonoChico());
            } else {
                Toast.makeText(this, "Teléfono no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCallPadres.setOnClickListener(v -> {
            if (currentChico != null && currentChico.getTelefonoPadres() != null && !currentChico.getTelefonoPadres().isEmpty()) {
                llamarTelefono(currentChico.getTelefonoPadres());
            } else {
                Toast.makeText(this, "Teléfono no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        // Configurar acción de dar de baja y toggle asistencia
        binding.btnBajaChico.setOnClickListener(v -> confirmarBaja());
        binding.btnToggleAsistenciaHoy.setOnClickListener(v -> toggleAsistenciaHoy());
    }

    private void personalizarDiseno(String agrupacion) {
        int primaryColor;
        int darkColor;
        if ("juanas".equalsIgnoreCase(agrupacion)) {
            primaryColor = ContextCompat.getColor(this, R.color.color_juanas);
            darkColor = ContextCompat.getColor(this, R.color.color_juanas_oscuro);
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
            binding.labelAsistencia.setTextColor(primaryColor);
            binding.labelContacto.setTextColor(primaryColor);
            binding.btnToggleAsistenciaHoy.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            binding.btnCallChico.setColorFilter(primaryColor);
            binding.btnCallPadres.setColorFilter(primaryColor);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.btnEdit.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        } else {
            primaryColor = ContextCompat.getColor(this, R.color.color_halcones);
            darkColor = ContextCompat.getColor(this, R.color.color_halcones_oscuro);
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
            binding.labelAsistencia.setTextColor(primaryColor);
            binding.labelContacto.setTextColor(primaryColor);
            binding.btnToggleAsistenciaHoy.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            binding.btnCallChico.setColorFilter(primaryColor);
            binding.btnCallPadres.setColorFilter(primaryColor);
            binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
            binding.btnEdit.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        }
    }

    private void cargarPerfilDesdePrefs() {
        if (chicoDni == null || chicoDni.isEmpty()) {
            chicoDni = prefs.getUserDni();
        }
        targetUserUid = prefs.getUserUid();
        if (targetUserUid == null) targetUserUid = chicoDni;
        String userNombre = prefs.getUserNombre();
        if (userNombre == null || userNombre.isEmpty()) userNombre = "Administrador";
        String brigadaLabel = "capitan".equalsIgnoreCase(userRol) ? "Capitanes" : "Admins";
        currentChico = new Chico(chicoDni, userNombre, brigadaLabel, 0, "N/A", "", "N/A", "", true);
        currentChico.setAgrupacion(agrupacion);
        cargarDatosChico(currentChico);
    }

    private void cargarDatosChico(Chico chico) {
        binding.textChicoNombre.setText(chico.getNombre());
        String dniVal = chico.getDni();
        boolean isRealDni = dniVal != null && !dniVal.isEmpty() && dniVal.length() <= 12 && !dniVal.contains("-") && (targetUserUid == null || !dniVal.equalsIgnoreCase(targetUserUid));
        if (isRealDni) {
            binding.textChicoDni.setText("DNI: " + dniVal);
        } else {
            binding.textChicoDni.setText("DNI: No registrado");
        }
        
        // Inicial en avatar
        if (chico.getNombre() != null && !chico.getNombre().isEmpty()) {
            binding.textAvatarLetter.setText(chico.getNombre().substring(0, 1).toUpperCase());
        }

        // Brigada badge
        String displayBrigada = getBrigadaDisplay(chico.getBrigada(), agrupacion);
        binding.textChicoBrigada.setText(displayBrigada.toUpperCase());
        
        int colorRes;
        String brigadaRaw = chico.getBrigada() != null ? chico.getBrigada().toLowerCase() : "";
        switch (brigadaRaw) {
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
            case "padre":
            case "sacerdote":
                colorRes = R.color.negro;
                break;
            case "halcones":
            case "juana":
            default:
                colorRes = R.color.brigada_halcones;
                break;
        }
        binding.textChicoBrigada.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));

        // Teléfonos y Edad
        if ("N/A".equals(chico.getFechaNacimiento()) || chico.getFechaNacimiento() == null) {
            binding.textChicoEdadNac.setText("No registrado");
        } else {
            binding.textChicoEdadNac.setText(chico.getEdad() + " años (" + chico.getFechaNacimiento() + ")");
        }
        
        binding.textChicoTel.setText(chico.getTelefonoChico() != null && !chico.getTelefonoChico().isEmpty() ? 
                chico.getTelefonoChico() : "No registrado");
        binding.textChicoPadresTel.setText(chico.getTelefonoPadres() != null && !chico.getTelefonoPadres().isEmpty() ? 
                chico.getTelefonoPadres() : "No registrado");

        boolean isOwnProfile = (chicoDni != null && chicoDni.equalsIgnoreCase(prefs.getUserDni()));

        if (isOwnProfile || "admin".equalsIgnoreCase(userRol) || "capitan".equalsIgnoreCase(userRol)) {
            binding.btnEdit.setVisibility(View.VISIBLE);
        } else {
            binding.btnEdit.setVisibility(View.GONE);
        }

        boolean isCapitanOrAdminMember = targetUserUid != null ||
                "capitanes".equalsIgnoreCase(brigadaRaw) ||
                "dirigentes".equalsIgnoreCase(brigadaRaw) ||
                "admins".equalsIgnoreCase(brigadaRaw) ||
                "capitan".equalsIgnoreCase(brigadaRaw) ||
                "admin".equalsIgnoreCase(brigadaRaw) ||
                "dirigente".equalsIgnoreCase(brigadaRaw) ||
                "padre".equalsIgnoreCase(brigadaRaw) ||
                (isOwnProfile && ("admin".equalsIgnoreCase(userRol) || "capitan".equalsIgnoreCase(userRol) || "dirigente".equalsIgnoreCase(userRol) || "padre".equalsIgnoreCase(userRol)));

        if (isCapitanOrAdminMember) {
            // Eliminar contacto de tutor para Padre, Capitanes, Dirigentes y Admins (son mayores de edad)
            binding.dividerPadres.setVisibility(View.GONE);
            binding.labelContactoPadres.setVisibility(View.GONE);
            binding.layoutContactoPadres.setVisibility(View.GONE);
        } else {
            binding.dividerPadres.setVisibility(View.VISIBLE);
            binding.labelContactoPadres.setVisibility(View.VISIBLE);
            binding.layoutContactoPadres.setVisibility(View.VISIBLE);
        }

        if (isOwnProfile) {
            binding.btnBajaChico.setVisibility(View.GONE);
        } else if ("admin".equalsIgnoreCase(userRol) || "capitan".equalsIgnoreCase(userRol) || "padre".equalsIgnoreCase(userRol)) {
            binding.btnBajaChico.setVisibility(View.VISIBLE);
        } else {
            binding.btnBajaChico.setVisibility(View.GONE);
        }

        boolean isHalconesGroup = !"juanas".equalsIgnoreCase(agrupacion);

        if (isHalconesGroup || isCapitanOrAdminMember) {
            // En Halcones NO hay asistencia para nadie. En Santa Juana, Dirigentes/Admins/Padre tampoco tienen asistencia.
            binding.labelAsistencia.setVisibility(View.GONE);
            binding.cardAsistencia.setVisibility(View.GONE);
            binding.btnToggleAsistenciaHoy.setVisibility(View.GONE);
        } else {
            // Solo las chicas de Santa Juana tienen registro de asistencia
            binding.labelAsistencia.setVisibility(View.VISIBLE);
            binding.cardAsistencia.setVisibility(View.VISIBLE);
            if ("admin".equalsIgnoreCase(userRol) || "capitan".equalsIgnoreCase(userRol) || "dirigente".equalsIgnoreCase(userRol) || "padre".equalsIgnoreCase(userRol)) {
                binding.btnToggleAsistenciaHoy.setVisibility(View.VISIBLE);
            } else {
                binding.btnToggleAsistenciaHoy.setVisibility(View.GONE);
            }
            dibujarAsistencia(chico);
        }

        if (targetUserUid != null) {
            binding.btnBajaChico.setText("ELIMINAR USUARIO");
        } else {
            binding.btnBajaChico.setText("DAR DE BAJA INTEGRANTE");
        }
    }

    private void dibujarAsistencia(Chico chico) {
        binding.layoutAsistenciaCeldas.removeAllViews();
        List<String> ultimosSabados = obtenerUltimosSabados();
        
        // Parsear asistencia guardada (ej: "2026-06-06,2026-06-13")
        String asistenciaStr = chico.getAsistencia();
        if (asistenciaStr == null) asistenciaStr = "";
        
        int presentes = 0;
        int totalCeldas = ultimosSabados.size();

        for (String sabado : ultimosSabados) {
            boolean presente = asistenciaStr.contains(sabado);
            if (presente) presentes++;

            // Crear celda circular
            LinearLayout cellLayout = new LinearLayout(this);
            cellLayout.setOrientation(LinearLayout.VERTICAL);
            cellLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            cellLayout.setLayoutParams(params);

            // Círculo
            View circle = new View(this);
            int sizePx = (int) (32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(sizePx, sizePx);
            circleParams.bottomMargin = (int) (6 * getResources().getDisplayMetrics().density);
            circle.setLayoutParams(circleParams);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            if (presente) {
                shape.setColor(getResources().getColor(R.color.verde_punto, getTheme()));
            } else {
                shape.setColor(getResources().getColor(R.color.rojo_punto, getTheme()));
            }
            circle.setBackground(shape);

            // Texto fecha (ej: "06 Jun")
            TextView textDate = new TextView(this);
            textDate.setGravity(Gravity.CENTER);
            textDate.setTextSize(10f);
            textDate.setTextColor(getResources().getColor(R.color.negro, getTheme()));
            try {
                textDate.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.montserrat));
            } catch (Exception ignored) {}

            try {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date date = parser.parse(sabado);
                SimpleDateFormat formatter = new SimpleDateFormat("dd MMM", new Locale("es", "ES"));
                textDate.setText(formatter.format(date));
            } catch (Exception e) {
                textDate.setText(sabado);
            }

            cellLayout.addView(circle);
            cellLayout.addView(textDate);
            binding.layoutAsistenciaCeldas.addView(cellLayout);
        }

        // Actualizar porcentaje
        int porcentaje = totalCeldas > 0 ? (presentes * 100) / totalCeldas : 100;
        binding.textPorcentajeAsistencia.setText(porcentaje + "%");
        if (porcentaje >= 75) {
            binding.textPorcentajeAsistencia.setTextColor(getResources().getColor(R.color.verde_punto, getTheme()));
        } else {
            binding.textPorcentajeAsistencia.setTextColor(getResources().getColor(R.color.rojo_punto, getTheme()));
        }

        // Actualizar texto del botón de asistencia para hoy
        String hoy = obtenerFechaHoy();
        if (asistenciaStr.contains(hoy)) {
            binding.btnToggleAsistenciaHoy.setText("MARCAR AUSENTE HOY");
            binding.btnToggleAsistenciaHoy.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.rojo_punto, getTheme())));
        } else {
            binding.btnToggleAsistenciaHoy.setText("MARCAR PRESENTE HOY");
            int primaryColor = agrupacion.equals("juanas") ? 
                    getResources().getColor(R.color.color_juanas, getTheme()) :
                    getResources().getColor(R.color.color_halcones, getTheme());
            binding.btnToggleAsistenciaHoy.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        }
    }

    private List<String> obtenerUltimosSabados() {
        List<String> sabados = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();

        // Encontrar el sábado más cercano (hoy o anterior)
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek != Calendar.SATURDAY) {
            int diff = Calendar.SATURDAY - dayOfWeek;
            if (diff > 0) diff -= 7; // El sábado anterior
            cal.add(Calendar.DAY_OF_YEAR, diff);
        }

        // Agregar 4 sábados
        for (int i = 0; i < 4; i++) {
            sabados.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, -7);
        }

        // Invertir para orden cronológico
        List<String> ordenados = new ArrayList<>();
        for (int i = sabados.size() - 1; i >= 0; i--) {
            ordenados.add(sabados.get(i));
        }
        return ordenados;
    }

    private String obtenerFechaHoy() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        
        // Mapear hoy al sábado de esta semana para tomar asistencia del día de reunión
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek != Calendar.SATURDAY) {
            int diff = Calendar.SATURDAY - dayOfWeek;
            if (diff > 0) diff -= 7; // Sábado anterior
            cal.add(Calendar.DAY_OF_YEAR, diff);
        }
        return sdf.format(cal.getTime());
    }

    private void toggleAsistenciaHoy() {
        if (currentChico == null) return;
        
        String hoy = obtenerFechaHoy();
        String asistencia = currentChico.getAsistencia();
        if (asistencia == null) asistencia = "";

        List<String> list = new ArrayList<>();
        if (!asistencia.isEmpty()) {
            for (String s : asistencia.split(",")) {
                if (!s.trim().isEmpty()) list.add(s.trim());
            }
        }

        if (list.contains(hoy)) {
            list.remove(hoy);
            Toast.makeText(this, "Se marcó AUSENTE para hoy", Toast.LENGTH_SHORT).show();
        } else {
            list.add(hoy);
            Toast.makeText(this, "Se marcó PRESENTE para hoy", Toast.LENGTH_SHORT).show();
        }

        // Unir de nuevo
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(",");
        }
        currentChico.setAsistencia(sb.toString());

        // Guardar
        repository.insertLocalChico(currentChico);
        repository.getChicosCollection(agrupacion).document(currentChico.getDni()).set(currentChico);
    }

    private void llamarTelefono(String numero) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + numero));
        startActivity(intent);
    }

    private void confirmarBaja() {
        String title = (targetUserUid != null) ? "Eliminar usuario" : "Dar de baja";
        String message = (targetUserUid != null) ?
                "¿Estás seguro de que deseas eliminar permanentemente a " + currentChico.getNombre() + "?" :
                "¿Estás seguro de que deseas dar de baja a " + currentChico.getNombre() + "?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Sí, confirmar", (dialog, which) -> {
                    if (currentChico == null) return;

                    String dni = currentChico.getDni();
                    java.util.Map<String, Object> bajaUpdate = new java.util.HashMap<>();
                    bajaUpdate.put("activo", false);

                    if (dni != null && !dni.isEmpty()) {
                        repository.deactivateLocalChico(dni);
                        repository.getChicosCollection(agrupacion).document(dni).update(bajaUpdate);
                        repository.getChicosCollection(agrupacion).whereEqualTo("dni", dni).get()
                                .addOnSuccessListener(query -> {
                                    if (query != null) {
                                        for (DocumentSnapshot doc : query.getDocuments()) {
                                            doc.getReference().update("activo", false);
                                        }
                                    }
                                });
                    }

                    if (targetUserUid != null && !targetUserUid.isEmpty()) {
                        repository.getUsuariosCollection(agrupacion).document(targetUserUid).update("activo", false);
                    }

                    if (dni != null && !dni.isEmpty()) {
                        repository.getUsuariosCollection(agrupacion).whereEqualTo("dni", dni).get()
                                .addOnSuccessListener(query -> {
                                    if (query != null) {
                                        for (DocumentSnapshot doc : query.getDocuments()) {
                                            doc.getReference().update("activo", false);
                                        }
                                    }
                                });
                    }

                    Toast.makeText(PerfilChicoActivity.this, "Integrante dado de baja", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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

    private void mostrarDialogoEditarChico() {
        if (currentChico == null) return;

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

        // Pre-llenar campos
        String currentDniVal = currentChico.getDni();
        boolean isRealDni = currentDniVal != null && !currentDniVal.isEmpty() && currentDniVal.length() <= 12 && !currentDniVal.contains("-") && (targetUserUid == null || !currentDniVal.equalsIgnoreCase(targetUserUid));

        if (isRealDni) {
            editDni.setText(currentDniVal);
        } else {
            editDni.setText("");
        }
        editDni.setEnabled(true); // Permite editar o agregar DNI
        editNombre.setText(currentChico.getNombre());
        editEdad.setText(String.valueOf(currentChico.getEdad()));
        editFechaNac.setText(currentChico.getFechaNacimiento());
        editTelChico.setText(currentChico.getTelefonoChico());
        editTelPadres.setText(currentChico.getTelefonoPadres());

        // Auto-formateador de fecha DD/MM/AAAA
        ChicosActivity.aplicarFormatoFechaAuto(editFechaNac);

        TextView labelBrigada = dialogView.findViewById(R.id.labelBrigada);
        if (labelBrigada != null && "juanas".equalsIgnoreCase(agrupacion)) {
            labelBrigada.setText("Compañía");
        }

        // Adaptar fondos de cajas de texto del diálogo al color del grupo
        int inputBg = agrupacion.equals("juanas") ? R.drawable.bg_input_field_juanas : R.drawable.bg_input_field;
        editDni.setBackgroundResource(inputBg);
        editNombre.setBackgroundResource(inputBg);
        editEdad.setBackgroundResource(inputBg);
        editFechaNac.setBackgroundResource(inputBg);
        editTelChico.setBackgroundResource(inputBg);
        editTelPadres.setBackgroundResource(inputBg);
        spinnerBrigada.setBackgroundResource(inputBg);

        // Configurar Spinner de Brigadas/Compañías
        String[] brigadas;
        if (agrupacion.equals("juanas")) {
            brigadas = new String[]{"Juana", "Goretti", "Inés", "Jacinta", "Dirigentes"};
        } else {
            brigadas = new String[]{"Halcones", "Conquistadores", "Pioneros", "Leñadores", "Capitanes"};
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, brigadas);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBrigada.setAdapter(spinnerAdapter);

        // Seleccionar la brigada actual en el spinner
        String currentBrigada = currentChico.getBrigada();
        String displayBrigada = getBrigadaDisplay(currentBrigada, agrupacion);
        for (int i = 0; i < brigadas.length; i++) {
            if (brigadas[i].equalsIgnoreCase(displayBrigada)) {
                spinnerBrigada.setSelection(i);
                break;
            }
        }

        builder.setTitle("Editar Integrante");
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String inputDni = editDni.getText().toString().trim();
            String nombre = editNombre.getText().toString().trim();
            String brigada = spinnerBrigada.getSelectedItem().toString();
            String fechaNac = editFechaNac.getText().toString().trim();
            String telChico = editTelChico.getText().toString().trim();
            String telPadres = editTelPadres.getText().toString().trim();

            if (nombre.isEmpty()) {
                Toast.makeText(PerfilChicoActivity.this, "El nombre es requerido", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar DNI si fue ingresado
            if (!inputDni.isEmpty()) {
                currentChico.setDni(inputDni);
            } else if (!isRealDni && chicoDni != null) {
                currentChico.setDni(chicoDni);
            }

            // Actualizar objeto
            currentChico.setNombre(nombre);
            currentChico.setBrigada(brigada);
            currentChico.setFechaNacimiento(fechaNac);
            currentChico.setEdad(currentChico.getEdad());
            currentChico.setTelefonoChico(telChico);
            currentChico.setTelefonoPadres(telPadres);
            
            if (currentChico.getAgrupacion() == null || currentChico.getAgrupacion().isEmpty()) {
                currentChico.setAgrupacion(agrupacion);
            }

            String docIdToUse = (chicoDni != null && !chicoDni.isEmpty()) ? chicoDni : currentChico.getDni();

            if (targetUserUid != null) {
                // Es un usuario de la colección de usuarios (Capitán / Admin)
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("nombre", currentChico.getNombre());
                updates.put("dni", currentChico.getDni());
                updates.put("telefonoChico", currentChico.getTelefonoChico());
                updates.put("telefonoPadres", currentChico.getTelefonoPadres());
                updates.put("fechaNacimiento", currentChico.getFechaNacimiento());

                if (currentChico.getDni().equalsIgnoreCase(prefs.getUserDni())) {
                    prefs.setUserNombre(currentChico.getNombre());
                }

                repository.getUsuariosCollection(agrupacion).document(targetUserUid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            // También actualizar en colección de chicos si existe
                            repository.getChicosCollection(agrupacion).document(docIdToUse).set(currentChico);
                            repository.insertLocalChico(currentChico);
                            runOnUiThread(() -> {
                                Toast.makeText(PerfilChicoActivity.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                                cargarDatosChico(currentChico);
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> Toast.makeText(PerfilChicoActivity.this, "Error al actualizar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        });
            } else {
                // Es un chico de la colección de chicos
                if (currentChico.getDni().equalsIgnoreCase(prefs.getUserDni())) {
                    prefs.setUserNombre(currentChico.getNombre());
                }
                repository.getChicosCollection(agrupacion).document(docIdToUse).set(currentChico)
                        .addOnSuccessListener(aVoid -> {
                            repository.insertLocalChico(currentChico);
                            runOnUiThread(() -> {
                                Toast.makeText(PerfilChicoActivity.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                                cargarDatosChico(currentChico);
                            });
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() -> Toast.makeText(PerfilChicoActivity.this, "Error al actualizar perfil", Toast.LENGTH_SHORT).show());
                        });
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.create().show();
    }
}
