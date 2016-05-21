package org.jointheleague.hackmashers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class MapsActivity extends AppCompatActivity implements LocationListener, PlaceSelectionListener {

    public static final int REQUEST_CODE_LOCATION = 1;

    public static final String DIRECTIONS_API = "https://maps.googleapis.com/maps/api/directions/json?";
    public final static String DRIVING = "driving";
    public final static String WALKING = "walking";
    public final static String BICYCLING = "bicycling";
    public final static String TRANSIT = "transit";

    private GoogleMap googleMap;
    private Polyline currentRoute;
    private Place searchedPlace;
    private GetDirections getDirectionsTask;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setupMap();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);//TODO Use the fused location provider instead.
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setHint(getString(R.string.search_hint));
        autocompleteFragment.setOnPlaceSelectedListener(this);
        makeSearchBarPretty(autocompleteFragment);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                MapsActivity.this.googleMap = googleMap;
                if (hasLocationPermissions()) {
                    //noinspection ResourceType
                    googleMap.setMyLocationEnabled(true);
                }
                setupLocationListener();
                drawCrime();
            }
        });
    }

    private void setupLocationListener() {
        if (hasLocationPermissions()) {
            requestLocationUpdates();
        } else {
            requestLocationPermissions();
        }
    }

    private void requestLocationUpdates() {
        if (hasLocationPermissions())
            //noinspection ResourceType
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE_LOCATION);
    }

    private void drawCrime() {
        InputStream data = getResources().openRawResource(R.raw.random_sample_10000);
        HeatmapTileProvider heatMap = DataUtil.getCrimeHeatMap(data);
        googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatMap));
    }

    private void pinSearchResult(Place place) {
        googleMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString()));
        googleMap.animateCamera(MapUtil.zoomToPlaceAndLatLng(place, getLastKnownLatLng()));
    }

    @Override
    public void onPlaceSelected(Place place) {
        searchedPlace = place;
        pinSearchResult(place);
        LatLng startCoordinates = null;//Temporary for testing
        startCoordinates = getLastKnownLatLng();
        if (getDirectionsTask != null) {
            getDirectionsTask.cancel(true);//TODO Make sure this doesn't break everything
        }
        getDirectionsTask = new GetDirections();
        getDirectionsTask.execute(startCoordinates, place.getLatLng(), WALKING);
        //TODO: Show data about destination by using DataUtils.getCrimeRate(place.getLatLng)
    }

    @Override
    public void onError(Status status) {

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
        googleMap.animateCamera(MapUtil.zoomToLocation(location));
        preventMoreUpdates();
    }

    private void preventMoreUpdates() {
        if (hasLocationPermissions()) {
            //noinspection ResourceType
            locationManager.removeUpdates(this);
        }
    }

    private LatLng getLastKnownLatLng() {
        if (hasLocationPermissions()) {
            //noinspection ResourceType
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        return null;
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

    class GetDirections extends AsyncTask<Object, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Object... params) {
            LatLng start = (LatLng) params[0];
            LatLng destination = (LatLng) params[1];
            String mode = (String) params[2];
            try {
                String url = getDirectionsURL(start, destination, mode);
                Log.d("URL", url);
                return JsonUtil.getJsonFromURL(url);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject directions) {
            try {
                Log.d("Directions JSON", directions.toString());
                JSONObject directionsJson = directions.getJSONArray("routes").getJSONObject(0);
                LatLng[] points = JsonUtil.getPointsFromDirections(directionsJson);
                if (currentRoute != null) {
                    currentRoute.remove();
                }
                currentRoute = googleMap.addPolyline(new PolylineOptions().add(points).color(ContextCompat.getColor(MapsActivity.this, R.color.colorPrimary)));
                LatLngBounds routeBounds = JsonUtil.getBoundsFromDirections(directionsJson);
                googleMap.animateCamera(MapUtil.zoomToRoute(routeBounds, searchedPlace));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String getDirectionsURL(LatLng start, LatLng destination, String mode) {
        return DIRECTIONS_API + "origin=" + start.latitude + "," + start.longitude + "&destination=" + destination.latitude + "," + destination.longitude + "&mode=" + mode;
    }

    private void makeSearchBarPretty(PlaceAutocompleteFragment autocompleteFragment) {
        //noinspection TryWithIdenticalCatches
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
}