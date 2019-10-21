package sword.langbook3.android.models;

public final class SynonymTranslationResult {
    public final int language;
    public final String text;
    public final boolean dynamic;

    public SynonymTranslationResult(int language, String text, boolean dynamic) {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        this.language = language;
        this.text = text;
        this.dynamic = dynamic;
    }
}
