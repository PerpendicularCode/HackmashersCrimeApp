package org.jointheleague.hackmashers;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class JsonUtil {

    public static JSONObject getJsonFromURL(String urlString) throws IOException, JSONException {
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
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static LatLng[] getPointsFromDirections(JSONObject directionsJson) throws JSONException {
        JSONArray steps = directionsJson.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
        ArrayList<LatLng> pointsList = new ArrayList<>();
        for (int i = 0; i < steps.length(); i++) {
            String encodedPolyline = steps.getJSONObject(i).getJSONObject("polyline").getString("points");
            pointsList.addAll(MapUtil.decodePolyline(encodedPolyline));
        }
        return pointsList.toArray(new LatLng[pointsList.size()]);
    }

    public static LatLngBounds getBoundsFromDirections(JSONObject directionsJson) throws JSONException {
        JSONObject boundsJSON = directionsJson.getJSONObject("bounds");
        JSONObject southwestJSON = boundsJSON.getJSONObject("southwest");
        JSONObject northeastJSON = boundsJSON.getJSONObject("northeast");
        return new LatLngBounds(new LatLng(southwestJSON.getDouble("lat"), southwestJSON.getDouble("lng")), new LatLng(northeastJSON.getDouble("lat"), northeastJSON.getDouble("lng")));
    }
}