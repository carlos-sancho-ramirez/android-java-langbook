package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.AcceptationConfirmationActivityDelegate;
import sword.langbook3.android.activities.delegates.AcceptationConfirmationActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class AcceptationConfirmationActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = activity.newIntent(AcceptationConfirmationActivity.class);
        intent.putExtra(AcceptationConfirmationActivityDelegate.ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public AcceptationConfirmationActivity() {
        super(ActivityExtensionsAdapter::new, new AcceptationConfirmationActivityDelegate<>());
    }
}
