package sword.langbook3.android.db;

import sword.collections.ImmutableSet;

public interface ConceptsChecker2<ConceptId> extends ConceptsChecker<ConceptId> {
    /**
     * Return a set containing the given amount of unassigned concepts.
     *
     * Note that the value of this method is immutable, and can be invalidated
     * after adding something in the database, as some of the concepts may be
     * in use after that operation.
     *
     * @param amount Number of unassigned concepts to be retrieved. It must be a positive number.
     * @return Set of unassigned concepts.
     */
    ImmutableSet<ConceptId> getNextAvailableConceptIds(int amount);
}
