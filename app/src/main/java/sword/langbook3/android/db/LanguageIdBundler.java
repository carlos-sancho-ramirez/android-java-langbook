package sword.langbook3.android.db;

import android.content.Intent;

public final class LanguageIdBundler {

    public static LanguageId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new LanguageId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, LanguageId languageId) {
        if (languageId != null) {
            intent.putExtra(key, languageId.key);
        }
    }

    private LanguageIdBundler() {
    }
}
