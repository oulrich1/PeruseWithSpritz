package com.oriahulrich.perusalwithspritz.lib;

//import org.opencv.android.Utils;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.Size;
//import org.opencv.imgproc.Imgproc;

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
//        org.opencv.android.Utils.bitmapToMat(image, tmpImage);
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
//        /// convert to greyscale
//        Imgproc.cvtColor( filteredImage, filteredImage, Imgproc.COLOR_RGB2GRAY );
//
//        int blockSize = 17;
//        double meanOffset = 3;
//        Imgproc.adaptiveThreshold( filteredImage, tmpImage,
//                                    255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
//                                    Imgproc.THRESH_OTSU, blockSize, meanOffset );
////        THRESH_BINARY
//
//        Bitmap newImage = Bitmap.createBitmap( filteredImage.width(),
//                                               filteredImage.height(),
//                                               Bitmap.Config.ARGB_8888 );
//
//        // note: tmpImage is adaptiveThresholded image
//        Utils.matToBitmap(filteredImage, newImage);
//
//        return newImage;
//    }
}
