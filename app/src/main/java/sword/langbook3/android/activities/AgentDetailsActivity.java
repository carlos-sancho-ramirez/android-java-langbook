package sword.langbook3.android.activities;

import android.content.Intent;

import sword.langbook3.android.activities.delegates.AgentDetailsActivityDelegate;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AgentIdBundler;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

import androidx.annotation.NonNull;

public final class AgentDetailsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, AgentId agent) {
        final Intent intent = context.newIntent(AgentDetailsActivity.class);
        AgentIdBundler.writeAsIntentExtra(intent, AgentDetailsActivityDelegate.ArgKeys.AGENT, agent);
        context.startActivity(intent);
    }

    public AgentDetailsActivity() {
        super(ActivityExtensionsAdapter::new, new AgentDetailsActivityDelegate<>());
    }
}
