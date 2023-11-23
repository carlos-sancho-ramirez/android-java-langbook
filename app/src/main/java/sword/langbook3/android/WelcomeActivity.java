package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.WelcomeActivityDelegate;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class WelcomeActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode) {
        final Intent intent = activity.newIntent(WelcomeActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public WelcomeActivity() {
        super(ActivityExtensionsAdapter::new, new WelcomeActivityDelegate<>());
    }
}
