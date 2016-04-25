package org.jointheleague.hackmashers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SearchView.OnQueryTextListener, ResultCallback<AutocompletePredictionBuffer> {

    public final static int REQUEST_CODE_LOCATION = 1;

    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private String[] autoCompletePlaceIDs;
    private ListView autoCompletesListView;
    private SearchView searchBox;
    private TextView searchText;
    private View loadingResults;
    private View noResults;
    private PendingResult<AutocompletePredictionBuffer> autoCompletePending;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setStatusBarColor();
        setupMap();
        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        searchBox = ((SearchView) findViewById(R.id.searchBox));
        searchBox.setQueryHint(getString(R.string.search_hint));
        searchText = (TextView) searchBox.findViewById(android.support.v7.appcompat.R.id.search_src_text);
//        searchText.setImeActionLabel("Cancel", EditorInfo.IME_ACTION_DONE);
        autoCompletesListView = (ListView) findViewById(R.id.autocompleteResults);
        loadingResults = findViewById(R.id.loadingResults);
        noResults = findViewById(R.id.noResults);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        searchBox.setOnQueryTextListener(this);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (loadingResults.getVisibility() == View.GONE && autoCompletePlaceIDs != null && autoCompletePlaceIDs.length > 0) {
                        pinSearchResult(0);
                        return false;
                    }
                }
                return true;
            }
        });
        searchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    closeSearch();
                }
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
        } else {
            requestLocationUpdates();
        }
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
    }

    private void closeSearch() {
        loadingResults.setVisibility(View.GONE);
        noResults.setVisibility(View.GONE);
        autoCompletesListView.setAdapter(null);
        searchBox.setQuery("", false);
        searchBox.setIconified(true);
        searchBox.setIconified(true);
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
    }

    private void pinSearchResult(int position) {
        autoCompletesListView.setAdapter(null);
        searchBox.setQuery("", false);
        searchBox.setIconified(true);
        searchBox.setIconified(true);
        PendingResult<PlaceBuffer> result = Places.GeoDataApi.getPlaceById(googleApiClient, autoCompletePlaceIDs[position]);
        result.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                Place place = places.get(0);
                googleMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString())/*.icon(BitmapDescriptorFactory.fromResource(R.mipmap.green_dot)).anchor(0.5f, 0.5f).infoWindowAnchor(0.5f, 0.5f)*/);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15);
                googleMap.animateCamera(cameraUpdate);
                places.release();
            }
        });
        autoCompletePlaceIDs = null;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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
    public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
        final String[] placeDescriptions = new String[autocompletePredictions.getCount()];
        autoCompletePlaceIDs = new String[autocompletePredictions.getCount()];
        for (int i = 0; i < autocompletePredictions.getCount(); i++) {
            placeDescriptions[i] = autocompletePredictions.get(i).getFullText(null).toString();
            autoCompletePlaceIDs[i] = autocompletePredictions.get(i).getPlaceId();
        }
        if (autoCompletePlaceIDs.length > 0 && searchText.hasFocus()) {
            ArrayAdapterFirstHighlighted placeDescAd = new ArrayAdapterFirstHighlighted(MapsActivity.this, R.layout.list_black_text, placeDescriptions);
            autoCompletesListView.setAdapter(placeDescAd);
            loadingResults.setVisibility(View.GONE);
        } else if (autoCompletePlaceIDs.length == 0 && searchText.hasFocus()) {
            loadingResults.setVisibility(View.GONE);
            noResults.setVisibility(View.VISIBLE);
        }
        autoCompletesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pinSearchResult(position);
            }
        });
        autocompletePredictions.release();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        autoCompletesListView.setAdapter(null);
        noResults.setVisibility(View.GONE);
        if (!newText.equals("")) {
            loadingResults.setVisibility(View.VISIBLE);
            LatLngBounds.Builder llbb = new LatLngBounds.Builder();
            llbb.include(new LatLng(0, 0));
            if (autoCompletePending != null) {
                autoCompletePending.cancel();
            }
            autoCompletePending = Places.GeoDataApi.getAutocompletePredictions(googleApiClient, newText, llbb.build(), null);
            autoCompletePending.setResultCallback(this);
        }
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        googleMap.animateCamera(cameraUpdate);
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

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}