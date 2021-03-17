package sword.langbook3.android.db;

import sword.database.DbValue;

public final class AlphabetIdManager implements IntSetter<AlphabetId> {

    @Override
    public AlphabetId getKeyFromInt(int key) {
        return (key != 0)? new AlphabetId(key) : null;
    }

    @Override
    public AlphabetId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static AlphabetId getNextAvailableId(ConceptsChecker manager) {
        return new AlphabetId(manager.getNextAvailableConceptId());
    }

    public static int getConceptId(AlphabetId alphabetId) {
        return (alphabetId != null)? alphabetId.key : 0;
    }

    public static AlphabetId conceptAsAlphabetId(int alphabetConcept) {
        return (alphabetConcept != 0)? new AlphabetId(alphabetConcept) : null;
    }
}
