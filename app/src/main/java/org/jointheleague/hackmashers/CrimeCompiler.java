package org.jointheleague.hackmashers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by SunFreezFilms on 5/21/16.
 */
public class CrimeCompiler {
    ArrayList<CrimePoint> crimes;
    public CrimeCompiler(InputStream data) {
        crimes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(data));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                crimes.add(new CrimePoint(csvLine));
            }
            data.close();
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + e);
        }
    }
}
