package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.langbook3.android.models.QuestionFieldDetails;

public interface QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> extends AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId>, QuizzesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> {
    Integer obtainQuiz(BunchId bunch, ImmutableList<QuestionFieldDetails<AlphabetId>> fields);
    void removeQuiz(int quizId);
    void updateScore(int quizId, AcceptationId acceptation, int score);
}
