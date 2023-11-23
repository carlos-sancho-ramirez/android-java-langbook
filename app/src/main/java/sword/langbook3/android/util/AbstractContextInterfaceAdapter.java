package sword.langbook3.android.util;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.langbook3.android.interf.ContextInterface;

abstract class AbstractContextInterfaceAdapter<Context extends android.content.Context> implements ContextInterface {

    @NonNull
    final Context _context;

    AbstractContextInterfaceAdapter(@NonNull Context context) {
        ensureNonNull(context);
        _context = context;
    }

    @Override
    public void startActivity(Intent intent) {
        _context.startActivity(intent);
    }

    @Override
    public String getString(@StringRes int resId, Object... formatArgs) {
        return _context.getString(resId, formatArgs);
    }

    @Override
    public Resources getResources() {
        return _context.getResources();
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return _context.checkPermission(permission, pid, uid);
    }

    @NonNull
    @Override
    public ContentResolver getContentResolver() {
        return _context.getContentResolver();
    }

    @NonNull
    @Override
    public PackageManager getPackageManager() {
        return _context.getPackageManager();
    }
}
