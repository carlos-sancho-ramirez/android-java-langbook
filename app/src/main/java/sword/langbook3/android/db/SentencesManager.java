package sword.langbook3.android.db;

import sword.collections.Set;
import sword.langbook3.android.models.SentenceSpan;

public interface SentencesManager extends AcceptationsManager, SentencesChecker {
    Integer addSentence(String text, Set<SentenceSpan> spans);
    boolean replaceSentence(int sentenceId, String newText, Set<SentenceSpan> newSpans);
    boolean removeSentence(int sentenceId);
    boolean copySentenceMeaning(int thisSentence, int pickedSentence);
}
