package sword.langbook3.android.db;

import sword.langbook3.android.models.DefinitionDetails;

public interface DefinitionsChecker<ConceptId> extends ConceptsChecker<ConceptId> {
    DefinitionDetails<ConceptId> getDefinition(ConceptId concept);
}
