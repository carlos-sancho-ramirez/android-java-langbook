package sword.langbook3.android.models;

public final class LanguageCreationResult<AlphabetId> {
    public final int language;
    public final AlphabetId mainAlphabet;

    public LanguageCreationResult(int language, AlphabetId mainAlphabet) {
        this.language = language;
        this.mainAlphabet = mainAlphabet;
    }
}
