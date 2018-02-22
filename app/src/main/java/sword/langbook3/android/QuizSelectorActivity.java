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

import static sword.langbook3.android.DbManager.idColumnName;

public final class QuizSelectorActivity extends Activity implements ListView.OnItemClickListener {

    private static final class BundleKeys {
        static final String BUNCH = "b";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int bunch) {
        Intent intent = new Intent(context, QuizSelectorActivity.class);
        intent.putExtra(BundleKeys.BUNCH, bunch);
        context.startActivity(intent);
    }

    private int _bunch;

    private QuizSelectorAdapter.Item[] composeAdapterItems(SQLiteDatabase db, int bunch) {
        final SparseArray<String> allAlphabets = QuizEditorActivity.readAllAlphabets(db);
        final DbManager.QuizDefinitionsTable quizzes = DbManager.Tables.quizDefinitions;
        final DbManager.QuestionFieldSets fieldSets = DbManager.Tables.questionFieldSets;
        Cursor cursor = db.rawQuery("SELECT" +
                " J0." + idColumnName +
                ",J1." + fieldSets.getColumnName(fieldSets.getAlphabetColumnIndex()) +
                ",J1." + fieldSets.getColumnName(fieldSets.getRuleColumnIndex()) +
                ",J1." + fieldSets.getColumnName(fieldSets.getFlagsColumnIndex()) +
                " FROM " + quizzes.getName() + " AS J0" +
                " JOIN " + fieldSets.getName() + " AS J1 ON J0." + quizzes.getColumnName(quizzes.getQuestionFieldsColumnIndex()) + "=J1." + fieldSets.getColumnName(fieldSets.getSetIdColumnIndex()) +
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

                if (field.getType() == DbManager.QuestionFieldFlags.TYPE_APPLY_RULE) {
                    sb.append(", ").append(field.rule);
                }
                sb.append(')');
            }

            items[i] = new QuizSelectorAdapter.Item(quizId, qsb.toString(), asb.toString());
        }

        return items;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_selector_activity);

        _bunch = getIntent().getIntExtra(BundleKeys.BUNCH, 0);
        if (_bunch == 0) {
            throw new AssertionError("Bunch not specified");
        }

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
