package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;

public interface DefinitionsManager extends DefinitionsChecker {
    void addDefinition(int baseConcept, int concept, ImmutableIntSet complements);
    boolean removeDefinition(int complementedConcept);
}
