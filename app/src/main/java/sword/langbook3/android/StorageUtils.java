package sword.langbook3.android;

import android.net.Uri;
import android.os.Environment;

public final class StorageUtils {

    private StorageUtils() {
    }

    public static boolean isExternalFileUri(Uri uri) {
        return uri != null && "file".equals(uri.getScheme()) &&
                uri.getPath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }
}
