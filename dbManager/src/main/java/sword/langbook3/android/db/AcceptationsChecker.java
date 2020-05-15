package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutablePair;
import sword.collections.IntKeyMap;
import sword.collections.IntSet;
import sword.langbook3.android.collections.ImmutableIntPair;
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
    Integer findMainAlphabetForLanguage(int language);

    ImmutableIntSet findAlphabetsByLanguage(int language);
    ImmutableIntKeyMap<String> getAcceptationTexts(int acceptation);
    Conversion getConversion(ImmutableIntPair pair);
    ImmutableIntPairMap getConversionsMap();
    ImmutableIntList getAcceptationCorrelationArray(int acceptation);
    ImmutableIntSet findAcceptationsByConcept(int concept);
    int conceptFromAcceptation(int acceptationId);
    boolean isAlphabetPresent(int targetAlphabet);
    Integer getLanguageFromAlphabet(int alphabet);
    ImmutableIntKeyMap<String> readAllLanguages(int preferredAlphabet);
    ImmutableIntKeyMap<String> readAllAlphabets(int preferredAlphabet);
    ImmutableIntKeyMap<String> getCorrelationWithText(int correlationId);
    DisplayableItem readConceptAcceptationAndText(int concept, int preferredAlphabet);
    String readConceptText(int concept, int preferredAlphabet);
    ImmutableIntKeyMap<String> readAlphabetsForLanguage(int language, int preferredAlphabet);
    boolean checkAlphabetCanBeRemoved(int alphabet);
    CorrelationDetailsModel getCorrelationDetails(int correlationId, int preferredAlphabet);
    Integer findCorrelation(IntKeyMap<String> correlation);
    boolean isAnyLanguagePresent();
    ImmutablePair<ImmutableIntKeyMap<String>, Integer> readAcceptationTextsAndLanguage(int acceptation);
    ImmutableIntPairMap findConversions(IntSet alphabets);
    ImmutableIntValueMap<String> readTextAndDynamicAcceptationsMapFromAcceptation(int acceptation);
}
