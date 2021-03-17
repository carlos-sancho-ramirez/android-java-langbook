package sword.langbook3.android.db;

import sword.collections.ImmutableSet;

public interface DefinitionsManager<ConceptId> extends DefinitionsChecker<ConceptId> {
    void addDefinition(ConceptId baseConcept, ConceptId concept, ImmutableSet<ConceptId> complements);
    boolean removeDefinition(ConceptId complementedConcept);
}
