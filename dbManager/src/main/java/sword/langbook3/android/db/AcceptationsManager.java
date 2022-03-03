package sword.langbook3.android.db;

import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;

public interface AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId> extends AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CorrelationId, CorrelationArrayId, AcceptationId> {

    /**
     * Add a new language for the given code.
     *
     * This method can return null if the language cannot be added,
     * Usually because the code provided is not valid or already exists in the database.
     *
     * @param code 2-char lowercase language code. Such as "es" for Spanish, "en" for English of "ja" for Japanese.
     * @return A pair containing the language created concept and its main alphabet, or null if it cannot be added.
     */
    LanguageCreationResult<LanguageId, AlphabetId> addLanguage(String code);
    boolean removeLanguage(LanguageId language);

    /**
     * Add a new alphabet to this database as a copy of the given sourceAlphabet.
     *
     * This method will check for any correlation using the sourceAlphabet and will
     * create within the same correlation a new entry for the new created alphabet,
     * pointing to the same symbol array.
     *
     * This method allows to link directly the concept of an already inserted acceptation with as a new alphabet.
     *
     * If all is OK, the new alphabet will be linked to the same language that the sourceAlphabet is.
     *
     * @param alphabet The identifier for this new alphabet to be added.
     *                 This must not exist already as an alphabet or language,
     *                 but it can be a concept within an acceptation.
     * @param sourceAlphabet Existing alphabet that will be cloned. This cannot be the target of a conversion.
     * @return true if the alphabet has been successfully added, and so, the database content has change.
     */
    boolean addAlphabetCopyingFromOther(AlphabetId alphabet, AlphabetId sourceAlphabet);

    /**
     * Add a new alphabet and a new conversion at once, being the resulting alphabet the target of the given conversion.
     * @param conversion Conversion to be evaluated and stored if no conflicts are found.
     * @return Whether the action was completed successfully, and so the database state content has changed.
     */
    boolean addAlphabetAsConversionTarget(Conversion<AlphabetId> conversion);

    boolean removeAlphabet(AlphabetId alphabet);

    /**
     * Include a new acceptation in the database, for the given concept and correlation array.
     * @param concept Concept bound to this acceptation.
     * @param correlationArray Correlation array for this acceptation.
     * @return An identifier for the new acceptation just included, or null in case the acceptation cannot be added.
     */
    AcceptationId addAcceptation(ConceptId concept, ImmutableCorrelationArray<AlphabetId> correlationArray);

    boolean updateAcceptationCorrelationArray(AcceptationId acceptation, ImmutableCorrelationArray<AlphabetId> newCorrelationArray);
    boolean removeAcceptation(AcceptationId acceptation);

    /**
     * Join 2 concepts in a single one, removing any reference to the given old concept.
     *
     * This method extracts the concept from the given acceptation and replace
     * any reference to the oldConcept for the extracted acceptation concept in the database.
     *
     * @param linkedAcceptation Acceptation from where the concept will be extracted.
     * @param oldConcept Concept to be replaced by the linked one.
     * @return Whether the database has changed.
     */
    boolean shareConcept(AcceptationId linkedAcceptation, ConceptId oldConcept);

    /**
     * Extract the correlation array assigned to the given linkedAcceptation and
     * creates a new acceptation with the same correlation array but with the given concept.
     * @param linkedAcceptation Acceptation from where the correlation array reference has to be copied.
     * @param concept Concept to be applied to the new acceptation created.
     */
    void duplicateAcceptationWithThisConcept(AcceptationId linkedAcceptation, ConceptId concept);

    /**
     * Replace a conversion in the database, insert a new one if non existing, or remove and existing one if the given is empty.
     * This will trigger the update of any word where this conversion may apply.
     * This will fail if the alphabets for the conversions are not existing or they do not belong to the same language.
     *
     * @param conversion New conversion to be included.
     * @return True if something changed in the database. False in case the new conversion cannot be applied, or it is exactly the same as it was there before.
     */
    boolean replaceConversion(Conversion<AlphabetId> conversion);

    /**
     * Creates a new character composition or replaces any existing one matching the identifier.
     * This method will return false if due to an error the action cannot be completed.
     * Potential errors that can be found are invalid characters on first or second,
     * invalid composition type or composition types that may generate an infinite composition loop.
     *
     * @param characterId Identifier for the character
     * @param first Character representation for the first part.
     * @param second Character representation for the second part.
     * @param compositionType Composition type
     * @return Whether the create/update action succeeded.
     */
    boolean updateCharacterComposition(CharacterId characterId, char first, char second, int compositionType);

    /**
     * Removes the character composition linked to the given identifier.
     *
     * @param characterId Identifier for the composition.
     */
    boolean removeCharacterComposition(CharacterId characterId);
}
