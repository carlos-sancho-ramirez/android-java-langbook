package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

public interface SentencesChecker<AlphabetId> extends AcceptationsChecker<AlphabetId> {
    boolean isSymbolArrayMerelyASentence(int symbolArrayId);

    /**
     * Returns the string representation for the given sentence.
     * @param sentenceId Identifier for the sentence
     * @return The text for the sentence, or null if the identifier points to nothing
     */
    String getSentenceText(int sentenceId);

    ImmutableSet<SentenceSpan> getSentenceSpans(int sentenceId);

    /**
     * Return a map for all sentences that has at least one span with the static acceptation provided,
     * or any dynamic acceptation coming from the given static one.
     *
     * @param staticAcceptation Static acceptation to by found.
     * @return Keys for the returned map are the sentence identifiers, values are the text representation of that sentence.
     */
    ImmutableIntKeyMap<String> getSampleSentences(int staticAcceptation);
    SentenceDetailsModel getSentenceDetails(int sentenceId);
}
