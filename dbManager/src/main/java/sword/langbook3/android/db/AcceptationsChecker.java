package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Set;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.CorrelationDetailsModel;
import sword.langbook3.android.models.DisplayableItem;

public interface AcceptationsChecker<LanguageId, AlphabetId, CorrelationId> extends ConceptsChecker {
    LanguageId findLanguageByCode(String code);

    /**
     * Return the main alphabet for a given language
     * @param language Identifier for an existing language.
     * @return The identifier for the main alphabet, or null if language is not present
     */
    AlphabetId findMainAlphabetForLanguage(LanguageId language);

    ImmutableSet<AlphabetId> findAlphabetsByLanguage(LanguageId language);
    ImmutableCorrelation<AlphabetId> getAcceptationTexts(int acceptation);
    Conversion<AlphabetId> getConversion(ImmutablePair<AlphabetId, AlphabetId> pair);
    ImmutableMap<AlphabetId, AlphabetId> getConversionsMap();
    ImmutableList<CorrelationId> getAcceptationCorrelationArray(int acceptation);
    ImmutableIntSet findAcceptationsByConcept(int concept);
    int conceptFromAcceptation(int acceptationId);
    boolean isAlphabetPresent(AlphabetId targetAlphabet);
    LanguageId getLanguageFromAlphabet(AlphabetId alphabet);
    ImmutableMap<LanguageId, String> readAllLanguages(AlphabetId preferredAlphabet);
    ImmutableMap<AlphabetId, String> readAllAlphabets(AlphabetId preferredAlphabet);
    ImmutableCorrelation<AlphabetId> getCorrelationWithText(CorrelationId id);
    DisplayableItem readConceptAcceptationAndText(int concept, AlphabetId preferredAlphabet);
    String readConceptText(int concept, AlphabetId preferredAlphabet);
    ImmutableMap<AlphabetId, String> readAlphabetsForLanguage(LanguageId language, AlphabetId preferredAlphabet);
    boolean checkAlphabetCanBeRemoved(AlphabetId alphabet);
    CorrelationDetailsModel<AlphabetId, CorrelationId> getCorrelationDetails(CorrelationId id, AlphabetId preferredAlphabet);
    CorrelationId findCorrelation(Correlation<AlphabetId> correlation);
    boolean isAnyLanguagePresent();
    ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> readAcceptationTextsAndLanguage(int acceptation);
    ImmutableMap<AlphabetId, AlphabetId> findConversions(Set<AlphabetId> alphabets);
    ImmutableIntValueMap<String> readTextAndDynamicAcceptationsMapFromAcceptation(int acceptation);
}
