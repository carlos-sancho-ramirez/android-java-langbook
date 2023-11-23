package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.SettingsActivityDelegate;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class SettingsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode) {
        final Intent intent = activity.newIntent(SettingsActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public SettingsActivity() {
        super(ActivityExtensionsAdapter::new, new SettingsActivityDelegate<>());
    }
}
