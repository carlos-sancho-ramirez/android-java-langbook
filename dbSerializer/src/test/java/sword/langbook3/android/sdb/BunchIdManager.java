package sword.langbook3.android.sdb;

import sword.database.DbValue;
import sword.langbook3.android.db.ConceptualizableSetter;

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
