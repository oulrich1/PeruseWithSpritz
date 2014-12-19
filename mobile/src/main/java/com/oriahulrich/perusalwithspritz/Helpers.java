package com.oriahulrich.perusalwithspritz;

import android.util.Log;

/**
 * Created by oriahulrich on 12/14/14.
 */
public class Helpers {
    public static String capitalize(String line)
    {
        if (line.isEmpty()) {
            return "";
        }

        String retString = Character.toUpperCase(line.charAt(0)) + "";

        if (line.length() > 1){
            retString = retString + line.substring(1);
        }

        return retString;
    }
}



