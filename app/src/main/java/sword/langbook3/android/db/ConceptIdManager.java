package sword.langbook3.android.db;

import sword.database.DbValue;

public final class ConceptIdManager implements ConceptSetter<ConceptId> {

    @Override
    public ConceptId getKeyFromInt(int key) {
        return (key != 0)? new ConceptId(key) : null;
    }

    @Override
    public ConceptId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public ConceptId recheckAvailability(ConceptId previousAvailableId, ConceptId concept) {
        final int conceptKey = (concept == null)? 0 : concept.key;
        return (previousAvailableId.key > conceptKey)? previousAvailableId : new ConceptId(conceptKey + 1);
    }
}
