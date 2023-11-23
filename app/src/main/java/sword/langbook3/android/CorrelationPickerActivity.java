package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.CorrelationPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.CorrelationPickerActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.CorrelationPickerActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class CorrelationPickerActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = activity.newIntent(CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public CorrelationPickerActivity() {
        super(ActivityExtensionsAdapter::new, new CorrelationPickerActivityDelegate<>());
    }
}
