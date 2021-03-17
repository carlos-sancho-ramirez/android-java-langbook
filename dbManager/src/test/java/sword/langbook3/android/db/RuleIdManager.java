package sword.langbook3.android.db;

import sword.database.DbValue;

final class RuleIdManager implements ConceptualizableSetter<ConceptIdHolder, RuleIdHolder> {

    @Override
    public RuleIdHolder getKeyFromInt(int key) {
        return (key == 0)? null : new RuleIdHolder(key);
    }

    @Override
    public RuleIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public RuleIdHolder getKeyFromConceptId(ConceptIdHolder concept) {
        return new RuleIdHolder(concept.key);
    }
}
