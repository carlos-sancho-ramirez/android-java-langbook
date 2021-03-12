package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;

public interface QuizzesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> extends AgentsChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId, RuleId> {
    ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>>> readQuizSelectorEntriesForBunch(BunchId bunch);
    Progress readQuizProgress(int quizId);
    QuizDetails<AlphabetId, BunchId, RuleId> getQuizDetails(int quizId);
    String readQuestionFieldText(AcceptationId acceptation, QuestionFieldDetails<AlphabetId, RuleId> field);
    ImmutableIntValueMap<AcceptationId> getCurrentKnowledge(int quizId);
}
