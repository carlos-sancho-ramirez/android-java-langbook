package sword.langbook3.android.db;

public final class ConceptId extends ConceptualId implements ConceptIdInterface {

    ConceptId(int key) {
        super(key);
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConceptId && ((ConceptId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
