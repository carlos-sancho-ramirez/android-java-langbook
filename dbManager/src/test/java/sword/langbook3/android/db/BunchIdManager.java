package sword.langbook3.android.db;

import sword.database.DbValue;

final class BunchIdManager implements ConceptualizableSetter<ConceptIdHolder, BunchIdHolder> {

    @Override
    public BunchIdHolder getKeyFromInt(int key) {
        return new BunchIdHolder(key);
    }

    @Override
    public BunchIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public BunchIdHolder getKeyFromConceptId(ConceptIdHolder concept) {
        return new BunchIdHolder(concept.key);
    }
}
