package com.dinanathdash.officeapp.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.os.Environment;
import android.content.ContentUris;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.io.FileNotFoundException;

public class FileUtils {

    public enum FileStatus {
        EXISTS,
        NOT_FOUND,
        ACCESS_DENIED,
        UNKNOWN_ERROR
    }

    public static FileStatus checkFileStatus(Context context, Uri uri) {
        return checkFileStatusWithPath(context, uri, null);
    }

    public static FileStatus checkFileStatusWithPath(Context context, Uri uri, String fallbackPath) {
        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            return file.exists() ? FileStatus.EXISTS : FileStatus.NOT_FOUND;
        } else if ("content".equals(uri.getScheme())) {
            // 1. Try querying the ContentResolver
            // If the file is deleted from the system (e.g. MediaStore), the query should return empty or null.
            try {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.getCount() == 0) {
                            // Cursor exists but is empty -> File likely deleted from database
                            return FileStatus.NOT_FOUND;
                        }
                    } finally {
                        cursor.close();
                    }
                } else {
                    // unexpected, but might mean not found or provider issue
                }
            } catch (SecurityException e) {
                // Permission revoked -> Try path fallback before giving up (Case 3 fix)
                if (fallbackPath != null && !fallbackPath.isEmpty()) {
                    File file = new File(fallbackPath);
                    if (file.exists()) {
                        return FileStatus.EXISTS;
                    }
                }
                return FileStatus.NOT_FOUND;
            } catch (Exception e) {
                // Ignore other query errors and fall through to stream check
            }

            // 2. Fallback / Confirmation: Try opening the stream
            // This confirms purely physical accessibility
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    inputStream.close();
                    return FileStatus.EXISTS;
                } else {
                    return FileStatus.NOT_FOUND;
                }
            } catch (FileNotFoundException e) {
                return FileStatus.NOT_FOUND;
            } catch (SecurityException e) {
                // Permission revoked -> Try path fallback before giving up (Case 3 fix)
                if (fallbackPath != null && !fallbackPath.isEmpty()) {
                    File file = new File(fallbackPath);
                    if (file.exists()) {
                        return FileStatus.EXISTS;
                    }
                }
                return FileStatus.NOT_FOUND;
            } catch (Exception e) {
                return FileStatus.UNKNOWN_ERROR;
            }
        }
        return FileStatus.UNKNOWN_ERROR;
    }

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
        if (extension == null) return com.dinanathdash.officeapp.R.drawable.ic_filter_other;
        
        String type = extension.toLowerCase();
        switch (type) {
            case "doc":
            case "docx":
                return com.dinanathdash.officeapp.R.drawable.ic_filter_doc;
            case "pdf":
                return com.dinanathdash.officeapp.R.drawable.ic_filter_pdf;
            case "xls":
            case "xlsx":
                return com.dinanathdash.officeapp.R.drawable.ic_filter_sheet;
            case "ppt":
            case "pptx":
                return com.dinanathdash.officeapp.R.drawable.ic_filter_presentation;
            case "txt":
                return com.dinanathdash.officeapp.R.drawable.ic_filter_txt;
            default:
                return com.dinanathdash.officeapp.R.drawable.ic_filter_other;
        }
    }

    public static boolean doesFileExist(Context context, Uri uri) {
        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            return file.exists();
        } else if ("content".equals(uri.getScheme())) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    inputStream.close();
                    return true;
                }
            } catch (Exception e) {
                // SecurityException, FileNotFoundException or other issues -> assume not accessible/doesn't exist
                return false;
            }
            return false;
        }
        return false;
    }

    public static String getPath(Context context, Uri uri) {
        if (uri == null) return null;

        // DocumentProvider
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && 
                android.provider.DocumentsContract.isDocumentUri(context, uri)) {
            
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = android.provider.DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = android.provider.DocumentsContract.getDocumentId(uri);
                
                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4);
                }
                
                try {
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                } catch (Exception e) {
                    // Fallback for some devices
                    return getDataColumn(context, uri, null, null);
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = android.provider.DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                     contentUri = android.provider.MediaStore.Files.getContentUri("external");
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri)) return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            // Warning: handle gracefully
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
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

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
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

    public static boolean isLocalFile(Context context, Uri uri) {
        // 1. If it's a file:// URI, it's local (assumed for this app's context)
        if ("file".equals(uri.getScheme())) {
            return true;
        }
        
        // 2. Check for known local authorities
        // These authorities serve local files, so we can trust them for persistence
        if ("content".equals(uri.getScheme())) {
             String authority = uri.getAuthority();
             if (authority != null) {
                 if (authority.equals("com.android.externalstorage.documents") ||
                     authority.equals("com.android.providers.downloads.documents") ||
                     authority.equals("com.android.providers.media.documents") ||
                     authority.equals("com.google.android.apps.nbu.files.provider")) { // Files by Google
                     return true;
                 }
             }
             
             // 3. Fallback: try to resolve the real path
             String path = getPath(context, uri);
             if (path != null) {
                 // Check if it points to /storage/emulated or /sdcard
                 // This covers most "Internal Storage" cases on Android
                 return path.startsWith("/storage/emulated/") || 
                        path.startsWith("/sdcard/") || 
                        path.startsWith("/storage/sdcard");
             }
        }
        
        // If we can't find a path, or it's remote (Google Photos etc), it's not "local" storage
        return false;
    }

    /**
     * Smart Match: Find a local file in Downloads folder that matches the given name and size.
     * This is used for Case 2: when Gmail provides a temp URI but the file is actually downloaded.
     * Returns the file URI if found, null otherwise.
     */
    public static Uri findLocalFileMatch(Context context, String displayName, long fileSize) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }

        // Check Downloads folder
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            return null;
        }

        File targetFile = new File(downloadsDir, displayName);
        if (targetFile.exists()) {
            // Verify size matches (within reasonable tolerance)
            long localSize = targetFile.length();
            if (fileSize > 0) {
                // Allow 1% difference to account for metadata differences
                long tolerance = (long) (fileSize * 0.01);
                if (Math.abs(localSize - fileSize) <= tolerance) {
                    return Uri.fromFile(targetFile);
                }
            } else {
                // If we don't have size info, just match by name
                return Uri.fromFile(targetFile);
            }
        }

        return null;
    }
}
