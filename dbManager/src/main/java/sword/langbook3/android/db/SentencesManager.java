package sword.langbook3.android.db;

import sword.collections.Set;
import sword.langbook3.android.models.SentenceSpan;

public interface SentencesManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId> extends AcceptationsManager<LanguageId, AlphabetId, CorrelationId>, SentencesChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId> {

    /**
     * Add a new sentence into the database attached to the given concept, text and set of spans.
     * @param concept Meaning of this sentence.
     * @param text Plain text for the sentence.
     * @param spans Set of spans for the plain text provided in order to include semantics.
     * @return The identifier for the new sentence, or null if it is not possible to be included.
     */
    Integer addSentence(int concept, String text, Set<SentenceSpan> spans);

    /**
     * Replaces the text and spans for an existing sentence, leaving the concept untouched.
     * @param sentenceId Identifier for the sentence to be updated.
     * @param newText New text for the sentence. This can be the same it was before, or a different one.
     * @param newSpans New set of spans for the given newText. This set will completely replace the previous one.
     *                 In case this set is empty, all current spans for the sentence will be removed.
     * @return Whether the operation succeeded or not. This will return true even if no change is performed within the database.
     */
    boolean updateSentenceTextAndSpans(int sentenceId, String newText, Set<SentenceSpan> newSpans);

    /**
     * Remove completely the sentence linked to the given identifier and its spans.
     * @param sentenceId Identifier for the sentence to be removed.
     * @return Whether the sentence has been removed or not. This may be false if there was no sentence for the given identifier.
     */
    boolean removeSentence(int sentenceId);

    /**
     * Replaces the concept of the targetSentence with the concept of the sourceSentence.
     * This will make them synonym or translation of each other.
     *
     * @param sourceSentenceId Sentence from where the concept will be copied.
     * @param targetSentenceId Sentence where the concept will be replaced.
     * @return Whether the operation succeeded. This can be false if either the source or the target sentence identifier is invalid.
     */
    boolean copySentenceConcept(int sourceSentenceId, int targetSentenceId);
}
