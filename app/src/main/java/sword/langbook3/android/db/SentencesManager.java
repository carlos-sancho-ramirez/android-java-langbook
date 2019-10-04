package sword.langbook3.android.db;

import sword.collections.ImmutableIntRange;

public interface SentencesManager extends AcceptationsManager, SentencesChecker {
    boolean copySentenceMeaning(int thisSentence, int pickedSentence);
    boolean removeSentence(int symbolArrayId);
    boolean addSpan(int symbolArrayId, ImmutableIntRange range, int acceptation);
    boolean removeSpan(int id);
}
