package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.DefinitionEditorActivityDelegate;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class DefinitionEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull DefinitionEditorActivityDelegate.Controller controller) {
        final Intent intent = activity.newIntent(DefinitionEditorActivity.class);
        intent.putExtra(DefinitionEditorActivityDelegate.ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public DefinitionEditorActivity() {
        super(ActivityExtensionsAdapter::new, new DefinitionEditorActivityDelegate<>());
    }
}
