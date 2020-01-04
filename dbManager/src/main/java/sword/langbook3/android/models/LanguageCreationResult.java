package sword.langbook3.android.models;

public final class LanguageCreationResult {
    public final int language;
    public final int mainAlphabet;

    public LanguageCreationResult(int language, int mainAlphabet) {
        if (language == mainAlphabet) {
            throw new IllegalArgumentException();
        }

        this.language = language;
        this.mainAlphabet = mainAlphabet;
    }
}
