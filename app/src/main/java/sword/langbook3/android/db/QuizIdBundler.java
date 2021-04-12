package sword.langbook3.android.db;

import android.content.Intent;

public final class QuizIdBundler {

    public static QuizId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new QuizId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, QuizId quizId) {
        if (quizId != null) {
            intent.putExtra(key, quizId.key);
        }
    }

    private QuizIdBundler() {
    }
}
