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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, PlaceSelectionListener {

    public static final int REQUEST_CODE_LOCATION = 1;
    public static final int DEFAULT_ZOOM = 15;
    public static final int ZOOM_PADDING_PIXELS = 50;
    public static final String DIRECTIONS_API = "https://maps.googleapis.com/maps/api/directions/json?";

    public final static String DRIVING = "driving";
    public final static String WALKING = "walking";
    public final static String BICYCLING = "bicycling";
    public final static String TRANSIT = "transit";

    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;//Unused right now
    private LocationManager locationManager;
    private PlaceAutocompleteFragment autocompleteFragment;
    private Polyline currentPath;
    private Place searchedPlace;
    private GetDirections getDirectionsTask;

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
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);//TODO Use the fused location provider instead.
        autocompleteFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setHint(getString(R.string.search_hint));
        autocompleteFragment.setOnPlaceSelectedListener(this);
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
        zoomToPlaceAndCurrentLocation(place);
    }

    private void zoomToPlaceAndCurrentLocation(Place place) {
        CameraUpdate cameraUpdate;
        if (place.getViewport() != null) {
            LatLngBounds bounds = place.getViewport().including(getLastKnownLatLng());
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        } else {
            LatLngBounds bounds = new LatLngBounds.Builder().include(place.getLatLng()).include(getLastKnownLatLng()).build();
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        }
        googleMap.animateCamera(cameraUpdate);
    }

    private LatLng getLastKnownLatLng() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        return null;
    }

    @Override
    public void onPlaceSelected(Place place) {
        searchedPlace = place;
        pinSearchResult(place);
        LatLng startCoordinates = null;//Temporary for testing
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startCoordinates = getLastKnownLatLng();
        }
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

    class GetDirections extends AsyncTask<Object, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Object... params) {

            LatLng start = (LatLng) params[0];
            LatLng destination = (LatLng) params[1];
            String mode = (String) params[2];
            try {
                String url = getDirectionsURL(start, destination, mode);
                Log.d("URL", url);
                return getJsonFromURL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject directions) {
            try {
                Log.d("JSON", directions.toString());
                JSONObject routeJSON = directions.getJSONArray("routes").getJSONObject(0);
                String encodedPolyline = routeJSON.getJSONObject("overview_polyline").getString("points");
                ArrayList<LatLng> pointsList = decodePolyline(encodedPolyline);
                LatLng[] points = pointsList.toArray(new LatLng[pointsList.size()]);
                if (currentPath != null) {
                    currentPath.remove();
                }
                currentPath = googleMap.addPolyline(new PolylineOptions().add(points).color(ContextCompat.getColor(MapsActivity.this, R.color.colorPrimary)));
                JSONObject boundsJSON = routeJSON.getJSONObject("bounds");
                JSONObject southwestJSON = boundsJSON.getJSONObject("southwest");
                JSONObject northeastJSON = boundsJSON.getJSONObject("northeast");
                LatLngBounds routeBounds = new LatLngBounds(new LatLng(southwestJSON.getDouble("lat"), southwestJSON.getDouble("lng")), new LatLng(northeastJSON.getDouble("lat"), northeastJSON.getDouble("lng")));
                zoomToRoute(routeBounds);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void zoomToRoute(LatLngBounds routeBounds) {
        CameraUpdate cameraUpdate;
        if (searchedPlace.getViewport() != null) {
            LatLngBounds placeBounds = searchedPlace.getViewport();
            LatLngBounds bounds = routeBounds.including(placeBounds.southwest).including(placeBounds.northeast);
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        } else {
            LatLngBounds bounds = routeBounds.including(searchedPlace.getLatLng());
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        }
        googleMap.animateCamera(cameraUpdate);
    }

    private JSONObject getJsonFromURL(String urlString) throws IOException, JSONException {
        try {
            URL url = new URL(urlString);
            URLConnection connection;
            connection = url.openConnection();
            connection.connect();
            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonText = builder.toString();
            return new JSONObject(jsonText);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getDirectionsURL(LatLng start, LatLng destination, String mode) {
        return DIRECTIONS_API + "origin=" + start.latitude + "," + start.longitude + "&destination=" + destination.latitude + "," + destination.longitude + "&mode=" + mode;
    }

    private ArrayList<LatLng> decodePolyline(String encoded) {

        ArrayList<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        return poly;
    }
}