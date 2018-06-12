package sword.langbook3.android;

import android.app.Activity;
import android.content.ContentValues;
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
import java.util.Iterator;
import java.util.List;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.IntSet;
import sword.langbook3.android.DbManager.QuestionField;
import sword.langbook3.android.LangbookDbSchema.AcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.AgentsTable;
import sword.langbook3.android.LangbookDbSchema.BunchAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.KnowledgeTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.RuledAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.Database;

import static sword.langbook3.android.DbManager.findQuestionFieldSet;
import static sword.langbook3.android.DbManager.findQuizDefinition;
import static sword.langbook3.android.DbManager.insertQuestionFieldSet;
import static sword.langbook3.android.DbManager.insertQuizDefinition;
import static sword.langbook3.android.LangbookReadableDatabase.readAllAlphabets;
import static sword.langbook3.android.LangbookReadableDatabase.readAllRules;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;
import static sword.langbook3.android.QuizSelectorActivity.NO_BUNCH;
import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public final class QuizEditorActivity extends Activity implements View.OnClickListener {

    private interface ArgKeys {
        String BUNCH = BundleKeys.BUNCH;
    }

    interface ResultKeys {
        String QUIZ = BundleKeys.QUIZ;
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Activity activity, int requestCode, int bunch) {
        Intent intent = new Intent(activity, QuizEditorActivity.class);
        intent.putExtra(ArgKeys.BUNCH, bunch);
        activity.startActivityForResult(intent, requestCode);
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

        _bunch = getIntent().getIntExtra(ArgKeys.BUNCH, NO_BUNCH);
        final DbManager manager = DbManager.getInstance();
        final Database db = manager.getDatabase();

        if (_bunch != NO_BUNCH) {
            final String bunchText = readConceptText(DbManager.getInstance().getDatabase(), _bunch, preferredAlphabet);
            final TextView bunchField = findViewById(R.id.bunch);
            bunchField.setText(bunchText);
        }

        ImmutableIntKeyMap<String> allAlphabets = readAllAlphabets(db, preferredAlphabet);
        final int alphabetCount = allAlphabets.size();
        _alphabetItems = new AdapterItem[alphabetCount];
        for (int i = 0; i < alphabetCount; i++) {
            _alphabetItems[i] = new AdapterItem(allAlphabets.keyAt(i), allAlphabets.valueAt(i));
        }

        ImmutableIntKeyMap<String> allRules = readAllRules(db, preferredAlphabet);
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

    private ImmutableIntSet readAllAcceptations(SQLiteDatabase db, int alphabet) {
        final StringQueriesTable strings = Tables.stringQueries;
        final Cursor cursor = db.rawQuery("SELECT " + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() +
                        " FROM " + strings.name() +
                        " WHERE " + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() + "=?" +
                        " AND " + strings.columns().get(strings.getMainAcceptationColumnIndex()).name() + '=' + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name(),
                new String[]{Integer.toString(alphabet)});

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        builder.add(cursor.getInt(0));
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return builder.build();
    }

    private ImmutableIntSet readAllAcceptationsInBunch(SQLiteDatabase db, int alphabet) {
        final BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final StringQueriesTable strings = Tables.stringQueries;
        final Cursor cursor = db.rawQuery("SELECT " + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name() +
                " FROM " + bunchAcceptations.name() + " AS J0" +
                " JOIN " + strings.name() + " AS J1 ON J0." + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name() + "=J1." + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() +
                " WHERE J0." + bunchAcceptations.columns().get(bunchAcceptations.getBunchColumnIndex()).name() + "=?" +
                " AND J1." + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() + "=?",
                new String[]{Integer.toString(_bunch), Integer.toString(alphabet)});

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        builder.add(cursor.getInt(0));
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return builder.build();
    }

    private ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptations(SQLiteDatabase db, int alphabet) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final StringQueriesTable strings = Tables.stringQueries;

        final String alphabetField = strings.columns().get(strings.getStringAlphabetColumnIndex()).name();
        final String conceptField = acceptations.columns().get(acceptations.getConceptColumnIndex()).name();
        final String dynAccField = strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name();

        final Cursor cursor = db.rawQuery("SELECT J0." + idColumnName +
                        " FROM " + acceptations.name() + " AS J0" +
                        " JOIN " + acceptations.name() + " AS J1 ON J0." + conceptField + "=J1." + conceptField +
                        " JOIN " + strings.name() + " AS J2 ON J1." + idColumnName + "=J2." + dynAccField +
                        " WHERE J2." + alphabetField + "=?" +
                        " AND J0." + idColumnName + "!=J1." + idColumnName,
                new String[]{Integer.toString(alphabet)}
        );

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        builder.add(cursor.getInt(0));
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return builder.build();
    }

    private ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptationsInBunch(SQLiteDatabase db, int alphabet) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final StringQueriesTable strings = Tables.stringQueries;

        final String alphabetField = strings.columns().get(strings.getStringAlphabetColumnIndex()).name();
        final String conceptField = acceptations.columns().get(acceptations.getConceptColumnIndex()).name();
        final String dynAccField = strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name();
        final String accField = bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name();

        final Cursor cursor = db.rawQuery("SELECT J1." + idColumnName +
                        " FROM " + bunchAcceptations.name() + " AS J0" +
                        " JOIN " + acceptations.name() + " AS J1 ON J0." + accField + "=J1." + idColumnName +
                        " JOIN " + acceptations.name() + " AS J2 ON J1." + conceptField + "=J2." + conceptField +
                        " JOIN " + strings.name() + " AS J3 ON J2." + idColumnName + "=J3." + dynAccField +
                        " WHERE J0." + bunchAcceptations.columns().get(bunchAcceptations.getBunchColumnIndex()).name() + "=?" +
                        " AND J3." + alphabetField + "=?" +
                        " AND J1." + idColumnName + "!=J2." + idColumnName,
                new String[]{Integer.toString(_bunch), Integer.toString(alphabet)}
        );

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        builder.add(cursor.getInt(0));
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return builder.build();
    }

    private ImmutableIntSet readAllRulableAcceptations(SQLiteDatabase db, int alphabet, int rule) {
        final StringQueriesTable strings = Tables.stringQueries;
        final RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final AgentsTable agents = Tables.agents;

        final String alphabetField = strings.columns().get(strings.getStringAlphabetColumnIndex()).name();
        final String dynAccField = strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name();
        final Cursor cursor = db.rawQuery("SELECT J0." + ruledAcceptations.columns().get(ruledAcceptations.getAcceptationColumnIndex()).name() +
                        " FROM " + ruledAcceptations.name() + " AS J0" +
                        " JOIN " + agents.name() + " AS J1 ON J0." + ruledAcceptations.columns().get(ruledAcceptations.getAgentColumnIndex()).name() + "=J1." + idColumnName +
                        " JOIN " + strings.name() + " AS J2 ON J0." + idColumnName + "=J2." + dynAccField +
                        " WHERE J2." + alphabetField + "=?" +
                        " AND J1." + agents.columns().get(agents.getRuleColumnIndex()).name() + "=?",
                new String[]{Integer.toString(alphabet), Integer.toString(rule)}
        );

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    SparseArray<Object> ids = new SparseArray<>();
                    Object dummy = new Object();
                    do {
                        ids.put(cursor.getInt(0), dummy);
                    } while (cursor.moveToNext());

                    final int idCount = ids.size();
                    for (int i = 0; i < idCount; i++) {
                        builder.add(ids.keyAt(i));
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        return builder.build();
    }

    private ImmutableIntSet readAllRulableAcceptationsInBunch(SQLiteDatabase db, int alphabet, int rule) {
        final BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final StringQueriesTable strings = Tables.stringQueries;
        final RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final AgentsTable agents = Tables.agents;

        final String alphabetField = strings.columns().get(strings.getStringAlphabetColumnIndex()).name();
        final String dynAccField = strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name();
        final Cursor cursor = db.rawQuery("SELECT J0." + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name() +
                        " FROM " + bunchAcceptations.name() + " AS J0" +
                        " JOIN " + ruledAcceptations.name() + " AS J1 ON J0." + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name() + "=J1." + ruledAcceptations.columns().get(ruledAcceptations.getAcceptationColumnIndex()).name() +
                        " JOIN " + agents.name() + " AS J2 ON J1." + ruledAcceptations.columns().get(ruledAcceptations.getAgentColumnIndex()).name() + "=J2." + idColumnName +
                        " JOIN " + strings.name() + " AS J3 ON J1." + idColumnName + "=J3." + dynAccField +
                        " WHERE J0." + bunchAcceptations.columns().get(bunchAcceptations.getBunchColumnIndex()).name() + "=?" +
                        " AND J3." + alphabetField + "=?" +
                        " AND J2." + agents.columns().get(agents.getRuleColumnIndex()).name() + "=?",
                new String[]{Integer.toString(_bunch), Integer.toString(alphabet), Integer.toString(rule)}
        );

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    SparseArray<Object> ids = new SparseArray<>();
                    Object dummy = new Object();
                    do {
                        ids.put(cursor.getInt(0), dummy);
                    } while (cursor.moveToNext());

                    final int idCount = ids.size();
                    for (int i = 0; i < idCount; i++) {
                        builder.add(ids.keyAt(i));
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        return builder.build();
    }

    private ImmutableIntSet readAllPossibleAcceptationForField(SQLiteDatabase db, FieldState field) {
        switch (field.type) {
            case FieldTypes.sameAcceptation:
                return (_bunch == NO_BUNCH)? readAllAcceptations(db, field.alphabet) : readAllAcceptationsInBunch(db, field.alphabet);

            case FieldTypes.sameConcept:
                return (_bunch == NO_BUNCH)? readAllPossibleSynonymOrTranslationAcceptations(db, field.alphabet) :
                        readAllPossibleSynonymOrTranslationAcceptationsInBunch(db, field.alphabet);

            case FieldTypes.appliedRule:
                return (_bunch == NO_BUNCH)? readAllRulableAcceptations(db, field.alphabet, field.rule) :
                        readAllRulableAcceptationsInBunch(db, field.alphabet, field.rule);

            default:
                throw new AssertionError();
        }
    }

    private ImmutableIntSet readAllPossibleAcceptations(SQLiteDatabase db) {
        final Iterator<FieldState> it = _questionFields.iterator();
        ImmutableIntSet result = readAllPossibleAcceptationForField(db, it.next());

        while (it.hasNext()) {
            final ImmutableIntSet set = readAllPossibleAcceptationForField(db, it.next());
            result = result.filter(set::contains);
        }

        for (FieldState field : _answerFields) {
            final ImmutableIntSet set = readAllPossibleAcceptationForField(db, field);
            result = result.filter(set::contains);
        }

        return result;
    }

    private void insertAllPossibilities(SQLiteDatabase db, int quizId, IntSet acceptations) {
        final KnowledgeTable table = Tables.knowledge;
        final String quizDefField = table.columns().get(table.getQuizDefinitionColumnIndex()).name();
        final String accField = table.columns().get(table.getAcceptationColumnIndex()).name();
        final String scoreField = table.columns().get(table.getScoreColumnIndex()).name();

        final ContentValues cv = new ContentValues();
        for (int acceptation : acceptations) {
            cv.clear();
            cv.put(quizDefField, quizId);
            cv.put(accField, acceptation);
            cv.put(scoreField, QuestionActivity.NO_SCORE);

            db.insert(table.name(), null, cv);
        }
    }

    private void startQuiz() {
        final SQLiteDatabase db = DbManager.getInstance().getWritableDatabase();

        final List<QuestionField> fields = new ArrayList<>();
        for (FieldState state : _questionFields) {
            fields.add(new QuestionField(state.alphabet, state.rule, state.type - 1));
        }

        for (FieldState state : _answerFields) {
            fields.add(new QuestionField(state.alphabet, state.rule, QuestionFieldFlags.IS_ANSWER | (state.type - 1)));
        }

        final Integer existingSetId = findQuestionFieldSet(db, fields);
        final Integer existingQuizId = (existingSetId != null)? findQuizDefinition(db, _bunch, existingSetId) : null;
        Integer quizId = null;
        if (existingQuizId == null) {
            final ImmutableIntSet acceptations = readAllPossibleAcceptations(db);
            if (acceptations.isEmpty()) {
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
            final Intent intent = new Intent();
            intent.putExtra(ResultKeys.QUIZ, quizId);
            setResult(RESULT_OK, intent);
            finish();
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
