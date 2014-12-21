package com.oriahulrich.perusalwithspritz;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    /** Create a File for saving an image or video specific to an app name */
    public static File getOutputMediaFile(int type, String optionalAppName){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        String appname = "CameraApp";
        if (optionalAppName != null && !optionalAppName.isEmpty()) {
            appname = optionalAppName;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appname);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + appname + timeStamp + ".jpg");
        } else if(type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + appname + timeStamp + ".mp4");
        } else {
            return null;
        }

        //USAGE:
        //        fileUri = Helpers.getOutputMediaFileUri(
        //                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
        //                           Helpers.getApplicationName(this)
        //                         ); // create a file to save the image

        return mediaFile;
    }

    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri( int type, String optionalAppName ){
        return Uri.fromFile(getOutputMediaFile(type, optionalAppName));
    }

    /** Check if this device has a camera */
    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    /** Generic Image File Creation.. */
    public static String createUniqueImageFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        return imageFileName;
    }

    ///NOTE: creates the image file in the public storage directory for pictures
    public static File createImageFile( String imageFileName ) throws IOException  {
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    public static File createUniqueImageFile() throws IOException {
        String imageFileName = createUniqueImageFilename();
        return createImageFile( imageFileName );
    }

    // tells the media scanner that this is a picture that should be
    // included in acceptable public images to be consumed by other apps
    // NOTE: the file name path must be to the public directory
    public static void globalAppsMediaScanIntent( Context context, String imageFullPath ) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imageFullPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    // taken from last post on stack overflow
    // http://stackoverflow.com/questions/8474821/how-to-get
    // -the-android-path-string-to-a-file-on-assets-folder
    public static File getRobotCacheFile(Context context, String assetname) throws IOException {
        File cacheFile = new File(context.getCacheDir(), assetname);
        try {
            InputStream inputStream = context.getAssets().open(assetname);
            try {
                FileOutputStream outputStream = new FileOutputStream(cacheFile);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            throw new IOException("Could not open " + assetname, e);
        }
        return cacheFile;
    }
}





