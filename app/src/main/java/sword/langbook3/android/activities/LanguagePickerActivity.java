package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.LanguagePickerActivityDelegate;
import sword.langbook3.android.activities.delegates.LanguagePickerActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.LanguagePickerActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class LanguagePickerActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = activity.newIntent(LanguagePickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public LanguagePickerActivity() {
        super(ActivityExtensionsAdapter::new, new LanguagePickerActivityDelegate<>());
    }
}
