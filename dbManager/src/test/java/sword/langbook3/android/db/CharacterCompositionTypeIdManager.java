package sword.langbook3.android.db;

import sword.database.DbValue;

final class CharacterCompositionTypeIdManager implements ConceptualizableSetter<ConceptIdHolder, CharacterCompositionTypeIdHolder> {

    @Override
    public CharacterCompositionTypeIdHolder getKeyFromInt(int key) {
        return new CharacterCompositionTypeIdHolder(key);
    }

    @Override
    public CharacterCompositionTypeIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public CharacterCompositionTypeIdHolder getKeyFromConceptId(ConceptIdHolder concept) {
        return new CharacterCompositionTypeIdHolder(concept.key);
    }
}
