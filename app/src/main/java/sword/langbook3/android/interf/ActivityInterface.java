package sword.langbook3.android.interf;

import android.annotation.TargetApi;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public interface ActivityInterface extends ContextInterface {
    Intent getIntent();
    void setTitle(CharSequence title);
    void setTitle(@StringRes int titleId);
    void setContentView(@LayoutRes int layoutResId);
    <T extends View> T findViewById(@IdRes int id);
    void startActivityForResult(Intent intent, int requestCode);
    void setResult(int resultCode);
    void setResult(int resultCode, Intent data);
    void finish();
    boolean isFinishing();
    void invalidateOptionsMenu();

    @NonNull
    MenuInflater getMenuInflater();

    @NonNull
    LayoutInflater getLayoutInflater();

    @TargetApi(23)
    void requestPermissions(@NonNull String[] permissions, int requestCode);
}
