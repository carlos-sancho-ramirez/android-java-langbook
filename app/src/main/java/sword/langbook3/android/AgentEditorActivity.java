package sword.langbook3.android;

import android.app.Activity;
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
import sword.collections.ImmutableIntSet;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntList;
import sword.collections.MutableIntSet;
import sword.collections.MutableList;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.AgentDetails;

import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;

public final class AgentEditorActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CODE_PICK_TARGET_BUNCH = 1;
    private static final int REQUEST_CODE_PICK_SOURCE_BUNCH = 2;
    private static final int REQUEST_CODE_PICK_DIFF_BUNCH = 3;
    private static final int REQUEST_CODE_PICK_RULE = 4;

    private static final int NO_RULE = 0;

    private interface ArgKeys {
        String AGENT = BundleKeys.AGENT;
        String SOURCE_BUNCH = BundleKeys.SOURCE_BUNCH;
        String DIFF_BUNCH = BundleKeys.DIFF_BUNCH;
        String TARGET_BUNCH = BundleKeys.TARGET_BUNCH;
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, AgentEditorActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void openWithSource(Activity activity, int requestCode, int sourceBunch) {
        final Intent intent = new Intent(activity, AgentEditorActivity.class);
        intent.putExtra(ArgKeys.SOURCE_BUNCH, sourceBunch);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void openWithDiff(Activity activity, int requestCode, int sourceBunch) {
        final Intent intent = new Intent(activity, AgentEditorActivity.class);
        intent.putExtra(ArgKeys.DIFF_BUNCH, sourceBunch);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void openWithTarget(Activity activity, int requestCode, int sourceBunch) {
        final Intent intent = new Intent(activity, AgentEditorActivity.class);
        intent.putExtra(ArgKeys.TARGET_BUNCH, sourceBunch);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, int agentId) {
        final Intent intent = new Intent(activity, AgentEditorActivity.class);
        intent.putExtra(ArgKeys.AGENT, agentId);
        activity.startActivityForResult(intent, requestCode);
    }

    static final class CorrelationEntry {
        public int alphabet;
        public String text;

        CorrelationEntry(int alphabet, String text) {
            this.alphabet = alphabet;
            this.text = text;
        }

        @Override
        public String toString() {
            return "CorrelationEntry("+ alphabet + ", " + text + ')';
        }

        @Override
        public int hashCode() {
            return alphabet * 37 + ((text == null)? 0 : text.hashCode());
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (!(other instanceof CorrelationEntry)) {
                return false;
            }

            final CorrelationEntry that = (CorrelationEntry) other;
            return alphabet == that.alphabet && equal(text, that.text);
        }
    }

    public static final class State implements Parcelable {
        boolean includeTargetBunch;
        int targetBunch;
        MutableIntList sourceBunches = MutableIntList.empty();
        MutableIntList diffBunches = MutableIntList.empty();
        MutableList<CorrelationEntry> startMatcher = MutableList.empty();
        MutableList<CorrelationEntry> startAdder = MutableList.empty();
        MutableList<CorrelationEntry> endMatcher = MutableList.empty();
        MutableList<CorrelationEntry> endAdder = MutableList.empty();
        int rule;

        State() {
        }

        private State(Parcel in) {
            includeTargetBunch = (in.readByte() & 1) != 0;
            targetBunch = in.readInt();

            final int sourceBunchesCount = in.readInt();
            for (int i = 0; i < sourceBunchesCount; i++) {
                sourceBunches.append(in.readInt());
            }

            final int diffBunchesCount = in.readInt();
            for (int i = 0; i < diffBunchesCount; i++) {
                diffBunches.append(in.readInt());
            }

            final int startMatcherLength = in.readInt();
            for (int i = 0; i < startMatcherLength; i++) {
                startMatcher.append(new CorrelationEntry(in.readInt(), in.readString()));
            }

            final int startAdderLength = in.readInt();
            for (int i = 0; i < startAdderLength; i++) {
                startAdder.append(new CorrelationEntry(in.readInt(), in.readString()));
            }

            final int endMatcherLength = in.readInt();
            for (int i = 0; i < endMatcherLength; i++) {
                endMatcher.append(new CorrelationEntry(in.readInt(), in.readString()));
            }

            final int endAdderLength = in.readInt();
            for (int i = 0; i < endAdderLength; i++) {
                endAdder.append(new CorrelationEntry(in.readInt(), in.readString()));
            }

            rule = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (includeTargetBunch? 1 : 0));
            dest.writeInt(targetBunch);

            dest.writeInt(sourceBunches.size());
            for (int value : sourceBunches) {
                dest.writeInt(value);
            }

            dest.writeInt(diffBunches.size());
            for (int value : diffBunches) {
                dest.writeInt(value);
            }

            dest.writeInt(startMatcher.size());
            for (CorrelationEntry entry : startMatcher) {
                dest.writeInt(entry.alphabet);
                dest.writeString(entry.text);
            }

            dest.writeInt(startAdder.size());
            for (CorrelationEntry entry : startAdder) {
                dest.writeInt(entry.alphabet);
                dest.writeString(entry.text);
            }

            dest.writeInt(endMatcher.size());
            for (CorrelationEntry entry : endMatcher) {
                dest.writeInt(entry.alphabet);
                dest.writeString(entry.text);
            }

            dest.writeInt(endAdder.size());
            for (CorrelationEntry entry : endAdder) {
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
    private LinearLayout _startMatchersContainer;
    private LinearLayout _startAddersContainer;
    private LinearLayout _endMatchersContainer;
    private LinearLayout _endAddersContainer;

    private int getAgentId() {
        return getIntent().getIntExtra(ArgKeys.AGENT, 0);
    }

    private int getSourceBunch() {
        return getIntent().getIntExtra(ArgKeys.SOURCE_BUNCH, 0);
    }

    private int getDiffBunch() {
        return getIntent().getIntExtra(ArgKeys.DIFF_BUNCH, 0);
    }

    private int getTargetBunch() {
        return getIntent().getIntExtra(ArgKeys.TARGET_BUNCH, 0);
    }

    private void updateBunchSet(LangbookChecker checker, ViewGroup container, MutableIntList bunches) {
        final int currentBunchViewCount = container.getChildCount();
        final int stateBunchCount = bunches.size();

        for (int i = currentBunchViewCount - 1; i >= stateBunchCount; i--) {
            container.removeViewAt(i);
        }

        for (int i = 0; i < stateBunchCount; i++) {
            final int concept = bunches.valueAt(i);
            if (i < currentBunchViewCount) {
                bindBunch(container.getChildAt(i), checker, concept, container, bunches);
            }
            else {
                addBunch(checker, concept, container, bunches);
            }
        }
    }

    private void updateCorrelation(ViewGroup container, MutableList<CorrelationEntry> correlation) {
        final int currentEntryViewCount = container.getChildCount();
        final int stateEntryCount = correlation.size();

        for (int i = currentEntryViewCount - 1; i >= stateEntryCount; i--) {
            container.removeViewAt(i);
        }

        for (int i = 0; i < stateEntryCount; i++) {
            final CorrelationEntry entry = correlation.get(i);
            if (i < currentEntryViewCount) {
                bindEntry(container.getChildAt(i), entry, container, correlation);
            }
            else {
                addEntry(entry, container, correlation);
            }
        }
    }

    private void setStateValues() {
        final LangbookChecker checker = DbManager.getInstance().getManager();
        _includeTargetBunchCheckBox.setChecked(_state.includeTargetBunch);

        if (_state.targetBunch != NO_BUNCH) {
            final TextView textView = findViewById(R.id.targetBunchText);
            textView.setText(checker.readConceptText(_state.targetBunch, _preferredAlphabet));
            _targetBunchChangeButton.setEnabled(_state.includeTargetBunch);
        }

        updateBunchSet(checker, _sourceBunchesContainer, _state.sourceBunches);
        updateBunchSet(checker, _diffBunchesContainer, _state.diffBunches);

        updateCorrelation(_startMatchersContainer, _state.startMatcher);
        updateCorrelation(_startAddersContainer, _state.startAdder);
        updateCorrelation(_endMatchersContainer, _state.endMatcher);
        updateCorrelation(_endAddersContainer, _state.endAdder);

        if (_state.startMatcher.anyMatch(entry -> !TextUtils.isEmpty(entry.text)) ||
                _state.startAdder.anyMatch(entry -> !TextUtils.isEmpty(entry.text)) ||
                _state.endMatcher.anyMatch(entry -> !TextUtils.isEmpty(entry.text)) ||
                _state.endAdder.anyMatch(entry -> !TextUtils.isEmpty(entry.text))) {

            enableFlagAndRuleFields();
        }

        final TextView textView = findViewById(R.id.ruleText);
        textView.setText((_state.rule != NO_RULE)? checker.readConceptText(_state.rule, _preferredAlphabet) : null);
    }

    private MutableList<CorrelationEntry> toCorrelationEntryList(IntKeyMap<String> correlation) {
        final MutableList<CorrelationEntry> result = MutableList.empty();
        for (IntKeyMap.Entry<String> entry : correlation.entries()) {
            result.append(new CorrelationEntry(entry.key(), entry.value()));
        }

        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_editor_activity);

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookChecker checker = DbManager.getInstance().getManager();
        _alphabets = checker.readAllAlphabets(_preferredAlphabet);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }
        else {
            _state = new State();

            final int agentId = getAgentId();
            final int sourceBunch = getSourceBunch();
            final int diffBunch = getDiffBunch();
            final int targetBunch = getTargetBunch();
            if (agentId != 0) {
                final AgentDetails agentDetails = checker.getAgentDetails(agentId);
                _state.targetBunch = agentDetails.targetBunch;
                _state.includeTargetBunch = agentDetails.targetBunch != 0;
                _state.sourceBunches = agentDetails.sourceBunches.toList().mutate();
                _state.diffBunches = agentDetails.diffBunches.toList().mutate();
                _state.startMatcher = toCorrelationEntryList(agentDetails.startMatcher);
                _state.startAdder = toCorrelationEntryList(agentDetails.startAdder);
                _state.endMatcher = toCorrelationEntryList(agentDetails.endMatcher);
                _state.endAdder = toCorrelationEntryList(agentDetails.endAdder);
                _state.rule = agentDetails.rule;
            }
            else if (sourceBunch != 0) {
                _state.sourceBunches.append(sourceBunch);
            }
            else if (diffBunch != 0) {
                _state.diffBunches.append(diffBunch);
            }
            else if (targetBunch != 0) {
                _state.targetBunch = targetBunch;
                _state.includeTargetBunch = true;
            }
        }

        _includeTargetBunchCheckBox = findViewById(R.id.includeTargetBunch);
        _includeTargetBunchCheckBox.setOnCheckedChangeListener(this);

        _targetBunchChangeButton = findViewById(R.id.targetBunchChangeButton);
        _targetBunchChangeButton.setOnClickListener(this);

        findViewById(R.id.addSourceBunchButton).setOnClickListener(this);
        findViewById(R.id.addDiffBunchButton).setOnClickListener(this);

        _sourceBunchesContainer = findViewById(R.id.sourceBunchesContainer);
        _diffBunchesContainer = findViewById(R.id.diffBunchesContainer);

        _startMatchersContainer = findViewById(R.id.startMatchersContainer);
        _startAddersContainer = findViewById(R.id.startAddersContainer);
        _endMatchersContainer = findViewById(R.id.endMatchersContainer);
        _endAddersContainer = findViewById(R.id.endAddersContainer);

        findViewById(R.id.addStartMatcherButton).setOnClickListener(this);
        findViewById(R.id.addStartAdderButton).setOnClickListener(this);
        findViewById(R.id.addEndMatcherButton).setOnClickListener(this);
        findViewById(R.id.addEndAdderButton).setOnClickListener(this);

        findViewById(R.id.ruleChangeButton).setOnClickListener(this);
        findViewById(R.id.saveButton).setOnClickListener(this);

        setStateValues();
    }

    private void addEntry(CorrelationEntry entry, ViewGroup container, MutableList<CorrelationEntry> entries) {
        getLayoutInflater().inflate(R.layout.agent_editor_correlation_entry, container, true);
        final View view = container.getChildAt(container.getChildCount() - 1);
        bindEntry(view, entry, container, entries);
    }

    private void bindEntry(View view, CorrelationEntry entry, ViewGroup container, MutableList<CorrelationEntry> entries) {
        final Spinner alphabetSpinner = view.findViewById(R.id.alphabet);
        alphabetSpinner.setAdapter(new AlphabetAdapter());
        final int position = _alphabets.keySet().indexOf(entry.alphabet);
        if (position >= 0) {
            alphabetSpinner.setSelection(position);
        }
        alphabetSpinner.setOnItemSelectedListener(new AlphabetSelectedListener(entry));

        final EditText textField = view.findViewById(R.id.text);
        textField.setText(entry.text);
        textField.addTextChangedListener(new CorrelationTextWatcher(entry));

        view.findViewById(R.id.removeButton).setOnClickListener(v -> removeEntry(entry, container, entries));
    }

    private static void removeEntry(CorrelationEntry entry, ViewGroup container, MutableList<CorrelationEntry> entries) {
        final int position = entries.indexOf(entry);
        if (position < 0) {
            throw new AssertionError();
        }

        container.removeViewAt(position);
        entries.removeAt(position);
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

    private void addBunch(LangbookChecker checker, int concept, ViewGroup container, MutableIntList bunches) {
        getLayoutInflater().inflate(R.layout.agent_editor_bunch_entry, container, true);
        final View view = container.getChildAt(container.getChildCount() - 1);
        bindBunch(view, checker, concept, container, bunches);
    }

    private void bindBunch(View view, LangbookChecker checker, int concept, ViewGroup container, MutableIntList bunches) {
        final TextView textView = view.findViewById(R.id.textView);
        textView.setText(checker.readConceptText(concept, _preferredAlphabet));
        view.findViewById(R.id.removeButton).setOnClickListener(v -> removeBunch(container, bunches, concept));
    }

    private static void removeBunch(ViewGroup container, MutableIntList bunches, int concept) {
        final int index = bunches.indexOf(concept);
        if (index < 0) {
            throw new AssertionError();
        }

        container.removeViewAt(index);
        bunches.removeAt(index);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
    }

    private static ImmutableIntKeyMap<String> buildCorrelation(List<CorrelationEntry> entries) {
        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        for (CorrelationEntry corrEntry : entries) {
            builder.put(corrEntry.alphabet, corrEntry.text);
        }
        return builder.build();
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

            case R.id.addStartMatcherButton:
                CorrelationEntry entry = new CorrelationEntry(_alphabets.keyAt(0), null);
                _state.startMatcher.append(entry);
                addEntry(entry, _startMatchersContainer, _state.startMatcher);
                break;

            case R.id.addStartAdderButton:
                entry = new CorrelationEntry(_alphabets.keyAt(0), null);
                _state.startAdder.append(entry);
                addEntry(entry, _startAddersContainer, _state.startAdder);
                break;

            case R.id.addEndMatcherButton:
                entry = new CorrelationEntry(_alphabets.keyAt(0), null);
                _state.endMatcher.append(entry);
                addEntry(entry, _endMatchersContainer, _state.endMatcher);
                break;

            case R.id.addEndAdderButton:
                entry = new CorrelationEntry(_alphabets.keyAt(0), null);
                _state.endAdder.append(entry);
                addEntry(entry, _endAddersContainer, _state.endAdder);
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
                    final int targetBunch = _state.includeTargetBunch ? _state.targetBunch : NO_BUNCH;

                    final ImmutableIntKeyMap<String> startMatcher = buildCorrelation(_state.startMatcher);
                    final ImmutableIntKeyMap<String> startAdder = buildCorrelation(_state.startAdder);
                    final ImmutableIntKeyMap<String> endMatcher = buildCorrelation(_state.endMatcher);
                    final ImmutableIntKeyMap<String> endAdder = buildCorrelation(_state.endAdder);

                    final int rule = (startMatcher.equals(startAdder) && endMatcher.equals(endAdder))? NO_RULE : _state.rule;

                    final int givenAgentId = getAgentId();
                    final LangbookManager manager = DbManager.getInstance().getManager();
                    final ImmutableIntSet sourceBunches = _state.sourceBunches.toImmutable().toSet();
                    final ImmutableIntSet diffBunches = _state.diffBunches.toImmutable().toSet();
                    if (givenAgentId == 0) {
                        final Integer agentId = manager.addAgent(targetBunch, sourceBunches, diffBunches,
                                startMatcher, startAdder, endMatcher, endAdder, rule);
                        final int message = (agentId != null) ? R.string.newAgentFeedback : R.string.newAgentError;
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        if (agentId != null) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                    else {
                        final boolean success = manager.updateAgent(givenAgentId, targetBunch, sourceBunches, diffBunches,
                                startMatcher, startAdder, endMatcher, endAdder, rule);
                        final int message = success? R.string.updateAgentFeedback : R.string.updateAgentError;
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LangbookManager manager = DbManager.getInstance().getManager();
        if (requestCode == REQUEST_CODE_PICK_TARGET_BUNCH) {
            if (resultCode == RESULT_OK) {
                final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, 0);
                if (acceptation == 0) {
                    throw new AssertionError();
                }

                final int concept = manager.conceptFromAcceptation(acceptation);
                _state.targetBunch = concept;

                final String text = manager.readConceptText(concept, _preferredAlphabet);

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
            final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, 0);
            if (acceptation == 0) {
                throw new AssertionError();
            }

            final int concept = manager.conceptFromAcceptation(acceptation);
            _state.sourceBunches.append(concept);
            addBunch(DbManager.getInstance().getManager(), concept, _sourceBunchesContainer, _state.sourceBunches);
        }
        else if (requestCode == REQUEST_CODE_PICK_DIFF_BUNCH && resultCode == RESULT_OK) {
            final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, 0);
            if (acceptation == 0) {
                throw new AssertionError();
            }

            final int concept = manager.conceptFromAcceptation(acceptation);
            _state.diffBunches.append(concept);
            addBunch(DbManager.getInstance().getManager(), concept, _diffBunchesContainer, _state.diffBunches);
        }
        else if (requestCode == REQUEST_CODE_PICK_RULE && resultCode == RESULT_OK) {
            final int acceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, 0);
            if (acceptation == 0) {
                throw new AssertionError();
            }

            final int concept = manager.conceptFromAcceptation(acceptation);
            _state.rule = concept;

            final String text = manager.readConceptText(concept, _preferredAlphabet);
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

        final MutableIntSet alphabets = MutableIntArraySet.empty();
        for (CorrelationEntry entry : _state.startMatcher) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in start matcher";
            }
            alphabets.add(entry.alphabet);

            if (TextUtils.isEmpty(entry.text)) {
                return "Start matcher entries cannot be empty";
            }
        }

        alphabets.clear();
        for (CorrelationEntry entry : _state.startAdder) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in start adder";
            }
            alphabets.add(entry.alphabet);
        }

        alphabets.clear();
        for (CorrelationEntry entry : _state.endMatcher) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in end matcher";
            }
            alphabets.add(entry.alphabet);

            if (TextUtils.isEmpty(entry.text)) {
                return "End matcher entries cannot be empty";
            }
        }

        alphabets.clear();
        for (CorrelationEntry entry : _state.endAdder) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in end adder";
            }
            alphabets.add(entry.alphabet);
        }

        if (_state.sourceBunches.isEmpty() && _state.startMatcher.isEmpty() && _state.endMatcher.isEmpty()) {
            // This would select all acceptations from the database, which has no sense
            return "Source bunches and matchers cannot be both empty";
        }

        final boolean ruleRequired = !_state.startMatcher.equals(_state.startAdder) || !_state.endMatcher.equals(_state.endAdder);
        if (ruleRequired && _state.rule == NO_RULE) {
            return "Rule is required when matcher and adder do not match";
        }

        return null;
    }
}
