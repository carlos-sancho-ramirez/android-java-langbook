package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;

public interface QuizzesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> extends AgentsChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> {
    ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails<AlphabetId>>> readQuizSelectorEntriesForBunch(int bunch);
    Progress readQuizProgress(int quizId);
    QuizDetails<AlphabetId> getQuizDetails(int quizId);
    String readQuestionFieldText(AcceptationId acceptation, QuestionFieldDetails<AlphabetId> field);
    ImmutableIntValueMap<AcceptationId> getCurrentKnowledge(int quizId);
}
