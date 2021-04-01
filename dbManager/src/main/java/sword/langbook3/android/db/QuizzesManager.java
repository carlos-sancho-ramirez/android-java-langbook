package sword.langbook3.android.db;

import sword.collections.ImmutableList;
import sword.langbook3.android.models.QuestionFieldDetails;

public interface QuizzesManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId> extends AgentsManager<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId>, QuizzesChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId> {
    Integer obtainQuiz(BunchId bunch, ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields);
    void removeQuiz(int quizId);
    void updateScore(int quizId, AcceptationId acceptation, int score);
}
