package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.AboutActivityDelegate;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class AboutActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context) {
        final Intent intent = context.newIntent(AboutActivity.class);
        context.startActivity(intent);
    }

    public AboutActivity() {
        super(ActivityExtensionsAdapter::new, new AboutActivityDelegate<>());
    }
}
