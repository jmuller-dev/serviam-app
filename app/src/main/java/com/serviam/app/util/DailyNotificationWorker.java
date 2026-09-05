package com.serviam.app.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.serviam.app.R;
import com.serviam.app.data.local.PreferencesManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DailyNotificationWorker extends Worker {

    public static final String CHANNEL_ID = "serviam_daily_notifications";
    public static final String WORK_NAME = "serviam_daily_work";

    public DailyNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PreferencesManager prefs = new PreferencesManager(context);
        String agrupacion = prefs.getSelectedAgrupacion();
        if (agrupacion == null || agrupacion.isEmpty()) {
            agrupacion = "halcones";
        }

        String userRol = prefs.getUserRol();
        SantoralUtils.SantoInfo santoInfo = SantoralUtils.getSantoDelDia();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String hoyMmDd = sdf.format(new Date());

        List<String> cumpleaneros = new ArrayList<>();
        try {
            QuerySnapshot snap = Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection(agrupacion).document("datos").collection("chicos")
                        .get()
            );
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    String fechaNac = doc.getString("fechaNacimiento");
                    String nombre = doc.getString("nombre");
                    String brigada = doc.getString("brigada");
                    if (fechaNac != null && fechaNac.contains(hoyMmDd)) {
                        String display = nombre + (brigada != null ? " (" + brigada + ")" : "");
                        cumpleaneros.add(display);
                    }
                }
            }
        } catch (Exception ignored) {}

        enviarNotificacionesSeparadas(context, cumpleaneros, santoInfo, agrupacion, userRol);
        return Result.success();
    }

    public static void enviarNotificacionDirecta(Context context, List<String> cumpleaneros, SantoralUtils.SantoInfo santoInfo, String agrupacion, String userRol) {
        enviarNotificacionesSeparadas(context, cumpleaneros, santoInfo, agrupacion, userRol);
    }

    public static void enviarNotificacionesSeparadas(Context context, List<String> cumpleaneros, SantoralUtils.SantoInfo santoInfo, String agrupacion, String userRol) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Notificaciones Diarias Serviam",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Avisos de cumpleaños y festividades del santoral");
                manager.createNotificationChannel(channel);
            }

            boolean esDirigente = "capitan".equalsIgnoreCase(userRol) || "admin".equalsIgnoreCase(userRol);

            // 1. Notificación de Cumpleaños: SOLO PARA DIRIGENTES (Capitanes y Admins)
            if (esDirigente && cumpleaneros != null && !cumpleaneros.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cumpleaneros.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(cumpleaneros.get(i));
                }
                String bodyCumple = "🎂 ¡Hoy cumple años " + sb.toString() + "!";
                
                NotificationCompat.Builder builderCumple = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Serviam App · Cumpleaños")
                        .setContentText(bodyCumple)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodyCumple))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true);

                manager.notify(1001, builderCumple.build());
            }

            // 2. Notificación de Santo / Festividad: PARA CHICOS Y DIRIGENTES (Todos)
            if (santoInfo != null && santoInfo.getNombre() != null) {
                String tituloSanto = "Serviam App · Festividad";
                String subtituloPatrono = santoInfo.getPatronoSubtitulo(agrupacion);
                String bodySanto = "✝️ ¡Feliz día de " + santoInfo.getNombre() + "!" +
                        (subtituloPatrono != null && !subtituloPatrono.trim().isEmpty() ? "\n" + subtituloPatrono : "");

                NotificationCompat.Builder builderSanto = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(tituloSanto)
                        .setContentText("¡Feliz día de " + santoInfo.getNombre() + "!")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodySanto))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true);

                manager.notify(1002, builderSanto.build());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void scheduleDailyWork(Context context) {
        try {
            Calendar dueDate = Calendar.getInstance();
            Calendar currentDate = Calendar.getInstance();

            // Programar para las 09:00 AM
            dueDate.set(Calendar.HOUR_OF_DAY, 9);
            dueDate.set(Calendar.MINUTE, 0);
            dueDate.set(Calendar.SECOND, 0);

            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.HOUR_OF_DAY, 24);
            }

            long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

            Constraints constraints = new Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build();

            PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(
                    DailyNotificationWorker.class,
                    24, TimeUnit.HOURS
            )
                    .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    dailyWorkRequest
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
