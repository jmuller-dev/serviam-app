package com.serviam.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SantoralUtils {

    public static class SantoInfo {
        private String nombre;
        private String patronoHalcones;
        private String patronoJuanas;

        public SantoInfo(String nombre, String patronoHalcones, String patronoJuanas) {
            this.nombre = nombre;
            this.patronoHalcones = patronoHalcones;
            this.patronoJuanas = patronoJuanas;
        }

        public SantoInfo(String nombre, String patronoGeneral) {
            this(nombre, patronoGeneral, patronoGeneral);
        }

        public String getNombre() { return nombre; }

        public String getPatronoSubtitulo(String agrupacion) {
            if ("juanas".equalsIgnoreCase(agrupacion)) {
                return patronoJuanas;
            }
            return patronoHalcones;
        }
    }

    private static final Map<String, SantoInfo> SANTORAL_MAP = new HashMap<>();

    static {
        // Enero
        SANTORAL_MAP.put("01-01", new SantoInfo("Santa María, Madre de Dios", "Solemnidad de la Virgen"));
        SANTORAL_MAP.put("01-06", new SantoInfo("Epifanía del Señor", "Día de Reyes"));
        SANTORAL_MAP.put("01-31", new SantoInfo("San Juan Bosco", "Padre y Maestro de la Juventud"));

        // Febrero
        SANTORAL_MAP.put("02-02", new SantoInfo("Nuestra Señora de la Candelaria", "Fiesta de la Luz"));
        SANTORAL_MAP.put("02-11", new SantoInfo("Nuestra Señora de Lourdes", "Jornada Mundial del Enfermo"));
        SANTORAL_MAP.put("02-14", new SantoInfo("San Valentín", "Patrono del Amor y la Amistad"));

        // Marzo
        SANTORAL_MAP.put("03-19", new SantoInfo("San José", "Esposo de la Virgen María y Protector"));
        SANTORAL_MAP.put("03-25", new SantoInfo("La Anunciación del Señor", "Fiesta de la Encarnación"));

        // Abril
        SANTORAL_MAP.put("04-23", new SantoInfo("San Jorge", "Patrono Mundial de los Scouts y Halcones", "Patrono Mundial de los Scouts"));
        SANTORAL_MAP.put("04-29", new SantoInfo("Santa Catalina de Siena", "Doctora de la Iglesia"));

        // Mayo
        SANTORAL_MAP.put("05-01", new SantoInfo("San José Obrero", "Patrono de los Trabajadores"));
        SANTORAL_MAP.put("05-13", new SantoInfo("Nuestra Señora de Fátima", "Virgen del Rosario"));
        SANTORAL_MAP.put("05-30", new SantoInfo("Santa Juana de Arco", null, "Patrona de la Agrupación Santa Juana de Arco"));

        // Junio
        SANTORAL_MAP.put("06-13", new SantoInfo("San Antonio de Padua", "Doctor de la Iglesia"));
        SANTORAL_MAP.put("06-24", new SantoInfo("San Juan Bautista", "Precursor del Señor"));
        SANTORAL_MAP.put("06-29", new SantoInfo("San Pedro y San Pablo", "Columnas de la Iglesia"));

        // Julio
        SANTORAL_MAP.put("07-16", new SantoInfo("Nuestra Señora del Carmen", "Patrona y Madre"));
        SANTORAL_MAP.put("07-25", new SantoInfo("Santiago el Mayor", "Apóstol y Guía"));
        SANTORAL_MAP.put("07-31", new SantoInfo("San Ignacio de Loyola", "Fundador de la Compañía de Jesús"));

        // Agosto
        SANTORAL_MAP.put("08-08", new SantoInfo("Santo Domingo de Guzmán", "Fundador de la Orden de Predicadores"));
        SANTORAL_MAP.put("08-15", new SantoInfo("La Asunción de la Virgen María", "Fiesta de la Asunción"));
        SANTORAL_MAP.put("08-30", new SantoInfo("Santa Rosa de Lima", "Patrona de América"));

        // Septiembre
        SANTORAL_MAP.put("09-08", new SantoInfo("Natividad de la Santísima Virgen María", "Fiesta Mariana"));
        SANTORAL_MAP.put("09-24", new SantoInfo("Nuestra Señora de la Merced", "Patrona de los Cautivos"));
        SANTORAL_MAP.put("09-29", new SantoInfo("San Miguel Arcángel", "Patrono de la brigada Halcones", null));

        // Octubre
        SANTORAL_MAP.put("10-01", new SantoInfo("Santa Teresita del Niño Jesús", "Patrona de las Misiones"));
        SANTORAL_MAP.put("10-04", new SantoInfo("San Francisco de Asís", "Ejemplo de Humildad y Hermandad"));
        SANTORAL_MAP.put("10-12", new SantoInfo("Nuestra Señora del Pilar", "Virgen de la Hispanidad"));
        SANTORAL_MAP.put("10-22", new SantoInfo("San Juan Pablo II", "El Papa de los Jóvenes"));

        // Noviembre
        SANTORAL_MAP.put("11-01", new SantoInfo("Día de Todos los Santos", "Solemnidad de Todos los Santos"));
        SANTORAL_MAP.put("11-02", new SantoInfo("Fieles Difuntos", "Conmemoración de los Difuntos"));
        SANTORAL_MAP.put("11-17", new SantoInfo("San Roque González", "Patrono de la brigada Conquistadores", null));
        SANTORAL_MAP.put("11-22", new SantoInfo("Santa Cecilia", "Patrona de la Música"));

        // Diciembre
        SANTORAL_MAP.put("12-08", new SantoInfo("Inmaculada Concepción", "Patrona de la Pureza"));
        SANTORAL_MAP.put("12-12", new SantoInfo("Nuestra Señora de Guadalupe", "Empatrona de las Américas"));
        SANTORAL_MAP.put("12-25", new SantoInfo("Natividad del Señor", "¡Feliz Navidad!"));
    }

    public static SantoInfo getSantoDelDia() {
        return getSantoDelDia(new Date());
    }

    public static SantoInfo getSantoDelDia(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());
        String key = sdf.format(date);
        
        if (SANTORAL_MAP.containsKey(key)) {
            return SANTORAL_MAP.get(key);
        }

        return null; // Ocultar si no hay santo en la fecha
    }

    public static String getFechaHoyFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES"));
        String fecha = sdf.format(new Date());
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }

    public static String getDiaMesHoy() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static Map.Entry<String, SantoInfo> getProximaFestividad(Date fromDate) {
        if (fromDate == null) fromDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd", Locale.getDefault());
        String currentKey = sdf.format(fromDate);

        String bestKey = null;
        int minDiff = 999;

        java.util.Calendar calFrom = java.util.Calendar.getInstance();
        calFrom.setTime(fromDate);
        int currentDayOfYear = calFrom.get(java.util.Calendar.DAY_OF_YEAR);

        for (String key : SANTORAL_MAP.keySet()) {
            try {
                String[] parts = key.split("-");
                int m = Integer.parseInt(parts[0]) - 1;
                int d = Integer.parseInt(parts[1]);

                java.util.Calendar calSanto = java.util.Calendar.getInstance();
                calSanto.set(java.util.Calendar.YEAR, calFrom.get(java.util.Calendar.YEAR));
                calSanto.set(java.util.Calendar.MONTH, m);
                calSanto.set(java.util.Calendar.DAY_OF_MONTH, d);

                int santoDayOfYear = calSanto.get(java.util.Calendar.DAY_OF_YEAR);
                int diff = santoDayOfYear - currentDayOfYear;
                if (diff <= 0) {
                    diff += 365;
                }

                if (diff < minDiff) {
                    minDiff = diff;
                    bestKey = key;
                }
            } catch (Exception ignored) {}
        }

        if (bestKey != null) {
            return new java.util.AbstractMap.SimpleEntry<>(bestKey, SANTORAL_MAP.get(bestKey));
        }
        return null;
    }
}
