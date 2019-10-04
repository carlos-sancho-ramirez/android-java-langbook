package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;

public interface QuizzesChecker extends AgentsChecker {
    ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails>> readQuizSelectorEntriesForBunch(int bunch);
    Progress readQuizProgress(int quizId);
    QuizDetails getQuizDetails(int quizId);
    String readQuestionFieldText(int acceptation, QuestionFieldDetails field);
    ImmutableIntPairMap getCurrentKnowledge(int quizId);
}
