package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.QuizResultActivityDelegate;
import sword.langbook3.android.activities.delegates.QuizResultActivityDelegate.ArgKeys;
import sword.langbook3.android.db.QuizId;
import sword.langbook3.android.db.QuizIdBundler;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class QuizResultActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, QuizId quizId) {
        final Intent intent = context.newIntent(QuizResultActivity.class);
        QuizIdBundler.writeAsIntentExtra(intent, ArgKeys.QUIZ, quizId);
        context.startActivity(intent);
    }

    public QuizResultActivity() {
        super(ActivityExtensionsAdapter::new, new QuizResultActivityDelegate<>());
    }
}
