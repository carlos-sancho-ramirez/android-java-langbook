package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.langbook3.android.models.QuestionFieldDetails;

public interface QuizzesManager extends AgentsManager, QuizzesChecker {
    Integer obtainQuiz(int bunch, ImmutableList<QuestionFieldDetails> fields);
    void removeQuiz(int quizId);
    void updateScore(int quizId, int acceptation, int score);
}
