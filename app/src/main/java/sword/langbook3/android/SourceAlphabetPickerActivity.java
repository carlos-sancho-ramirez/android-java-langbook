package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.SourceAlphabetPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.SourceAlphabetPickerActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.SourceAlphabetPickerActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class SourceAlphabetPickerActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = activity.newIntent(SourceAlphabetPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public SourceAlphabetPickerActivity() {
        super(ActivityExtensionsAdapter::new, new SourceAlphabetPickerActivityDelegate<>());
    }
}
