package com.oxi.g2010.demo.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.oxi.g2010.demo.FileType;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by wei.cui on 2017/3/11.
 */

public class FileUtils {

    public static String getPathFromContentUri(Context context, Uri uri) {
        String photoPath = "";
        if (context == null || uri == null) {
            return photoPath;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if (isExternalStorageDocument(uri)) {
                String[] split = docId.split(":");
                if (split.length >= 2) {
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        photoPath = Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }
            } else if (isDownloadsDocument(uri)) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                photoPath = getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                String[] split = docId.split(":");
                if (split.length >= 2) {
                    String type = split[0];
                    Uri contentUris = null;
                    if ("image".equals(type)) {
                        contentUris = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUris = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUris = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    String selection = MediaStore.Images.Media._ID + "=?";
                    String[] selectionArgs = new String[]{split[1]};
                    photoPath = getDataColumn(context, contentUris, selection, selectionArgs);
                }
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            photoPath = uri.getPath();
        } else {
            photoPath = getDataColumn(context, uri, null, null);
        }

        return photoPath;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return null;
    }


    //type
    public static void saveImage(Context context, byte[] ImageBuffer, int type) {
        String PATH_IMAGE = "";
        Date dt = new Date();
        Long time = dt.getTime();

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {

            PATH_IMAGE = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + File.separator + "OXiUart";

        } else {
            PATH_IMAGE = context.getApplicationContext().getFilesDir().getAbsolutePath()
                    + File.separator + "OXiUart";
        }

        File dir = new File(PATH_IMAGE);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // String filename = type+ "_" + time;
        String filename = "" + time;

        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        String backname = "";
        if (type == 1) {
            backname = ".bmp";
        } else if (type == 2) {
            backname = ".iso";
        }

        try {
            fos = new FileOutputStream(dir + File.separator + filename + backname);
            bos = new BufferedOutputStream(fos);

            try {
                bos.write(ImageBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();

                    Uri localUri = Uri.fromFile(new File(dir, filename + backname));
                    Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
                    context.sendBroadcast(localIntent);

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    public static FileType getFileType(String filePath) throws IOException {

        File file = new File(filePath);
        return  getFileType(file);
    }

    public static FileType getFileType(File file) throws IOException {

        InputStream is = new FileInputStream(file);
        return getFileType(is);

    }

    public static FileType getFileType(InputStream is) throws IOException {

        byte[] src = new byte[28];
        is.read(src, 0, 28);
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v).toUpperCase();
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        FileType[] fileTypes = FileType.values();
        for (FileType fileType : fileTypes) {
            if (stringBuilder.toString().startsWith(fileType.getValue())) {
                return fileType;
            }
        }
        return null;
    }


}
