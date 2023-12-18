package sword.langbook3.android.activities;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.CharacterCompositionDefinitionEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.CharacterCompositionDefinitionEditorActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.CharacterCompositionDefinitionEditorActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class CharacterCompositionDefinitionEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = context.newIntent(CharacterCompositionDefinitionEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        context.startActivity(intent);
    }

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = activity.newIntent(CharacterCompositionDefinitionEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public CharacterCompositionDefinitionEditorActivity() {
        super(ActivityExtensionsAdapter::new, new CharacterCompositionDefinitionEditorActivityDelegate<>());
    }
}
