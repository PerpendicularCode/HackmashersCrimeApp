package org.jointheleague.hackmashers;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;


public class RouteInfo {
    public static final String DIRECTIONS_API = "https://maps.googleapis.com/maps/api/directions/json?";

    public final static String DRIVING = "driving";
    public final static String WALKING = "walking";
    public final static String BICYCLING = "bicycling";
    public final static String TRANSIT = "transit";

    private Place startPlace = null;
    private Place destinationPlace = null;
    private LatLng startLatLng;
    private LatLng destinationLatLng;
    private String modeOfTransportation;

    public RouteInfo(LatLng start, LatLng destination, String modeOfTransportation) {
        startLatLng = start;
        destinationLatLng = destination;
        this.modeOfTransportation = modeOfTransportation;
    }

    public RouteInfo(Place start, LatLng destination, String modeOfTransportation) {
        this(start.getLatLng(), destination, modeOfTransportation);
        startPlace = start;
    }

    public RouteInfo(LatLng start, Place destination, String modeOfTransportation) {
        this(start, destination.getLatLng(), modeOfTransportation);
        destinationPlace = destination;
    }

    public RouteInfo(Place start, Place destination, String modeOfTransportation) {
        this(start.getLatLng(), destination.getLatLng(), modeOfTransportation);
        startPlace = start;
        destinationPlace = destination;
    }

    public String getGoogleDirectionsURL() {
        return DIRECTIONS_API + "origin=" + startLatLng.latitude + "," + startLatLng.longitude
                + "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude
                + "&mode=" + modeOfTransportation;
    }

    public LatLng getStartLatLng() {
        return startLatLng;
    }

    public LatLng getDestinationLatLng() {
        return destinationLatLng;
    }

    public String getModeOfTransportation() {
        return modeOfTransportation;
    }

    public Place getStartPlace() {
        return startPlace;
    }

    public Place getDestinationPlace() {
        return destinationPlace;
    }
}