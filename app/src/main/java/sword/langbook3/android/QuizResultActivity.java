package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class QuizResultActivity extends Activity {

    private static final int REQUEST_CODE_QUESTION = 1;

    private static final class BundleKeys {
        static final String QUIZ = "quiz";
    }

    private static final class SavedKeys {
        static final String QUIZ_FINISHED = "finished";
        static final String GOOD_ANSWER_AMOUNT = "gA";
        static final String BAD_ANSWER_AMOUNT = "bA";
    }

    public static void open(Context context, int quizId) {
        Intent intent = new Intent(context, QuizResultActivity.class);
        intent.putExtra(BundleKeys.QUIZ, quizId);
        context.startActivity(intent);
    }

    private boolean _quizFinished;
    private int _goodAnswerCount;
    private int _badAnswerCount;

    private int _quizId;
    private TextView _textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_result_activity);

        if (savedInstanceState != null) {
            _quizFinished = savedInstanceState.getBoolean(SavedKeys.QUIZ_FINISHED);
            _goodAnswerCount = savedInstanceState.getInt(SavedKeys.GOOD_ANSWER_AMOUNT);
            _badAnswerCount = savedInstanceState.getInt(SavedKeys.BAD_ANSWER_AMOUNT);
        }

        _quizId = getIntent().getIntExtra(BundleKeys.QUIZ, 0);
        _textView = findViewById(R.id.textView);
    }

    private void refreshUi() {
        final int totalAnswerCount = _goodAnswerCount + _badAnswerCount;

        final StringBuilder sb = new StringBuilder();
        sb.append("Total given answers: ").append(totalAnswerCount)
                .append("\nGood answers: ").append(_goodAnswerCount)
                .append("\nBad answers: ").append(_badAnswerCount);
        _textView.setText(sb.toString());
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
                refreshUi();
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
