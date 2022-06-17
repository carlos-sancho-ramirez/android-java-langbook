package sword.langbook3.android.db;

import sword.collections.Function;
import sword.collections.ImmutableList;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;
import sword.langbook3.android.models.CharacterCompositionEditorModel;
import sword.langbook3.android.models.CharacterDetailsModel;
import sword.langbook3.android.models.CorrelationDetails2;
import sword.langbook3.android.models.IdentifiableCharacterCompositionResult;
import sword.langbook3.android.models.IdentifiableResult;
import sword.langbook3.android.models.SearchResult;

public interface AcceptationsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> extends ConceptsChecker2<ConceptId>, AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> {
    @Override
    CorrelationDetails2<AlphabetId, CharacterId, CorrelationId, AcceptationId> getCorrelationDetails(CorrelationId id, AlphabetId preferredAlphabet);

    CharacterId findCharacter(char ch);
    String getToken(CharacterId characterId);
    CharacterDetailsModel<CharacterId, AcceptationId> getCharacterDetails(CharacterId characterId, AlphabetId preferredAlphabet);
    ImmutableList<IdentifiableResult<CharacterId>> getCharacterPickerItems(String items);
    CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> getCharacterCompositionDetails(CharacterId characterId);
    ImmutableList<String> suggestCharacterTokens(String filterText);
    ImmutableList<SearchResult<CharacterId, Object>> searchCharacterTokens(String filterText, Function<String, String> textConverter);
    ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> getCharacterCompositionTypes(AlphabetId preferredAlphabet);
    String getAcceptationDisplayableText(AcceptationIdInterface acceptation, AlphabetId preferredAlphabet);

    /**
     * Return the character composition definition assigned to the given
     * identifier, or null if nothing is assigned to it.
     *
     * @param id Identifier for the character composition definition to retrieve.
     * @return The assigned definition, or null if nothing is assigned to the given identifier.
     */
    CharacterCompositionDefinitionRegister getCharacterCompositionDefinition(CharacterCompositionTypeId id);
    CharacterCompositionTypeId findCharacterCompositionDefinition(CharacterCompositionDefinitionRegister register);
    boolean isConceptDefinedAsCharacterCompositionType(ConceptId concept);
}
