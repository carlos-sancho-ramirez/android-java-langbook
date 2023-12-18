package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.LinkageMechanismSelectorActivityDelegate;
import sword.langbook3.android.activities.delegates.LinkageMechanismSelectorActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.LinkageMechanismSelectorActivityDelegate.Controller;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class LinkageMechanismSelectorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = activity.newIntent(LinkageMechanismSelectorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public LinkageMechanismSelectorActivity() {
        super(ActivityExtensionsAdapter::new, new LinkageMechanismSelectorActivityDelegate<>());
    }
}
