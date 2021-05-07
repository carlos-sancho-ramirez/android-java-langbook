package sword.langbook3.android.db;

import sword.database.DbValue;

final class AlphabetIdManager implements ConceptualizableSetter<ConceptIdHolder, AlphabetIdHolder> {

    @Override
    public AlphabetIdHolder getKeyFromInt(int key) {
        return (key != 0)? new AlphabetIdHolder(key) : null;
    }

    @Override
    public AlphabetIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public AlphabetIdHolder getKeyFromConceptId(ConceptIdHolder concept) {
        return new AlphabetIdHolder(concept.key);
    }
}
