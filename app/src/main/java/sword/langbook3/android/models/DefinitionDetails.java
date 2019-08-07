package sword.langbook3.android.models;

import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntSet;

public final class DefinitionDetails {

    public final int baseConcept;
    public final ImmutableIntSet complements;

    public DefinitionDetails(int baseConcept, ImmutableIntSet complements) {
        if (baseConcept == 0) {
            throw new IllegalArgumentException();
        }

        this.baseConcept = baseConcept;
        this.complements = (complements == null)? ImmutableIntArraySet.empty() : complements;
    }
}
