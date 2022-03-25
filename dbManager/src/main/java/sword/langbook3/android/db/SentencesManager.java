package sword.langbook3.android.db;

import sword.collections.Set;
import sword.langbook3.android.models.SentenceSpan;

public interface SentencesManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> extends AcceptationsManager<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, CorrelationId, CorrelationArrayId, AcceptationId>, SentencesChecker<ConceptId, LanguageId, AlphabetId, CharacterId, CharacterCompositionTypeId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> {

    /**
     * Add a new sentence into the database attached to the given concept, text and set of spans.
     * @param concept Meaning of this sentence.
     * @param text Plain text for the sentence.
     * @param spans Set of spans for the plain text provided in order to include semantics.
     * @return The identifier for the new sentence, or null if it is not possible to be included.
     */
    SentenceId addSentence(ConceptId concept, String text, Set<SentenceSpan<AcceptationId>> spans);

    /**
     * Replaces the text and spans for an existing sentence, leaving the concept untouched.
     * @param sentenceId Identifier for the sentence to be updated.
     * @param newText New text for the sentence. This can be the same it was before, or a different one.
     * @param newSpans New set of spans for the given newText. This set will completely replace the previous one.
     *                 In case this set is empty, all current spans for the sentence will be removed.
     * @return Whether the operation succeeded or not. This will return true even if no change is performed within the database.
     */
    boolean updateSentenceTextAndSpans(SentenceId sentenceId, String newText, Set<SentenceSpan<AcceptationId>> newSpans);

    /**
     * Remove completely the sentence linked to the given identifier and its spans.
     * @param sentenceId Identifier for the sentence to be removed.
     * @return Whether the sentence has been removed or not. This may be false if there was no sentence for the given identifier.
     */
    boolean removeSentence(SentenceId sentenceId);
}
