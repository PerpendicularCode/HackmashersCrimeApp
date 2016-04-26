package org.jointheleague.hackmashers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.Field;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    public final static int REQUEST_CODE_LOCATION = 1;
    public static final int DEFAULT_ZOOM = 15;

    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;//Unused right now
    private LocationManager locationManager;
    private PlaceAutocompleteFragment autocompleteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setupMap();
        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setHint(getString(R.string.search_hint));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                pinSearchResult(place);
            }

            @Override
            public void onError(Status status) {
            }
        });
        makeSearchBarPretty();
    }

    private void makeSearchBarPretty() {
        try {
            Field searchButtonField = autocompleteFragment.getClass().getDeclaredField("zzaRh");
            searchButtonField.setAccessible(true);
            ImageButton searchButton = (ImageButton) searchButtonField.get(autocompleteFragment);
            Field clearButtonField = autocompleteFragment.getClass().getDeclaredField("zzaRi");
            clearButtonField.setAccessible(true);
            ImageButton clearButton = (ImageButton) clearButtonField.get(autocompleteFragment);
            Field searchInputField = autocompleteFragment.getClass().getDeclaredField("zzaRj");
            searchInputField.setAccessible(true);
            EditText searchInput = (EditText) searchInputField.get(autocompleteFragment);

            searchButton.setColorFilter(Color.BLACK);
            searchButton.setImageAlpha(Color.alpha(ContextCompat.getColor(this, R.color.halfTransparent)));
            clearButton.setColorFilter(Color.BLACK);
            clearButton.setImageAlpha(Color.alpha(ContextCompat.getColor(this, R.color.halfTransparent)));
            searchInput.setTextColor(ContextCompat.getColor(this, R.color.searchedText));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    private void setupMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void setupListeners() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
        } else {
            requestLocationUpdates();
        }
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
    }

    private void pinSearchResult(Place place) {
        googleMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString())/*.icon(BitmapDescriptorFactory.fromResource(R.mipmap.green_dot)).anchor(0.5f, 0.5f).infoWindowAnchor(0.5f, 0.5f)*/);
        CameraUpdate cameraUpdate;
        if (place.getViewport() != null) {
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(place.getViewport(), 25);
        } else {
            cameraUpdate = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), DEFAULT_ZOOM);
        }
        googleMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.googleMap.setMyLocationEnabled(true);
        }
        setupListeners();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationUpdates();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);
        googleMap.animateCamera(cameraUpdate);
        preventMoreUpdates();
    }

    private void preventMoreUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}