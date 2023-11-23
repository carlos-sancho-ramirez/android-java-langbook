package sword.langbook3.android.interf;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public interface ContextInterface {
    void startActivity(Intent intent);
    String getString(@StringRes int resId, Object... formatArgs);
    Resources getResources();
    int checkPermission(String permission, int pid, int uid);

    @NonNull
    ContentResolver getContentResolver();

    @NonNull
    PackageManager getPackageManager();
}
