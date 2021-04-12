package sword.langbook3.android.db;

import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;

public interface QuizzesChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId, QuizId> extends AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> {
    ImmutableMap<QuizId, ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>>> readQuizSelectorEntriesForBunch(BunchId bunch);
    Progress readQuizProgress(QuizId quizId);
    QuizDetails<AlphabetId, BunchId, RuleId> getQuizDetails(QuizId quizId);
    String readQuestionFieldText(AcceptationId acceptation, QuestionFieldDetails<AlphabetId, RuleId> field);
    ImmutableIntValueMap<AcceptationId> getCurrentKnowledge(QuizId quizId);
}
