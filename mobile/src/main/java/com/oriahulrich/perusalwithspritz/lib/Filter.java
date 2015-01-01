package com.oriahulrich.perusalwithspritz.lib;

import android.graphics.Bitmap;
import android.util.Log;

//import org.opencv.android.Utils;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.Point;
//import org.opencv.core.Size;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.photo.Photo;

/**
 * Created by oriahulrich on 12/20/14.
 */
public class Filter {

    private static final String TAG = "Filter";

//    public static Bitmap doFiltering( Bitmap image ) {
//
//        if ( image == null )
//            return null;
//
//        Mat tmpImage = new Mat();
//
//        Utils.bitmapToMat(image, tmpImage);
//
//        if ( tmpImage.type() == CvType.CV_8UC4 ) {
//            Imgproc.cvtColor( tmpImage, tmpImage, Imgproc.COLOR_RGBA2RGB );
//            tmpImage.convertTo(tmpImage, CvType.CV_8UC3);
//        }
//
//        Mat filteredImage = new Mat( tmpImage.rows(), tmpImage.cols(), CvType.CV_8UC3 );
//
//        if ( tmpImage.rows() <= 0
//             || filteredImage.rows() <= 0 )
//        {
//            return image; // failure
//        }
//
//        /* - - Kernal Size - - */
//        int kSize = 15;
//        Size kernSize = new Size(kSize, kSize);
//
//        // std-deviations //
//        double sigmaColor = kSize / 2.0;  // larger -> more colors are included in color averaging
//        double sigmaSpace = kSize * 2.0;  // larger -> pixels closer to the location where the mean
//                                          //           is at will be weighted higher
//
//        // smooth the colors a little bit //
//        try {
//            Imgproc.bilateralFilter(tmpImage, filteredImage, kSize,
//                    sigmaSpace, sigmaColor,
//                    Imgproc.BORDER_DEFAULT);
//        } catch ( Exception e ) {
//            Log.d(TAG, "Bilateral Filter Failed/Crashed");
//        }
//
//        /// TODO: COLOR REDUCTION ///
//        // find edges and create a mask of the image that
//        // references pixels that are not edge pixels
//
//        // sub sample to reduce color space
//
//        // find color clusters of the reduced color space
//
//        // mean shift them and repeatedly and merge clusters
//        // "close" enough in color space
//        // ( idea: what about repeatedly assigning pixels to
//        // their cluster, and then re-find the clusters.. slow?)
//
//        /// convert to greyscale
//        Imgproc.cvtColor( filteredImage, filteredImage, Imgproc.COLOR_RGB2GRAY );
//
//        int blockSize = 17;
//        double meanOffset = 3;
//        Imgproc.adaptiveThreshold( filteredImage, tmpImage,
//                                    255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
//                                    Imgproc.THRESH_BINARY, blockSize, meanOffset );
//
//        // slow and does not work to well
////        Photo.fastNlMeansDenoising( tmpImage, filteredImage, 3, 7, 21);
//
//        Bitmap newImage = Bitmap.createBitmap( filteredImage.width(),
//                                               filteredImage.height(),
//                                               Bitmap.Config.ARGB_8888 );
//
//        // note: tmpImage is adaptiveThresholded image
//        Utils.matToBitmap( filteredImage, newImage );
//
//        return newImage;
//    }
}
