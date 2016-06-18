package org.jointheleague.hackmashers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class MapsActivity extends AppCompatActivity implements LocationListener, PlaceSelectionListener {

    public static final int REQUEST_CODE_LOCATION = 1;
    public static final String[] LOCATION_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private GoogleMap googleMap;
    private Polyline currentRoute;
    private GetDirections getDirectionsTask;
    private LocationManager locationManager;

    public HeatmapTileProvider heatmap;
    public TileOverlay heatmapOverlay;

    private InputStream data;

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
        //Setup data
        data = getResources().openRawResource(R.raw.random_sample_10000);

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.settings_button_background, null);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        Canvas canvas2 = new Canvas(bitmap);
        int color = (178 & 0xFF) << 24;
        canvas.drawColor(color, PorterDuff.Mode.DST_IN);
        BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);

        findViewById(R.id.settingsButton).setBackground(bd);
        findViewById(R.id.addLocButton).setBackground(bd);

        ((ImageButton) findViewById(R.id.settingsButton)).setColorFilter(Color.rgb(102, 102, 102));
    }

    public void startSettings(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
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
            ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, REQUEST_CODE_LOCATION);
        }
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
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

    private void requestLocationUpdates() {
        if (hasLocationPermissions())
            //noinspection ResourceType
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
    }

    @Override
    public void onLocationChanged(Location location) {
        //Zooms to current location on startup
        googleMap.animateCamera(MapUtil.zoomToLocation(location));
        preventMoreUpdates();
    }

    private void preventMoreUpdates() {
        if (hasLocationPermissions()) {
            //noinspection ResourceType
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (DataUtil.IS_HEATMAP) {
            heatmap.setOpacity(DataUtil.HEATMAP_OPACITY);
        } else {
            heatmap.setOpacity(0);
        }
        heatmap.setRadius(DataUtil.HEATMAP_RADIUS);
        heatmapOverlay.clearTileCache();
    }

    private void drawCrime() {
        heatmap = DataUtil.getCrimeHeatMap(data);
        heatmapOverlay = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmap));
    }

    @Override
    public void onPlaceSelected(Place place) {
        pinPlace(place);
        LatLng startLatLng = getCurrentLatLng();//TODO Make a comprehensive way to select start and destination locations for routing.
        Place destinationPlace = place;
        if (getDirectionsTask != null) {
            getDirectionsTask.cancel(true);//TODO Make sure this works. This interrupts the current AsyncTask thread if it still running, and should stop it.
        }
        getDirectionsTask = new GetDirections();
        getDirectionsTask.execute(new RouteInfo(startLatLng, destinationPlace, RouteInfo.WALKING));

        Marker currentSelected = this.googleMap.addMarker(new MarkerOptions()
                .position(place.getLatLng())
                .title(place.getAddress().toString())
                .snippet("Overall Crime: 0 "));
        //TODO: Add multiple lines to the Marker
        currentSelected.showInfoWindow();
    }

    private void pinPlace(Place place) {
        googleMap.addMarker(new MarkerOptions()
                .position(place.getLatLng())
                .title(place.getName().toString()));
        googleMap.animateCamera(MapUtil.zoomToPlaceAndLatLng(place, getCurrentLatLng()));
    }

    private LatLng getCurrentLatLng() {
        if (hasLocationPermissions()) {
            //noinspection ResourceType
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        return null;
    }

    class GetDirections extends AsyncTask<RouteInfo, Void, JSONObject> {
        RouteInfo routeInfo;

        @Override
        protected JSONObject doInBackground(RouteInfo... routeInfo) {
            this.routeInfo = routeInfo[0];
            try {
                String url = this.routeInfo.getGoogleDirectionsURL();
                Log.d("Directions URL", url);
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
                currentRoute = googleMap.addPolyline(new PolylineOptions()
                        .add(points)
                        .color(ContextCompat.getColor(MapsActivity.this, R.color.colorPrimary)));
                LatLngBounds routeBounds = JsonUtil.getBoundsFromDirections(directionsJson);
                googleMap.animateCamera(MapUtil.zoomToRoute(routeBounds, routeInfo.getStartPlace(), routeInfo.getDestinationPlace()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void makeSearchBarPretty(PlaceAutocompleteFragment autocompleteFragment) {
        //noinspection TryWithIdenticalCatches
        try {
            Field searchButtonField = autocompleteFragment.getClass().getDeclaredField("zzaYx");
            searchButtonField.setAccessible(true);
            ImageButton searchButton = (ImageButton) searchButtonField.get(autocompleteFragment);
            Field clearButtonField = autocompleteFragment.getClass().getDeclaredField("zzaYy");
            clearButtonField.setAccessible(true);
            ImageButton clearButton = (ImageButton) clearButtonField.get(autocompleteFragment);
            Field searchInputField = autocompleteFragment.getClass().getDeclaredField("zzaYz");
            searchInputField.setAccessible(true);
            EditText searchInput = (EditText) searchInputField.get(autocompleteFragment);

            searchButton.setColorFilter(Color.WHITE);
            searchButton.setImageAlpha(Color.alpha(ContextCompat.getColor(this, R.color.searchButtonTransparency)));
            clearButton.setColorFilter(Color.WHITE);
            clearButton.setImageAlpha(Color.alpha(ContextCompat.getColor(this, R.color.searchButtonTransparency)));
            searchInput.setTextColor(ContextCompat.getColor(this, R.color.searchedText));
            searchInput.setHintTextColor(ContextCompat.getColor(this, R.color.searchHint));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Status status) {
        Log.e("Error", "Error when selecting a place: " + status);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Location provider", "Location provider status changed: " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Location provider", "Location provider enabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Location provider", "Location provider disabled");
    }
}