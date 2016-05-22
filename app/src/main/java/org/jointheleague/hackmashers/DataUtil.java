package org.jointheleague.hackmashers;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DataUtil {
    public final static int[] COLORS = new int[]{Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED};
    public final static float[] START_POINTS = new float[]{1 / 16.0f, 2 / 16.0f, 4 / 16.0f, 8 / 16.0f, 1};

    public final static String[] CRIME_TYPES = {
            "VEHICLE BREAK-IN/THEFT",
            "THEFT/LARCENY",
            "FRAUD",
            "ASSAULT",
            "SEX CRIMES",
            "VANDALISM",
            "BURGLARY",
            "MOTOR VEHICLE THEFT",
            "ROBBERY",
            "DRUGS/ALCOHOL VIOLATIONS",
            "DUI",
            "WEAPONS",
            "ARSON",
            "HOMICIDE"
    };
    public final static int DATE_INDEX = 0;
    public final static int TIME_INDEX = 1;
    public final static int TYPE_INDEX = 2;
    public final static int LAT_INDEX = 3;
    public final static int LNG_INDEX = 4;

    private int DATA_SIZE_X = 240;
    private int DATA_SIZE_Y = 240;
    public int[][] allData = new int[DATA_SIZE_X][DATA_SIZE_Y];

    public int getCrimeRate(float lat, float lon) {
        //TODO:Calculate the correct grid square and value from the data array
        return 0;
    }

    public void loadData() {
        //TODO:Set Data_Size vars and load data into the int array
    }

    public static HeatmapTileProvider getCrimeHeatMap(InputStream data) {
        ArrayList<String[]> incidents = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(data));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(", ");
                incidents.add(row);
            }
            data.close();
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + e);
        }
        ArrayList<LatLng> locations = new ArrayList<>();
        for (String[] incident : incidents) {
            locations.add(new LatLng(Double.parseDouble(incident[LAT_INDEX]), Double.parseDouble(incident[LNG_INDEX])));
        }
        return new HeatmapTileProvider.Builder()
                .data(locations)
                .gradient(new Gradient(COLORS, START_POINTS)).build();
    }
}