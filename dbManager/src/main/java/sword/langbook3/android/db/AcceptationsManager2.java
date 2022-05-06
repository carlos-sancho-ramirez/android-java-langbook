package sword.langbook3.android.db;

import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;
import sword.langbook3.android.models.CharacterCompositionRepresentation;

public interface AcceptationsManager2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId> extends AcceptationsChecker2<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId>, AcceptationsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> {

    /**
     * Assigns the given unicode to the character id.
     *
     * This operation may fail if the given unicode is already assigned to another character.
     * This operation will remove any token assigned to the character as well.
     *
     * @param characterId character identifier
     * @param unicode unicode to be assigned
     * @return whether the value has been performed without problems.
     */
    boolean assignUnicode(CharacterId characterId, char unicode);

    /**
     * Updates the current assigned token.
     *
     * This operation may fail if the character identifier is not already present in {@link sword.langbook3.android.db.LangbookDbSchema.CharacterTokensTable}.
     *
     * @param characterId character identifier
     * @param token new token text that will replace the previous assigned one.
     * @return whether the database has been updated.
     */
    boolean updateToken(CharacterId characterId, String token);

    /**
     * Replaces all characters matching the oldCharacter by the given characterId.
     *
     * This operation may fail if both characters have already an unicode assigned, as only one unicode is possible per character.
     * In case one of the characters has an unicode assigned, any token assigned to the other will be dropped.
     * In case both characters have a token assigned, the one assigned to the oldCharacter will be dropped.
     *
     * This operation may fail as well in case merging this characters will lead to loops in the character compositions.
     *
     * @param characterId character identifier that will persist.
     * @param oldCharacter character identifier that will disappear from the database after this operation has been performed.
     * @return whether the action has been performed without problems.
     */
    boolean mergeCharacters(CharacterId characterId, CharacterId oldCharacter);

    /**
     * Creates a new character composition or replaces any existing one matching the identifier.
     * This method will return false if due to an error the action cannot be completed.
     * Potential errors that can be found are invalid characters on first or second,
     * invalid composition type or composition types that may generate an infinite composition loop.
     *
     * @param characterId Identifier for the character
     * @param first First part. This can be a string with a single character or a token with braces.
     * @param second Second part. This can be a string with a single character or a token with braces.
     * @param compositionType Composition type
     * @return Whether the create/update action succeeded.
     */
    boolean updateCharacterComposition(CharacterId characterId, CharacterCompositionRepresentation first, CharacterCompositionRepresentation second, CharacterCompositionTypeId compositionType);

    /**
     * Removes the character composition linked to the given identifier.
     *
     * @param characterId Identifier for the composition.
     */
    boolean removeCharacterComposition(CharacterId characterId);

    /**
     * Assigns the given definition to the given identifier.
     *
     * As it is not possible to duplicate character composition definitions,
     * this method will return false in case the existing definition is already
     * assigned to a different identifier. If that is not the case, this method
     * will return true, even if the given definition is currently matching the
     * assigned identifier and no changes are required.
     *
     * This method will replace any existing definition assigned to the given
     * identifier. If the identifier was not in used, a new entry will be
     * included.
     *
     * @param typeId Identifier for the definition
     * @param register Definition data to be stored.
     * @return Whether the operation has been completed or not.
     */
    boolean updateCharacterCompositionDefinition(CharacterCompositionTypeId typeId, CharacterCompositionDefinitionRegister register);
}
