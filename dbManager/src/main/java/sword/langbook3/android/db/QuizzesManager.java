package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.langbook3.android.models.QuestionFieldDetails;

public interface QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> extends AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId>, QuizzesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> {
    Integer obtainQuiz(int bunch, ImmutableList<QuestionFieldDetails<AlphabetId>> fields);
    void removeQuiz(int quizId);
    void updateScore(int quizId, AcceptationId acceptation, int score);
}
