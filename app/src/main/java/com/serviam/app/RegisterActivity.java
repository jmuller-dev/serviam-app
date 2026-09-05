package com.serviam.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.repository.ServiamRepository;
import com.serviam.app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private ServiamRepository repository;
    private String agrupacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ServiamRepository(this);
        
        // Obtenemos la agrupación seleccionada
        agrupacion = getIntent().getStringExtra("agrupacion");
        if (agrupacion == null) {
            agrupacion = "halcones";
        }

        // Adaptamos el diseño visual a la agrupación
        adaptarDiseno(agrupacion);

        // Botón volver
        binding.cajaAsociacion.setOnClickListener(v -> finish());
        binding.textVolver.setOnClickListener(v -> finish());

        // Botón crear cuenta
        binding.btnCrearCuenta.setOnClickListener(v -> realizarRegistro());

        // Listener para alternar visibilidad de la contraseña
        binding.btnTogglePass.setOnClickListener(v -> {
            if (binding.editPass.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
                binding.editPass.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                binding.btnTogglePass.setAlpha(0.5f);
            } else {
                binding.editPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                binding.btnTogglePass.setAlpha(1.0f);
            }
            binding.editPass.setSelection(binding.editPass.getText().length());
        });

        // Listener para alternar visibilidad de repetir contraseña
        binding.btnTogglePassRepeat.setOnClickListener(v -> {
            if (binding.editPassRepeat.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
                binding.editPassRepeat.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                binding.btnTogglePassRepeat.setAlpha(0.5f);
            } else {
                binding.editPassRepeat.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                binding.btnTogglePassRepeat.setAlpha(1.0f);
            }
            binding.editPassRepeat.setSelection(binding.editPassRepeat.getText().length());
        });
    }

    private void adaptarDiseno(String agrupacion) {
        if (agrupacion.equals("halcones")) {
            binding.logoHalcones.setImageResource(R.drawable.logo_halcones);
            binding.textSeleccion.setText(R.string.a_halcones);
            binding.getRoot().setBackgroundResource(R.color.color_halcones);
            binding.btnCrearCuenta.setTextColor(getResources().getColor(R.color.color_halcones, getTheme()));
            binding.textVolver.setTextColor(getResources().getColor(R.color.color_halcones_mas_claro, getTheme()));
            binding.cajaAsociacion.setBackgroundResource(R.color.color_halcones_oscuro);
            
            // Fondos de caja de texto
            binding.inputDni.setBackgroundResource(R.drawable.bg_input_field);
            binding.inputPass.setBackgroundResource(R.drawable.bg_input_field);
            binding.inputPassRepeat.setBackgroundResource(R.drawable.bg_input_field);
        } else if (agrupacion.equals("juanas")) {
            binding.logoHalcones.setImageResource(R.drawable.logo_juanas);
            binding.textSeleccion.setText(R.string.santa_juana);
            binding.getRoot().setBackgroundResource(R.color.color_juanas);
            binding.btnCrearCuenta.setTextColor(getResources().getColor(R.color.color_juanas, getTheme()));
            binding.textVolver.setTextColor(getResources().getColor(R.color.color_juanas_mas_claro, getTheme()));
            binding.cajaAsociacion.setBackgroundResource(R.color.color_juanas_oscuro);
            
            // Fondos de caja de texto
            binding.inputDni.setBackgroundResource(R.drawable.bg_input_field_juanas);
            binding.inputPass.setBackgroundResource(R.drawable.bg_input_field_juanas);
            binding.inputPassRepeat.setBackgroundResource(R.drawable.bg_input_field_juanas);
        }
    }

    private void realizarRegistro() {
        String dni = binding.editDni.getText().toString().trim();
        String pass = binding.editPass.getText().toString().trim();
        String passRepeat = binding.editPassRepeat.getText().toString().trim();

        if (dni.isEmpty()) {
            binding.editDni.setError("El DNI es obligatorio");
            binding.editDni.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            binding.editPass.setError("La contraseña es obligatoria");
            binding.editPass.requestFocus();
            return;
        }

        if (!pass.equals(passRepeat)) {
            binding.editPassRepeat.setError("Las contraseñas no coinciden");
            binding.editPassRepeat.requestFocus();
            return;
        }

        // Generamos un email ficticio para Firebase Auth basado en el DNI
        String email = dni + "@serviam.app";

        binding.btnCrearCuenta.setEnabled(false);

        // 1. Verificamos si el DNI está en la whitelist de chicos
        repository.checkDniInChicos(agrupacion, dni).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                String nombre = doc.getString("nombre");
                String brigada = doc.getString("brigada");
                String rol = "chico";
                if ("Capitanes".equalsIgnoreCase(brigada) || "Dirigentes".equalsIgnoreCase(brigada) || "capitan".equalsIgnoreCase(brigada) || "dirigente".equalsIgnoreCase(brigada)) {
                    rol = "capitan";
                }
                crearCuentaFirebase(email, pass, nombre, dni, rol);
            } else {
                // 2. Si no es un chico, verificamos la whitelist de usuarios/capitanes precargados
                repository.checkDniInUsuarios(agrupacion, dni).addOnCompleteListener(taskCap -> {
                    if (taskCap.isSuccessful() && taskCap.getResult() != null && !taskCap.getResult().isEmpty()) {
                        DocumentSnapshot doc = taskCap.getResult().getDocuments().get(0);
                        String nombre = doc.getString("nombre");
                        String rol = doc.getString("rol");
                        crearCuentaFirebase(email, pass, nombre, dni, rol != null ? rol : "capitan");
                    } else {
                        binding.btnCrearCuenta.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "El DNI no está registrado en la whitelist. Hablá con tu Capitán.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void crearCuentaFirebase(String email, String pass, String nombre, String dni, String rol) {
        repository.register(email, pass).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful() && authTask.getResult() != null) {
                String uid = authTask.getResult().getUser().getUid();

                // Registramos al usuario en la colección correspondiente en Firestore
                repository.guardarUsuario(agrupacion, uid, nombre, dni, email, rol).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        // Guardamos localmente en preferencias
                        PreferencesManager prefs = new PreferencesManager(RegisterActivity.this);
                        prefs.setSession(uid, dni, rol, agrupacion, nombre, true);

                        Toast.makeText(RegisterActivity.this, "¡Registro Exitoso! Bienvenido/a " + nombre, Toast.LENGTH_SHORT).show();

                        // Navegamos al Dashboard según el rol
                        Intent intent;
                        if ("capitan".equalsIgnoreCase(rol) || "admin".equalsIgnoreCase(rol)) {
                            intent = new Intent(RegisterActivity.this, DashboardCapitanActivity.class);
                        } else {
                            intent = new Intent(RegisterActivity.this, DashboardChicoActivity.class);
                        }
                        intent.putExtra("AGRUPACION", agrupacion);
                        startActivity(intent);
                        finishAffinity(); // Cerramos la pila de navegación para no volver al registro
                    } else {
                        binding.btnCrearCuenta.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Error al crear perfil: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                binding.btnCrearCuenta.setEnabled(true);
                if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(RegisterActivity.this, "El DNI ya tiene una cuenta registrada.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Error al registrarse: " + authTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
