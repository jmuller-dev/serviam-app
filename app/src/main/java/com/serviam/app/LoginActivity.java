package com.serviam.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.serviam.app.data.local.PreferencesManager;
import com.serviam.app.data.repository.ServiamRepository;

// Importamos el View Binding auto-generado para activity_login.xml
import com.serviam.app.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ServiamRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Habilita el diseño de borde a borde (Edge to Edge)
        EdgeToEdge.enable(this);
        
        // Inflamos el XML usando View Binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Ajustamos márgenes (padding) con respecto a la barra de estado y de navegación
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Recuperamos el dato que enviamos desde SelectionActivity
        String agrupacion = getIntent().getStringExtra("agrupacion");

        repository = new ServiamRepository(this);

        // 2. Si no es nulo, adaptamos la pantalla del Login para esa agrupación
        if (agrupacion != null) {
            adaptarDiseno(agrupacion);
        }

        // Listener para el botón "INGRESAR" (Autenticación real con Firebase)
        binding.btnIngresar.setOnClickListener(v -> {
            String dni = binding.editDni.getText().toString().trim();
            String password = binding.editPass.getText().toString().trim();

            if (dni.isEmpty()) {
                binding.editDni.setError("El DNI es obligatorio");
                binding.editDni.requestFocus();
                return;
            }

            if (dni.length() < 7) {
                binding.editDni.setError("Ingresá un DNI válido");
                binding.editDni.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                binding.editPass.setError("La contraseña es obligatoria");
                binding.editPass.requestFocus();
                return;
            }

            // Generamos el correo electrónico basado en el DNI
            String email = dni + "@serviam.app";
            binding.btnIngresar.setEnabled(false);

            // Iniciar sesión con Firebase Auth
            repository.login(email, password).addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful() && authTask.getResult() != null) {
                    String uid = authTask.getResult().getUser().getUid();

                    // Recuperar el perfil del usuario de Firestore
                    repository.obtenerUsuario(agrupacion, uid).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful() && dbTask.getResult() != null && dbTask.getResult().exists()) {
                            com.google.firebase.firestore.DocumentSnapshot doc = dbTask.getResult();
                            String nombre = doc.getString("nombre");
                            String rol = doc.getString("rol");
                            
                            // Guardar sesión local
                            PreferencesManager prefs = new PreferencesManager(LoginActivity.this);
                            prefs.setSession(uid, dni, rol, agrupacion, nombre, binding.checkRecordarme.isChecked());

                            Toast.makeText(LoginActivity.this, "¡Bienvenido/a " + nombre + "!", Toast.LENGTH_SHORT).show();

                            // Ir al Dashboard según el rol
                            Intent intent;
                            if ("capitan".equalsIgnoreCase(rol) || "admin".equalsIgnoreCase(rol)) {
                                intent = new Intent(LoginActivity.this, DashboardCapitanActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, DashboardChicoActivity.class);
                            }
                            intent.putExtra("AGRUPACION", agrupacion);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            binding.btnIngresar.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Error al recuperar datos del perfil.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    binding.btnIngresar.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Error de ingreso: " + authTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        // Listener para ir a la pantalla de Registro
        binding.btnRegistrate.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.putExtra("agrupacion", agrupacion);
            startActivity(intent);
        });

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

        // Listener para el botón "Volver a la selección" (tanto la caja inferior como el texto)
        binding.cajaAsociacion.setOnClickListener(v -> finish());
        binding.textVolver.setOnClickListener(v -> finish());
    }

    /**
     * Adapta visualmente el diseño del Login de acuerdo con la agrupación elegida.
     * @param agrupacion "halcones" o "juanas"
     */
    private void adaptarDiseno(String agrupacion) {
        if (agrupacion.equals("halcones")) {
            // El diseño en activity_login.xml por defecto ya está configurado para Halcones,
            // pero nos aseguramos de asignar los recursos correctos por si acaso.
            binding.logoHalcones.setImageResource(R.drawable.logo_halcones);
            binding.textBienvenido.setText(R.string.bienvenido);
            binding.textSeleccion.setText(R.string.a_halcones);
            
            // Colores correspondientes a Halcones
            binding.getRoot().setBackgroundResource(R.color.color_halcones);
            binding.btnIngresar.setTextColor(getResources().getColor(R.color.color_halcones, getTheme()));
            binding.textVolver.setTextColor(getResources().getColor(R.color.color_halcones_mas_claro, getTheme()));
            binding.cajaAsociacion.setBackgroundResource(R.color.color_halcones_oscuro);
            
            // Fondos de caja de texto
            binding.inputDni.setBackgroundResource(R.drawable.bg_input_field);
            binding.inputPass.setBackgroundResource(R.drawable.bg_input_field);
            
        } else if (agrupacion.equals("juanas")) {
            // Cambiamos el logo y textos para Santa Juana de Arco
            binding.logoHalcones.setImageResource(R.drawable.logo_juanas);
            binding.textBienvenido.setText(R.string.bienvenida); // "Bienvenida" femenino
            binding.textSeleccion.setText(R.string.santa_juana);
            
            // Colores correspondientes a Juanas (Rojos)
            binding.getRoot().setBackgroundResource(R.color.color_juanas);
            binding.btnIngresar.setTextColor(getResources().getColor(R.color.color_juanas, getTheme()));
            binding.textVolver.setTextColor(getResources().getColor(R.color.color_juanas_mas_claro, getTheme()));
            binding.cajaAsociacion.setBackgroundResource(R.color.color_juanas_oscuro);
            
            // Fondos de caja de texto
            binding.inputDni.setBackgroundResource(R.drawable.bg_input_field_juanas);
            binding.inputPass.setBackgroundResource(R.drawable.bg_input_field_juanas);
        }
    }
}
