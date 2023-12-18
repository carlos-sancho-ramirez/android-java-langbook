package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;
import sword.langbook3.android.activities.delegates.AgentEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.AgentEditorActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.AgentEditorActivityDelegate.Controller;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AgentEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = context.newIntent(AgentEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        context.startActivity(intent);
    }

    public AgentEditorActivity() {
        super(ActivityExtensionsAdapter::new, new AgentEditorActivityDelegate<>());
    }
}
