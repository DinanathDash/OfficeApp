package com.example.officeapp.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class FileUtils {

    public static String getFileExtension(Context context, Uri uri) {
        String extension;
        // Check uri format to avoid null
        if (uri.getScheme().equals("content")) {
            // If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            // If scheme is a File
            // This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }
        return extension;
    }
    
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if(cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getReadableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        String fileName = getFileName(context, uri);
        File tempFile = new File(context.getCacheDir(), fileName);
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        outputStream.close();
        inputStream.close();
        
        return tempFile;
    }
    public static boolean deleteFile(Context context, Uri uri) {
        try {
            // Check if it's a file URI
            if ("file".equals(uri.getScheme())) {
                File file = new File(uri.getPath());
                return file.delete();
            } else if ("content".equals(uri.getScheme())) {
                // Try DocumentsContract if applicable
                try {
                     if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
                         return android.provider.DocumentsContract.deleteDocument(context.getContentResolver(), uri);
                     }
                } catch (Exception e) {
                    // Fallthrough
                }
                
                // Try ContentResolver
                try {
                    int rows = context.getContentResolver().delete(uri, null, null);
                    return rows > 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static long getFileSize(Context context, Uri uri) {
        long size = 0;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if(index >= 0) {
                        size = cursor.getLong(index);
                    }
                }
            } finally {
                if(cursor != null) cursor.close();
            }
        } else if (uri.getScheme().equals("file")) {
             File file = new File(uri.getPath());
             if (file.exists()) {
                 size = file.length();
             }
        }
        return size;
    }
    public static int getFileIconResource(String extension) {
        if (extension == null) return com.example.officeapp.R.drawable.ic_filter_other;
        
        String type = extension.toLowerCase();
        switch (type) {
            case "doc":
            case "docx":
                return com.example.officeapp.R.drawable.ic_filter_doc;
            case "pdf":
                return com.example.officeapp.R.drawable.ic_filter_pdf;
            case "xls":
            case "xlsx":
                return com.example.officeapp.R.drawable.ic_filter_sheet;
            case "ppt":
            case "pptx":
                return com.example.officeapp.R.drawable.ic_filter_presentation;
            case "txt":
                return com.example.officeapp.R.drawable.ic_filter_txt;
            default:
                return com.example.officeapp.R.drawable.ic_filter_other;
        }
    }

    public static boolean doesFileExist(Context context, Uri uri) {
        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            return file.exists();
        } else if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, 
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                return cursor != null && cursor.getCount() > 0;
            } catch (Exception e) {
                // SecurityException or other issues -> assume not accessible/doesn't exist
                return false;
            }
        }
        return false;
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;
        if (android.content.ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            android.content.ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }
}
