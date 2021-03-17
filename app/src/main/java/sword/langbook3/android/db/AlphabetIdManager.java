package sword.langbook3.android.db;

import sword.database.DbValue;

public final class AlphabetIdManager implements ConceptualizableSetter<ConceptId, AlphabetId> {

    @Override
    public AlphabetId getKeyFromInt(int key) {
        return (key != 0)? new AlphabetId(key) : null;
    }

    @Override
    public AlphabetId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static AlphabetId conceptAsAlphabetId(ConceptId concept) {
        return (concept == null)? null : new AlphabetId(concept.key);
    }

    public static int getConceptId(AlphabetId alphabetId) {
        return (alphabetId != null)? alphabetId.key : 0;
    }

    @Override
    public AlphabetId getKeyFromConceptId(ConceptId concept) {
        return conceptAsAlphabetId(concept);
    }
}
