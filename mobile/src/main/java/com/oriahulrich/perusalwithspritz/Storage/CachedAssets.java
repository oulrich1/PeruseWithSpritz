package com.oriahulrich.perusalwithspritz.Storage;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reference: https://github.com/GautamGupta/Simple-Android-OCR/blob/master/src/com/datumdroid/android/ocr/simple/SimpleAndroidOCRActivity.java
 *
 * Thank you GautamGupta for providing the world with simple samples
 *
 * Helper class that handles moving data around. It helps to
 * cache data and will check if the data should be overritten,
 * deleted. etc.. TODO: use reference counting to keep track
 * of stored assets to delete
 *
 */
public class CachedAssets {
    private Context mContext;
    private static CachedAssets cachedAssets  = null;

    /* singleton constructor */
    public static CachedAssets getInstance(Context context){
        if(cachedAssets == null) {
            cachedAssets = new CachedAssets(context);
        }
        return cachedAssets;
    }

    private CachedAssets(Context context){
        mContext = context;
    }

    private static final String TAG = ">> Cached Assets:: ";

    /** clear the cached assets that exist in the storage/sdcard */
    public boolean clear(String filename){
        return false;
    }

    /** clear the entire cache */
    public boolean clearEach(String[] filenames){
        return false;
    }

    public boolean exists(String path){
        return (new File(path)).exists();
    }

    /**@param assetFilenamePath source relative to the assets directory
       @param storageFilenamePath destination relative to the external storage
     only writes the storage file if it does not exist.. returns true if created */
    public boolean store(String assetFilenamePath, String storageFilenamePath){
        boolean isSuccess = false;
        if (!(new File(storageFilenamePath)).exists()) {
            AssetManager assetManager = mContext.getAssets();
            InputStream in;
            OutputStream out;
            try {
                in = assetManager.open(assetFilenamePath);
                out = new FileOutputStream(storageFilenamePath);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.v(TAG, "Copied: '" + assetFilenamePath + "' to: " + storageFilenamePath + "'.");
                isSuccess = true;
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        } else {
            isSuccess = true;
        }
        return isSuccess;
    }

    /** checks and makes the directories in the list of paths */
    public int mkdirs(String[] paths){
        boolean isFailed = false;
        int countFailed = 0;
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    isFailed = true;
                    countFailed++;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }
        return countFailed;
    }

}
