package com.serviam.app.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.serviam.app.data.local.AppDatabase;
import com.serviam.app.data.local.ChicoDao;
import com.serviam.app.data.model.Chico;
import java.util.List;

public class ServiamRepository {

    private final ChicoDao chicoDao;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    public ServiamRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.chicoDao = db.chicoDao();
        
        this.firestore = FirebaseFirestore.getInstance();
        // Habilitar persistencia offline solo la primera vez (evita crash al reabrir actividades)
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            this.firestore.setFirestoreSettings(settings);
        } catch (IllegalStateException ignored) {
            // Firestore ya fue inicializado en otra pantalla
        }
        
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    // --- Firebase Auth ---

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public Task<AuthResult> login(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    // --- Firestore Collection Helpers ---

    public CollectionReference getUsuariosCollection(String agrupacion) {
        return firestore.collection(agrupacion).document("datos").collection("usuarios");
    }

    public CollectionReference getChicosCollection(String agrupacion) {
        return firestore.collection(agrupacion).document("datos").collection("chicos");
    }

    public CollectionReference getCampamentosCollection(String agrupacion) {
        return firestore.collection(agrupacion).document("datos").collection("campamentos");
    }

    public CollectionReference getMaterialesCollection(String agrupacion) {
        return firestore.collection(agrupacion).document("datos").collection("materiales");
    }

    public CollectionReference getEventosCollection(String agrupacion) {
        return firestore.collection(agrupacion).document("datos").collection("eventos");
    }

    // --- Whitelist & User Check ---

    /**
     * Busca en la colección de chicos si existe un registro con el DNI ingresado (Whitelist).
     */
    public Task<QuerySnapshot> checkDniInChicos(String agrupacion, String dni) {
        return getChicosCollection(agrupacion)
                .whereEqualTo("dni", dni)
                .get();
    }

    /**
     * Busca en la colección de usuarios si existe un registro con el DNI ingresado (Capitanes/Admin).
     */
    public Task<QuerySnapshot> checkDniInUsuarios(String agrupacion, String dni) {
        return getUsuariosCollection(agrupacion)
                .whereEqualTo("dni", dni)
                .get();
    }

    /**
     * Guarda el usuario registrado en Firestore
     */
    public Task<Void> guardarUsuario(String agrupacion, String uid, String nombre, String dni, String email, String rol) {
        com.serviam.app.data.model.Usuario usuario = new com.serviam.app.data.model.Usuario(
                uid, nombre, dni, email, rol, agrupacion
        );
        return getUsuariosCollection(agrupacion).document(uid).set(usuario);
    }

    /**
     * Obtiene los datos del usuario actual desde Firestore
     */
    public Task<DocumentSnapshot> obtenerUsuario(String agrupacion, String uid) {
        return getUsuariosCollection(agrupacion).document(uid).get();
    }

    // --- Room Local Database (Chicos) ---

    /**
     * Sincroniza en tiempo real la colección de chicos de Firestore con la base de datos local Room.
     */
    public void syncChicosFromFirestore(String agrupacion) {
        getChicosCollection(agrupacion).addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                List<Chico> validChicos = new java.util.ArrayList<>();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    try {
                        Object rawDni = doc.get("dni");
                        String dni = rawDni != null ? String.valueOf(rawDni).trim() : doc.getId();
                        if (dni.isEmpty()) dni = doc.getId();

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

                        boolean activo = isDocumentActivo(doc);

                        Chico c = new Chico(dni, nombre, brigada, edad, fechaNac, telChico, telPadres, asistencia, activo);
                        c.setAgrupacion(agrupacion);
                        validChicos.add(c);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (!validChicos.isEmpty()) {
                    insertLocalChicos(validChicos);
                }
            }
        });
    }

    public LiveData<List<Chico>> getLocalActiveChicos(String agrupacion) {
        return chicoDao.getActiveChicos(agrupacion);
    }

    public LiveData<List<Chico>> getLocalChicosByBrigada(String agrupacion, String brigada) {
        return chicoDao.getChicosByBrigada(agrupacion, brigada);
    }

    public LiveData<Chico> getLocalChicoByDni(String dni) {
        return chicoDao.getChicoByDni(dni);
    }

    public void insertLocalChico(Chico chico) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            chicoDao.insertChico(chico);
        });
    }

    public void insertLocalChicos(List<Chico> chicos) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            chicoDao.insertAll(chicos);
        });
    }

    public void deactivateLocalChico(String dni) {
        AppDatabase.databaseWriteExecutor.execute(() -> chicoDao.deactivateChico(dni));
    }

    public static boolean isDocumentActivo(DocumentSnapshot doc) {
        if (doc == null) return true;
        try {
            Object raw = doc.get("activo");
            if (raw == null) return true;
            if (raw instanceof Boolean) {
                return (Boolean) raw;
            }
            if (raw instanceof String) {
                return !"false".equalsIgnoreCase(((String) raw).trim());
            }
            if (raw instanceof Number) {
                return ((Number) raw).intValue() != 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
