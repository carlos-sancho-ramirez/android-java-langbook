package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutablePair;
import sword.collections.IntKeyMap;
import sword.collections.IntSet;
import sword.langbook3.android.DisplayableItem;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.CorrelationDetailsModel;

public interface AcceptationsChecker extends ConceptsChecker {
    String getSymbolArray(int id);
    Integer findLanguageByCode(String code);
    ImmutableIntSet findAlphabetsByLanguage(int language);
    ImmutableIntKeyMap<String> getAcceptationTexts(int acceptation);
    Conversion getConversion(ImmutableIntPair pair);
    ImmutableIntPairMap getConversionsMap();
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
    ImmutableIntValueMap<String> readTextAndDynamicAcceptationsMapFromStaticAcceptation(int staticAcceptation);
}