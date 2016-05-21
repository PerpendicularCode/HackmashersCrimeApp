package org.jointheleague.hackmashers;

import android.location.Location;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

public class MapUtil {

    public static final int DEFAULT_ZOOM = 15;
    public static final int ZOOM_PADDING_PIXELS = 50;

    public static CameraUpdate zoomToPlaceAndLatLng(Place place, LatLng latLng) {
        CameraUpdate cameraUpdate;
        if (place.getViewport() != null) {
            LatLngBounds bounds = place.getViewport().including(latLng);
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        } else {
            LatLngBounds bounds = new LatLngBounds.Builder().include(place.getLatLng()).include(latLng).build();
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        }
        return cameraUpdate;
    }

    public static CameraUpdate zoomToLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        return CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);
    }

    public static CameraUpdate zoomToRoute(LatLngBounds routeBounds, Place endpoint) {
        CameraUpdate cameraUpdate;
        if (endpoint.getViewport() != null) {
            LatLngBounds placeBounds = endpoint.getViewport();
            LatLngBounds bounds = routeBounds.including(placeBounds.southwest).including(placeBounds.northeast);
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        } else {
            LatLngBounds bounds = routeBounds.including(endpoint.getLatLng());
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, ZOOM_PADDING_PIXELS);
        }
        return cameraUpdate;
    }

    public static ArrayList<LatLng> decodePolyline(String encoded) {

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
