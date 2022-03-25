package sword.langbook3.android.db;

import sword.database.DbValue;

public final class CharacterCompositionTypeIdManager implements ConceptualizableSetter<ConceptId, CharacterCompositionTypeId> {

    @Override
    public CharacterCompositionTypeId getKeyFromInt(int key) {
        return new CharacterCompositionTypeId(key);
    }

    @Override
    public CharacterCompositionTypeId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static CharacterCompositionTypeId conceptAsCharacterCompositionTypeId(ConceptId concept) {
        return (concept == null)? null : new CharacterCompositionTypeId(concept.key);
    }

    @Override
    public CharacterCompositionTypeId getKeyFromConceptId(ConceptId concept) {
        return conceptAsCharacterCompositionTypeId(concept);
    }
}
