package sword.langbook3.android.models;

public final class LanguageCreationResult<LanguageId, AlphabetId> {
    public final LanguageId language;
    public final AlphabetId mainAlphabet;

    public LanguageCreationResult(LanguageId language, AlphabetId mainAlphabet) {
        this.language = language;
        this.mainAlphabet = mainAlphabet;
    }
}
