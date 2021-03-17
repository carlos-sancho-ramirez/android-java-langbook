package sword.langbook3.android.db;

import sword.database.DbValue;

final class ConceptIdManager implements ConceptSetter<ConceptIdHolder> {

    @Override
    public ConceptIdHolder getKeyFromInt(int key) {
        return new ConceptIdHolder(key);
    }

    @Override
    public ConceptIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public ConceptIdHolder recheckAvailability(ConceptIdHolder previousAvailableId, ConceptIdHolder concept) {
        return previousAvailableId.equals(concept)? new ConceptIdHolder(previousAvailableId.key + 1) : previousAvailableId;
    }
}
