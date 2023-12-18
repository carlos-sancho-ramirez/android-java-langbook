package sword.langbook3.android.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.langbook3.android.interf.ActivityExtensions;

public final class ActivityExtensionsAdapter extends AbstractContextExtensionsAdapter<Activity> implements ActivityExtensions {

    public ActivityExtensionsAdapter(@NonNull Activity activity) {
        super(activity);
    }

    @Override
    public Intent getIntent() {
        return _context.getIntent();
    }

    @Override
    public void setTitle(CharSequence title) {
        _context.setTitle(title);
    }

    @Override
    public void setTitle(@StringRes int titleId) {
        _context.setTitle(titleId);
    }

    @Override
    public void setResult(int resultCode) {
        _context.setResult(resultCode);
    }

    @Override
    public void setResult(int resultCode, Intent data) {
        _context.setResult(resultCode, data);
    }

    @Override
    public void finish() {
        _context.finish();
    }

    @Override
    public boolean isFinishing() {
        return _context.isFinishing();
    }

    @Override
    public void invalidateOptionsMenu() {
        _context.invalidateOptionsMenu();
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return _context.getMenuInflater();
    }

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        return _context.getLayoutInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResId) {
        _context.setContentView(layoutResId);
    }

    @Override
    public <T extends View> T findViewById(@IdRes int id) {
        return _context.findViewById(id);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        _context.startActivityForResult(intent, requestCode);
    }

    @Override
    @TargetApi(23)
    public void requestPermissions(@NonNull String[] permissions, int requestCode) {
        _context.requestPermissions(permissions, requestCode);
    }
}
