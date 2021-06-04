package sword.langbook3.android.db;

import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

public interface SentencesChecker<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, SentenceId> extends AcceptationsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, CorrelationArrayId, AcceptationId> {
    /**
     * Checks if the given symbolArray is not used neither as a correlation nor as a conversion,
     * and then it is merely a sentence.
     */
    boolean isSymbolArrayMerelyASentence(SymbolArrayId symbolArrayId);

    /**
     * Returns the string representation for the given sentence.
     * @param sentenceId Identifier for the sentence
     * @return The text for the sentence, or null if the identifier points to nothing
     */
    String getSentenceText(SentenceId sentenceId);

    ImmutableSet<SentenceSpan<AcceptationId>> getSentenceSpans(SentenceId sentenceId);

    /**
     * Return a map for all sentences that has at least one span with the static acceptation provided,
     * or any dynamic acceptation coming from the given static one.
     *
     * @param staticAcceptation Static acceptation to by found.
     * @return Keys for the returned map are the sentence identifiers, values are the text representation of that sentence.
     */
    ImmutableMap<SentenceId, String> getSampleSentences(AcceptationId staticAcceptation);
    SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> getSentenceDetails(SentenceId sentenceId);
}
