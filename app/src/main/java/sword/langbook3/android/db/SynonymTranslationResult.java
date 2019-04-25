package sword.langbook3.android.db;

public final class SynonymTranslationResult {
    public final int language;
    public final String text;

    SynonymTranslationResult(int language, String text) {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        this.language = language;
        this.text = text;
    }
}
