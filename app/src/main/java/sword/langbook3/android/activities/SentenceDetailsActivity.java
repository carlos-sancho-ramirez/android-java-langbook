package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.SentenceDetailsActivityDelegate;
import sword.langbook3.android.activities.delegates.SentenceDetailsActivityDelegate.ArgKeys;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class SentenceDetailsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, SentenceId sentenceId) {
        final Intent intent = activity.newIntent(SentenceDetailsActivity.class);
        SentenceIdBundler.writeAsIntentExtra(intent, ArgKeys.SENTENCE_ID, sentenceId);

        if (requestCode != 0) {
            activity.startActivityForResult(intent, requestCode);
        }
        else {
            activity.startActivity(intent);
        }
    }

    public SentenceDetailsActivity() {
        super(ActivityExtensionsAdapter::new, new SentenceDetailsActivityDelegate<>());
    }
}
