package org.jointheleague.hackmashers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
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

public class MapsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<AutocompletePredictionBuffer>, SearchView.OnQueryTextListener, LocationListener {

    public final static int REQUEST_CODE_LOCATION = 1;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private String[] placeIDs;
    private ListView autoResultsListView;
    private SearchView searchBox;
    private boolean search = false;
    private PendingResult myAutoCompleteResult;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
//        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setStatusBarColor();
        setUpMapIfNeeded();
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        searchBox = ((SearchView) findViewById(R.id.searchBox));
        searchBox.setQueryHint(getString(R.string.search_hint));
        searchBox.setOnQueryTextListener(this);
        TextView searchText = (TextView) searchBox.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchText.setImeActionLabel("Cancel", EditorInfo.IME_ACTION_DONE);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    closeSearch();
                }
                return false;
            }
        });
        autoResultsListView = (ListView) findViewById(R.id.autocompleteResults);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.theme));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
        } else {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private synchronized void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();//Async(new OnMapReadyCallback() {
//                @Override
//                public synchronized void onMapReady(GoogleMap googleMap) {
//                    mMap = googleMap;
//                    notify();
//                }
//            });
//            try {
//                wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(AutocompletePredictionBuffer autocompletePredictions) {
        final String[] placeDescs = new String[autocompletePredictions.getCount()];
        placeIDs = new String[autocompletePredictions.getCount()];
        final Place[] placeAutoResults = new Place[autocompletePredictions.getCount()];
        for (int i = 0; i < autocompletePredictions.getCount(); i++) {
            placeDescs[i] = autocompletePredictions.get(i).getDescription();
            placeIDs[i] = autocompletePredictions.get(i).getPlaceId();
        }
        if (placeIDs.length > 0 && search) {
            ArrayAdapter<String> placeDescAd = new ArrayAdapter<String>(MapsActivity.this, R.layout.list_black_text, placeDescs);
            autoResultsListView.setAdapter(placeDescAd);
            findViewById(R.id.loadingResults).setVisibility(View.GONE);

        } else if (placeIDs.length == 0 && search) {
            findViewById(R.id.loadingResults).setVisibility(View.GONE);
            findViewById(R.id.noResults).setVisibility(View.VISIBLE);
        }
        autoResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                autoResultsListView.setAdapter(null);
                searchBox.setQuery("", false);
                searchBox.setIconified(true);
                searchBox.setIconified(true);
                PendingResult<PlaceBuffer> result = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeIDs[position]);
                result.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (search) {
                            int i = 0;
                            for (Place place : places) {
                                placeAutoResults[i] = place;
                                i++;
                            }
                            mMap.addMarker(new MarkerOptions().position(placeAutoResults[0].getLatLng()).title(placeAutoResults[0].getName().toString())/*.icon(BitmapDescriptorFactory.fromResource(R.mipmap.green_dot))*/.anchor(0.5f, 0.5f).infoWindowAnchor(0.5f, 0.5f));
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(placeAutoResults[0].getLatLng(), 15);
                            mMap.animateCamera(cameraUpdate);
                            search = false;
                            places.release();
                        }
                    }
                });
            }
        });
        autocompletePredictions.release();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private void closeSearch() {
        search = false;
        findViewById(R.id.loadingResults).setVisibility(View.GONE);
        findViewById(R.id.noResults).setVisibility(View.GONE);
        autoResultsListView.setAdapter(null);
        searchBox.setQuery("", false);
        searchBox.setIconified(true);
        searchBox.setIconified(true);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        autoResultsListView.setAdapter(null);
        findViewById(R.id.noResults).setVisibility(View.GONE);
        if (!newText.equals("")) {
            findViewById(R.id.loadingResults).setVisibility(View.VISIBLE);
            search = true;
            LatLngBounds.Builder llbb = new LatLngBounds.Builder();
            llbb.include(new LatLng(0, 0));
            if (myAutoCompleteResult != null) {
                myAutoCompleteResult.cancel();
            }
            myAutoCompleteResult =
                    Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, newText,
                            llbb.build(), AutocompleteFilter.create(null));
            myAutoCompleteResult.setResultCallback(this);
        }
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {//Zooms to location as soon as available
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        mMap.animateCamera(cameraUpdate);
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