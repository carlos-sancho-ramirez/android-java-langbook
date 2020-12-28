package sword.langbook3.android.db;

public final class AlphabetIdManager implements IntIdManager<AlphabetId> {

    @Override
    public int getInt(AlphabetId id) {
        return (id != null)? id.key : 0;
    }

    @Override
    public AlphabetId getKeyFromInt(int key) {
        return (key != 0)? new AlphabetId(key) : null;
    }

    public static AlphabetId getNextAvailableId(ConceptsChecker manager) {
        return new AlphabetId(manager.getMaxConcept() + 1);
    }

    public static int getConceptId(AlphabetId alphabetId) {
        return (alphabetId != null)? alphabetId.key : 0;
    }

    public static AlphabetId conceptAsAlphabetId(int alphabetConcept) {
        return (alphabetConcept != 0)? new AlphabetId(alphabetConcept) : null;
    }
}
