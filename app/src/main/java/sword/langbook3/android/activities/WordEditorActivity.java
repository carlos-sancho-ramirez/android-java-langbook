package sword.langbook3.android.activities;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class WordEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = activity.newIntent(WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(@NonNull ContextExtensions context, Controller controller) {
        final Intent intent = context.newIntent(WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        context.startActivity(intent);
    }

    public WordEditorActivity() {
        super(ActivityExtensionsAdapter::new, new WordEditorActivityDelegate<>());
    }
}
