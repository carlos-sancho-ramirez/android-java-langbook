package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.models.Conversion;

public interface AcceptationsChecker extends ConceptsChecker {
    Integer findLanguageByCode(String code);
    ImmutableIntSet findAlphabetsByLanguage(int language);
    ImmutableIntKeyMap<String> getAcceptationTexts(int acceptation);
    Conversion getConversion(ImmutableIntPair pair);
    ImmutableIntPairMap getConversionsMap();
    ImmutableIntSet findAcceptationsByConcept(int concept);
}
