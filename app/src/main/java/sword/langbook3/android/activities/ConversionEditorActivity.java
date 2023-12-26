package sword.langbook3.android.activities;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.ConversionEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.ConversionEditorActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.ConversionEditorActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class ConversionEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = activity.newIntent(ConversionEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public ConversionEditorActivity() {
        super(ActivityExtensionsAdapter::new, new ConversionEditorActivityDelegate<>());
    }
}
