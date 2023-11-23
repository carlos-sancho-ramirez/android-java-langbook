package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;
import sword.langbook3.android.activities.delegates.AcceptationPickerActivityDelegate;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class AcceptationPickerActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull AcceptationPickerActivityDelegate.Controller controller) {
        final Intent intent = activity.newIntent(AcceptationPickerActivity.class);
        intent.putExtra(AcceptationPickerActivityDelegate.ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public AcceptationPickerActivity() {
        super(ActivityExtensionsAdapter::new, new AcceptationPickerActivityDelegate<>());
    }
}
