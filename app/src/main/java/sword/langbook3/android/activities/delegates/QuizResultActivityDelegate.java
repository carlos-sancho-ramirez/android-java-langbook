package sword.langbook3.android.activities.delegates;

import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import sword.collections.ImmutableIntList;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.ProgressUtils;
import sword.langbook3.android.activities.QuestionActivity;
import sword.langbook3.android.R;
import sword.langbook3.android.db.QuizId;
import sword.langbook3.android.db.QuizIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.Progress;

public final class QuizResultActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> {
    private static final int REQUEST_CODE_QUESTION = 1;

    public interface ArgKeys {
        String QUIZ = BundleKeys.QUIZ;
    }

    private interface SavedKeys {
        String QUIZ_FINISHED = "finished";
        String GOOD_ANSWER_AMOUNT = "gA";
        String BAD_ANSWER_AMOUNT = "bA";
    }

    private Activity _activity;
    private boolean _quizFinished;
    private int _goodAnswerCount;
    private int _badAnswerCount;

    private QuizId _quizId;
    private TextView _textView;
    private View _knowledgeBarView;
    private Progress _progress;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.quiz_result_activity);

        if (savedInstanceState != null) {
            _quizFinished = savedInstanceState.getBoolean(SavedKeys.QUIZ_FINISHED);
            _goodAnswerCount = savedInstanceState.getInt(SavedKeys.GOOD_ANSWER_AMOUNT);
            _badAnswerCount = savedInstanceState.getInt(SavedKeys.BAD_ANSWER_AMOUNT);
        }

        _quizId = QuizIdBundler.readAsIntentExtra(activity.getIntent(), ArgKeys.QUIZ);
        _textView = activity.findViewById(R.id.textView);
        _knowledgeBarView = activity.findViewById(R.id.knowledgeBarView);
    }

    private void refreshUi() {
        final int totalAnswerCount = _goodAnswerCount + _badAnswerCount;

        if (_progress == null) {
            _progress = DbManager.getInstance().getManager().readQuizProgress(_quizId);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Total current answers: ").append(totalAnswerCount)
                .append("\nGood answers: ").append(_goodAnswerCount)
                .append("\nBad answers: ").append(_badAnswerCount);

        sb.append("\nTotal answers given for this quiz: ").append(_progress.getAnsweredQuestionsCount());
        final ImmutableIntList amounts = _progress.getAmountPerScore();
        for (int i = 0; i < amounts.size(); i++) {
            sb.append("\n  With score ").append(MIN_ALLOWED_SCORE + i)
                    .append(": ").append(amounts.get(i));
        }

        sb.append("\n\nTotal possible questions: ").append(_progress.getNumberOfQuestions())
                .append("\nQueried so far: ").append(_progress.getAnsweredQuestionsCount())
                .append(" out of ").append(_progress.getNumberOfQuestions())
                .append(" (").append(ProgressUtils.getCompletenessString(_progress)).append(')');
        _textView.setText(sb.toString());
        _knowledgeBarView.setBackground(ProgressUtils.getDrawable(_progress));
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_QUESTION) {
            _quizFinished = true;
            if (data == null) {
                activity.finish();
            }
            else {
                _goodAnswerCount = data.getIntExtra(QuestionActivityDelegate.ReturnKeys.GOOD_ANSWER_COUNT, 0);
                _badAnswerCount = data.getIntExtra(QuestionActivityDelegate.ReturnKeys.BAD_ANSWER_COUNT, 0);
            }
        }
    }

    @Override
    public void onResume(@NonNull Activity activity) {
        if (!_quizFinished) {
            QuestionActivity.open(activity, REQUEST_CODE_QUESTION, _quizId);
        }
        else {
            refreshUi();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle out) {
        out.putBoolean(SavedKeys.QUIZ_FINISHED, _quizFinished);
        out.putInt(SavedKeys.GOOD_ANSWER_AMOUNT, _goodAnswerCount);
        out.putInt(SavedKeys.BAD_ANSWER_AMOUNT, _badAnswerCount);
    }
}
