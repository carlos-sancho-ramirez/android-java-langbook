package sword.langbook3.android;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import sword.langbook3.android.DbManager.QuestionField;

import static sword.langbook3.android.DbManager.findQuestionFieldSet;
import static sword.langbook3.android.DbManager.findQuizDefinition;
import static sword.langbook3.android.DbManager.idColumnName;
import static sword.langbook3.android.DbManager.insertQuestionFieldSet;
import static sword.langbook3.android.DbManager.insertQuizDefinition;

public class QuizEditorActivity extends Activity implements View.OnClickListener {

    private static final class BundleKeys {
        static final String BUNCH = "b";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int bunch) {
        Intent intent = new Intent(context, QuizEditorActivity.class);
        intent.putExtra(BundleKeys.BUNCH, bunch);
        context.startActivity(intent);
    }

    private static class FieldTypeAdapter extends BaseAdapter {

        private final String[] _entries;
        private LayoutInflater _inflater;

        FieldTypeAdapter(String[] entries) {
            _entries = entries;
        }

        @Override
        public int getCount() {
            return _entries.length;
        }

        @Override
        public String getItem(int position) {
            return _entries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                if (_inflater == null) {
                    _inflater = LayoutInflater.from(parent.getContext());
                }

                view = _inflater.inflate(R.layout.quiz_type_item, parent, false);
            }
            else {
                view = convertView;
            }

            final TextView textView = view.findViewById(R.id.itemTextView);
            textView.setText(_entries[position]);

