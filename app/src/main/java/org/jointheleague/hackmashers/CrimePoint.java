package org.jointheleague.hackmashers;

/**
 * Created by SunFreezFilms on 5/21/16.
 */
public class CrimePoint {
    String lattitude, longitude, crime;
    double latNum, lonNum;
    int crimeNum;

    //Example: 100.00, 100.00: 1
    public CrimePoint(String input) {
        int split1Pos = input.indexOf(',');
        int split2Pos = input.indexOf(':');

        if (split1Pos != -1 && split2Pos != -1) {
            try {
                lattitude = input.substring(0, split1Pos).trim();
            } catch (Exception e) {
                lattitude = "0";
            }

            try {
                longitude = input.substring(split1Pos + 1, split2Pos).trim();
            } catch (Exception e) {
                longitude = "0";
            }

            try {
                crime = input.substring(split2Pos + 1).trim();
            } catch (Exception e) {
                crime = "0";
            }

            latNum = Double.parseDouble(lattitude);
            lonNum = Double.parseDouble(longitude);
            crimeNum = Integer.parseInt(crime);
        }
    }

}
