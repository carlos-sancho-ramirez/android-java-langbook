package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;

public interface QuizzesChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId extends BunchSetIdInterface, RuleId, AgentId> extends AgentsChecker<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId> {
    ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>>> readQuizSelectorEntriesForBunch(BunchId bunch);
    Progress readQuizProgress(int quizId);
    QuizDetails<AlphabetId, BunchId, RuleId> getQuizDetails(int quizId);
    String readQuestionFieldText(AcceptationId acceptation, QuestionFieldDetails<AlphabetId, RuleId> field);
    ImmutableIntValueMap<AcceptationId> getCurrentKnowledge(int quizId);
}
