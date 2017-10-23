package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
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
        static final String POSSIBLE_QUESTION_COUNT = "pqc";
    }

    public static void open(Context context, int quizId) {
        Intent intent = new Intent(context, QuizResultActivity.class);
        intent.putExtra(BundleKeys.QUIZ, quizId);
        context.startActivity(intent);
    }

    private boolean _quizFinished;
    private int _goodAnswerCount;
    private int _badAnswerCount;
    private int _possibleQuestionCount;

    private int _quizId;
    private TextView _textView;
    private View _knowledgeBarView;
    private int[] _progress;
    private int _totalScoredAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_result_activity);

        if (savedInstanceState != null) {
            _quizFinished = savedInstanceState.getBoolean(SavedKeys.QUIZ_FINISHED);
            _goodAnswerCount = savedInstanceState.getInt(SavedKeys.GOOD_ANSWER_AMOUNT);
            _badAnswerCount = savedInstanceState.getInt(SavedKeys.BAD_ANSWER_AMOUNT);
            _possibleQuestionCount = savedInstanceState.getInt(SavedKeys.POSSIBLE_QUESTION_COUNT);
        }

        _quizId = getIntent().getIntExtra(BundleKeys.QUIZ, 0);
        _textView = findViewById(R.id.textView);
        _knowledgeBarView = findViewById(R.id.knowledgeBarView);
    }

    private void refreshUi() {
        final int totalAnswerCount = _goodAnswerCount + _badAnswerCount;

        if (_progress == null) {
            _progress = readProgress();
            _totalScoredAnswers = 0;
            for (int value : _progress) {
                _totalScoredAnswers += value;
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Total current answers: ").append(totalAnswerCount)
                .append("\nGood answers: ").append(_goodAnswerCount)
                .append("\nBad answers: ").append(_badAnswerCount);

        sb.append("\nTotal answers given for this quiz: ").append(_totalScoredAnswers);
        for (int i = 0; i < _progress.length; i++) {
            sb.append("\n  With score " + (QuestionActivity.MIN_ALLOWED_SCORE + i) + ": " + _progress[i]);
        }

        final float queriedPercentage = (_possibleQuestionCount > 0)? (float) _totalScoredAnswers / _possibleQuestionCount * 100 : 100;
        sb.append("\n\nTotal possible questions: ").append(_possibleQuestionCount)
                .append("\nQueried so far: ").append(_totalScoredAnswers)
                .append(" out of ").append(_possibleQuestionCount)
                .append(" (").append(queriedPercentage).append("%)");
        _textView.setText(sb.toString());

        _knowledgeBarView.setBackground((_totalScoredAnswers > 0)? new KnowledgeDrawable(_progress) : null);
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

    private int[] readProgress() {
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        final DbManager.KnowledgeTable knowledge = DbManager.Tables.knowledge;

        final Cursor cursor = db.rawQuery("SELECT " + knowledge.getColumnName(knowledge.getScoreColumnIndex()) + " FROM " + knowledge.getName() + " WHERE " + knowledge.getColumnName(knowledge.getQuizDefinitionColumnIndex()) + "=?",
                new String[] { Integer.toString(_quizId)});

        final int[] progress = new int[QuestionActivity.MAX_ALLOWED_SCORE - QuestionActivity.MIN_ALLOWED_SCORE + 1];
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    _possibleQuestionCount = cursor.getCount();
                    do {
                        final int score = cursor.getInt(0);
                        if (score != QuestionActivity.NO_SCORE) {
                            progress[score - QuestionActivity.MIN_ALLOWED_SCORE]++;
                        }
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return progress;
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
        out.putInt(SavedKeys.POSSIBLE_QUESTION_COUNT, _possibleQuestionCount);
    }
}
