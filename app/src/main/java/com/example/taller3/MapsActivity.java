package com.example.taller3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.taller3.databinding.ActivityMapsBinding;
import com.example.taller3.models.Usuario;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;
    public static final int REQUEST_CHECK_SETTINGS = 201;
    private final static float INITIAL_ZOOM_LEVEL = 14.5f;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    public Location mCurrentLocation;
    public void setmCurrentLocation(Location mCurrentLocation) {
        this.mCurrentLocation = mCurrentLocation;
    }
    @Override
    public void onBackPressed() {
    }
    public static final String PATH_USERS = "users/";
    Usuario Client = new Usuario();
    Usuario Client2 = new Usuario();
    private String siguiendoa;
    public String getSiguiendoa() {
        return siguiendoa;
    }
    public void setSiguiendoa(String siguiendoa) {
        this.siguiendoa = siguiendoa;
    }

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0;
    private double currentLong = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.example.taller3.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = createLocationRequest();
        binding.toolbarMapas.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.logout) {
                try {
                    stopLocationUpdates();
                    mAuth.signOut();
                    //stopLocationUpdates();
                    Thread.sleep(1000);
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                    finishActivity(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        });
        binding.PDisponiblesBTN.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ListaDisponiblesActivity.class));
            finish();
        });
        binding.toolbarMapas.getMenu().findItem(R.id.disponible).getActionView().findViewById(R.id.switch2).setOnClickListener(v -> {
            Log.d("Switch", "Switch" + ((Switch) v).isChecked());
            if (((Switch) v).isChecked()) {
                myRef = database.getReference(PATH_USERS + Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                myRef.getDatabase().getReference(PATH_USERS + mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Client = task.getResult().getValue(Usuario.class);
                        Client.setIsdisponible(true);
                        myRef.setValue(Client);
                    }
                });
            } else {
                myRef = database.getReference(PATH_USERS + mAuth.getCurrentUser().getUid());
                myRef.getDatabase().getReference(PATH_USERS + mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Client = task.getResult().getValue(Usuario.class);
                        Client.setIsdisponible(false);
                        myRef.setValue(Client);
                    }
                });
            }
        });
        myRef = database.getReference(PATH_USERS);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Usuario user = child.getValue(Usuario.class);
                    if (user != null) {
                        if (!child.getKey().equals(mAuth.getCurrentUser().getUid())) {
                            if (user.isIsdisponible()) {
                                toastDisponible(user);
                            }
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (checkPermissions()) {
            mostrarpuntosjson();
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        for (Location location : locationResult.getLocations()) {
                            setmCurrentLocation(location);
                            mCurrentLocation = location;
                            currentLat = location.getLatitude();
                            currentLong = location.getLongitude();
                        }
                    }
                }
            };
            myRef = database.getReference(PATH_USERS + mAuth.getCurrentUser().getUid());
            myRef.getDatabase().getReference(PATH_USERS + mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Client = task.getResult().getValue(Usuario.class);
                    Location location = new Location("locationA");
                    location.setLatitude(currentLat);
                    location.setLongitude(currentLong);
                    setmCurrentLocation(location);
                    mCurrentLocation = location;
                    if (Client.getLatitud() != null && Client.getLongitud() != null) {
                        if (!Client.getLatitud().equals(currentLat) && !Client.getLongitud().equals(currentLong)) {
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL));
                            LatLng clatlng = new LatLng(currentLat, currentLong);
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(clatlng));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLat, currentLong)));
                        } else {
                            Client.setLatitud(currentLat);
                            Client.setLongitud(currentLong);
                            myRef.setValue(Client);
                        }
                    }
                    if (Client.getSiguiendoa() != null) {
                        if (!Client.getSiguiendoa().isEmpty()) {
                            setSiguiendoa(Client.getSiguiendoa());
                            siguiendoa = Client.getSiguiendoa();
                            myRef = database.getReference(PATH_USERS + getSiguiendoa());
                            myRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Client2 = snapshot.getValue(Usuario.class);
                                    if (Client2 != null) {
                                        LatLng sydney = new LatLng(Client2.getLatitud(), Client2.getLongitud());
                                        mMap.addMarker(new MarkerOptions().position(sydney).title(Client2.getNombre()));
                                        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                                        mMap.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL));
                                        Location location = new Location("locationA");
                                        location.setLatitude(Client2.getLatitud());
                                        location.setLongitude(Client2.getLongitud());
                                        float distance = mCurrentLocation.distanceTo(location);
                                        Toast.makeText(getApplicationContext(), "Distancia: " + distance, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                        }
                    }
                }
            });
            mMap.setMyLocationEnabled(true);
            mMap.isMyLocationEnabled();
            Location location2 = mMap.getMyLocation();
            if (location2 != null) {
                LatLng latLng = new LatLng(location2.getLatitude(), location2.getLatitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, INITIAL_ZOOM_LEVEL));
            }
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        for (Location location : locationResult.getLocations()) {
                            setmCurrentLocation(location);
                            mCurrentLocation = location;
                            currentLat = location.getLatitude();
                            currentLong = location.getLongitude();
                            if (mAuth.getCurrentUser()==null){break;}
                            myRef = database.getReference(PATH_USERS + mAuth.getCurrentUser().getUid());
                            myRef.getDatabase().getReference(PATH_USERS + mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Client = task.getResult().getValue(Usuario.class);
                                    if (Client.getLatitud() != null && Client.getLongitud() != null) {
                                        if (Client.getLatitud()!=currentLat && Client.getLongitud()!=(currentLong)) {
                                            Location locationA = new Location("point A");
                                            locationA.setLatitude(mCurrentLocation.getLatitude());
                                            locationA.setLongitude(mCurrentLocation.getLongitude());
                                            Location locationB = new Location("point B");
                                            locationB.setLatitude(Client.getLatitud());
                                            locationB.setLongitude(Client.getLongitud());
                                            float distance = locationA.distanceTo(locationB);
                                            float distanceKm = distance / 1000;
                                            if (distanceKm > 0.01) {
                                                mMap.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL));
                                                mMap.getUiSettings().setAllGesturesEnabled(true);
                                                mMap.getUiSettings().setCompassEnabled(true);
                                                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                                                LatLng clatlng = new LatLng(location.getLatitude(), location.getLongitude());
                                                mMap.animateCamera(CameraUpdateFactory.newLatLng(clatlng));
                                                Client.setLatitud(currentLat);
                                                Client.setLongitud(currentLong);
                                                myRef.setValue(Client);
                                            }
                                        }
                                    } else {
                                        Client.setLatitud(currentLat);
                                        Client.setLongitud(currentLong);
                                        myRef.setValue(Client);
                                    }
                                }
                            });
                        }
                    }
                }
            };
            if (getSiguiendoa() != null) {
                myRef = database.getReference(PATH_USERS + getSiguiendoa());
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Client2 = snapshot.getValue(Usuario.class);
                        if (Client2 != null) {
                            LatLng sydney = new LatLng(Client2.getLatitud(), Client2.getLongitud());
                            mMap.addMarker(new MarkerOptions().position(sydney).title(Client2.getNombre()));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                            mMap.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL));
                            Location location = new Location("locationA");
                            location.setLatitude(Client2.getLatitud());
                            location.setLongitude(Client2.getLongitud());
                            float distance = mCurrentLocation.distanceTo(location);
                            Toast.makeText(getApplicationContext(), "Distancia: " + distance, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            }
        }
        startLocationUpdates();
    }
    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else {
            requestPermissions();
            return false;
        }
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }
    public void mostrarpuntosjson() {
        try {
            InputStream is = getAssets().open("locations.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONObject root = new JSONObject(json);
            JSONArray array = root.getJSONArray("locationsArray");
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                String nombre = jsonObject.getString("name");
                double latitud = jsonObject.getDouble("latitude");
                double longitud = jsonObject.getDouble("longitude");
                LatLng sydney = new LatLng(latitud, longitud);
                mMap.addMarker(new MarkerOptions().position(sydney).title(nombre));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
            }
        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
    private LocationRequest createLocationRequest() {
        return LocationRequest.create()
                .setInterval(3000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int LOCATION_PERMISSION_ID = 103;
        if (requestCode == LOCATION_PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Ya hay permiso para acceder a la localizacion", Toast.LENGTH_LONG).show();
                turnOnLocationAndStartUpdates();
            } else {
                Toast.makeText(this, "Permiso de ubicacion denegado", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void turnOnLocationAndStartUpdates() {
        mLocationRequest = createLocationRequest();
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task =
                client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            mLocationCallback = new LocationCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        mMap.setMyLocationEnabled(true);
                        mMap.isMyLocationEnabled();
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL));
                        mMap.getUiSettings().setAllGesturesEnabled(true);
                        mMap.getUiSettings().setCompassEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        LatLng clatlng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(clatlng));
                        setmCurrentLocation(location);
                        mCurrentLocation = location;
                        myRef = database.getReference(PATH_USERS + mAuth.getCurrentUser().getUid());
                        myRef.getDatabase().getReference(PATH_USERS + mAuth.getCurrentUser().getUid()).get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Client = task.getResult().getValue(Usuario.class);
                                Client.setLatitud(mCurrentLocation.getLatitude());
                                Client.setLongitud(mCurrentLocation.getLongitude());
                                myRef.setValue(Client);
                            }
                        });
                    }
                }
            };
            mostrarpuntosjson();
            startLocationUpdates();
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }
    private void toastDisponible(Usuario u) {
        Intent intent = new Intent(this, ListaDisponiblesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Toast.makeText(getApplicationContext(), u.getNombre() + " esta disponible", Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                startLocationUpdates();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            turnOnLocationAndStartUpdates();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    @Override
    public void onRestart() {
        super.onRestart();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onStop() {
        super.onStop();
        stopLocationUpdates();
    }
}