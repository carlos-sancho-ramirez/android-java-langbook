package sword.langbook3.android;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

class LangbookPreferences {
    private static final String file = "langbook";
    private static final String preferredAlphabet = "preferredAlphabet";
    private static final int defaultPreferredAlphabet = 4; // Spanish

    private static LangbookPreferences instance;
    private final Context _context;

    static void createInstance(Context context) {
        instance = new LangbookPreferences(context);
    }

    static LangbookPreferences getInstance() {
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

    public int getPreferredAlphabet() {
        return getPreferences().getInt(preferredAlphabet, defaultPreferredAlphabet);
    }

    public void setPreferredAlphabet(int alphabet) {
        getPreferences().edit().putInt(preferredAlphabet, alphabet).apply();
    }
}
