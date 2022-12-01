package com.oxi.g2010.demo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

/**
 * @author wei.cui@oxitechnology.com
 * @version 创建时间：Jan 10, 2017 6:18:29 PM 类说明
 */

public class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

    // buffer 转 bmpbuffer
    public static byte[] bufferToBmpbuffer(byte[] buffer, int imageWidth, int imageHeight) {

        byte[] bmpBuffer = new byte[imageWidth * imageHeight + 1078];

        byte[] temp_head = {0x42, 0x4d,// file type
                0x0, 0x0, 0x0, 0x00, // file size***
                0x00, 0x00, // reserved
                0x00, 0x00,// reserved
                0x36, 0x4, 0x00, 0x00,// head byte***
                // infoheader
                0x28, 0x00, 0x00, 0x00,// struct size

                // 0x00,0x01,0x00,0x00,//map width***
                0x00, 0x00, 0x0, 0x00,// map width***
                // 0x68,0x01,0x00,0x00,//map height***
                0x00, 0x00, 0x00, 0x00,// map height***

                0x01, 0x00,// must be 1
                0x08, 0x00,// color count
                0x00, 0x00, 0x00, 0x00, // compression
                // 0x00,0x68,0x01,0x00,//data size***
                0x00, 0x00, 0x00, 0x00,// data size***
                0x00, 0x00, 0x00, 0x00, // dpix
                0x00, 0x00, 0x00, 0x00, // dpiy
                0x00, 0x00, 0x00, 0x00,// color used
                0x00, 0x00, 0x00, 0x00,// color important
        };

        byte[] head = new byte[1078];
        System.arraycopy(temp_head, 0, head, 0, temp_head.length);

        int i, j;
        long num;
        num = imageWidth;
        head[18] = (byte) (num & 0xFF);
        num = num >> 8;
        head[19] = (byte) (num & 0xFF);
        num = num >> 8;
        head[20] = (byte) (num & 0xFF);
        num = num >> 8;
        head[21] = (byte) (num & 0xFF);

        num = imageHeight;
        head[22] = (byte) (num & 0xFF);
        num = num >> 8;
        head[23] = (byte) (num & 0xFF);
        num = num >> 8;
        head[24] = (byte) (num & 0xFF);
        num = num >> 8;
        head[25] = (byte) (num & 0xFF);

        j = 0;
        for (i = 54; i < 1078; i = i + 4) {
            head[i] = head[i + 1] = head[i + 2] = (byte) j;
            head[i + 3] = 0;
            j++;
        }

        System.arraycopy(head, 0, bmpBuffer, 0, head.length);
        System.arraycopy(buffer, 0, bmpBuffer, 1078, imageWidth * imageHeight);

        return bmpBuffer;
    }


    static public void translate180(byte[] imageData, int width, int height) {
        byte[] tempData = new byte[width];

        for (int i = 0; i < height / 2; i++) {
            System.arraycopy(imageData, width * i, tempData, 0, width);
            System.arraycopy(imageData, width * (height - i - 1), imageData, width * i, width);
            System.arraycopy(tempData, 0, imageData, width * (height - i - 1), width);
        }
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, float sx, float sy) {

        if (bitmap == null) {
            Log.e(TAG, "scale bitmap ,bitmap is null ");
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(0.5f, 0.5f); // 长和宽放大缩小的比例
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        return bitmap;
    }


    public static Bitmap createBitmap(byte[] data) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        return bitmap;
    }


    public static byte[] getBufferFromBmpBuffer(byte[] bmpBuffer) {

        int width;
        int height;

        Bitmap bmp = BitmapFactory.decodeByteArray(bmpBuffer, 0, bmpBuffer.length, null);
        if (bmp == null) {
            Log.e(TAG, "decode image buffer fail");
            return null;
        }

        width = bmp.getWidth();
        height = bmp.getHeight();

        byte[] imageBuffer = new byte[256 * 360];
        byte[] buffer = new byte[bmpBuffer.length - 1078];

        System.arraycopy(bmpBuffer, 1078, buffer, 0, buffer.length);

        if (width != 256 || height != 360) {

            adjustImage(buffer, width, height, imageBuffer);
            return imageBuffer;
        }
        return buffer;
    }


    public static void adjustImage(byte[] bufferIn, int inWidth, int inHeight, byte[] bufferOut) {

        int wDiff = inWidth - 256;
        int hDiff = inHeight - 360;

        Arrays.fill(bufferOut, (byte) 0xFF);

        if (wDiff < 0) {
            Log.d("cw_test", "a !");
            if (hDiff > 0) {
                Log.d("cw_test", "1 !");
                for (int i = 0; i < 360; i++) {
                    System.arraycopy(bufferIn, ((hDiff / 2) + i) * inWidth, bufferOut,
                            i * 256 + Math.abs(wDiff) / 2, inWidth);
                }

            } else if (hDiff < 0) {
                Log.d("cw_test", "2 !");
                for (int i = (Math.abs(hDiff) / 2); i < (360 - (Math.abs(hDiff) / 2)); i++) {
                    Log.d("cw_test", "i: " + i);
                    System.arraycopy(bufferIn, (i - (Math.abs(hDiff) / 2)) * inWidth,
                            bufferOut, i * 256 + (Math.abs(wDiff) / 2), inWidth);
                }

            }

        } else if (wDiff > 0) {
            Log.d("cw_test", "b !");
            if (hDiff > 0) {
                Log.d("cw_test", "1 !");
                for (int i = 0; i < 360; i++) {
                    Log.d("cw_test", "i: " + i);
                    System.arraycopy(bufferIn, (i + (hDiff / 2)) * inWidth + (wDiff / 2),
                            bufferOut, i * 256, 256);
                }

            } else if (hDiff < 0) {
                Log.d("cw_test", "2 !");
                for (int i = (Math.abs(hDiff) / 2); i < (360 - (Math.abs(hDiff) / 2)); i++) {
                    System.arraycopy(bufferIn, (i + hDiff / 2) * inWidth,
                            bufferOut, i * 256, 256);
                }
            }
        }
    }

    public static boolean isBmpFile(String filePath) {

        boolean is = false;
        File file = new File(filePath);
        String fileName = file.getName();

        if (fileName.endsWith(".bmp"))
            is = true;

        return is;

    }

    public static boolean isIso(String filePath) {
        boolean is = false;
        File file = new File(filePath);
        String fileName = file.getName();

        if (fileName.endsWith(".iso"))
            is = true;

        return is;

    }

}
