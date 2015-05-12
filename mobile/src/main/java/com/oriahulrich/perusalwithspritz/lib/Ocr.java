package com.oriahulrich.perusalwithspritz.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.oriahulrich.perusalwithspritz.Storage.CachedAssets;

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

    static {
//        System.loadLibrary("lept");
//        System.loadLibrary("tess");
//        if (!OpenCVLoader.initDebug()) {
//            // Handle initialization error
//            Log.d("","OpenCV Init Error");
//        }
    }

    public static final String TAG = "Ocr";

    Bitmap  mImageBitmap;

    private Context mContext;
    private CachedAssets mCache;
    // connection between assets and storage

    /* tessdata location and storage directories */
    public static String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/";

    public static final String LANG = "eng";

    private static TessBaseAPI m_baseApi = null;
    private static boolean mIsTessInit = false;

    public Ocr(Context context) {
        mContext = context;
        mImageBitmap = null;
    }
    // resets the tess instance
    public static void reset() {
        resetTessInstance();
    }
    public boolean setImage(Bitmap image)  {
        mImageBitmap = image;
        return true;
    }
    public Bitmap getImage()  {
        return mImageBitmap;
    }
    public String performOcrGetText() {
        Result result = performOcr();
        return result.text;
    }

    public Result performOcr() {
        Result result = new Result();
        result.isValid = false;

        // initialization
        m_baseApi = getTessInstance();
        if ( !initTesseract() ) {
            result.text = "Tess failed to init, or disabled";
            return result;
        }

        // invalid image
        if ( mImageBitmap == null ) {
            result.text = "Image not set..";
            return result;
        }

        // filtering
        mImageBitmap = Filter.doFiltering(mImageBitmap);

        // recognition
        try {
            m_baseApi.setImage(mImageBitmap);
            result.text = m_baseApi.getUTF8Text();
            m_baseApi.end();
            result.isValid = true;
        } catch (Exception e) {
            Log.d(TAG, "Tesseract Exception: " + e.getMessage());
            result.text = "Error performing tesseract..";
            return result;
        }
        return result;
    }

    private static TessBaseAPI getTessInstance() {
        if (m_baseApi == null) {
            m_baseApi = new TessBaseAPI();
            mIsTessInit = false;
        }
        return m_baseApi;
    }

    private static void resetTessInstance() {
        m_baseApi.end();
        m_baseApi = null;
        m_baseApi = getTessInstance();
    }

    private boolean initTesseract()
    {
        if ( mIsTessInit ) {
            return true;
        }

        mCache = CachedAssets.getInstance(mContext);
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        /* create the directories in storage (sdcard)
            - init directories that tess will have access to */
        int err_code = mCache.mkdirs(paths);
        if (err_code > 0) {
            Log.d(TAG, "Unable to create directories: " + paths);
        }

        boolean isSuccess;
        String sourcePath;
        String destinationPath;

        /* transfer the data from assets to the sdcard -  to init test data */
        sourcePath = "tessdata/" + LANG + ".traineddata";
        destinationPath = DATA_PATH + "tessdata/" + LANG + ".traineddata";
        isSuccess = mCache.store(sourcePath, destinationPath);

        if (isSuccess) {
            Log.d(TAG, "Successfully created the cache: " + destinationPath);
        }

        try {
            m_baseApi = getTessInstance();
            //m_baseApi.setDebug(true);
            m_baseApi.init(DATA_PATH, LANG); // looks for tessdata/eng.traineddata
            mIsTessInit = true;
        } catch ( Exception e ) {
            Log.d( TAG, " Tesseract failed to initialize: " + e.getMessage() );
            mIsTessInit = false;
        }

        return mIsTessInit;
    }

}
