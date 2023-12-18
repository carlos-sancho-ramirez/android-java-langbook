package sword.langbook3.android.activities;

import sword.langbook3.android.activities.delegates.MainSearchActivityDelegate;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class MainSearchActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public MainSearchActivity() {
        super(ActivityExtensionsAdapter::new, new MainSearchActivityDelegate<>());
    }
}
