package sword.langbook3.android;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import sword.langbook3.android.QuizSelectionActivity.QuizTypes;
import sword.langbook3.android.QuizSelectionActivity.StringPair;

import static sword.langbook3.android.DbManager.idColumnName;

public class QuestionActivity extends Activity implements View.OnClickListener {

    private static final int MIN_ALLOWED_SCORE = 0;
    private static final int MAX_ALLOWED_SCORE = 20;
    private static final int INITIAL_SCORE = 10;
    private static final int SCORE_INCREMENT = 1;
    private static final int SCORE_DECREMENT = 2;

    private static final long CLICK_MILLIS_TIME_INTERVAL = 800;
    private static final class BundleKeys {
        static final String QUIZ = "quiz";
    }

    private static final class SavedKeys {
        static final String ACCEPTATION = "acc";
        static final String IS_ANSWER_VISIBLE = "av";
        static final String GOOD_ANSWER_COUNT = "ga";
        static final String BAD_ANSWER_COUNT = "ba";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int quizId) {
        Intent intent = new Intent(context, QuestionActivity.class);
        intent.putExtra(BundleKeys.QUIZ, quizId);
        context.startActivity(intent);
    }

    private final SparseIntArray _knowledge = new SparseIntArray();

    private int _quizId;
    private int _quizType;
    private int _bunch;
    private int _sourceAlphabet;
    private int _aux;

    private int _goodAnswerCount;
    private int _badAnswerCount;
    private int _acceptation;
    private StringPair _texts;
    private boolean _isAnswerVisible;

    private TextView _scoreTextView;
    private TextView _questionTextView;
    private TextView _answerTextView;
    private long _lastClickTime;
    private int[] _possibleAcceptations;

    private void readQuizDefinition(SQLiteDatabase db) {
        final DbManager.QuizDefinitionsTable table = DbManager.Tables.quizDefinitions;
        final Cursor cursor = db.rawQuery("SELECT " +
                        table.getColumnName(table.getQuizTypeColumnIndex()) + ',' +
                        table.getColumnName(table.getSourceBunchColumnIndex()) + ',' +
                        table.getColumnName(table.getSourceAlphabetColumnIndex()) + ',' +
                        table.getColumnName(table.getAuxiliarColumnIndex()) +
                        " FROM " + table.getName() + " WHERE " + idColumnName + "=?",
                new String[] {Integer.toString(_quizId)});

        if (!cursor.moveToFirst() || cursor.getCount() != 1) {
            throw new AssertionError();
        }

        try {
            _quizType = cursor.getInt(0);
            _bunch = cursor.getInt(1);
            _sourceAlphabet = cursor.getInt(2);
            _aux = cursor.getInt(3);
        }
        finally {
            cursor.close();
        }
    }

