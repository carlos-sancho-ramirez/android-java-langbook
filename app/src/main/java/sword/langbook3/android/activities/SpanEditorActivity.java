package sword.langbook3.android.activities;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.SpanEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.SpanEditorActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class SpanEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = activity.newIntent(SpanEditorActivity.class);
        intent.putExtra(SpanEditorActivityDelegate.ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public SpanEditorActivity() {
        super(ActivityExtensionsAdapter::new, new SpanEditorActivityDelegate<>());
    }
}
