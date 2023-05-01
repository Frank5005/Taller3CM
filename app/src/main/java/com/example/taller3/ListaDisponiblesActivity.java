package com.example.taller3;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.taller3.adapters.UsersAdapter;
import com.example.taller3.databinding.ActivityListaDisponiblesBinding;
import com.example.taller3.listeners.UserListener;
import com.example.taller3.models.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
public class ListaDisponiblesActivity extends AppCompatActivity implements UserListener {
    ActivityListaDisponiblesBinding binding;
    Usuario Client = new Usuario();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;
    private FirebaseAuth mAuth;
    public static final String PATH_USERS = "users/";
    Usuario personadispo = new Usuario();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListaDisponiblesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        myRef = database.getReference(PATH_USERS);
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            getUsers();
        }
    }
    private void getUsers() {
        List<Usuario> users = new ArrayList<>();
        myRef = database.getReference(PATH_USERS);
        myRef.getDatabase().getReference(PATH_USERS).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DataSnapshot walker : task.getResult().getChildren()) {
                    if (!Objects.requireNonNull(mAuth.getCurrentUser()).getUid().equals(walker.getKey())) {
                        personadispo = walker.getValue(Usuario.class);
                        assert personadispo != null;
                        if (personadispo.isIsdisponible()) {
                            users.add(new Usuario(walker.getKey(), personadispo.getNombre(), personadispo.getApellido(), personadispo.getCorreo(), personadispo.getFotodeperfil(), personadispo.getNumerodeidentificacion(), personadispo.getLatitud(), personadispo.getLongitud()));
                        }
                    }
                }
                if (users.size() > 0) {
                    UsersAdapter usersAdapter = new UsersAdapter(users, this);
                    binding.usersList.setAdapter(usersAdapter);
                    binding.usersList.setVisibility(View.VISIBLE);
                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    @Override
    public void onUserClicked(Usuario user) {
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        myRef = database.getReference(PATH_USERS + Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
        myRef.getDatabase().getReference(PATH_USERS + mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Client = task.getResult().getValue(Usuario.class);
                assert Client != null;
                if (Client.getSiguiendoa() == null) {
                    Client.setSiguiendoa(user.getId());
                    myRef.setValue(Client);
                } else {
                    if (!Objects.equals(Client.getSiguiendoa(), user.getId())) {
                        Client.setSiguiendoa(user.getId());
                        myRef.setValue(Client);
                    }
                }
            }
        });
        startActivity(intent);
        finish();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
        finish();
    }
}