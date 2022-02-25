package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Set;
import sword.langbook3.android.models.CharacterCompositionDetailsModel;
import sword.langbook3.android.models.CharacterPickerItem;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.CorrelationDetailsModel;
import sword.langbook3.android.models.DisplayableItem;

public interface AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId> extends ConceptsChecker<ConceptId> {
    LanguageId findLanguageByCode(String code);

    /**
     * Return the main alphabet for a given language
     * @param language Identifier for an existing language.
     * @return The identifier for the main alphabet, or null if language is not present
     */
    AlphabetId findMainAlphabetForLanguage(LanguageId language);

    ImmutableSet<AlphabetId> findAlphabetsByLanguage(LanguageId language);
    ImmutableCorrelation<AlphabetId> getAcceptationTexts(AcceptationId acceptation);
    Conversion<AlphabetId> getConversion(ImmutablePair<AlphabetId, AlphabetId> pair);
    ImmutableMap<AlphabetId, AlphabetId> getConversionsMap();
    ImmutableList<CorrelationId> getAcceptationCorrelationArray(AcceptationId acceptation);
    ImmutablePair<ImmutableCorrelationArray<AlphabetId>, ImmutableList<CorrelationId>> getCorrelationArrayWithText(CorrelationArrayId correlationArrayId);
    ImmutableSet<AcceptationId> findAcceptationsByConcept(ConceptId concept);
    ConceptId conceptFromAcceptation(AcceptationId acceptationId);
    boolean isAlphabetPresent(AlphabetId targetAlphabet);
    LanguageId getLanguageFromAlphabet(AlphabetId alphabet);
    ImmutableMap<LanguageId, String> readAllLanguages(AlphabetId preferredAlphabet);
    ImmutableMap<AlphabetId, String> readAllAlphabets(AlphabetId preferredAlphabet);
    ImmutableCorrelation<AlphabetId> getCorrelationWithText(CorrelationId id);
    DisplayableItem<AcceptationId> readConceptAcceptationAndText(ConceptId concept, AlphabetId preferredAlphabet);
    String readConceptText(ConceptId concept, AlphabetId preferredAlphabet);
    ImmutableMap<AlphabetId, String> readAlphabetsForLanguage(LanguageId language, AlphabetId preferredAlphabet);
    boolean checkAlphabetCanBeRemoved(AlphabetId alphabet);
    CorrelationDetailsModel<AlphabetId, CharacterId, CorrelationId, AcceptationId> getCorrelationDetails(CorrelationId id, AlphabetId preferredAlphabet);
    CorrelationId findCorrelation(Correlation<AlphabetId> correlation);
    boolean isAnyLanguagePresent();
    ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> readAcceptationTextsAndLanguage(AcceptationId acceptation);
    ImmutableMap<AlphabetId, AlphabetId> findConversions(Set<AlphabetId> alphabets);
    ImmutableMap<String, AcceptationId> readTextAndDynamicAcceptationsMapFromAcceptation(AcceptationId acceptation);
    CharacterCompositionDetailsModel<CharacterId, AcceptationId> getCharacterCompositionDetails(CharacterId characterId);
    ImmutableList<CharacterPickerItem<CharacterId>> getCharacterPickerItems(String items);
}
