package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.langbook3.android.models.QuestionFieldDetails;

public interface QuizzesManager<AlphabetId> extends AgentsManager<AlphabetId>, QuizzesChecker<AlphabetId> {
    Integer obtainQuiz(int bunch, ImmutableList<QuestionFieldDetails<AlphabetId>> fields);
    void removeQuiz(int quizId);
    void updateScore(int quizId, int acceptation, int score);
}
