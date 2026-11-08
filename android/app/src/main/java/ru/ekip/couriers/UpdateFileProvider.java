package ru.ekip.couriers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Read-only content URI so Android's standard package installer can consume the verified cached APK. */
public final class UpdateFileProvider extends ContentProvider {
    private static final String AUTHORITY = "ru.ekip.couriers.updates";
    private static final String APK_PATH = "ekip-update.apk";

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) {
        validate(uri);
        return "application/vnd.android.package-archive";
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File apk = resolve(uri);
        String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = APK_PATH;
            else if (OpenableColumns.SIZE.equals(columns[i])) row[i] = apk.length();
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Update APK is read-only");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Read-only provider"); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Read-only provider"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Read-only provider"); }

    private File resolve(Uri uri) {
        validate(uri);
        File apk = new File(getContext().getCacheDir(), APK_PATH);
        if (!apk.isFile()) throw new IllegalArgumentException("Update APK is missing");
        return apk;
    }

    private void validate(Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme()) || !AUTHORITY.equals(uri.getAuthority())
                || uri.getPathSegments().size() != 1 || !APK_PATH.equals(uri.getLastPathSegment())) {
            throw new IllegalArgumentException("Invalid update URI");
        }
    }
}
