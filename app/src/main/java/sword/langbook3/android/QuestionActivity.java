package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import sword.langbook3.android.QuizSelectionActivity.QuizTypes;
import sword.langbook3.android.QuizSelectionActivity.StringPair;

import static sword.langbook3.android.DbManager.idColumnName;

public class QuestionActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

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
        static final String LEAVE_DIALOG_PRESENT = "ldp";
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

    private AlertDialog _dialog;
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

    private int[] readAllPossibleSynonymOrTranslationAcceptations(SQLiteDatabase db, int bunch, int sourceAlphabet, int targetAlphabet) {
        int[] result = new int[0];
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
        final String conceptField = acceptations.getColumnName(acceptations.getConceptColumnIndex());
        final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
        final String staAccField = strings.getColumnName(strings.getMainAcceptationColumnIndex());
        final Cursor cursor = db.rawQuery("SELECT J0." + dynAccField +
                " FROM " + strings.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + dynAccField + "=J1." + idColumnName +
                        " JOIN " + acceptations.getName() + " AS J2 ON J1." + conceptField + "=J2." + conceptField +
                        " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + dynAccField +
                        " JOIN " + bunchAcceptations.getName() + " AS J4 ON J0." + dynAccField + "=J4." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) +
                " WHERE J0." + dynAccField + "=J0." + staAccField +
                        " AND J1." + idColumnName + "!=J2." + idColumnName +
                        " AND J0." + alphabetField + "=?" +
                        " AND J3." + alphabetField + "=?" +
                        " AND J4." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=?",
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

        return result;
    }

    private int[] readAllRulableAcceptations(SQLiteDatabase db, int bunch, int alphabet, int rule) {
        int[] result = new int[0];
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;
        final DbManager.RuledConceptsTable ruledConcepts = DbManager.Tables.ruledConcepts;
        final DbManager.AgentsTable agents = DbManager.Tables.agents;

        final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
        final String conceptField = acceptations.getColumnName(acceptations.getConceptColumnIndex());
        final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
        final String staAccField = strings.getColumnName(strings.getMainAcceptationColumnIndex());
        final Cursor cursor = db.rawQuery("SELECT J1." + idColumnName +
                " FROM " + bunchAcceptations.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) + "=J1." + idColumnName +
                        " JOIN " + ruledConcepts.getName() + " AS J2 ON J1." + conceptField + "=J2." + ruledConcepts.getColumnName(ruledConcepts.getConceptColumnIndex()) +
                        " JOIN " + agents.getName() + " AS J3 ON J2." + ruledConcepts.getColumnName(ruledConcepts.getAgentColumnIndex()) + "=J3." + idColumnName +
                        " JOIN " + strings.getName() + " AS J4 ON J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) + "=J4." + staAccField +
                        " JOIN " + acceptations.getName() + " AS J5 ON J4." + dynAccField + "=J5." + idColumnName +
                " WHERE J0." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=?" +
                        " AND J4." + alphabetField + "=?" +
                        " AND J3." + agents.getColumnName(agents.getRuleColumnIndex()) + "=?" +
                        " AND J4." + staAccField + "!=J4." + dynAccField +
                        " AND J2." + idColumnName + "=J5." + acceptations.getColumnName(acceptations.getConceptColumnIndex()),
                new String[]{Integer.toString(bunch), Integer.toString(alphabet), Integer.toString(rule)}
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    SparseArray<Object> ids = new SparseArray<>();
                    Object dummy = new Object();
                    do {
                        ids.put(cursor.getInt(0), dummy);
                    } while (cursor.moveToNext());

                    final int idCount = ids.size();
                    result = new int[idCount];
                    for (int i = 0; i < idCount; i++) {
                        result[i] = ids.keyAt(i);
                    }
                }
            }
            finally {
                cursor.close();
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

    private StringPair readSynonymOrTranslationQuestionTexts(SQLiteDatabase db, int acceptation, int sourceAlphabet, int targetAlphabet) {
        StringPair result = null;
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
        final String conceptField = acceptations.getColumnName(acceptations.getConceptColumnIndex());
        final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
        final Cursor cursor = db.rawQuery("SELECT " + alphabetField + ',' + strings.getColumnName(strings.getStringColumnIndex()) + ",J1." + idColumnName +
                " FROM " + acceptations.getName() + " AS J0" +
                    " JOIN " + acceptations.getName() + " AS J1 ON J0." + conceptField + "=J1." + conceptField +
                    " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + dynAccField +
                " WHERE J0." + idColumnName + "=?",
                new String[]{Integer.toString(acceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String question = null;
                    Set<String> answers = new HashSet<>();
                    do {
                        if (cursor.getInt(2) == acceptation && cursor.getInt(0) == sourceAlphabet) {
                            question = cursor.getString(1);
                        }
                        else if (cursor.getInt(2) != acceptation && cursor.getInt(0) == targetAlphabet) {
                            answers.add(cursor.getString(1));
                        }
                    } while (cursor.moveToNext());

                    if (question != null && !answers.isEmpty()) {
                        boolean addComma = false;
                        String answerText = null;
                        for (String answer : answers) {
                            if (addComma) {
                                answerText += ", " + answer;
                            }
                            else {
                                answerText = answer;
                                addComma = true;
                            }
                        }

                        result = new StringPair(question, answerText);
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unable to find texts for acceptation " + acceptation + " (" + sourceAlphabet + " -> " + targetAlphabet + ")");
        }

        return result;
    }

    private StringPair readRuleQuestionTexts(SQLiteDatabase db, int acceptation, int alphabet, int rule) {
        StringPair result = null;
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;
        final DbManager.RuledConceptsTable ruledConcepts = DbManager.Tables.ruledConcepts;
        final DbManager.AgentsTable agents = DbManager.Tables.agents;

        final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
        final String conceptField = acceptations.getColumnName(acceptations.getConceptColumnIndex());
        final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
        final String staAccField = strings.getColumnName(strings.getMainAcceptationColumnIndex());
        final Cursor cursor = db.rawQuery("SELECT J4." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                    " JOIN " + ruledConcepts.getName() + " AS J1 ON J0." + conceptField + "=J1." + ruledConcepts.getColumnName(ruledConcepts.getConceptColumnIndex()) +
                    " JOIN " + agents.getName() + " AS J2 ON J1." + ruledConcepts.getColumnName(ruledConcepts.getAgentColumnIndex()) + "=J2." + idColumnName +
                    " JOIN " + acceptations.getName() + " AS J3 ON J1." + idColumnName + "=J3." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                    " JOIN " + strings.getName() + " AS J4 ON J3." + idColumnName + "=J4." + dynAccField +
                " WHERE J0." + idColumnName + "=?" +
                    " AND J4." + alphabetField + "=?" +
                    " AND J2." + agents.getColumnName(agents.getRuleColumnIndex()) + "=?" +
                    " AND J4." + staAccField + "=J0." + idColumnName,
                new String[]{Integer.toString(acceptation), Integer.toString(alphabet), Integer.toString(rule)}
        );

        String answerText = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    Set<String> answers = new HashSet<>();
                    do {
                        answers.add(cursor.getString(0));
                    } while (cursor.moveToNext());

                    if (!answers.isEmpty()) {
                        boolean addComma = false;
                        for (String answer : answers) {
                            if (addComma) {
                                answerText += ", " + answer;
                            }
                            else {
                                answerText = answer;
                                addComma = true;
                            }
                        }
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        if (answerText == null) {
            throw new IllegalArgumentException("Unable to find answer text for acceptation " + acceptation + " (" + alphabet + " with rule " + rule + ")");
        }

        final Cursor cursor2 = db.rawQuery("SELECT " + strings.getColumnName(strings.getStringColumnIndex()) + " FROM " + strings.getName() + " WHERE " + dynAccField + "=? AND " + alphabetField + "=?", new String[] { Integer.toString(acceptation), Integer.toString(alphabet) });

        if (cursor2 != null) {
            try {
                if (cursor2.moveToFirst()) {
                    result = new StringPair(cursor2.getString(0), answerText);
                }
            }
            finally {
                cursor2.close();
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unable to find question text for acceptation " + acceptation + " (" + alphabet + " with rule " + rule + ")");
        }

        return result;
    }

    private int[] readAllPossibleAcceptations(SQLiteDatabase db, int quizType, int bunch, int sourceAlphabet, int aux) {
        switch (quizType) {
            case QuizTypes.interAlphabet:
                return readAllPossibleInterAlphabetAcceptations(db, bunch, sourceAlphabet, aux);

            case QuizTypes.synonym:
                return readAllPossibleSynonymOrTranslationAcceptations(db, bunch, sourceAlphabet, sourceAlphabet);

            case QuizTypes.translation:
                return readAllPossibleSynonymOrTranslationAcceptations(db, bunch, sourceAlphabet, aux);

            case QuizTypes.appliedRule:
                return readAllRulableAcceptations(db, bunch, sourceAlphabet, aux);
        }

        return new int[0];
    }

    private StringPair readQuestionTexts(SQLiteDatabase db, int quizType, int acceptation, int sourceAlphabet, int aux) {
        switch (quizType) {
            case QuizTypes.interAlphabet:
                return readInterAlphabetQuestionTexts(db, acceptation, sourceAlphabet, aux);

            case QuizTypes.synonym:
                return readSynonymOrTranslationQuestionTexts(db, acceptation, sourceAlphabet, sourceAlphabet);

            case QuizTypes.translation:
                return readSynonymOrTranslationQuestionTexts(db, acceptation, sourceAlphabet, aux);

            case QuizTypes.appliedRule:
                return readRuleQuestionTexts(db, acceptation, sourceAlphabet, aux);
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
            final int ponderationThreshold = MIN_ALLOWED_SCORE + (MAX_ALLOWED_SCORE - MIN_ALLOWED_SCORE) * 3 / 4;
            final int diff = ponderationThreshold - score;
            ponderationCount += (diff > 0)? diff * diff : 1;
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
        boolean leaveDialogPresent = false;
        if (savedInstanceState != null) {
            _acceptation = savedInstanceState.getInt(SavedKeys.ACCEPTATION, 0);
            _goodAnswerCount = savedInstanceState.getInt(SavedKeys.GOOD_ANSWER_COUNT);
            _badAnswerCount = savedInstanceState.getInt(SavedKeys.BAD_ANSWER_COUNT);
            shouldRevealAnswer = savedInstanceState.getBoolean(SavedKeys.IS_ANSWER_VISIBLE);
            leaveDialogPresent = savedInstanceState.getBoolean(SavedKeys.LEAVE_DIALOG_PRESENT);
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

        if (leaveDialogPresent) {
            showLeaveConfirmation();
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
        out.putBoolean(SavedKeys.LEAVE_DIALOG_PRESENT, _dialog != null);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                QuestionActivity.super.onBackPressed();
                break;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        _dialog = null;
    }

    private void showLeaveConfirmation() {
        _dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.questionLeaveConfirmation)
                .setPositiveButton(R.string.yes, this)
                .setNegativeButton(R.string.no, this)
                .setOnDismissListener(this)
                .create();
        _dialog.show();
    }

    @Override
    public void onBackPressed() {
        showLeaveConfirmation();
    }
}
