package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.HashSet;
import java.util.Set;

import sword.collections.ImmutableIntList;
import sword.langbook3.android.DbManager.QuestionField;
import sword.langbook3.android.LangbookDbSchema.KnowledgeTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.Tables;

import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public final class QuizSelectorActivity extends Activity implements ListView.OnItemClickListener {

    private static final int REQUEST_CODE_EDITOR = 1;
    private static final int NO_QUIZ = 0;

    private interface ArgKeys {
        String BUNCH = BundleKeys.BUNCH;
    }

    private interface SavedKeys {
        String FIRST_ACTION_EXECUTED = "fae";
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

    private int _bunch;
    private SparseArray<String> _ruleTexts;

    private boolean _finishIfEmptyWhenStarting;
    private boolean _activityStarted;
    private boolean _firstActionExecuted;
    private ListView _listView;

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

    private String getRuleText(SQLiteDatabase db, int rule) {
        if (_ruleTexts == null) {
            _ruleTexts = QuizEditorActivity.readAllRules(db);
        }

        return _ruleTexts.get(rule);
    }

    private QuizSelectorAdapter.Item[] composeAdapterItems(SQLiteDatabase db, int bunch) {
        final SparseArray<String> allAlphabets = QuizEditorActivity.readAllAlphabets(db);
        final QuizDefinitionsTable quizzes = Tables.quizDefinitions;
        final QuestionFieldSets fieldSets = Tables.questionFieldSets;
        Cursor cursor = db.rawQuery("SELECT" +
                " J0." + idColumnName +
                ",J1." + fieldSets.columns().get(fieldSets.getAlphabetColumnIndex()).name() +
                ",J1." + fieldSets.columns().get(fieldSets.getRuleColumnIndex()).name() +
                ",J1." + fieldSets.columns().get(fieldSets.getFlagsColumnIndex()).name() +
                " FROM " + quizzes.name() + " AS J0" +
                " JOIN " + fieldSets.name() + " AS J1 ON J0." + quizzes.columns().get(quizzes.getQuestionFieldsColumnIndex()).name() + "=J1." + fieldSets.columns().get(fieldSets.getSetIdColumnIndex()).name() +
                " WHERE J0." + quizzes.columns().get(quizzes.getBunchColumnIndex()).name() + "=?",
                new String[] {Integer.toString(bunch)});

        final SparseArray<Set<QuestionField>> resultMap = new SparseArray<>();

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final int quizId = cursor.getInt(0);
                        QuestionField field = new QuestionField(cursor.getInt(1), cursor.getInt(2), cursor.getInt(3));
                        Set<QuestionField> set = resultMap.get(quizId);
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
            final Set<QuestionField> set = resultMap.valueAt(i);

            StringBuilder qsb = null;
            StringBuilder asb = null;
            for (QuestionField field : set) {
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
                        .append(getString(field.getTypeStringResId()));

                if (field.getType() == QuestionFieldFlags.TYPE_APPLY_RULE) {
                    sb.append(", ").append(getRuleText(db, field.rule));
                }
                sb.append(')');
            }

            items[i] = new QuizSelectorAdapter.Item(quizId, qsb.toString(), asb.toString(), readProgress(db, quizId));
        }

        return items;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_selector_activity);

        _bunch = getIntent().getIntExtra(ArgKeys.BUNCH, 0);

        if (savedInstanceState != null) {
            _firstActionExecuted = savedInstanceState.getBoolean(SavedKeys.FIRST_ACTION_EXECUTED);
        }

        _listView = findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        _activityStarted = true;

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(
                DbManager.getInstance().getReadableDatabase(), _bunch);

        if (items.length == 0) {
            if (!_firstActionExecuted) {
                QuizEditorActivity.open(this, REQUEST_CODE_EDITOR, _bunch);
            }
            else if (_finishIfEmptyWhenStarting) {
                finish();
            }
        }

        _listView.setAdapter(new QuizSelectorAdapter(items));
        _firstActionExecuted = true;
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
        outState.putBoolean(SavedKeys.FIRST_ACTION_EXECUTED, _firstActionExecuted);
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
}
