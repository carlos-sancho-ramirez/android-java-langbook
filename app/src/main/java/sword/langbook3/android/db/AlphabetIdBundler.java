package sword.langbook3.android.db;

import android.content.Intent;
import android.content.SharedPreferences;

public final class AlphabetIdBundler {

    private static final AlphabetId defaultPreferredAlphabet = new AlphabetId(4); // Spanish

    public static AlphabetId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new AlphabetId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, AlphabetId alphabetId) {
        if (alphabetId != null) {
            intent.putExtra(key, alphabetId.key);
        }
    }

    public static AlphabetId readFromPreferences(SharedPreferences preferences, String key) {
        final int rawAlphabet = preferences.getInt(key, 0);
        return (rawAlphabet != 0)? new AlphabetId(rawAlphabet) : defaultPreferredAlphabet;
    }

    public static void writeIntoPreferences(SharedPreferences preferences, String key, AlphabetId alphabet) {
        if (alphabet != null) {
            preferences.edit().putInt(key, alphabet.key).apply();
        }
    }

    private AlphabetIdBundler() {
    }
}
