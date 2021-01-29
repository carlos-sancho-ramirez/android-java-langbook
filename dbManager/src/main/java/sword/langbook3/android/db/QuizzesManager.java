package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.langbook3.android.models.QuestionFieldDetails;

public interface QuizzesManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> extends AgentsManager<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId>, QuizzesChecker<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> {
    Integer obtainQuiz(int bunch, ImmutableList<QuestionFieldDetails<AlphabetId>> fields);
    void removeQuiz(int quizId);
    void updateScore(int quizId, int acceptation, int score);
}
