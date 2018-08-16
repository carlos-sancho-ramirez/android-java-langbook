package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntList;
import sword.collections.MutableIntSet;
import sword.collections.MutableList;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

import static sword.langbook3.android.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;
import static sword.langbook3.android.QuizSelectorActivity.NO_BUNCH;

public final class AgentEditorActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CODE_PICK_TARGET_BUNCH = 1;
    private static final int REQUEST_CODE_PICK_SOURCE_BUNCH = 2;
    private static final int REQUEST_CODE_PICK_DIFF_BUNCH = 3;
    private static final int REQUEST_CODE_PICK_RULE = 4;

    private static final int NO_RULE = 0;

    private interface SavedKeys {
        String STATE = "st";
    }

    public static void open(Context context) {
        final Intent intent = new Intent(context, AgentEditorActivity.class);
        context.startActivity(intent);
    }

    static final class CorrelationEntry {
        public int alphabet;
        public String text;

        CorrelationEntry(int alphabet, String text) {
            this.alphabet = alphabet;
            this.text = text;
        }
    }

    public static final class State implements Parcelable {
        boolean includeTargetBunch;
        int targetBunch;
        MutableIntList sourceBunches = MutableIntList.empty();
        MutableIntList diffBunches = MutableIntList.empty();
        MutableList<CorrelationEntry> matcher = MutableList.empty();
        MutableList<CorrelationEntry> adder = MutableList.empty();
        boolean matchWordStarting;
        int rule;

        State() {
        }

        private State(Parcel in) {
            final int booleans = in.readByte();
            includeTargetBunch = (booleans & 1) != 0;
            matchWordStarting = (booleans & 2) != 0;
            targetBunch = in.readInt();

            final int sourceBunchesCount = in.readInt();
            for (int i = 0; i < sourceBunchesCount; i++) {
                sourceBunches.append(in.readInt());
            }

            final int diffBunchesCount = in.readInt();
            for (int i = 0; i < diffBunchesCount; i++) {
                diffBunches.append(in.readInt());
            }

            final int matcherLength = in.readInt();
            for (int i = 0; i < matcherLength; i++) {
                matcher.append(new CorrelationEntry(in.readInt(), in.readString()));
            }

            final int adderLength = in.readInt();
            for (int i = 0; i < adderLength; i++) {
                adder.append(new CorrelationEntry(in.readInt(), in.readString()));
            }

            rule = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            int booleans = includeTargetBunch? 1 : 0;
            if (matchWordStarting) {
                booleans |= 2;
            }
            dest.writeByte((byte) booleans);
            dest.writeInt(targetBunch);

            dest.writeInt(sourceBunches.size());
            for (int value : sourceBunches) {
                dest.writeInt(value);
            }

            dest.writeInt(diffBunches.size());
            for (int value : diffBunches) {
                dest.writeInt(value);
            }

            dest.writeInt(matcher.size());
            for (CorrelationEntry entry : matcher) {
                dest.writeInt(entry.alphabet);
                dest.writeString(entry.text);
            }

            dest.writeInt(adder.size());
            for (CorrelationEntry entry : adder) {
                dest.writeInt(entry.alphabet);
                dest.writeString(entry.text);
            }

            dest.writeInt(rule);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<State> CREATOR = new Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                return new State(in);
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }

    private State _state;

    private int _preferredAlphabet;
    private ImmutableIntKeyMap<String> _alphabets;
    private boolean _enabledFlagAndRuleFields;

    private CheckBox _includeTargetBunchCheckBox;
    private Button _targetBunchChangeButton;
    private LinearLayout _sourceBunchesContainer;
    private LinearLayout _diffBunchesContainer;
    private LinearLayout _matchersContainer;
    private LinearLayout _addersContainer;
    private CheckBox _matchWordStartingCheckBox;

    private void updateAlphabets() {
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int acceptationsOffset = alphabets.columns().size();
        final int stringsOffset = acceptationsOffset + acceptations.columns().size();

        final MutableIntSet foundAlphabets = MutableIntSet.empty();
        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        final DbQuery query = new DbQuery.Builder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, acceptationsOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(
                        alphabets.getIdColumnIndex(),
                        stringsOffset + strings.getStringAlphabetColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex());

        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            final int id = row.get(0).toInt();
            final int strAlphabet = row.get(1).toInt();

            if (strAlphabet == _preferredAlphabet || !foundAlphabets.contains(id)) {
                foundAlphabets.add(id);
                result.put(id, row.get(2).toText());
            }
        }

        _alphabets = result.toImmutable();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_editor_activity);
        updateAlphabets();

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }
        else {
            _state = new State();
        }

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _includeTargetBunchCheckBox = findViewById(R.id.includeTargetBunch);
        if (_state.includeTargetBunch) {
            _includeTargetBunchCheckBox.setChecked(true);
        }
        _includeTargetBunchCheckBox.setOnCheckedChangeListener(this);

        final Database db = DbManager.getInstance().getDatabase();
        _targetBunchChangeButton = findViewById(R.id.targetBunchChangeButton);
        if (_state.targetBunch != NO_BUNCH) {
            final TextView textView = findViewById(R.id.targetBunchText);
            textView.setText(readConceptText(db, _state.targetBunch, _preferredAlphabet));

            if (_state.includeTargetBunch) {
                _targetBunchChangeButton.setEnabled(true);
            }
        }
        _targetBunchChangeButton.setOnClickListener(this);

        findViewById(R.id.addSourceBunchButton).setOnClickListener(this);
        _sourceBunchesContainer = findViewById(R.id.sourceBunchesContainer);
        for (int concept : _state.sourceBunches) {
            addSourceBunch(concept);
        }

        findViewById(R.id.addDiffBunchButton).setOnClickListener(this);
        _diffBunchesContainer = findViewById(R.id.diffBunchesContainer);
        for (int concept : _state.diffBunches) {
            addDiffBunch(concept);
        }

        _matchersContainer = findViewById(R.id.matchersContainer);
        for (CorrelationEntry entry : _state.matcher) {
            addMatcherEntry(entry);
        }
        findViewById(R.id.addMatcherButton).setOnClickListener(this);

        _addersContainer = findViewById(R.id.addersContainer);
        for (CorrelationEntry entry : _state.adder) {
            addAdderEntry(entry);
        }
        findViewById(R.id.addAdderButton).setOnClickListener(this);

        _matchWordStartingCheckBox = findViewById(R.id.matchWordStarting);
        if (_state.matchWordStarting ||
                _state.matcher.anyMatch(entry -> !TextUtils.isEmpty(entry.text)) ||
                _state.adder.anyMatch(entry -> !TextUtils.isEmpty(entry.text))) {

            enableFlagAndRuleFields();
            if (_state.matchWordStarting) {
                _matchWordStartingCheckBox.setChecked(true);
            }
        }
        _matchWordStartingCheckBox.setOnCheckedChangeListener(this);

        findViewById(R.id.ruleChangeButton).setOnClickListener(this);

        if (_state.rule != NO_RULE) {
            final TextView textView = findViewById(R.id.ruleText);
            textView.setText(readConceptText(db, _state.rule, _preferredAlphabet));
        }

        findViewById(R.id.saveButton).setOnClickListener(this);
    }

    private void addMatcherEntry(CorrelationEntry entry) {
        getLayoutInflater().inflate(R.layout.agent_editor_correlation_entry, _matchersContainer, true);
        final View view = _matchersContainer.getChildAt(_matchersContainer.getChildCount() - 1);

        final Spinner alphabetSpinner = view.findViewById(R.id.alphabet);
        alphabetSpinner.setAdapter(new AlphabetAdapter());
        final int position = _alphabets.keySet().indexOf(entry.alphabet);
        if (position >= 0) {
            alphabetSpinner.setSelection(position);
        }
        alphabetSpinner.setOnItemSelectedListener(new AlphabetSelectedListener(entry));

        final EditText textField = view.findViewById(R.id.text);
        if (entry.text != null) {
            textField.setText(entry.text);
        }
        textField.addTextChangedListener(new CorrelationTextWatcher(entry));

        view.findViewById(R.id.removeButton).setOnClickListener(v -> removeMatcherEntry(entry));
    }

    private void removeMatcherEntry(CorrelationEntry entry) {
        final int position = _state.matcher.indexOf(entry);
        if (position < 0) {
            throw new AssertionError();
        }

        _matchersContainer.removeViewAt(position);
        _state.matcher.removeAt(position);
    }

    private void addAdderEntry(CorrelationEntry entry) {
        getLayoutInflater().inflate(R.layout.agent_editor_correlation_entry, _addersContainer, true);
        final View view = _addersContainer.getChildAt(_addersContainer.getChildCount() - 1);

        final Spinner alphabetSpinner = view.findViewById(R.id.alphabet);
        alphabetSpinner.setAdapter(new AlphabetAdapter());
        final int position = _alphabets.keySet().indexOf(entry.alphabet);
        if (position >= 0) {
            alphabetSpinner.setSelection(position);
        }
        alphabetSpinner.setOnItemSelectedListener(new AlphabetSelectedListener(entry));

        final EditText textField = view.findViewById(R.id.text);
        if (entry.text != null) {
            textField.setText(entry.text);
        }
        textField.addTextChangedListener(new CorrelationTextWatcher(entry));

        view.findViewById(R.id.removeButton).setOnClickListener(v -> removeAdderEntry(entry));
    }

    private void removeAdderEntry(CorrelationEntry entry) {
        final int position = _state.adder.indexOf(entry);
        if (position < 0) {
            throw new AssertionError();
        }

        _addersContainer.removeViewAt(position);
        _state.adder.removeAt(position);
    }

    private final class AlphabetSelectedListener implements AdapterView.OnItemSelectedListener {

        private final CorrelationEntry _entry;

        AlphabetSelectedListener(CorrelationEntry entry) {
            if (entry == null) {
                throw new IllegalArgumentException();
            }

            _entry = entry;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            _entry.alphabet = _alphabets.keyAt(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Nothing to be done
        }
    }

    private final class CorrelationTextWatcher implements TextWatcher {

        private final CorrelationEntry _entry;

        CorrelationTextWatcher(CorrelationEntry entry) {
            _entry = entry;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Nothing to be done
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Nothing to be done
        }

        @Override
        public void afterTextChanged(Editable s) {
            _entry.text = s.toString();
            enableFlagAndRuleFields();
        }
    }

    private void enableFlagAndRuleFields() {
        if (!_enabledFlagAndRuleFields) {
            _matchWordStartingCheckBox.setEnabled(true);
            findViewById(R.id.rulePickerPanel).setVisibility(View.VISIBLE);
            _enabledFlagAndRuleFields = true;
        }
    }

    private final class AlphabetAdapter extends BaseAdapter {

        private LayoutInflater _inflater;

        @Override
        public int getCount() {
            return _alphabets.size();
        }

        @Override
        public Integer getItem(int position) {
            return _alphabets.keyAt(position);
        }

        @Override
        public long getItemId(int position) {
            return _alphabets.keyAt(position);
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
            textView.setText(_alphabets.valueAt(position));

            return view;
        }
    }

    private void addSourceBunch(int concept) {
        getLayoutInflater().inflate(R.layout.agent_editor_bunch_entry, _sourceBunchesContainer, true);
        final View view = _sourceBunchesContainer.getChildAt(_sourceBunchesContainer.getChildCount() - 1);

        final TextView textView = view.findViewById(R.id.textView);
        textView.setText(readConceptText(DbManager.getInstance().getDatabase(), concept, _preferredAlphabet));

        view.findViewById(R.id.removeButton).setOnClickListener(v -> removeSourceBunch(concept));
    }

    private void removeSourceBunch(int concept) {
        final int index = _state.sourceBunches.indexOf(concept);
        if (index < 0) {
            throw new AssertionError();
        }

        _sourceBunchesContainer.removeViewAt(index);
        _state.sourceBunches.removeAt(index);
    }

    private void addDiffBunch(int concept) {
        getLayoutInflater().inflate(R.layout.agent_editor_bunch_entry, _diffBunchesContainer, true);
        final View view = _diffBunchesContainer.getChildAt(_diffBunchesContainer.getChildCount() - 1);

        final TextView textView = view.findViewById(R.id.textView);
        textView.setText(readConceptText(DbManager.getInstance().getDatabase(), concept, _preferredAlphabet));

        view.findViewById(R.id.removeButton).setOnClickListener(v -> removeDiffBunch(concept));
    }

    private void removeDiffBunch(int concept) {
        final int index = _state.diffBunches.indexOf(concept);
        if (index < 0) {
            throw new AssertionError();
        }

        _diffBunchesContainer.removeViewAt(index);
        _state.diffBunches.removeAt(index);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.includeTargetBunch:
                if (isChecked) {
                    if (_state.targetBunch == NO_BUNCH) {
                        AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_TARGET_BUNCH);
                    }
                    else {
                        _targetBunchChangeButton.setEnabled(true);
                    }
                }
                else {
                    _targetBunchChangeButton.setEnabled(false);
                }
                _state.includeTargetBunch = isChecked;
                break;

            case R.id.matchWordStarting:
                _state.matchWordStarting = isChecked;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.targetBunchChangeButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_TARGET_BUNCH);
                break;

            case R.id.addSourceBunchButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_SOURCE_BUNCH);
                break;

            case R.id.addDiffBunchButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_DIFF_BUNCH);
                break;

            case R.id.addMatcherButton:
                CorrelationEntry entry = new CorrelationEntry(_alphabets.keyAt(0), null);
                _state.matcher.append(entry);
                addMatcherEntry(entry);
                break;

            case R.id.addAdderButton:
                entry = new CorrelationEntry(_alphabets.keyAt(0), null);
                _state.adder.append(entry);
                addAdderEntry(entry);
                break;

            case R.id.ruleChangeButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_RULE);
                break;

            case R.id.saveButton:
                final String errorMessage = getErrorMessage();
                if (errorMessage != null) {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                }
                else {
                    final Database db = DbManager.getInstance().getDatabase();
                    final int targetBunch = _state.includeTargetBunch ? _state.targetBunch : NO_BUNCH;

                    final ImmutableIntKeyMap.Builder<String> matcherBuilder = new ImmutableIntKeyMap.Builder<>();
                    for (CorrelationEntry corrEntry : _state.matcher) {
                        matcherBuilder.put(corrEntry.alphabet, corrEntry.text);
                    }
                    final ImmutableIntKeyMap<String> matcher = matcherBuilder.build();

                    final ImmutableIntKeyMap.Builder<String> adderBuilder = new ImmutableIntKeyMap.Builder<>();
                    for (CorrelationEntry corrEntry : _state.adder) {
                        adderBuilder.put(corrEntry.alphabet, corrEntry.text);
                    }
                    final ImmutableIntKeyMap<String> adder = adderBuilder.build();

                    final int rule = matcher.equals(adder)? NO_RULE : _state.rule;
                    final int flags = _state.matchWordStarting? 1 : 0;

                    final Integer agentId = LangbookDatabase.addAgent(db, targetBunch,
                            _state.sourceBunches.toImmutable().toSet(), _state.diffBunches.toImmutable().toSet(),
                            matcher, adder, rule, flags);
                    final int message = (agentId != null) ? R.string.newAgentFeedback : R.string.newAgentError;
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    if (agentId != null) {
                        finish();
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Database db = DbManager.getInstance().getDatabase();
        if (requestCode == REQUEST_CODE_PICK_TARGET_BUNCH) {
            if (resultCode == RESULT_OK) {
                final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                if (acceptation == 0) {
                    throw new AssertionError();
                }

                final int concept = conceptFromAcceptation(db, acceptation);
                _state.targetBunch = concept;

                final String text = readConceptText(db, concept, _preferredAlphabet);

                final TextView textView = findViewById(R.id.targetBunchText);
                textView.setText(text);
                _targetBunchChangeButton.setEnabled(true);
            }
            else if (_state.targetBunch == NO_BUNCH) {
                _targetBunchChangeButton.setEnabled(false);
                _includeTargetBunchCheckBox.setChecked(false);
            }
        }
        else if (requestCode == REQUEST_CODE_PICK_SOURCE_BUNCH && resultCode == RESULT_OK) {
            final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
            if (acceptation == 0) {
                throw new AssertionError();
            }

            final int concept = conceptFromAcceptation(db, acceptation);
            _state.sourceBunches.append(concept);
            addSourceBunch(concept);
        }
        else if (requestCode == REQUEST_CODE_PICK_DIFF_BUNCH && resultCode == RESULT_OK) {
            final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
            if (acceptation == 0) {
                throw new AssertionError();
            }

            final int concept = conceptFromAcceptation(db, acceptation);
            _state.diffBunches.append(concept);
            addDiffBunch(concept);
        }
        else if (requestCode == REQUEST_CODE_PICK_RULE && resultCode == RESULT_OK) {
            final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
            if (acceptation == 0) {
                throw new AssertionError();
            }

            final int concept = conceptFromAcceptation(db, acceptation);
            _state.rule = concept;

            final String text = readConceptText(db, concept, _preferredAlphabet);
            final TextView textView = findViewById(R.id.ruleText);
            textView.setText(text);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    private String getErrorMessage() {
        if (_state.includeTargetBunch && _state.targetBunch == NO_BUNCH) {
            return "No valid target bunch";
        }

        for (int bunch : _state.sourceBunches) {
            if (bunch == NO_BUNCH || _state.includeTargetBunch && bunch == _state.targetBunch || _state.diffBunches.contains(bunch)) {
                return "Invalid bunch selection";
            }
        }

        for (int bunch : _state.diffBunches) {
            if (bunch == NO_BUNCH || _state.includeTargetBunch && bunch == _state.targetBunch) {
                return "Invalid bunch selection";
            }
        }

        final MutableIntSet alphabets = MutableIntSet.empty();
        for (CorrelationEntry entry : _state.matcher) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in matcher";
            }
            alphabets.add(entry.alphabet);

            if (TextUtils.isEmpty(entry.text)) {
                return "Matcher entries cannot be empty";
            }
        }

        alphabets.clear();
        for (CorrelationEntry entry : _state.adder) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in adder";
            }
            alphabets.add(entry.alphabet);
        }

        if (_state.sourceBunches.isEmpty() && _state.matcher.isEmpty()) {
            // This would select all acceptations from the database, which has no sense
            return "Source bunches and matcher cannot be both empty";
        }

        final boolean ruleRequired = !_state.matcher.equals(_state.adder);
        if (ruleRequired && _state.rule == NO_RULE) {
            return "Rule is required when matcher and adder do not match";
        }

        return null;
    }
}
