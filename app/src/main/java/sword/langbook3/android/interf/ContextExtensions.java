package sword.langbook3.android.interf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public interface ContextExtensions extends ContextInterface {
    @NonNull
    Intent newIntent(Class<? extends Activity> cls);

    void showToast(@StringRes int text);
    void showToast(CharSequence text);

    @NonNull
    AlertDialog.Builder newAlertDialogBuilder();
}
