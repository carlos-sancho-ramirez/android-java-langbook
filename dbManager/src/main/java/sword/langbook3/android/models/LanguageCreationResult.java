package sword.langbook3.android.models;

import sword.langbook3.android.db.AlphabetId;

public final class LanguageCreationResult {
    public final int language;
    public final AlphabetId mainAlphabet;

    public LanguageCreationResult(int language, AlphabetId mainAlphabet) {
        if (language == mainAlphabet.key) {
            throw new IllegalArgumentException();
        }

        this.language = language;
        this.mainAlphabet = mainAlphabet;
    }
}
