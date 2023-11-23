package sword.langbook3.android;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.LanguageAdderActivityDelegate;
import sword.langbook3.android.activities.delegates.LanguageAdderActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.LanguageAdderActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class LanguageAdderActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = activity.newIntent(LanguageAdderActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public LanguageAdderActivity() {
        super(ActivityExtensionsAdapter::new, new LanguageAdderActivityDelegate<>());
    }
}
