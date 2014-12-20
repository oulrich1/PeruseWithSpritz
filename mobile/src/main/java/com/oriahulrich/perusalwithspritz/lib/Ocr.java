package com.oriahulrich.perusalwithspritz.lib;

import android.net.Uri;

import com.googlecode.tesseract.android.TessBaseAPI;    // for tesseract api
import com.googlecode.tesseract.android.ResultIterator; // for results
import com.googlecode.tesseract.android.PageIterator;   // for pages

import com.googlecode.leptonica.android.Pix;            // this is their image class
import com.googlecode.leptonica.android.JpegIO;         //
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.leptonica.android.Box;

/**
 * Created by oriahulrich on 12/20/14.
 */
public class Ocr {
    public class Result {
        public String text;
        public String boxfile;
        public boolean isValid;

        Result() {
            text = "";
            boxfile = "";
            isValid = false;
        }
    }

    public Ocr() {

    }

    public boolean setImage(Uri imageUri)  {

        return true;
    }

    public String performOcrGetText() {

        Result result = performOcr();
        return result.text;
    }

    public Result performOcr() {
        Result result = new Result();
        result.text = "<Not implemented yet>";
        result.isValid = true;
        return result;
    }
}