    private int[] readAllPossibleInterAlphabetAcceptations(SQLiteDatabase db, int bunch, int sourceAlphabet, int targetAlphabet) {
        int[] result = new int[0];
        if (sourceAlphabet != targetAlphabet) {
            final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
            final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

            final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
            final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
            final String staAccField = strings.getColumnName(strings.getMainAcceptationColumnIndex());
            final Cursor cursor = db.rawQuery("SELECT J0." + dynAccField +
                " FROM " + strings.getName() + " AS J0" +
                    " JOIN " + strings.getName() + " AS J1 ON J0." + dynAccField + "=J1." + dynAccField +
                    " JOIN " + bunchAcceptations.getName() + " AS J2 ON J0." + dynAccField + "=J2." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) +
                " WHERE J0." + dynAccField + "=J0." + staAccField +
                    " AND J0." + alphabetField + "=?" +
                    " AND J1." + alphabetField + "=?" +
                    " AND J2." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=?",
                            new String[]{Integer.toString(sourceAlphabet), Integer.toString(
                                    targetAlphabet), Integer.toString(bunch)}
                    );

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        result = new int[cursor.getCount()];
                        int index = 0;
                        do {
                            result[index++] = cursor.getInt(0);
                        } while (cursor.moveToNext());
                    }
                }
                finally {
                    cursor.close();
                }
            }
        }

        return result;
    }

    private StringPair readInterAlphabetQuestionTexts(SQLiteDatabase db, int acceptation, int sourceAlphabet, int targetAlphabet) {
        StringPair result = null;
        if (sourceAlphabet != targetAlphabet) {
            final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

            final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
            final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
            final Cursor cursor = db.rawQuery("SELECT " + alphabetField + ',' + strings.getColumnName(strings.getStringColumnIndex()) +
                            " FROM " + strings.getName() +
                            " WHERE " + dynAccField + "=?",
                    new String[]{Integer.toString(acceptation)});

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String question = null;
                        String answer = null;
                        do {
                            final int alphabet = cursor.getInt(0);
                            if (alphabet == sourceAlphabet) {
                                question = cursor.getString(1);
                            }
                            else if (alphabet == targetAlphabet) {
                                answer = cursor.getString(1);
                            }

                            if (question != null && answer != null) {
                                result = new StringPair(question, answer);
                            }
                        } while (result == null && cursor.moveToNext());
                    }
                }
                finally {
                    cursor.close();
                }
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unable to find texts for acceptation " + acceptation + " (" + sourceAlphabet + " -> " + targetAlphabet + ")");
        }

        return result;
    }

    private int[] readAllPossibleAcceptations(SQLiteDatabase db, int quizType, int bunch, int sourceAlphabet, int aux) {
        switch (quizType) {
            case QuizTypes.interAlphabet:
                return readAllPossibleInterAlphabetAcceptations(db, bunch, sourceAlphabet, aux);
        }

        return new int[0];
    }

    private StringPair readQuestionTexts(SQLiteDatabase db, int quizType, int acceptation, int sourceAlphabet, int aux) {
        switch (quizType) {
            case QuizTypes.interAlphabet:
                return readInterAlphabetQuestionTexts(db, acceptation, sourceAlphabet, aux);
        }

        throw new UnsupportedOperationException("Unsupported quiz type");
    }

    private int selectAcceptation(int[] acceptations) {
        final int size = acceptations.length;
        final int[] ponders = new int[size];
        int ponderationCount = 0;
        for (int i = 0; i < size; i++) {
            ponders[i] = ponderationCount;
            final int score = _knowledge.get(acceptations[i], INITIAL_SCORE);
            ponderationCount += MAX_ALLOWED_SCORE + 1 - score;
        }

        final long currentTimeMillis = System.currentTimeMillis();
        final int randomIndex = (int) (currentTimeMillis % ponderationCount);

        int min = 0;
        int max = size;
        do {
            int middle = min + (max - min) / 2;
            if (ponders[middle] < randomIndex) {
                min = middle;
            }
            else {
                max = middle;
            }
        } while(max - min > 1);

        return acceptations[min];
    }

    private int selectNewAcceptation() {
        if (_possibleAcceptations == null) {
            final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
            _possibleAcceptations = readAllPossibleAcceptations(db, _quizType, _bunch, _sourceAlphabet, _aux);
        }

        return selectAcceptation(_possibleAcceptations);
    }

    private void updateTextFields() {
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        _texts = readQuestionTexts(db, _quizType, _acceptation, _sourceAlphabet, _aux);
        _questionTextView.setText(_texts.source);
        _answerTextView.setText("?");

        _scoreTextView.setText("" + _goodAnswerCount + " - " + _badAnswerCount);
    }

    private void toggleAnswerVisibility() {
        final Button revealAnswerButton = findViewById(R.id.revealAnswerButton);
        final LinearLayout rateButtonBar = findViewById(R.id.rateButtonBar);

        if (!_isAnswerVisible) {
            _answerTextView.setText(_texts.target);
            revealAnswerButton.setVisibility(View.GONE);
            rateButtonBar.setVisibility(View.VISIBLE);
            _isAnswerVisible = true;
        }
        else {
            _answerTextView.setText("?");
            revealAnswerButton.setVisibility(View.VISIBLE);
            rateButtonBar.setVisibility(View.GONE);
            _isAnswerVisible = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.question_activity);

        _quizId = getIntent().getIntExtra(BundleKeys.QUIZ, 0);
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        readQuizDefinition(db);
        readCurrentKnowledge(db);

        boolean shouldRevealAnswer = false;
        if (savedInstanceState != null) {
            _acceptation = savedInstanceState.getInt(SavedKeys.ACCEPTATION, 0);
            _goodAnswerCount = savedInstanceState.getInt(SavedKeys.GOOD_ANSWER_COUNT);
            _badAnswerCount = savedInstanceState.getInt(SavedKeys.BAD_ANSWER_COUNT);
            shouldRevealAnswer = savedInstanceState.getBoolean(SavedKeys.IS_ANSWER_VISIBLE);
        }

        if (_acceptation == 0) {
            _acceptation = selectNewAcceptation();
        }

        _questionTextView = findViewById(R.id.questionText);
        _answerTextView = findViewById(R.id.answerText);
        _scoreTextView = findViewById(R.id.scoreTextView);
        updateTextFields();

        findViewById(R.id.revealAnswerButton).setOnClickListener(this);
        findViewById(R.id.goodAnswerButton).setOnClickListener(this);
        findViewById(R.id.badAnswerButton).setOnClickListener(this);

        if (shouldRevealAnswer) {
            toggleAnswerVisibility();
        }
    }

    private void readCurrentKnowledge(SQLiteDatabase db) {
        final DbManager.KnowledgeTable table = DbManager.Tables.knowledge;
        final Cursor cursor = db.rawQuery("SELECT " +
                        table.getColumnName(table.getAcceptationColumnIndex()) + ',' +
                        table.getColumnName(table.getScoreColumnIndex()) +
                        " FROM " + table.getName() + " WHERE " +
                        table.getColumnName(table.getQuizDefinitionColumnIndex()) + "=?",
                new String[] {Integer.toString(_quizId)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    _knowledge.put(cursor.getInt(0), cursor.getInt(1));
                }
            }
            finally {
                cursor.close();
            }
        }
    }

    private void insertKnowledge(SQLiteDatabase db, int score) {
        final DbManager.KnowledgeTable table = DbManager.Tables.knowledge;
        ContentValues cv = new ContentValues();
        cv.put(table.getColumnName(table.getQuizDefinitionColumnIndex()), _quizId);
        cv.put(table.getColumnName(table.getAcceptationColumnIndex()), _acceptation);
        cv.put(table.getColumnName(table.getScoreColumnIndex()), score);

        db.insert(table.getName(), null, cv);
    }

    private void updateKnowledge(SQLiteDatabase db, int score) {
        final DbManager.KnowledgeTable table = DbManager.Tables.knowledge;
        ContentValues cv = new ContentValues();
        cv.put(table.getColumnName(table.getScoreColumnIndex()), score);
        final String whereClause = table.getColumnName(table.getQuizDefinitionColumnIndex()) +
                "=? AND " + table.getColumnName(table.getAcceptationColumnIndex()) + "=?";
        db.update(table.getName(), cv, whereClause,
                new String[] { Integer.toString(_quizId), Integer.toString(_acceptation)});
    }

    private void registerGoodAnswer() {
        final SQLiteDatabase db = DbManager.getInstance().getWritableDatabase();
        final int foundScore = _knowledge.get(_acceptation, MIN_ALLOWED_SCORE - 1);
        if (foundScore < MIN_ALLOWED_SCORE) {
            final int newScore = INITIAL_SCORE + SCORE_INCREMENT;
            _knowledge.put(_acceptation, newScore);
            insertKnowledge(db, newScore);
        }
        else if (foundScore < MAX_ALLOWED_SCORE) {
            final int newProposedScore = foundScore + SCORE_INCREMENT;
            final int newScore = (newProposedScore > MAX_ALLOWED_SCORE)? MAX_ALLOWED_SCORE : newProposedScore;
            _knowledge.put(_acceptation, newScore);
            updateKnowledge(db, newScore);
        }

        _goodAnswerCount++;
    }

    private void registerBadAnswer() {
        final SQLiteDatabase db = DbManager.getInstance().getWritableDatabase();
        final int foundScore = _knowledge.get(_acceptation, MIN_ALLOWED_SCORE - 1);
        if (foundScore < MIN_ALLOWED_SCORE) {
            final int newScore = INITIAL_SCORE - SCORE_DECREMENT;
            _knowledge.put(_acceptation, newScore);
            insertKnowledge(db, newScore);
        }
        else if (foundScore > MIN_ALLOWED_SCORE) {
            final int newProposedScore = foundScore - SCORE_DECREMENT;
            final int newScore = (newProposedScore >= MIN_ALLOWED_SCORE)? newProposedScore : MIN_ALLOWED_SCORE;
            _knowledge.put(_acceptation, newScore);
            updateKnowledge(db, newScore);
        }

        _badAnswerCount++;
    }

    @Override
    public void onClick(View view) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - _lastClickTime > CLICK_MILLIS_TIME_INTERVAL) {
            _lastClickTime = currentTime;
            switch (view.getId()) {
                case R.id.revealAnswerButton:
                    toggleAnswerVisibility();
                    break;

                case R.id.goodAnswerButton:
                    registerGoodAnswer();
                    _acceptation = selectNewAcceptation();
                    updateTextFields();
                    toggleAnswerVisibility();
                    break;

                case R.id.badAnswerButton:
                    registerBadAnswer();
                    _acceptation = selectNewAcceptation();
                    updateTextFields();
                    toggleAnswerVisibility();
                    break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(SavedKeys.ACCEPTATION, _acceptation);
        out.putBoolean(SavedKeys.IS_ANSWER_VISIBLE, _isAnswerVisible);
        out.putInt(SavedKeys.GOOD_ANSWER_COUNT, _goodAnswerCount);
        out.putInt(SavedKeys.BAD_ANSWER_COUNT, _badAnswerCount);
    }
}
