package sword.langbook3.android.db;

import android.content.Intent;

public final class SentenceIdBundler {

    public static SentenceId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new SentenceId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, SentenceId sentenceId) {
        if (sentenceId != null) {
            intent.putExtra(key, sentenceId.key);
        }
    }

    private SentenceIdBundler() {
    }
}
