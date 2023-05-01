package com.example.taller3;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.taller3.databinding.ActivityLoginBinding;
import com.example.taller3.models.Usuario;

import java.util.Objects;
public class LoginActivity extends AppCompatActivity {
    ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    DatabaseReference myRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        binding.loginBTN.setVisibility(View.GONE);
        binding.editTextTextPersonName.setVisibility(View.GONE);
        binding.editTextTextPassword.setVisibility(View.GONE);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        } else {
            binding.loginBTN.setVisibility(View.VISIBLE);
            binding.editTextTextPersonName.setVisibility(View.VISIBLE);
            binding.editTextTextPassword.setVisibility(View.VISIBLE);
        }
        binding.loginBTN.setOnClickListener(v -> {
            String email = binding.editTextTextPersonName.getText().toString();
            String password = binding.editTextTextPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                binding.editTextTextPersonName.setError("Email is required");
                binding.editTextTextPassword.setError("Password is required");
            }
            if (!isEmail(binding.editTextTextPersonName)) {
                binding.editTextTextPersonName.setError("Email is not valid");
            } else {
                login(String.valueOf(binding.editTextTextPersonName.getText()), String.valueOf(binding.editTextTextPassword.getText()));
            }
        });
    }
    private void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                updateUI(mAuth.getCurrentUser());
            } else {
                showMessage(Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }
    private void showMessage(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            myRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            myRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        Usuario usuario = task.getResult().getValue(Usuario.class);
                        assert usuario != null;
                        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                        finish();
                    }
                }
            });
        }
    }
    public boolean isEmail(@NonNull EditText text) {
        CharSequence email = text.getText().toString();
        return (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}