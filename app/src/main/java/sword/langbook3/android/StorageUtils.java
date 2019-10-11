package sword.langbook3.android;

import android.net.Uri;

public final class StorageUtils {

    private StorageUtils() {
    }

    // TODO: Improve this logic to determine if the given Uri requires the READ or WRITE external storage permission.
    public static boolean isExternalFileUri(Uri uri) {
        return uri != null && "file".equals(uri.getScheme());
    }
}
