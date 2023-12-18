package sword.langbook3.android.activities;

import android.content.Intent;

import sword.langbook3.android.activities.delegates.QuizEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.QuizEditorActivityDelegate.ArgKeys;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class QuizEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(ActivityExtensions activity, int requestCode, BunchId bunch) {
        final Intent intent = activity.newIntent(QuizEditorActivity.class);
        if (bunch != null) {
            BunchIdBundler.writeAsIntentExtra(intent, ArgKeys.BUNCH, bunch);
        }
        activity.startActivityForResult(intent, requestCode);
    }

    public QuizEditorActivity() {
        super(ActivityExtensionsAdapter::new, new QuizEditorActivityDelegate<>());
    }
}
