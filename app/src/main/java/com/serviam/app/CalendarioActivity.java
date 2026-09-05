package com.serviam.app;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.model.Evento;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityCalendarioBinding;
import com.serviam.app.util.SantoralUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarioActivity extends AppCompatActivity {

    private ActivityCalendarioBinding binding;
    private ServiamRepository repository;
    private PreferencesManager prefs;
    private String agrupacion;
    private String userRol;
    private Calendar selectedCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCalendarioBinding.inflate(getLayoutInflater());
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

        // Configurar FAB para Capitanes y Admins
        if ("capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol)) {
            binding.fabAddEvento.setVisibility(View.VISIBLE);
            binding.fabAddEvento.setOnClickListener(v -> mostrarDialogoCrearEvento());
        } else {
            binding.fabAddEvento.setVisibility(View.GONE);
        }

        // Listener del CalendarView
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, month);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            actualizarVistaFecha();
        });

        actualizarVistaFecha();
    }

    private void personalizarDiseno(String agrupacion) {
        int primaryColor;
        int darkColor;
        if ("juanas".equalsIgnoreCase(agrupacion)) {
            primaryColor = getResources().getColor(R.color.color_juanas, getTheme());
            darkColor = getResources().getColor(R.color.color_juanas_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_juanas);
        } else {
            primaryColor = getResources().getColor(R.color.color_halcones, getTheme());
            darkColor = getResources().getColor(R.color.color_halcones_oscuro, getTheme());
            binding.rootLayout.setBackgroundResource(R.color.color_halcones);
        }
        binding.btnBack.setBackgroundTintList(ColorStateList.valueOf(darkColor));
        binding.fabAddEvento.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
    }

    private void actualizarVistaFecha() {
        Date selectedDate = selectedCalendar.getTime();
        
        // Formatos
        SimpleDateFormat sdfDisplay = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES"));
        String dateStrDisplay = sdfDisplay.format(selectedDate);
        dateStrDisplay = dateStrDisplay.substring(0, 1).toUpperCase() + dateStrDisplay.substring(1);
        binding.textSelectedDateLabel.setText(dateStrDisplay);

        SimpleDateFormat sdfDdMmYyyy = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaSelectedFormatted = sdfDdMmYyyy.format(selectedDate);

        SimpleDateFormat sdfMmDd = new SimpleDateFormat("MM-dd", Locale.getDefault());
        String fechaMmDd = sdfMmDd.format(selectedDate);

        SimpleDateFormat sdfDdMm = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String fechaDdMm = sdfDdMm.format(selectedDate);

        // 1. ACTIVIDAD DEL DÍA
        cargarActividadDelDia(fechaSelectedFormatted);

        // 2. CUMPLEAÑOS DEL DÍA
        cargarCumpleanosDelDia(fechaDdMm);

        // 3. FESTIVIDAD DEL DÍA
        cargarFestividadDelDia(selectedDate);

        // 4. PRÓXIMA ACTIVIDAD MÁS CERCANA
        cargarProximaActividad(selectedDate);

        // 5. PRÓXIMOS CUMPLEAÑOS
        cargarProximosCumpleanos();

        // 6. PRÓXIMA FESTIVIDAD
        cargarProximaFestividad(selectedDate);
    }

    private void cargarActividadDelDia(String fechaFormatted) {
        repository.getEventosCollection(agrupacion)
                .whereEqualTo("fecha", fechaFormatted)
                .get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        binding.cardActividadDia.setVisibility(View.VISIBLE);
                        binding.textActividadTitulo.setText(doc.getString("titulo"));
                        
                        String hora = doc.getString("hora");
                        String lugar = doc.getString("lugar");
                        String info = (hora != null ? hora : "") + (lugar != null && !lugar.isEmpty() ? " — " + lugar : "");
                        binding.textActividadInfo.setText(info);
                        
                        String desc = doc.getString("descripcion");
                        if (desc != null && !desc.isEmpty()) {
                            binding.textActividadDesc.setText(desc);
                            binding.textActividadDesc.setVisibility(View.VISIBLE);
                        } else {
                            binding.textActividadDesc.setVisibility(View.GONE);
                        }
                    } else {
                        binding.cardActividadDia.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> binding.cardActividadDia.setVisibility(View.GONE));
    }

    private void cargarCumpleanosDelDia(String hoyDdMm) {
        repository.getChicosCollection(agrupacion).get()
                .addOnSuccessListener(query -> {
                    List<String> cumpleaneros = new ArrayList<>();
                    if (query != null) {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            Boolean activoBool = doc.getBoolean("activo");
                            boolean activo = activoBool != null ? activoBool : true;
                            if (!activo) continue; // Filtrar dados de baja

                            String fechaNac = doc.getString("fechaNacimiento");
                            String nombre = doc.getString("nombre");
                            String brigada = doc.getString("brigada");

                            if (fechaNac != null && fechaNac.contains(hoyDdMm)) {
                                String displayBrigada = ChicosActivity.getBrigadaDisplay(brigada, agrupacion);
                                cumpleaneros.add(nombre + " (" + displayBrigada + ")");
                            }
                        }
                    }

                    if (!cumpleaneros.isEmpty()) {
                        binding.cardCumpleanosDia.setVisibility(View.VISIBLE);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < cumpleaneros.size(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(cumpleaneros.get(i));
                        }
                        binding.textCumpleanosDiaList.setText("🎂 ¡Hoy cumple años " + sb.toString() + "!");
                    } else {
                        binding.cardCumpleanosDia.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> binding.cardCumpleanosDia.setVisibility(View.GONE));
    }

    private void cargarFestividadDelDia(Date date) {
        SantoralUtils.SantoInfo santo = SantoralUtils.getSantoDelDia(date);
        if (santo != null && santo.getNombre() != null) {
            binding.cardFestividadDia.setVisibility(View.VISIBLE);
            binding.textSantoDiaTitle.setText(santo.getNombre());
            String subtitulo = santo.getPatronoSubtitulo(agrupacion);
            if (subtitulo != null && !subtitulo.isEmpty()) {
                binding.textSantoDiaNombre.setText(subtitulo);
                binding.textSantoDiaNombre.setVisibility(View.VISIBLE);
            } else {
                binding.textSantoDiaNombre.setVisibility(View.GONE);
            }
        } else {
            binding.cardFestividadDia.setVisibility(View.GONE);
        }
    }

    private void cargarProximaActividad(Date fromDate) {
        repository.getEventosCollection(agrupacion).get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        DocumentSnapshot proximoDoc = null;
                        long minDiffMillis = Long.MAX_VALUE;
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                        for (DocumentSnapshot doc : query.getDocuments()) {
                            String fechaStr = doc.getString("fecha");
                            if (fechaStr == null) continue;
                            try {
                                Date evDate = sdf.parse(fechaStr);
                                if (evDate != null && !evDate.before(fromDate)) {
                                    long diff = evDate.getTime() - fromDate.getTime();
                                    if (diff < minDiffMillis) {
                                        minDiffMillis = diff;
                                        proximoDoc = doc;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        if (proximoDoc != null) {
                            binding.textProximaActividadTitulo.setText(proximoDoc.getString("titulo"));
                            String fecha = proximoDoc.getString("fecha");
                            String hora = proximoDoc.getString("hora");
                            String lugar = proximoDoc.getString("lugar");
                            String info = (fecha != null ? fecha : "") + 
                                    (hora != null && !hora.isEmpty() ? " — " + hora : "") +
                                    (lugar != null && !lugar.isEmpty() ? " (" + lugar + ")" : "");
                            binding.textProximaActividadInfo.setText(info);
                            binding.cardProximaActividad.setVisibility(View.VISIBLE);
                        } else {
                            binding.textProximaActividadTitulo.setText("No hay actividades futuras agendadas");
                            binding.textProximaActividadInfo.setText("Los dirigentes avisarán pronto de las nuevas reuniones.");
                            binding.cardProximaActividad.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.textProximaActividadTitulo.setText("No hay actividades agendadas");
                        binding.textProximaActividadInfo.setText("Próximamente se publicará el cronograma.");
                        binding.cardProximaActividad.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.textProximaActividadTitulo.setText("No hay actividades agendadas");
                    binding.textProximaActividadInfo.setText("");
                });
    }

    private void cargarProximosCumpleanos() {
        repository.getChicosCollection(agrupacion).get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        Calendar calToday = Calendar.getInstance();
                        int currentDayOfYear = calToday.get(Calendar.DAY_OF_YEAR);

                        List<String> proximosList = new ArrayList<>();

                        for (DocumentSnapshot doc : query.getDocuments()) {
                            Boolean activoBool = doc.getBoolean("activo");
                            boolean activo = activoBool != null ? activoBool : true;
                            if (!activo) continue;

                            String fechaNac = doc.getString("fechaNacimiento");
                            String nombre = doc.getString("nombre");
                            String brigada = doc.getString("brigada");

                            if (fechaNac != null && fechaNac.length() >= 5) {
                                try {
                                    String[] parts = fechaNac.split("/");
                                    if (parts.length >= 2) {
                                        int day = Integer.parseInt(parts[0]);
                                        int month = Integer.parseInt(parts[1]) - 1;

                                        Calendar calBday = Calendar.getInstance();
                                        calBday.set(Calendar.MONTH, month);
                                        calBday.set(Calendar.DAY_OF_MONTH, day);

                                        int bdayDayOfYear = calBday.get(Calendar.DAY_OF_YEAR);
                                        int diff = bdayDayOfYear - currentDayOfYear;
                                        if (diff < 0) diff += 365;

                                        if (diff > 0 && diff <= 30) {
                                            String displayBrigada = ChicosActivity.getBrigadaDisplay(brigada, agrupacion);
                                            proximosList.add(nombre + " (" + displayBrigada + ") — " + parts[0] + "/" + parts[1]);
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        if (!proximosList.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < Math.min(proximosList.size(), 4); i++) {
                                sb.append("• ").append(proximosList.get(i)).append("\n");
                            }
                            binding.textProximosCumpleanosList.setText(sb.toString().trim());
                        } else {
                            binding.textProximosCumpleanosList.setText("No hay cumpleaños registrados en los próximos 30 días.");
                        }
                    } else {
                        binding.textProximosCumpleanosList.setText("No hay cumpleaños próximos.");
                    }
                })
                .addOnFailureListener(e -> binding.textProximosCumpleanosList.setText("Sin cumpleaños próximos."));
    }

    private void cargarProximaFestividad(Date fromDate) {
        Map.Entry<String, SantoralUtils.SantoInfo> entry = SantoralUtils.getProximaFestividad(fromDate);
        if (entry != null && entry.getValue() != null) {
            SantoralUtils.SantoInfo santo = entry.getValue();
            binding.textProximaFestividadTitle.setText(santo.getNombre());
            String subtitulo = santo.getPatronoSubtitulo(agrupacion);
            String keyFecha = entry.getKey();
            String fechaPrefix = "";
            if (keyFecha != null && keyFecha.contains("-")) {
                String[] p = keyFecha.split("-");
                fechaPrefix = p[1] + "/" + p[0] + " · ";
            }
            binding.textProximaFestividadInfo.setText(fechaPrefix + (subtitulo != null ? subtitulo : "Festividad Litúrgica"));
            binding.cardProximaFestividad.setVisibility(View.VISIBLE);
        } else {
            binding.cardProximaFestividad.setVisibility(View.GONE);
        }
    }

    private void mostrarDialogoCrearEvento() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_chico, null); // Reutilizamos dialog_agregar_chico o adaptamos
        
        // Creamos layout programático simple y limpio para el diálogo de evento
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText editTitulo = new EditText(this);
        editTitulo.setHint("Título del evento (ej: Reunión General)");
        
        EditText editFecha = new EditText(this);
        editFecha.setHint("Fecha (DD/MM/AAAA)");
        ChicosActivity.aplicarFormatoFechaAuto(editFecha);

        // Prellenar fecha seleccionada
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        editFecha.setText(sdf.format(selectedCalendar.getTime()));

        EditText editHora = new EditText(this);
        editHora.setHint("Hora (ej: 16:00 hs)");

        EditText editLugar = new EditText(this);
        editLugar.setHint("Lugar (ej: Predio Parroquial)");

        EditText editDesc = new EditText(this);
        editDesc.setHint("Detalles (ej: Traer uniforme completo)");

        int inputBg = agrupacion.equals("juanas") ? R.drawable.bg_input_field_juanas : R.drawable.bg_input_field;
        editTitulo.setBackgroundResource(inputBg);
        editFecha.setBackgroundResource(inputBg);
        editHora.setBackgroundResource(inputBg);
        editLugar.setBackgroundResource(inputBg);
        editDesc.setBackgroundResource(inputBg);

        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, margin, 0, margin);

        layout.addView(editTitulo, lp);
        layout.addView(editFecha, lp);
        layout.addView(editHora, lp);
        layout.addView(editLugar, lp);
        layout.addView(editDesc, lp);

        builder.setTitle("Crear Actividad / Evento");
        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String titulo = editTitulo.getText().toString().trim();
            String fecha = editFecha.getText().toString().trim();
            String hora = editHora.getText().toString().trim();
            String lugar = editLugar.getText().toString().trim();
            String desc = editDesc.getText().toString().trim();

            if (titulo.isEmpty() || fecha.isEmpty()) {
                Toast.makeText(CalendarioActivity.this, "El título y la fecha son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            String docId = repository.getEventosCollection(agrupacion).document().getId();
            String userNombre = prefs.getUserNombre();

            Evento evento = new Evento(docId, titulo, desc, fecha, hora, lugar, agrupacion, userNombre);

            repository.getEventosCollection(agrupacion).document(docId).set(evento)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(CalendarioActivity.this, "Evento guardado en el calendario", Toast.LENGTH_SHORT).show();
                        actualizarVistaFecha();
                    })
                    .addOnFailureListener(e -> Toast.makeText(CalendarioActivity.this, "Error al guardar evento", Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancelar", null);
        builder.create().show();
    }
}
