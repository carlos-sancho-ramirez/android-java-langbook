package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;
import sword.langbook3.android.activities.delegates.FixedTextAcceptationPickerActivityDelegate;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class FixedTextAcceptationPickerActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull FixedTextAcceptationPickerActivityDelegate.Controller controller) {
        final Intent intent = activity.newIntent(FixedTextAcceptationPickerActivity.class);
        intent.putExtra(FixedTextAcceptationPickerActivityDelegate.ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public FixedTextAcceptationPickerActivity() {
        super(ActivityExtensionsAdapter::new, new FixedTextAcceptationPickerActivityDelegate<>());
    }
}