            return view;
        }
    }

    private static final class AdapterItem {

        final int id;
        final String name;

        AdapterItem(int id, String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }

            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof AdapterItem)) {
                return false;
            }

            AdapterItem that = (AdapterItem) other;
            return id == that.id && name.equals(that.name);
        }
    }

    private static class AlphabetAdapter extends BaseAdapter {

        private final AdapterItem[] _entries;
        private LayoutInflater _inflater;

        AlphabetAdapter(AdapterItem[] entries) {
            _entries = entries;
        }

        @Override
        public int getCount() {
            return _entries.length;
        }

        @Override
        public AdapterItem getItem(int position) {
            return _entries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                if (_inflater == null) {
                    _inflater = LayoutInflater.from(parent.getContext());
                }

                view = _inflater.inflate(R.layout.quiz_type_item, parent, false);
            }
            else {
                view = convertView;
            }

            final TextView textView = view.findViewById(R.id.itemTextView);
            textView.setText(_entries[position].name);

            return view;
        }
    }

    static SparseArray<String> readAllAlphabets(SQLiteDatabase db) {
        final DbManager.AlphabetsTable alphabets = DbManager.Tables.alphabets;
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + idColumnName +
                        ",J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + alphabets.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + idColumnName + "=J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " ORDER BY J0." + idColumnName, null);

        final SparseArray<String> result = new SparseArray<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int alphabet = cursor.getInt(0);
                    int textAlphabet = cursor.getInt(1);
                    String text = cursor.getString(2);

                    while (cursor.moveToNext()) {
                        if (alphabet == cursor.getInt(0)) {
                            if (textAlphabet != preferredAlphabet && cursor.getInt(1) == preferredAlphabet) {
                                textAlphabet = preferredAlphabet;
                                text = cursor.getString(2);
                            }
                        }
                        else {
                            result.put(alphabet, text);

                            alphabet = cursor.getInt(0);
                            textAlphabet = cursor.getInt(1);
                            text = cursor.getString(2);
                        }
                    }

                    result.put(alphabet, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private SparseArray<String> readAllRules(SQLiteDatabase db) {
        final DbManager.AgentsTable agents = DbManager.Tables.agents;
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.RuledConceptsTable ruledConcepts = DbManager.Tables.ruledConcepts;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J1." + agents.getColumnName(agents.getRuleColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + ruledConcepts.getName() + " AS J0" +
                        " JOIN " + agents.getName() + " AS J1 ON J0." + ruledConcepts.getColumnName(ruledConcepts.getAgentColumnIndex()) + "=J1." + idColumnName +
                        " JOIN " + acceptations.getName() + " AS J2 ON J1." + agents.getColumnName(agents.getRuleColumnIndex()) + "=J2." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " ORDER BY J1." + agents.getColumnName(agents.getRuleColumnIndex()), null);

        final SparseArray<String> result = new SparseArray<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int rule = cursor.getInt(0);
                    int textAlphabet = cursor.getInt(1);
                    String text = cursor.getString(2);

                    while (cursor.moveToNext()) {
                        if (rule == cursor.getInt(0)) {
                            if (textAlphabet != preferredAlphabet && cursor.getInt(1) == preferredAlphabet) {
                                textAlphabet = preferredAlphabet;
                                text = cursor.getString(2);
                            }
                        }
                        else {
                            result.put(rule, text);

                            rule = cursor.getInt(0);
                            textAlphabet = cursor.getInt(1);
                            text = cursor.getString(2);
                        }
                    }

                    result.put(rule, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    static final class FieldTypes {
        static final int sameAcceptation = 1;
        static final int sameConcept = 2;
        static final int appliedRule = 3;
    }

    private static final class FieldState {
        int type;
        int alphabet;
        int rule;
    }

    private final ArrayList<FieldState> _questionFields = new ArrayList<>(1);
    private final ArrayList<FieldState> _answerFields = new ArrayList<>(1);
    private int _bunch;

    private AdapterItem[] _alphabetItems;
    private AdapterItem[] _ruleItems;

    private final class FieldListener implements Spinner.OnItemSelectedListener, View.OnClickListener {

        final FieldState fieldState;
        final Spinner ruleSpinner;

        FieldListener(FieldState fieldState, Spinner ruleSpinner) {
            this.fieldState = fieldState;
            this.ruleSpinner = ruleSpinner;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            switch (adapterView.getId()) {
                case R.id.fieldType:
                    fieldState.type = position;
                    final int visibility;
                    if (position == FieldTypes.appliedRule) {
                        if (fieldState.rule == 0) {
                            final int pos = ruleSpinner.getSelectedItemPosition();
                            if (_ruleItems != null && pos >= 0 && pos < _ruleItems.length) {
                                fieldState.rule = _ruleItems[pos].id;
                            }
                        }
                        visibility = View.VISIBLE;
                    }
                    else {
                        visibility = View.GONE;
                    }
                    ruleSpinner.setVisibility(visibility);
                    break;

                case R.id.fieldAlphabet:
                    fieldState.alphabet = ((AlphabetAdapter) adapterView.getAdapter()).getItem(position).id;
                    break;

                case R.id.fieldRule:
                    fieldState.rule = ((AlphabetAdapter) adapterView.getAdapter()).getItem(position).id;
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Nothing to be done
        }

        @Override
        public void onClick(View view) {
            removeField(fieldState);
        }
    }

    private void setUpFieldViews(View fieldViewGroup, FieldState fieldState) {
        final Spinner ruleSpinner = fieldViewGroup.findViewById(R.id.fieldRule);
        final FieldListener listener = new FieldListener(fieldState, ruleSpinner);

        final Spinner typeSpinner = fieldViewGroup.findViewById(R.id.fieldType);
        final String[] typeEntries = new String[] {
                getString(R.string.questionTypeNoValue),
                getString(R.string.questionTypeSameAcceptation),
                getString(R.string.questionTypeSameConcept),
                getString(R.string.questionTypeAppliedRule)
        };

        typeSpinner.setAdapter(new FieldTypeAdapter(typeEntries));
        typeSpinner.setOnItemSelectedListener(listener);

        final Spinner alphabetSpinner = fieldViewGroup.findViewById(R.id.fieldAlphabet);
        alphabetSpinner.setAdapter(new AlphabetAdapter(_alphabetItems));
        alphabetSpinner.setOnItemSelectedListener(listener);

        ruleSpinner.setAdapter(new AlphabetAdapter(_ruleItems));
        ruleSpinner.setOnItemSelectedListener(listener);

        fieldViewGroup.findViewById(R.id.removeFieldButton).setOnClickListener(listener);
    }

    private void addField(ArrayList<FieldState> list, int viewList) {
        final FieldState fieldState = new FieldState();
        list.add(fieldState);

        final ViewGroup viewGroup = findViewById(viewList);
        getLayoutInflater().inflate(R.layout.quiz_editor_field_entry, viewGroup, true);

        final View fieldViewGroup = viewGroup.getChildAt(viewGroup.getChildCount() - 1);
        setUpFieldViews(fieldViewGroup, fieldState);

        final int fieldCount = viewGroup.getChildCount();
        for (int i = 0; i < fieldCount; i++) {
            viewGroup.getChildAt(i).findViewById(R.id.removeFieldButton).setEnabled(true);
        }
    }

    private boolean removeFieldInList(FieldState field, ArrayList<FieldState> fields, int listResId) {
        int index = fields.indexOf(field);
        if (index >= 0) {
            fields.remove(index);
            final ViewGroup list = findViewById(listResId);
            list.removeViewAt(index);

            if (fields.size() == 1) {
                list.getChildAt(0).findViewById(R.id.removeFieldButton).setEnabled(false);
            }

            return true;
        }

        return false;
    }

    private void removeField(FieldState field) {
        if (!removeFieldInList(field, _questionFields, R.id.questionList)) {
            removeFieldInList(field, _answerFields, R.id.answerList);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_editor_activity);

        _bunch = getIntent().getIntExtra(BundleKeys.BUNCH, 0);
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();

        if (_bunch != 0) {
            final String bunchText = AcceptationDetailsActivity.readConceptText(db, _bunch);
            final TextView bunchField = findViewById(R.id.bunch);
            bunchField.setText(bunchText);
        }

        SparseArray<String> allAlphabets = readAllAlphabets(db);
        final int alphabetCount = allAlphabets.size();
        _alphabetItems = new AdapterItem[alphabetCount];
        for (int i = 0; i < alphabetCount; i++) {
            _alphabetItems[i] = new AdapterItem(allAlphabets.keyAt(i), allAlphabets.valueAt(i));
        }

        SparseArray<String> allRules = readAllRules(db);
        final int ruleCount = allRules.size();
        _ruleItems = new AdapterItem[ruleCount];
        for (int i = 0; i < ruleCount; i++) {
            _ruleItems[i] = new AdapterItem(allRules.keyAt(i), allRules.valueAt(i));
        }

        _questionFields.add(new FieldState());
        _answerFields.add(new FieldState());

        final ViewGroup questionViewGroup = findViewById(R.id.questionList);
        setUpFieldViews(questionViewGroup.getChildAt(0), _questionFields.get(0));

        final ViewGroup answerViewGroup = findViewById(R.id.answerList);
        setUpFieldViews(answerViewGroup.getChildAt(0), _answerFields.get(0));

        findViewById(R.id.addQuestionButton).setOnClickListener(this);
        findViewById(R.id.addAnswerButton).setOnClickListener(this);
        findViewById(R.id.startButton).setOnClickListener(this);
    }

    private Set<Integer> readAllAcceptationsInBunch(SQLiteDatabase db, int alphabet) {
        final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;
        final Cursor cursor = db.rawQuery("SELECT " + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) +
                " FROM " + bunchAcceptations.getName() + " AS J0" +
                " JOIN " + strings.getName() + " AS J1 ON J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) + "=J1." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=?" +
                " AND J1." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) + "=?",
                new String[]{Integer.toString(_bunch), Integer.toString(alphabet)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    Set<Integer> result = new HashSet<>();
                    do {
                        result.add(cursor.getInt(0));
                    } while (cursor.moveToNext());
                    return result;
                }
            }
            finally {
                cursor.close();
            }
        }

        return new HashSet<>();
    }

    private Set<Integer> readAllPossibleSynonymOrTranslationAcceptationsInBunch(SQLiteDatabase db, int alphabet) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
        final String conceptField = acceptations.getColumnName(acceptations.getConceptColumnIndex());
        final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
        final String accField = bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex());

        final Cursor cursor = db.rawQuery("SELECT J1." + idColumnName +
                        " FROM " + bunchAcceptations.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + accField + "=J1." + idColumnName +
                        " JOIN " + acceptations.getName() + " AS J2 ON J1." + conceptField + "=J2." + conceptField +
                        " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + dynAccField +
                        " WHERE J0." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=?" +
                        " AND J3." + alphabetField + "=?" +
                        " AND J1." + idColumnName + "!=J2." + idColumnName,
                new String[]{Integer.toString(_bunch), Integer.toString(alphabet)}
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    Set<Integer> result = new HashSet<>();
                    do {
                        result.add(cursor.getInt(0));
                    } while (cursor.moveToNext());
                    return result;
                }
            }
            finally {
                cursor.close();
            }
        }

        return new HashSet<>();
    }

    private Set<Integer> readAllRulableAcceptationsInBunch(SQLiteDatabase db, int alphabet, int rule) {
        final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;
        final DbManager.RuledAcceptationsTable ruledAcceptations = DbManager.Tables.ruledAcceptations;
        final DbManager.AgentsTable agents = DbManager.Tables.agents;

        final String alphabetField = strings.getColumnName(strings.getStringAlphabetColumnIndex());
        final String dynAccField = strings.getColumnName(strings.getDynamicAcceptationColumnIndex());
        final Cursor cursor = db.rawQuery("SELECT J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) +
                        " FROM " + bunchAcceptations.getName() + " AS J0" +
                        " JOIN " + ruledAcceptations.getName() + " AS J1 ON J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) + "=J1." + ruledAcceptations.getColumnName(ruledAcceptations.getAcceptationColumnIndex()) +
                        " JOIN " + agents.getName() + " AS J2 ON J1." + ruledAcceptations.getColumnName(ruledAcceptations.getAgentColumnIndex()) + "=J2." + idColumnName +
                        " JOIN " + strings.getName() + " AS J3 ON J1." + idColumnName + "=J3." + dynAccField +
                        " WHERE J0." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=?" +
                        " AND J3." + alphabetField + "=?" +
                        " AND J2." + agents.getColumnName(agents.getRuleColumnIndex()) + "=?",
                new String[]{Integer.toString(_bunch), Integer.toString(alphabet), Integer.toString(rule)}
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
                    Set<Integer> result = new HashSet<>(idCount);
                    for (int i = 0; i < idCount; i++) {
                        result.add(ids.keyAt(i));
                    }

                    return result;
                }
            }
            finally {
                cursor.close();
            }
        }

        return new HashSet<>();
    }

    private Set<Integer> readAllPossibleAcceptationForField(SQLiteDatabase db, FieldState field) {
        switch (field.type) {
            case FieldTypes.sameAcceptation:
                return readAllAcceptationsInBunch(db, field.alphabet);

            case FieldTypes.sameConcept:
                return readAllPossibleSynonymOrTranslationAcceptationsInBunch(db, field.alphabet);

            case FieldTypes.appliedRule:
                return readAllRulableAcceptationsInBunch(db, field.alphabet, field.rule);

            default:
                throw new AssertionError();
        }
    }

    private Set<Integer> readAllPossibleAcceptations(SQLiteDatabase db) {
        final Iterator<FieldState> it = _questionFields.iterator();
        final Set<Integer> result = readAllPossibleAcceptationForField(db, it.next());

        while (it.hasNext()) {
            result.retainAll(readAllPossibleAcceptationForField(db, it.next()));
        }

        for (FieldState field : _answerFields) {
            result.retainAll(readAllPossibleAcceptationForField(db, field));
        }

        return result;
    }

    private void insertAllPossibilities(SQLiteDatabase db, int quizId, Set<Integer> acceptations) {
        final DbManager.KnowledgeTable table = DbManager.Tables.knowledge;
        final String quizDefField = table.getColumnName(table.getQuizDefinitionColumnIndex());
        final String accField = table.getColumnName(table.getAcceptationColumnIndex());
        final String scoreField = table.getColumnName(table.getScoreColumnIndex());

        final ContentValues cv = new ContentValues();
        for (int acceptation : acceptations) {
            cv.clear();
            cv.put(quizDefField, quizId);
            cv.put(accField, acceptation);
            cv.put(scoreField, QuestionActivity.NO_SCORE);

            db.insert(table.getName(), null, cv);
        }
    }

    private void startQuiz() {
        final SQLiteDatabase db = DbManager.getInstance().getWritableDatabase();

        final List<QuestionField> fields = new ArrayList<>();
        for (FieldState state : _questionFields) {
            fields.add(new QuestionField(state.alphabet, state.rule, state.type - 1));
        }

        for (FieldState state : _answerFields) {
            fields.add(new QuestionField(state.alphabet, state.rule, DbManager.QuestionFieldFlags.IS_ANSWER | (state.type - 1)));
        }

        final Integer existingSetId = findQuestionFieldSet(db, fields);
        final Integer existingQuizId = (existingSetId != null)? findQuizDefinition(db, _bunch, existingSetId) : null;
        Integer quizId = null;
        if (existingQuizId == null) {
            final Set<Integer> acceptations = readAllPossibleAcceptations(db);
            if (acceptations.size() == 0) {
                Toast.makeText(this, R.string.noValidQuestions, Toast.LENGTH_SHORT).show();
            }
            else {
                final int setId = (existingSetId != null) ? existingSetId : insertQuestionFieldSet(db, fields);
                quizId = insertQuizDefinition(db, _bunch, setId);
                insertAllPossibilities(db, quizId, acceptations);
            }
        }
        else {
            quizId = existingQuizId;
        }

        if (quizId != null) {
            QuizResultActivity.open(this, quizId);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startButton:
                startQuiz();
                break;

            case R.id.addQuestionButton:
                addField(_questionFields, R.id.questionList);
                break;

            case R.id.addAnswerButton:
                addField(_answerFields, R.id.answerList);
                break;
        }
    }
}
