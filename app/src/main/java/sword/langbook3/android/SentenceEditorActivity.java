package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.SentenceDetailsActivityDelegate;
import sword.langbook3.android.activities.delegates.SentenceEditorActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.SentenceEditorActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class SentenceEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = activity.newIntent(SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public SentenceEditorActivity() {
        super(ActivityExtensionsAdapter::new, new SentenceDetailsActivityDelegate<>());
    }
}
