package sword.langbook3.android.db;

import android.content.Intent;

public final class ConceptIdBundler {

    public static ConceptId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new ConceptId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, ConceptId alphabetId) {
        if (alphabetId != null) {
            intent.putExtra(key, alphabetId.key);
        }
    }

    private ConceptIdBundler() {
    }
}
