package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.QuizSelectorActivityDelegate;
import sword.langbook3.android.activities.delegates.QuizSelectorActivityDelegate.ArgKeys;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class QuizSelectorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, BunchId bunch) {
        Intent intent = context.newIntent(QuizSelectorActivity.class);
        if (bunch != null) {
            BunchIdBundler.writeAsIntentExtra(intent, ArgKeys.BUNCH, bunch);
        }
        context.startActivity(intent);
    }

    public QuizSelectorActivity() {
        super(ActivityExtensionsAdapter::new, new QuizSelectorActivityDelegate<>());
    }
}
