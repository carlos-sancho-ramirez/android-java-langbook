package sword.langbook3.android.db;

import sword.langbook3.android.models.DefinitionDetails;

public interface DefinitionsChecker extends ConceptsChecker {
    DefinitionDetails getDefinition(int concept);
}
