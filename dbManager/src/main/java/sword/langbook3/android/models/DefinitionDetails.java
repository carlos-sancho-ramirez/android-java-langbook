package sword.langbook3.android.models;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;

public final class DefinitionDetails<ConceptId> {

    public final ConceptId baseConcept;
    public final ImmutableSet<ConceptId> complements;

    public DefinitionDetails(ConceptId baseConcept, ImmutableSet<ConceptId> complements) {
        if (baseConcept == null) {
            throw new IllegalArgumentException();
        }

        this.baseConcept = baseConcept;
        this.complements = (complements == null)? ImmutableHashSet.empty() : complements;
    }
}
