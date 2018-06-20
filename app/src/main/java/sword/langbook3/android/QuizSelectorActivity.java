package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.IntToIntFunction;
import sword.langbook3.android.LangbookDbSchema.KnowledgeTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.LangbookReadableDatabase.QuestionFieldDetails;
import sword.langbook3.android.db.Database;

import static sword.langbook3.android.LangbookReadableDatabase.readAllAlphabets;
import static sword.langbook3.android.LangbookReadableDatabase.readAllRules;
import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public final class QuizSelectorActivity extends Activity implements ListView.OnItemClickListener, ListView.MultiChoiceModeListener, DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_EDITOR = 1;
    private static final int NO_QUIZ = 0;

    private interface ArgKeys {
        String BUNCH = BundleKeys.BUNCH;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    /**
     * Bunch identifier used within a quiz definition to denote that no bunch should be applied.
     * When this identifier is used, questions are not coming from acceptations within an specific bunch,
     * but it can be any acceptation within the database that matches the field restrictions.
     */
    public static final int NO_BUNCH = 0;

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int bunch) {
        Intent intent = new Intent(context, QuizSelectorActivity.class);
        intent.putExtra(ArgKeys.BUNCH, bunch);
        context.startActivity(intent);
    }

    private QuizSelectorActivityState _state;

    private int _bunch;
    private ImmutableIntKeyMap<String> _ruleTexts;

    private boolean _finishIfEmptyWhenStarting;
    private boolean _activityStarted;
    private ListView _listView;
    private ActionMode _listActionMode;

    static final class Progress {
        private final ImmutableIntList amountPerScore;
        private final int totalAnsweredQuestions;
        private final int numberOfQuestions;

        Progress(ImmutableIntList amountPerScore, int numberOfQuestions) {
            int answeredQuestions = 0;
            for (int amount : amountPerScore) {
                answeredQuestions += amount;
            }

            if (answeredQuestions > numberOfQuestions) {
                throw new IllegalArgumentException();
            }

            this.amountPerScore = amountPerScore;
            totalAnsweredQuestions = answeredQuestions;
            this.numberOfQuestions = numberOfQuestions;
        }

        ImmutableIntList getAmountPerScore() {
            return amountPerScore;
        }

        int getAnsweredQuestionsCount() {
            return totalAnsweredQuestions;
        }

        int getNumberOfQuestions() {
            return numberOfQuestions;
        }

        String getCompletenessString() {
            final float completeness = (float) totalAnsweredQuestions * 100 / numberOfQuestions;
            return String.format("%.1f%%", completeness);
        }

        KnowledgeDrawable getDrawable() {
            return (totalAnsweredQuestions > 0)? new KnowledgeDrawable(amountPerScore) : null;
        }
    }

    static Progress readProgress(SQLiteDatabase db, int quizId) {
        final KnowledgeTable knowledge = Tables.knowledge;

        final Cursor cursor = db.rawQuery("SELECT " + knowledge.columns().get(knowledge.getScoreColumnIndex()).name() + " FROM " + knowledge.name() + " WHERE " + knowledge.columns().get(knowledge.getQuizDefinitionColumnIndex()).name() + "=?",
                new String[] { Integer.toString(quizId)});

        int numberOfQuestions = 0;
        final int[] progress = new int[QuestionActivity.MAX_ALLOWED_SCORE - QuestionActivity.MIN_ALLOWED_SCORE + 1];
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    numberOfQuestions = cursor.getCount();
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

        final ImmutableIntList.Builder builder = new ImmutableIntList.Builder();
        for (int value : progress) {
            builder.add(value);
        }

        return new Progress(builder.build(), numberOfQuestions);
    }

    private String getRuleText(Database db, int rule) {
        if (_ruleTexts == null) {
            _ruleTexts = readAllRules(db, preferredAlphabet);
        }

        return _ruleTexts.get(rule);
    }

    private static int getTypeStringResId(QuestionFieldDetails field) {
        switch (field.getType()) {
            case QuestionFieldFlags.TYPE_SAME_ACC:
                return R.string.questionTypeSameAcceptation;

            case QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return R.string.questionTypeSameConcept;

            case QuestionFieldFlags.TYPE_APPLY_RULE:
                return R.string.questionTypeAppliedRule;
        }

        return 0;
    }

    private QuizSelectorAdapter.Item[] composeAdapterItems(Database db, int bunch) {
        final ImmutableIntKeyMap<String> allAlphabets = readAllAlphabets(db, preferredAlphabet);
        final QuizDefinitionsTable quizzes = Tables.quizDefinitions;
        final QuestionFieldSets fieldSets = Tables.questionFieldSets;
        final SQLiteDatabase sqlDb = DbManager.getInstance().getReadableDatabase();
        Cursor cursor = sqlDb.rawQuery("SELECT" +
                " J0." + idColumnName +
                ",J1." + fieldSets.columns().get(fieldSets.getAlphabetColumnIndex()).name() +
                ",J1." + fieldSets.columns().get(fieldSets.getRuleColumnIndex()).name() +
                ",J1." + fieldSets.columns().get(fieldSets.getFlagsColumnIndex()).name() +
                " FROM " + quizzes.name() + " AS J0" +
                " JOIN " + fieldSets.name() + " AS J1 ON J0." + quizzes.columns().get(quizzes.getQuestionFieldsColumnIndex()).name() + "=J1." + fieldSets.columns().get(fieldSets.getSetIdColumnIndex()).name() +
                " WHERE J0." + quizzes.columns().get(quizzes.getBunchColumnIndex()).name() + "=?",
                new String[] {Integer.toString(bunch)});

        final SparseArray<Set<QuestionFieldDetails>> resultMap = new SparseArray<>();

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final int quizId = cursor.getInt(0);
                        QuestionFieldDetails field = new QuestionFieldDetails(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3));
                        Set<QuestionFieldDetails> set = resultMap.get(quizId);
                        if (set == null) {
                            set = new HashSet<>();
                        }

                        set.add(field);
                        resultMap.put(quizId, set);
                    } while(cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        final int quizCount = resultMap.size();
        final QuizSelectorAdapter.Item[] items = new QuizSelectorAdapter.Item[quizCount];

        for (int i = 0; i < quizCount; i++) {
            final int quizId = resultMap.keyAt(i);
            final Set<QuestionFieldDetails> set = resultMap.valueAt(i);

            StringBuilder qsb = null;
            StringBuilder asb = null;
            for (QuestionFieldDetails field : set) {
                final StringBuilder sb;
                if (field.isAnswer()) {
                    if (asb == null) {
                        asb = new StringBuilder();
                    }
                    else {
                        asb.append('\n');
                    }
                    sb = asb;
                }
                else {
                    if (qsb == null) {
                        qsb = new StringBuilder();
                    }
                    else {
                        qsb.append('\n');
                    }
                    sb = qsb;
                }
                sb.append('(')
                        .append(allAlphabets.get(field.alphabet, "?")).append(", ")
                        .append(getString(getTypeStringResId(field)));

                if (field.getType() == QuestionFieldFlags.TYPE_APPLY_RULE) {
                    sb.append(", ").append(getRuleText(db, field.rule));
                }
                sb.append(')');
            }

            items[i] = new QuizSelectorAdapter.Item(quizId, qsb.toString(), asb.toString(), readProgress(sqlDb, quizId));
        }

        return items;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_selector_activity);

        _bunch = getIntent().getIntExtra(ArgKeys.BUNCH, 0);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        if (_state == null) {
            _state = new QuizSelectorActivityState();
        }

        _listView = findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        _activityStarted = true;

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(
                DbManager.getInstance().getDatabase(), _bunch);

        if (items.length == 0) {
            if (!_state.firstActionExecuted()) {
                QuizEditorActivity.open(this, REQUEST_CODE_EDITOR, _bunch);
            }
            else if (_finishIfEmptyWhenStarting) {
                finish();
            }
        }

        _listView.setAdapter(new QuizSelectorAdapter(items));
        _listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        _listView.setMultiChoiceModeListener(this);

        ImmutableIntSet selection = _state.getListSelection();
        if (selection.isEmpty()) {
            _state.setReady();
        }
        else {
            for (int position : selection) {
                _listView.setItemChecked(position, true);
            }

            if (_state.shouldDisplayDeleteDialog()) {
                showDeleteConfirmationDialog();
            }
        }

        _finishIfEmptyWhenStarting = false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int quizId = ((QuizSelectorAdapter) parent.getAdapter()).getItem(position).getQuizId();
        QuizResultActivity.open(this, quizId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.quiz_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemNewQuizDefinition:
                QuizEditorActivity.open(this, REQUEST_CODE_EDITOR, _bunch);
                return true;
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDITOR) {
            final int quizId = (data != null)? data.getIntExtra(QuizEditorActivity.ResultKeys.QUIZ, NO_QUIZ) : NO_QUIZ;
            if (resultCode == RESULT_OK && quizId != NO_QUIZ) {
                QuizResultActivity.open(this, quizId);
            }
            else if (_activityStarted) {
                if (_listView.getAdapter().isEmpty()) {
                    finish();
                }
            }
            else {
                _finishIfEmptyWhenStarting = true;
            }
        }
    }

    @Override
    protected void onStop() {
        _activityStarted = false;
        super.onStop();
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        _state.changeListSelection(position, checked);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        _listActionMode = mode;
        menu.add(getString(R.string.menuItemDelete));
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (_listActionMode != mode) {
            throw new AssertionError();
        }

        _state.setDeletingState();
        showDeleteConfirmationDialog();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (_listActionMode != null) {
            _state.clearQuizSelection();
            _listActionMode = null;
        }
    }

    private void showDeleteConfirmationDialog() {
        final String message = getString(R.string.deleteQuizConfirmationText);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteState())
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final QuizSelectorAdapter adapter = (QuizSelectorAdapter) _listView.getAdapter();
        final IntToIntFunction mapFunc = position -> adapter.getItem(position).getQuizId();
        final ImmutableIntSet quizzes = _state.getListSelection().map(mapFunc);
        _state.clearDeleteStateAndSelection();
        final ActionMode listActionMode = _listActionMode;
        _listActionMode = null;
        listActionMode.finish();

        final Database db = DbManager.getInstance().getDatabase();
        for (int quizId : quizzes) {
            LangbookDatabase.removeQuiz(db, quizId);
        }
        showFeedback(getString(R.string.deleteQuizzesFeedback));

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(
                DbManager.getInstance().getDatabase(), _bunch);

        if (items.length == 0) {
            finish();
        }

        _listView.setAdapter(new QuizSelectorAdapter(items));
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
