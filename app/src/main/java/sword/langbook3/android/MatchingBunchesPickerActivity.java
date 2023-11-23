package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.MatchingBunchesPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.MatchingBunchesPickerActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.MatchingBunchesPickerActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class MatchingBunchesPickerActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, Controller controller) {
        final Intent intent = activity.newIntent(MatchingBunchesPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public MatchingBunchesPickerActivity() {
        super(ActivityExtensionsAdapter::new, new MatchingBunchesPickerActivityDelegate<>());
    }
}
