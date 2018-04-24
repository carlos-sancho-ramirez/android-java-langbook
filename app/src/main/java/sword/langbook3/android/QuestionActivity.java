package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import sword.langbook3.android.DbManager.QuestionField;
import sword.langbook3.android.LangbookDbSchema.AcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.AgentsTable;
import sword.langbook3.android.LangbookDbSchema.KnowledgeTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.RuledAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.Tables;

import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public class QuestionActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    static final int NO_SCORE = 0;
    static final int MIN_ALLOWED_SCORE = 1;
    static final int MAX_ALLOWED_SCORE = 20;
    private static final int INITIAL_SCORE = 10;
    private static final int SCORE_INCREMENT = 1;
    private static final int SCORE_DECREMENT = 2;

    private static final long CLICK_MILLIS_TIME_INTERVAL = 600;

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

    static final class ReturnKeys {
        static final String GOOD_ANSWER_COUNT = "g";
        static final String BAD_ANSWER_COUNT = "b";
    }

    public static void open(Activity activity, int requestCode, int quizId) {
        Intent intent = new Intent(activity, QuestionActivity.class);
        intent.putExtra(BundleKeys.QUIZ, quizId);
        activity.startActivityForResult(intent, requestCode);
    }

    private final SparseIntArray _knowledge = new SparseIntArray();

    private int _quizId;
    private QuestionField[] _fields;
    private TextView[] _fieldTextViews;

    private int _goodAnswerCount;
    private int _badAnswerCount;
    private int _acceptation;

    private boolean _isAnswerVisible;

    private AlertDialog _dialog;
    private TextView _scoreTextView;
    private long _lastClickTime;
    private int[] _possibleAcceptations;

    private void readQuizDefinition(SQLiteDatabase db) {
        final QuizDefinitionsTable quizTable = Tables.quizDefinitions;
        final QuestionFieldSets fieldTable = Tables.questionFieldSets;
        final Cursor cursor = db.rawQuery("SELECT " +
                        fieldTable.columns().get(fieldTable.getAlphabetColumnIndex()).name() + ',' +
                        fieldTable.columns().get(fieldTable.getRuleColumnIndex()).name() + ',' +
                        fieldTable.columns().get(fieldTable.getFlagsColumnIndex()).name() +
                        " FROM " + quizTable.name() + " AS J0" +
                        " JOIN " + fieldTable.name() + " AS J1 ON J0." + quizTable.columns().get(quizTable.getQuestionFieldsColumnIndex()).name() + "=J1." + fieldTable.columns().get(fieldTable.getSetIdColumnIndex()).name() +
                        " WHERE J0." + idColumnName + "=?",
                new String[] {Integer.toString(_quizId)});

        try {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                _fields = new QuestionField[cursor.getCount()];
                _fieldTextViews = new TextView[cursor.getCount()];
                int i = 0;
                do {
                    _fields[i++] = new QuestionField(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2));
                } while (cursor.moveToNext());

                if (i != _fields.length) {
                    throw new AssertionError();
                }
            }
        }
        finally {
            cursor.close();
        }
    }

    private String readSameAcceptationQuestionText(SQLiteDatabase db, int index) {
        final StringQueriesTable strings = Tables.stringQueries;
        final Cursor cursor = db.rawQuery("SELECT " + strings.columns().get(strings.getStringColumnIndex()).name() +
                            " FROM " + strings.name() +
                            " WHERE " + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() + "=?" +
                            " AND " + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() + "=?",
                    new String[]{Integer.toString(_acceptation), Integer.toString(_fields[index].alphabet)});


        try {
            if (!cursor.moveToFirst()) {
                throw new AssertionError();
            }

            return cursor.getString(0);
        }
        finally {
            cursor.close();
        }
    }

    private String readSameConceptQuestionText(SQLiteDatabase db, int index) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final StringQueriesTable strings = Tables.stringQueries;
        final Cursor cursor = db.rawQuery("SELECT J2." + strings.columns().get(strings.getStringColumnIndex()).name() +
                        " FROM " + acceptations.name() + " AS J0" +
                        " JOIN " + acceptations.name() + " AS J1 ON J0." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() + "=J1." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() +
                        " JOIN " + strings.name() + " AS J2 ON J1." + idColumnName + "=J2." + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() +
                        " WHERE J0." + idColumnName + "=?" +
                        " AND J2." + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() + "=?" +
                        " AND J1." + idColumnName + "!=J0." + idColumnName,
                        new String[]{Integer.toString(_acceptation), Integer.toString(_fields[index].alphabet)});

        try {
            if (!cursor.moveToFirst()) {
                throw new AssertionError();
            }

            final StringBuilder sb = new StringBuilder(cursor.getString(0));
            while (cursor.moveToNext()) {
                sb.append(", ").append(cursor.getString(0));
            }

            return sb.toString();
        }
        finally {
            cursor.close();
        }
    }

    private String readApplyRuleQuestionText(SQLiteDatabase db, int index) {
        final AgentsTable agents = Tables.agents;
        final RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final StringQueriesTable strings = Tables.stringQueries;
        final Cursor cursor = db.rawQuery("SELECT " + strings.columns().get(strings.getStringColumnIndex()).name() +
                        " FROM " + ruledAcceptations.name() + " AS J0" +
                        " JOIN " + strings.name() + " AS J1 ON J0." + idColumnName + "=J1." + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() +
                        " JOIN " + agents.name() + " AS J2 ON J0." + ruledAcceptations.columns().get(ruledAcceptations.getAgentColumnIndex()).name() + "=J2." + idColumnName +
                        " WHERE " + ruledAcceptations.columns().get(ruledAcceptations.getAcceptationColumnIndex()).name() + "=?" +
                        " AND " + agents.columns().get(agents.getRuleColumnIndex()).name() + "=?" +
                        " AND " + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() + "=?",
                new String[]{
                        Integer.toString(_acceptation),
                        Integer.toString(_fields[index].rule),
                        Integer.toString(_fields[index].alphabet)});

        try {
            if (!cursor.moveToFirst()) {
                throw new AssertionError();
            }

            return cursor.getString(0);
        }
        finally {
            cursor.close();
        }
    }

    private String readFieldText(SQLiteDatabase db, int index) {
        switch (_fields[index].getType()) {
            case QuestionFieldFlags.TYPE_SAME_ACC:
                return readSameAcceptationQuestionText(db, index);

            case QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return readSameConceptQuestionText(db, index);

            case QuestionFieldFlags.TYPE_APPLY_RULE:
                return readApplyRuleQuestionText(db, index);
        }

        throw new UnsupportedOperationException("Unsupported question field type");
    }

    private void selectAcceptation() {
        final int size = _possibleAcceptations.length;

        final int[] ponders = new int[size];
        int ponderationCount = 0;
        for (int i = 0; i < size; i++) {
            ponders[i] = ponderationCount;
            final int score = _knowledge.get(_possibleAcceptations[i], INITIAL_SCORE);
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

        _acceptation = _possibleAcceptations[min];
    }

    private void updateTextFields() {
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();

        final int fieldCount = _fields.length;
        for (int i = 0; i < fieldCount; i++) {
            final String text = !_fields[i].isAnswer()? readFieldText(db, i) : "?";
            _fieldTextViews[i].setText(text);
        }

        _scoreTextView.setText("" + _goodAnswerCount + " - " + _badAnswerCount);
    }

    private void toggleAnswerVisibility() {
        final Button revealAnswerButton = findViewById(R.id.revealAnswerButton);
        final LinearLayout rateButtonBar = findViewById(R.id.rateButtonBar);

        if (!_isAnswerVisible) {
            SQLiteDatabase db = null;
            for (int i = 0; i < _fields.length; i++) {
                if (_fields[i].isAnswer()) {
                    if (db == null) {
                        db = DbManager.getInstance().getReadableDatabase();
                    }

                    _fieldTextViews[i].setText(readFieldText(db, i));
                }
            }

            revealAnswerButton.setVisibility(View.GONE);
            rateButtonBar.setVisibility(View.VISIBLE);
            _isAnswerVisible = true;
        }
        else {
            for (int i = 0; i < _fields.length; i++) {
                if (_fields[i].isAnswer()) {
                    _fieldTextViews[i].setText("?");
                }
            }

            revealAnswerButton.setVisibility(View.VISIBLE);
            rateButtonBar.setVisibility(View.GONE);
            _isAnswerVisible = false;
        }
    }

    private final View.OnClickListener mFieldClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (_isAnswerVisible) {
                AcceptationDetailsActivity.open(QuestionActivity.this, _acceptation, _acceptation);
            }
        }
    };

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
            selectAcceptation();
        }

        final LinearLayout fieldsPanel = findViewById(R.id.fieldsPanel);
        final LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < _fields.length; i++) {
            inflater.inflate(R.layout.question_field, fieldsPanel, true);
            _fieldTextViews[i] = (TextView) fieldsPanel.getChildAt(fieldsPanel.getChildCount() - 1);
            if (!_fields[i].isAnswer()) {
                _fieldTextViews[i].setOnClickListener(mFieldClickListener);
            }
        }
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
        final KnowledgeTable table = Tables.knowledge;
        final Cursor cursor = db.rawQuery("SELECT " +
                        table.columns().get(table.getAcceptationColumnIndex()).name() + ',' +
                        table.columns().get(table.getScoreColumnIndex()).name() +
                        " FROM " + table.name() + " WHERE " +
                        table.columns().get(table.getQuizDefinitionColumnIndex()).name() + "=?",
                new String[] {Integer.toString(_quizId)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    _possibleAcceptations = new int[cursor.getCount()];
                    int index = 0;
                    do {
                        final int acceptation = cursor.getInt(0);
                        final int score = cursor.getInt(1);

                        _possibleAcceptations[index++] = acceptation;
                        if (score != NO_SCORE) {
                            _knowledge.put(acceptation, score);
                        }
                    } while (cursor.moveToNext());

                    if (index != _possibleAcceptations.length) {
                        throw new AssertionError();
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        if (_possibleAcceptations == null) {
            throw new AssertionError();
        }
    }

    private void updateKnowledge(SQLiteDatabase db, int score) {
        final KnowledgeTable table = Tables.knowledge;
        ContentValues cv = new ContentValues();
        cv.put(table.columns().get(table.getScoreColumnIndex()).name(), score);
        final String whereClause = table.columns().get(table.getQuizDefinitionColumnIndex()).name() +
                "=? AND " + table.columns().get(table.getAcceptationColumnIndex()).name() + "=?";
        db.update(table.name(), cv, whereClause,
                new String[] { Integer.toString(_quizId), Integer.toString(_acceptation)});
    }

    private void registerGoodAnswer() {
        final SQLiteDatabase db = DbManager.getInstance().getWritableDatabase();
        final int foundScore = _knowledge.get(_acceptation, MIN_ALLOWED_SCORE - 1);
        if (foundScore < MIN_ALLOWED_SCORE) {
            final int newScore = INITIAL_SCORE + SCORE_INCREMENT;
            _knowledge.put(_acceptation, newScore);
            updateKnowledge(db, newScore);
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
            updateKnowledge(db, newScore);
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
                    selectAcceptation();
                    updateTextFields();
                    toggleAnswerVisibility();
                    break;

                case R.id.badAnswerButton:
                    registerBadAnswer();
                    selectAcceptation();
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
                final Intent intent = new Intent();
                intent.putExtra(ReturnKeys.GOOD_ANSWER_COUNT, _goodAnswerCount);
                intent.putExtra(ReturnKeys.BAD_ANSWER_COUNT, _badAnswerCount);
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
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
