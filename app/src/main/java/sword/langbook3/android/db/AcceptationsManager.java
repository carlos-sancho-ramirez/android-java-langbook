package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.models.Conversion;

public interface AcceptationsManager extends AcceptationsChecker {

    /**
     * Add a new language for the given code.
     *
     * This method can return null if the language cannot be added,
     * Usually because the code provided is not valid or already exists in the database.
     *
     * @param code 2-char lowercase language code. Such as "es" for Spanish, "en" for English of "ja" for Japanese.
     * @return A pair containing the language created concept in the left and its main alphabet on its right, or null if it cannot be added.
     */
    ImmutableIntPair addLanguage(String code);
    boolean removeLanguage(int language);

    /**
     * Add a new alphabet to this database as a copy of the given sourceAlphabet.
     *
     * This method is a simplification of {@link #addAlphabetCopyingFromOther(int, int)}
     * where alphabet is a unused reference, calculated after checking the database.
     *
     * @param sourceAlphabet Existing alphabet that will be cloned. This cannot be the target of a conversion.
     * @return A new alphabet identifier if all went fine, or null if the alphabet cannot be added.
     */
    Integer addAlphabetCopyingFromOther(int sourceAlphabet);

    /**
     * Add a new alphabet to this database as a copy of the given sourceAlphabet.
     *
     * This method will check for any correlation using the sourceAlphabet and will
     * create within the same correlation a new entry for the new created alphabet,
     * pointing to the same symbol array.
     *
     * This method allows to link directly the concept of an already inserted acceptation with as a new alphabet.
     * If there is no predefined alphabet reference to be used in this method, maybe the method
     * {@link #addAlphabetCopyingFromOther(int)} should be called instead.
     *
     * If all is OK, the new alphabet will be linked to the same language that the sourceAlphabet is.
     *
     * @param alphabet The identifier for this new alphabet to be added.
     *                 This must not exist already as an alphabet or language,
     *                 but it can be a concept within an acceptation.
     * @param sourceAlphabet Existing alphabet that will be cloned. This cannot be the target of a conversion.
     * @return true if the alphabet has been successfully added, and so, the database content has change.
     */
    boolean addAlphabetCopyingFromOther(int alphabet, int sourceAlphabet);

    /**
     * Add a new alphabet and a new conversion at once, being the resulting alphabet the target of the given conversion.
     * @param conversion Conversion to be evaluated and stored if no conflicts are found.
     * @return Whether the action was completed successfully, and so the database state content has changed.
     */
    boolean addAlphabetAsConversionTarget(Conversion conversion);

    boolean removeAlphabet(int alphabet);

    Integer addAcceptation(int concept, ImmutableList<ImmutableIntKeyMap<String>> correlationArray);

    boolean updateAcceptationCorrelationArray(int acceptation, ImmutableList<ImmutableIntKeyMap<String>> newCorrelationArray);
    boolean removeAcceptation(int acceptation);

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
    boolean shareConcept(int linkedAcceptation, int oldConcept);

    /**
     * Extract the correlation array assigned to the given linkedAcceptation and
     * creates a new acceptation with the same correlation array but with the given concept.
     * @param linkedAcceptation Acceptation from where the correlation array reference has to be copied.
     * @param concept Concept to be applied to the new acceptation created.
     */
    void duplicateAcceptationWithThisConcept(int linkedAcceptation, int concept);

    /**
     * Replace a conversion in the database, insert a new one if non existing, or remove and existing one if the given is empty.
     * This will trigger the update of any word where this conversion may apply.
     * This will fail if the alphabets for the conversions are not existing or they do not belong to the same language.
     *
     * @param conversion New conversion to be included.
     * @return True if something changed in the database. Usually false in case the new conversion cannot be applied.
     */
    boolean replaceConversion(Conversion conversion);
}
