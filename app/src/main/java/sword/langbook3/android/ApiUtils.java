package sword.langbook3.android;

import android.os.Build;

public final class ApiUtils {

    private ApiUtils() {
    }

    public static boolean isAtLeastApi23() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
