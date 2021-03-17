package sword.langbook3.android.db;

public final class AlphabetId extends ConceptualId implements AlphabetIdInterface<ConceptId> {

    AlphabetId(int key) {
        super(key);
    }

    @Override
    public ConceptId getConceptId() {
        return new ConceptId(key);
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AlphabetId && ((AlphabetId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
