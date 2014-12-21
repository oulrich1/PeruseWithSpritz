package com.oriahulrich.perusalwithspritz.lib;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by oriahulrich on 12/20/14.
 */
public class Filter {
    public static Bitmap doFiltering( Bitmap image ) {

        Mat tmpImage = new Mat();
        Mat filteredImage = new Mat();

        Utils.bitmapToMat(image, tmpImage);

        /* - - Kernal Size - - */
        int kSize = 7;
        Size kernSize = new Size(15,15);

        // std-deviations //
        double sigmaColor = kSize / 2.0;  // larger -> more colors are included in color averaging
        double sigmaSpace = kSize * 2.0;  // larger -> pixels closer to the location where the mean
                                          //           is at will be weighted higher

        // smooth the colors a little bit //
        Imgproc.adaptiveBilateralFilter( tmpImage, filteredImage,
                                         kernSize, sigmaSpace, sigmaColor,
                                         new Point(-1,-1), Imgproc.BORDER_DEFAULT );

        /// TODO: COLOR REDUCTION ///
        // find edges and create a mask of the image that
        // references pixels that are not edge pixels

        // sub sample to reduce color space

        // find color clusters of the reduced color space

        // mean shift them and repeatedly and merge clusters
        // "close" enough in color space
        // ( idea: what about repeatedly assigning pixels to
        // their cluster, and then re-find the clusters.. slow?)

        /// convert to greyscale
        Imgproc.cvtColor( filteredImage, filteredImage, Imgproc.COLOR_RGB2GRAY );

        /// SOME METHOD TO BINARIZE THE IMAGE WITHOUT REMOVING THE CHARACTERS ///
        int blockSize = 17;
        double meanOffset = 3;
        Imgproc.adaptiveThreshold( filteredImage, tmpImage,
                                    255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                                    Imgproc.THRESH_BINARY, blockSize, meanOffset );



        Bitmap newImage = Bitmap.createBitmap( filteredImage.width(),
                                               filteredImage.height(),
                                               Bitmap.Config.RGB_565 );

        // note: tmpImage is adaptiveThresholded image
        Utils.matToBitmap( tmpImage, newImage );

        return newImage;
    }
}
