package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.langbook3.android.AlphabetAdapter;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.RuleAdapter;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.QuizId;
import sword.langbook3.android.db.QuizIdBundler;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.QuestionFieldDetails;

public final class QuizEditorActivityDelegate<Activity extends ActivityInterface> extends AbstractActivityDelegate<Activity> implements View.OnClickListener {
    public interface ArgKeys {
        String BUNCH = BundleKeys.BUNCH;
    }

    interface ResultKeys {
        String QUIZ = BundleKeys.QUIZ;
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
        RuleId rule;
    }

    private Activity _activity;
    private final ArrayList<FieldState> _questionFields = new ArrayList<>(1);
    private final ArrayList<FieldState> _answerFields = new ArrayList<>(1);
    private BunchId _bunch;
    private AlphabetId _preferredAlphabet;

    private ImmutableMap<AlphabetId, String> _alphabetItems;
    private ImmutableMap<RuleId, String> _ruleItems;

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
                        if (fieldState.rule == null) {
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
                _activity.getString(R.string.questionTypeNoValue),
                _activity.getString(R.string.questionTypeSameAcceptation),
                _activity.getString(R.string.questionTypeSameConcept),
                _activity.getString(R.string.questionTypeAppliedRule)
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

        final ViewGroup viewGroup = _activity.findViewById(viewList);
        _activity.getLayoutInflater().inflate(R.layout.quiz_editor_field_entry, viewGroup, true);

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
            final ViewGroup list = _activity.findViewById(listResId);
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
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.quiz_editor_activity);

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _bunch = BunchIdBundler.readAsIntentExtra(activity.getIntent(), ArgKeys.BUNCH);
        final DbManager manager = DbManager.getInstance();
        final LangbookDbChecker checker = manager.getManager();

        if (_bunch != null) {
            final String bunchText = checker.readConceptText(_bunch.getConceptId(), _preferredAlphabet);
            final TextView bunchField = activity.findViewById(R.id.bunch);
            bunchField.setText(bunchText);
        }

        _alphabetItems = checker.readAllAlphabets(_preferredAlphabet);
        _ruleItems = checker.readAllRules(_preferredAlphabet);

        _questionFields.add(new FieldState());
        _answerFields.add(new FieldState());

        final ViewGroup questionViewGroup = activity.findViewById(R.id.questionList);
        setUpFieldViews(questionViewGroup.getChildAt(0), _questionFields.get(0));

        final ViewGroup answerViewGroup = activity.findViewById(R.id.answerList);
        setUpFieldViews(answerViewGroup.getChildAt(0), _answerFields.get(0));

        activity.findViewById(R.id.addQuestionButton).setOnClickListener(this);
        activity.findViewById(R.id.addAnswerButton).setOnClickListener(this);
        activity.findViewById(R.id.startButton).setOnClickListener(this);
    }

    private static QuestionFieldDetails<AlphabetId, RuleId> composeQuestionField(FieldState field) {
        return new QuestionFieldDetails<>(field.alphabet, field.rule, field.type - 1);
    }

    private static QuestionFieldDetails<AlphabetId, RuleId> composeAnswerField(FieldState field) {
        return new QuestionFieldDetails<>(field.alphabet, field.rule, LangbookDbSchema.QuestionFieldFlags.IS_ANSWER | (field.type - 1));
    }

    private ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> composeFields() {
        final ImmutableList.Builder<QuestionFieldDetails<AlphabetId, RuleId>> builder = new ImmutableList.Builder<>();
        for (FieldState state : _questionFields) {
            builder.add(composeQuestionField(state));
        }
        for (FieldState state : _answerFields) {
            builder.add(composeAnswerField(state));
        }

        return builder.build();
    }

    private void startQuiz() {
        final QuizId quizId = DbManager.getInstance().getManager().obtainQuiz(_bunch, composeFields());

        final Intent intent = new Intent();
        QuizIdBundler.writeAsIntentExtra(intent, ResultKeys.QUIZ, quizId);
        _activity.setResult(RESULT_OK, intent);
        _activity.finish();
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.startButton) {
            startQuiz();
        }
        else if (id == R.id.addQuestionButton) {
            addField(_questionFields, R.id.questionList);
        }
        else if (id == R.id.addAnswerButton) {
            addField(_answerFields, R.id.answerList);
        }
    }
}
