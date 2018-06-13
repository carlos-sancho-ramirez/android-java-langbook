package sword.langbook3.android;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
import sword.collections.IntSet;
import sword.langbook3.android.DbManager.QuestionField;
import sword.langbook3.android.LangbookDbSchema.KnowledgeTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbExporter;

import static sword.langbook3.android.DbManager.findQuestionFieldSet;
import static sword.langbook3.android.DbManager.findQuizDefinition;
import static sword.langbook3.android.DbManager.insertQuestionFieldSet;
import static sword.langbook3.android.DbManager.insertQuizDefinition;
import static sword.langbook3.android.LangbookReadableDatabase.readAllAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.readAllAcceptationsInBunch;
import static sword.langbook3.android.LangbookReadableDatabase.readAllAlphabets;
import static sword.langbook3.android.LangbookReadableDatabase.readAllPossibleSynonymOrTranslationAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.readAllPossibleSynonymOrTranslationAcceptationsInBunch;
import static sword.langbook3.android.LangbookReadableDatabase.readAllRulableAcceptations;
import static sword.langbook3.android.LangbookReadableDatabase.readAllRulableAcceptationsInBunch;
import static sword.langbook3.android.LangbookReadableDatabase.readAllRules;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;
import static sword.langbook3.android.QuizSelectorActivity.NO_BUNCH;

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

    private ImmutableIntSet readAllPossibleAcceptationForField(DbExporter.Database db, FieldState field) {
        switch (field.type) {
            case FieldTypes.sameAcceptation:
                return (_bunch == NO_BUNCH)? readAllAcceptations(db, field.alphabet) : readAllAcceptationsInBunch(db, field.alphabet, _bunch);

            case FieldTypes.sameConcept:
                return (_bunch == NO_BUNCH)? readAllPossibleSynonymOrTranslationAcceptations(db, field.alphabet) :
                        readAllPossibleSynonymOrTranslationAcceptationsInBunch(db, field.alphabet, _bunch);

            case FieldTypes.appliedRule:
                return (_bunch == NO_BUNCH)? readAllRulableAcceptations(db, field.alphabet, field.rule) :
                        readAllRulableAcceptationsInBunch(db, field.alphabet, field.rule, _bunch);

            default:
                throw new AssertionError();
        }
    }

    private ImmutableIntSet readAllPossibleAcceptations(DbExporter.Database db) {
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
        final Database db = DbManager.getInstance().getDatabase();
        final SQLiteDatabase sqlDb = DbManager.getInstance().getWritableDatabase();

        final List<QuestionField> fields = new ArrayList<>();
        for (FieldState state : _questionFields) {
            fields.add(new QuestionField(state.alphabet, state.rule, state.type - 1));
        }

        for (FieldState state : _answerFields) {
            fields.add(new QuestionField(state.alphabet, state.rule, QuestionFieldFlags.IS_ANSWER | (state.type - 1)));
        }

        final Integer existingSetId = findQuestionFieldSet(sqlDb, fields);
        final Integer existingQuizId = (existingSetId != null)? findQuizDefinition(sqlDb, _bunch, existingSetId) : null;
        Integer quizId = null;
        if (existingQuizId == null) {
            final ImmutableIntSet acceptations = readAllPossibleAcceptations(db);
            if (acceptations.isEmpty()) {
                Toast.makeText(this, R.string.noValidQuestions, Toast.LENGTH_SHORT).show();
            }
            else {
                final int setId = (existingSetId != null) ? existingSetId : insertQuestionFieldSet(sqlDb, fields);
                quizId = insertQuizDefinition(sqlDb, _bunch, setId);
                insertAllPossibilities(sqlDb, quizId, acceptations);
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
