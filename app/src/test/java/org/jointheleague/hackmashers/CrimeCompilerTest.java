package org.jointheleague.hackmashers;

import android.renderscript.ScriptGroup;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Created by SunFreezFilms on 6/18/16.
 */
public class CrimeCompilerTest {
    File dummy;
    CrimeCompiler cc;
    InputStream data;


    @Test
    public void crime_Nums_Available() {
        dummy = new File("/Users/SunFreezFilms/Desktop/ChumpSci/Hackmashers/HackmashersCrimeApp/app/src/main/res/raw/dummy.csv");
        try {
            data = new FileInputStream(dummy);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        cc = new CrimeCompiler(data);
        assertEquals(1, cc.crimes.get(0).crimeNum);
        assertEquals(1, cc.crimes.get(1).crimeNum);
        assertEquals(1, cc.crimes.get(2).crimeNum);
        assertEquals(1, cc.crimes.get(3).crimeNum);
        assertEquals(1, cc.crimes.get(4).crimeNum);
    }
}