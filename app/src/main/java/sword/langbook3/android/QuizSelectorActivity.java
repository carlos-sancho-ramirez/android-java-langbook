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

import sword.langbook3.android.DbManager.QuestionField;
import sword.langbook3.android.LangbookDbSchema.KnowledgeTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.Tables;

import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public final class QuizSelectorActivity extends Activity implements ListView.OnItemClickListener {

    private static final class BundleKeys {
        static final String BUNCH = "b";
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
        intent.putExtra(BundleKeys.BUNCH, bunch);
        context.startActivity(intent);
    }

    private int _bunch;
    private SparseArray<String> _ruleTexts;

    static final class Progress {
        // Due to int[] can be modified, this class may become inconsistent if value is changed.
        // TODO: Make this class completely immutable
        private final int[] amountPerScore;
        private final int totalAnsweredQuestions;
        private final int numberOfQuestions;

        Progress(int[] amountPerScore, int numberOfQuestions) {
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

        int[] getAmountPerScore() {
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

        final Cursor cursor = db.rawQuery("SELECT " + knowledge.getColumnName(knowledge.getScoreColumnIndex()) + " FROM " + knowledge.name() + " WHERE " + knowledge.getColumnName(knowledge.getQuizDefinitionColumnIndex()) + "=?",
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

        return new Progress(progress, numberOfQuestions);
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
                ",J1." + fieldSets.getColumnName(fieldSets.getAlphabetColumnIndex()) +
                ",J1." + fieldSets.getColumnName(fieldSets.getRuleColumnIndex()) +
                ",J1." + fieldSets.getColumnName(fieldSets.getFlagsColumnIndex()) +
                " FROM " + quizzes.name() + " AS J0" +
                " JOIN " + fieldSets.name() + " AS J1 ON J0." + quizzes.getColumnName(quizzes.getQuestionFieldsColumnIndex()) + "=J1." + fieldSets.getColumnName(fieldSets.getSetIdColumnIndex()) +
                " WHERE J0." + quizzes.getColumnName(quizzes.getBunchColumnIndex()) + "=?",
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

        _bunch = getIntent().getIntExtra(BundleKeys.BUNCH, 0);

        final QuizSelectorAdapter.Item[] items = composeAdapterItems(
                DbManager.getInstance().getReadableDatabase(), _bunch);

        if (items.length == 0) {
            QuizEditorActivity.open(this, _bunch);
        }
        else {
            ListView listView = findViewById(R.id.listView);
            listView.setAdapter(new QuizSelectorAdapter(items));
            listView.setOnItemClickListener(this);
        }
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
                QuizEditorActivity.open(this, _bunch);
                return true;
        }

        return false;
    }
}
