package sword.langbook3.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.langbook3.android.interf.ContextExtensions;

abstract class AbstractContextExtensionsAdapter<Context extends android.content.Context> extends AbstractContextInterfaceAdapter<Context> implements ContextExtensions {

    AbstractContextExtensionsAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public Intent newIntent(Class<? extends Activity> cls) {
        return new Intent(_context, cls);
    }

    @Override
    public void showToast(@StringRes int text) {
        Toast.makeText(_context, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showToast(CharSequence text) {
        Toast.makeText(_context, text, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    @Override
    public AlertDialog.Builder newAlertDialogBuilder() {
        return new AlertDialog.Builder(_context);
    }
}
