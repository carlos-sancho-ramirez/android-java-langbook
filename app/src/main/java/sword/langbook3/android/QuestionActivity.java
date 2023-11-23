package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.QuestionActivityDelegate;
import sword.langbook3.android.activities.delegates.QuestionActivityDelegate.ArgKeys;
import sword.langbook3.android.db.QuizId;
import sword.langbook3.android.db.QuizIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class QuestionActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, QuizId quizId) {
        final Intent intent = activity.newIntent(QuestionActivity.class);
        QuizIdBundler.writeAsIntentExtra(intent, ArgKeys.QUIZ, quizId);
        activity.startActivityForResult(intent, requestCode);
    }

    public QuestionActivity() {
        super(ActivityExtensionsAdapter::new, new QuestionActivityDelegate<>());
    }
}
