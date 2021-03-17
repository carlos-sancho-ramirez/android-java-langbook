package sword.langbook3.android.db;

public interface ConceptSetter<ConceptId extends ConceptIdInterface> extends IntSetter<ConceptId> {
    /**
     * Finds an available unused concept identifier assuming that the given concept is in use.
     * <p>
     * This method assumes that the previousAvailableId parameter is a concept obtained by calling
     * to {@link ConceptsChecker#getNextAvailableConceptId()}, which checks the current state of
     * the database and finds a potential new conceptId to be assigned.
     * <p>
     * This method allow contrasting that identifier with some other ones that may or may not be
     * included in the database to ensure that there is no conflict. This method will provide a
     * new identifier in case of conflict, or the same one provided in previousAvailableId
     * parameter if no conflict is present.
     *
     * @param previousAvailableId Concept identifier obtained by calling previously to this method, or {@link ConceptsChecker#getNextAvailableConceptId()}.
     * @param concept Concept to have into account to prevent a potential conflict regarding its availability.
     * @return A new unused concept, or the same provided if there is no conflict.
     */
    ConceptId recheckAvailability(ConceptId previousAvailableId, ConceptId concept);
}
