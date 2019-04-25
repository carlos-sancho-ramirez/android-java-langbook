package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import sword.collections.ImmutableIntList;

import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;

public final class QuizResultActivity extends Activity {

    private static final int REQUEST_CODE_QUESTION = 1;

    private interface ArgKeys {
        String QUIZ = BundleKeys.QUIZ;
    }

    private interface SavedKeys {
        String QUIZ_FINISHED = "finished";
        String GOOD_ANSWER_AMOUNT = "gA";
        String BAD_ANSWER_AMOUNT = "bA";
    }

    public static void open(Context context, int quizId) {
        Intent intent = new Intent(context, QuizResultActivity.class);
        intent.putExtra(ArgKeys.QUIZ, quizId);
        context.startActivity(intent);
    }

    private boolean _quizFinished;
    private int _goodAnswerCount;
    private int _badAnswerCount;

    private int _quizId;
    private TextView _textView;
    private View _knowledgeBarView;
    private QuizSelectorActivity.Progress _progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_result_activity);

        if (savedInstanceState != null) {
            _quizFinished = savedInstanceState.getBoolean(SavedKeys.QUIZ_FINISHED);
            _goodAnswerCount = savedInstanceState.getInt(SavedKeys.GOOD_ANSWER_AMOUNT);
            _badAnswerCount = savedInstanceState.getInt(SavedKeys.BAD_ANSWER_AMOUNT);
        }

        _quizId = getIntent().getIntExtra(ArgKeys.QUIZ, 0);
        _textView = findViewById(R.id.textView);
        _knowledgeBarView = findViewById(R.id.knowledgeBarView);
    }

    private void refreshUi() {
        final int totalAnswerCount = _goodAnswerCount + _badAnswerCount;

        if (_progress == null) {
            _progress = QuizSelectorActivity.readProgress(DbManager.getInstance().getReadableDatabase(), _quizId);
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
                .append(" (").append(_progress.getCompletenessString()).append(')');
        _textView.setText(sb.toString());
        _knowledgeBarView.setBackground(_progress.getDrawable());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_QUESTION) {
            _quizFinished = true;
            if (data == null) {
                finish();
            }
            else {
                _goodAnswerCount = data.getIntExtra(QuestionActivity.ReturnKeys.GOOD_ANSWER_COUNT, 0);
                _badAnswerCount = data.getIntExtra(QuestionActivity.ReturnKeys.BAD_ANSWER_COUNT, 0);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!_quizFinished) {
            QuestionActivity.open(this, REQUEST_CODE_QUESTION, _quizId);
        }
        else {
            refreshUi();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean(SavedKeys.QUIZ_FINISHED, _quizFinished);
        out.putInt(SavedKeys.GOOD_ANSWER_AMOUNT, _goodAnswerCount);
        out.putInt(SavedKeys.BAD_ANSWER_AMOUNT, _badAnswerCount);
    }
}
