package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.collections.Set;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.CorrelationDetailsModel;
import sword.langbook3.android.models.DisplayableItem;

public interface AcceptationsChecker extends ConceptsChecker {
    Integer findLanguageByCode(String code);

    /**
     * Return the main alphabet for a given language
     * @param language Identifier for an existing language.
     * @return The identifier for the main alphabet, or null if language is not present
     */
    AlphabetId findMainAlphabetForLanguage(int language);

    ImmutableSet<AlphabetId> findAlphabetsByLanguage(int language);
    ImmutableMap<AlphabetId, String> getAcceptationTexts(int acceptation);
    Conversion getConversion(ImmutablePair<AlphabetId, AlphabetId> pair);
    ImmutableMap<AlphabetId, AlphabetId> getConversionsMap();
    ImmutableIntList getAcceptationCorrelationArray(int acceptation);
    ImmutableIntSet findAcceptationsByConcept(int concept);
    int conceptFromAcceptation(int acceptationId);
    boolean isAlphabetPresent(AlphabetId targetAlphabet);
    Integer getLanguageFromAlphabet(AlphabetId alphabet);
    ImmutableIntKeyMap<String> readAllLanguages(AlphabetId preferredAlphabet);
    ImmutableMap<AlphabetId, String> readAllAlphabets(AlphabetId preferredAlphabet);
    ImmutableMap<AlphabetId, String> getCorrelationWithText(int correlationId);
    DisplayableItem readConceptAcceptationAndText(int concept, AlphabetId preferredAlphabet);
    String readConceptText(int concept, AlphabetId preferredAlphabet);
    ImmutableMap<AlphabetId, String> readAlphabetsForLanguage(int language, AlphabetId preferredAlphabet);
    boolean checkAlphabetCanBeRemoved(AlphabetId alphabet);
    CorrelationDetailsModel getCorrelationDetails(int correlationId, AlphabetId preferredAlphabet);
    Integer findCorrelation(Map<AlphabetId, String> correlation);
    boolean isAnyLanguagePresent();
    ImmutablePair<ImmutableMap<AlphabetId, String>, Integer> readAcceptationTextsAndLanguage(int acceptation);
    ImmutableMap<AlphabetId, AlphabetId> findConversions(Set<AlphabetId> alphabets);
    ImmutableIntValueMap<String> readTextAndDynamicAcceptationsMapFromAcceptation(int acceptation);
}
