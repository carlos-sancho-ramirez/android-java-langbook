package sword.langbook3.android;

import android.content.Context;
import android.content.SharedPreferences;

import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdBundler;

import static android.content.Context.MODE_PRIVATE;

public final class LangbookPreferences {
    private static final String file = "langbook";
    private static final String preferredAlphabet = "preferredAlphabet";

    private static LangbookPreferences instance;
    private final Context _context;

    static void createInstance(Context context) {
        instance = new LangbookPreferences(context);
    }

    public static LangbookPreferences getInstance() {
        if (instance == null) {
            throw new AssertionError("instance not present");
        }

        return instance;
    }

    private LangbookPreferences(Context context) {
        if (context == null) {
            throw new IllegalArgumentException();
        }

        _context = context;
    }

    private SharedPreferences getPreferences() {
        return _context.getSharedPreferences(LangbookPreferences.file, MODE_PRIVATE);
    }

    public AlphabetId getPreferredAlphabet() {
        return AlphabetIdBundler.readFromPreferences(getPreferences(), preferredAlphabet);
    }

    public void setPreferredAlphabet(AlphabetId alphabet) {
        AlphabetIdBundler.writeIntoPreferences(getPreferences(), preferredAlphabet, alphabet);
    }
}
