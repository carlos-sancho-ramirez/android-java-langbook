package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.models.QuestionFieldDetails;

public final class QuizEditorActivity extends Activity implements View.OnClickListener {

    private interface ArgKeys {
        String BUNCH = BundleKeys.BUNCH;
    }

    interface ResultKeys {
        String QUIZ = BundleKeys.QUIZ;
    }

    public static void open(Activity activity, int requestCode, BunchId bunch) {
        Intent intent = new Intent(activity, QuizEditorActivity.class);
        BunchIdBundler.writeAsIntentExtra(intent, ArgKeys.BUNCH, bunch);
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

    interface FieldTypes {
        int sameAcceptation = 1;
        int sameConcept = 2;
        int appliedRule = 3;
    }

    private static final class FieldState {
        int type;
        AlphabetId alphabet;
        int rule;
    }

    private final ArrayList<FieldState> _questionFields = new ArrayList<>(1);
    private final ArrayList<FieldState> _answerFields = new ArrayList<>(1);
    private BunchId _bunch;
    private AlphabetId _preferredAlphabet;

    private ImmutableMap<AlphabetId, String> _alphabetItems;
    private ImmutableIntKeyMap<String> _ruleItems;

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
                            if (_ruleItems != null && pos >= 0 && pos < _ruleItems.size()) {
                                fieldState.rule = _ruleItems.keyAt(pos);
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
                    fieldState.alphabet = ((AlphabetAdapter) adapterView.getAdapter()).getItem(position).key();
                    break;

                case R.id.fieldRule:
                    fieldState.rule = ((RuleAdapter) adapterView.getAdapter()).getItem(position).key();
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

        ruleSpinner.setAdapter(new RuleAdapter(_ruleItems));
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

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _bunch = BunchIdBundler.readAsIntentExtra(getIntent(), ArgKeys.BUNCH);
        final DbManager manager = DbManager.getInstance();
        final LangbookDbChecker checker = manager.getManager();

        if (!_bunch.isNoBunchForQuiz()) {
            final String bunchText = checker.readConceptText(_bunch.getConceptId(), _preferredAlphabet);
            final TextView bunchField = findViewById(R.id.bunch);
            bunchField.setText(bunchText);
        }

        _alphabetItems = checker.readAllAlphabets(_preferredAlphabet);
        _ruleItems = checker.readAllRules(_preferredAlphabet);

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

    private static QuestionFieldDetails<AlphabetId> composeQuestionField(FieldState field) {
        return new QuestionFieldDetails<>(field.alphabet, field.rule, field.type - 1);
    }

    private static QuestionFieldDetails<AlphabetId> composeAnswerField(FieldState field) {
        return new QuestionFieldDetails<>(field.alphabet, field.rule, QuestionFieldFlags.IS_ANSWER | (field.type - 1));
    }

    private ImmutableList<QuestionFieldDetails<AlphabetId>> composeFields() {
        final ImmutableList.Builder<QuestionFieldDetails<AlphabetId>> builder = new ImmutableList.Builder<>();
        for (FieldState state : _questionFields) {
            builder.add(composeQuestionField(state));
        }
        for (FieldState state : _answerFields) {
            builder.add(composeAnswerField(state));
        }

        return builder.build();
    }

    private void startQuiz() {
        final int quizId = DbManager.getInstance().getManager().obtainQuiz(_bunch, composeFields());

        final Intent intent = new Intent();
        intent.putExtra(ResultKeys.QUIZ, quizId);
        setResult(RESULT_OK, intent);
        finish();
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
