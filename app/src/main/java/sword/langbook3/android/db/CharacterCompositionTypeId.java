package sword.langbook3.android.db;

public final class CharacterCompositionTypeId extends ConceptualId implements CharacterCompositionTypeIdInterface<ConceptId> {

    CharacterCompositionTypeId(int key) {
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
        return other instanceof CharacterCompositionTypeId && ((CharacterCompositionTypeId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
