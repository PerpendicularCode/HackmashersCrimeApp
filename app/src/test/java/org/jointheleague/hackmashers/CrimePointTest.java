package org.jointheleague.hackmashers;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by SunFreezFilms on 6/4/16.
 */
public class CrimePointTest {
    CrimePoint p;

//    public void setUp() throws Exception {
//        p = new CrimePoint("100.00, 100.00: 1");
//    }

    @Test
    public void strings_Are_Set() throws Exception {
        p = new CrimePoint("100.01789030, 102.034560: 3");
        assertEquals("100.01789030", p.lattitude);
        assertEquals("102.034560", p.longitude);
        assertEquals("3", p.crime);
    }

    @Test
    public void nums_Are_Set() throws Exception {
        p = new CrimePoint("100.01789030, 102.034560: 3");
        assertEquals(100.01789030, p.latNum, .01);
        assertEquals(102.034560, p.lonNum, .01);
        assertEquals(3, p.crimeNum);
    }
}