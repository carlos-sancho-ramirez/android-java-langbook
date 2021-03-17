package sword.langbook3.android.db;

import sword.database.DbValue;

public final class BunchIdManager implements ConceptualizableSetter<ConceptId, BunchId> {

    @Override
    public BunchId getKeyFromInt(int key) {
        return new BunchId(key);
    }

    @Override
    public BunchId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static BunchId conceptAsBunchId(ConceptId concept) {
        return (concept == null)? null : new BunchId(concept.key);
    }

    @Override
    public BunchId getKeyFromConceptId(ConceptId concept) {
        return conceptAsBunchId(concept);
    }
}
