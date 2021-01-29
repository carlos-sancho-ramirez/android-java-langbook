package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;

public interface QuizzesChecker<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> extends AgentsChecker<LanguageId, AlphabetId, CorrelationId, CorrelationArrayId> {
    ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails<AlphabetId>>> readQuizSelectorEntriesForBunch(int bunch);
    Progress readQuizProgress(int quizId);
    QuizDetails<AlphabetId> getQuizDetails(int quizId);
    String readQuestionFieldText(int acceptation, QuestionFieldDetails<AlphabetId> field);
    ImmutableIntPairMap getCurrentKnowledge(int quizId);
}
