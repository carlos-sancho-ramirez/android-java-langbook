package sword.langbook3.android.db;

public final class BunchId extends ConceptualId implements BunchIdInterface<ConceptId> {

    BunchId(int key) {
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
        return other instanceof BunchId && ((BunchId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
