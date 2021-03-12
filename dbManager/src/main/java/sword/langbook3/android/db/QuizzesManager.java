package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.langbook3.android.models.QuestionFieldDetails;

public interface QuizzesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> extends AgentsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId>, QuizzesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> {
    Integer obtainQuiz(BunchId bunch, ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields);
    void removeQuiz(int quizId);
    void updateScore(int quizId, AcceptationId acceptation, int score);
}
