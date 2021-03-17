package sword.langbook3.android.db;

public final class LanguageId extends ConceptualId implements LanguageIdInterface<ConceptId> {

    LanguageId(int key) {
        super(key);
    }

    @Override
    public ConceptId getConceptId() {
        return new ConceptId(key);
    }

    public AlphabetId getSuggestedAlphabetId(int index) {
        return new AlphabetId(key + index + 1);
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LanguageId && ((LanguageId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
