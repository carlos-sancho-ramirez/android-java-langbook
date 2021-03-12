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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.List;
import sword.collections.MutableHashSet;
import sword.collections.MutableList;
import sword.collections.MutableSet;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdBundler;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.CorrelationEntryListParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.models.AgentDetails;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;

public final class AgentEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_PICK_TARGET_BUNCH = 1;
    private static final int REQUEST_CODE_PICK_SOURCE_BUNCH = 2;
    private static final int REQUEST_CODE_PICK_DIFF_BUNCH = 3;
    private static final int REQUEST_CODE_PICK_RULE = 4;

    private static final int NO_RULE = 0;

    private interface ArgKeys {
        String AGENT = BundleKeys.AGENT;
        String TARGET_BUNCH = BundleKeys.TARGET_BUNCH;
        String SOURCE_BUNCH = BundleKeys.SOURCE_BUNCH;
        String DIFF_BUNCH = BundleKeys.DIFF_BUNCH;
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    public static void open(Context context) {
        final Intent intent = new Intent(context, AgentEditorActivity.class);
        context.startActivity(intent);
    }

    public static void openWithTarget(Context context, BunchId targetBunch) {
        final Intent intent = new Intent(context, AgentEditorActivity.class);
        BunchIdBundler.writeAsIntentExtra(intent, ArgKeys.TARGET_BUNCH, targetBunch);
        context.startActivity(intent);
    }

    public static void openWithSource(Context context, BunchId sourceBunch) {
        final Intent intent = new Intent(context, AgentEditorActivity.class);
        BunchIdBundler.writeAsIntentExtra(intent, ArgKeys.SOURCE_BUNCH, sourceBunch);
        context.startActivity(intent);
    }

    public static void openWithDiff(Context context, BunchId diffBunch) {
        final Intent intent = new Intent(context, AgentEditorActivity.class);
        BunchIdBundler.writeAsIntentExtra(intent, ArgKeys.DIFF_BUNCH, diffBunch);
        context.startActivity(intent);
    }

    public static void open(Context context, int agentId) {
        final Intent intent = new Intent(context, AgentEditorActivity.class);
        intent.putExtra(ArgKeys.AGENT, agentId);
        context.startActivity(intent);
    }

    public static final class State implements Parcelable {
        MutableList<BunchId> targetBunches = MutableList.empty();
        MutableList<BunchId> sourceBunches = MutableList.empty();
        MutableList<BunchId> diffBunches = MutableList.empty();
        MutableList<Correlation.Entry<AlphabetId>> startMatcher = MutableList.empty();
        MutableList<Correlation.Entry<AlphabetId>> startAdder = MutableList.empty();
        MutableList<Correlation.Entry<AlphabetId>> endMatcher = MutableList.empty();
        MutableList<Correlation.Entry<AlphabetId>> endAdder = MutableList.empty();
        int rule;

        State() {
        }

        private State(Parcel in) {
            final int targetBunchesCount = in.readInt();
            for (int i = 0; i < targetBunchesCount; i++) {
                targetBunches.append(BunchIdParceler.read(in));
            }

            final int sourceBunchesCount = in.readInt();
            for (int i = 0; i < sourceBunchesCount; i++) {
                sourceBunches.append(BunchIdParceler.read(in));
            }

            final int diffBunchesCount = in.readInt();
            for (int i = 0; i < diffBunchesCount; i++) {
                diffBunches.append(BunchIdParceler.read(in));
            }

            CorrelationEntryListParceler.readInto(in, startMatcher);
            CorrelationEntryListParceler.readInto(in, startAdder);
            CorrelationEntryListParceler.readInto(in, endMatcher);
            CorrelationEntryListParceler.readInto(in, endAdder);

            rule = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(targetBunches.size());
            for (BunchId value : targetBunches) {
                BunchIdParceler.write(dest, value);
            }

            dest.writeInt(sourceBunches.size());
            for (BunchId value : sourceBunches) {
                BunchIdParceler.write(dest, value);
            }

            dest.writeInt(diffBunches.size());
            for (BunchId value : diffBunches) {
                BunchIdParceler.write(dest, value);
            }

            CorrelationEntryListParceler.write(dest, startMatcher);
            CorrelationEntryListParceler.write(dest, startAdder);
            CorrelationEntryListParceler.write(dest, endMatcher);
            CorrelationEntryListParceler.write(dest, endAdder);

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

    private AlphabetId _preferredAlphabet;
    private ImmutableMap<AlphabetId, String> _alphabets;
    private boolean _enabledFlagAndRuleFields;

    private LinearLayout _targetBunchesContainer;
    private LinearLayout _sourceBunchesContainer;
    private LinearLayout _diffBunchesContainer;
    private LinearLayout _startMatchersContainer;
    private LinearLayout _startAddersContainer;
    private LinearLayout _endMatchersContainer;
    private LinearLayout _endAddersContainer;

    private int getAgentId() {
        return getIntent().getIntExtra(ArgKeys.AGENT, 0);
    }

    private BunchId getSourceBunch() {
        return BunchIdBundler.readAsIntentExtra(getIntent(), ArgKeys.SOURCE_BUNCH);
    }

    private BunchId getDiffBunch() {
        return BunchIdBundler.readAsIntentExtra(getIntent(), ArgKeys.DIFF_BUNCH);
    }

    private BunchId getTargetBunch() {
        return BunchIdBundler.readAsIntentExtra(getIntent(), ArgKeys.TARGET_BUNCH);
    }

    private void updateBunchSet(LangbookDbChecker checker, ViewGroup container, MutableList<BunchId> bunches) {
        final int currentBunchViewCount = container.getChildCount();
        final int stateBunchCount = bunches.size();

        for (int i = currentBunchViewCount - 1; i >= stateBunchCount; i--) {
            container.removeViewAt(i);
        }

        for (int i = 0; i < stateBunchCount; i++) {
            final BunchId bunch = bunches.valueAt(i);
            if (i < currentBunchViewCount) {
                bindBunch(container.getChildAt(i), checker, bunch, container, bunches);
            }
            else {
                addBunch(checker, bunch, container, bunches);
            }
        }
    }

    private void updateCorrelation(ViewGroup container, MutableList<Correlation.Entry<AlphabetId>> correlation) {
        final int currentEntryViewCount = container.getChildCount();
        final int stateEntryCount = correlation.size();

        for (int i = currentEntryViewCount - 1; i >= stateEntryCount; i--) {
            container.removeViewAt(i);
        }

        for (int i = 0; i < stateEntryCount; i++) {
            final Correlation.Entry<AlphabetId> entry = correlation.get(i);
            if (i < currentEntryViewCount) {
                bindEntry(container.getChildAt(i), entry, container, correlation);
            }
            else {
                addEntry(entry, container, correlation);
            }
        }
    }

    private void setStateValues() {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();

        updateBunchSet(checker, _targetBunchesContainer, _state.targetBunches);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_editor_activity);

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        _alphabets = checker.readAllAlphabets(_preferredAlphabet);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }
        else {
            _state = new State();

            final int agentId = getAgentId();
            final BunchId sourceBunch = getSourceBunch();
            final BunchId diffBunch = getDiffBunch();
            final BunchId targetBunch = getTargetBunch();
            if (agentId != 0) {
                final AgentDetails<AlphabetId, BunchId> agentDetails = checker.getAgentDetails(agentId);
                _state.targetBunches = agentDetails.targetBunches.toList().mutate();
                _state.sourceBunches = agentDetails.sourceBunches.toList().mutate();
                _state.diffBunches = agentDetails.diffBunches.toList().mutate();
                _state.startMatcher = agentDetails.startMatcher.toCorrelationEntryList();
                _state.startAdder = agentDetails.startAdder.toCorrelationEntryList();
                _state.endMatcher = agentDetails.endMatcher.toCorrelationEntryList();
                _state.endAdder = agentDetails.endAdder.toCorrelationEntryList();
                _state.rule = agentDetails.rule;
            }
            else if (targetBunch != null) {
                _state.targetBunches.append(targetBunch);
            }
            else if (sourceBunch != null) {
                _state.sourceBunches.append(sourceBunch);
            }
            else if (diffBunch != null) {
                _state.diffBunches.append(diffBunch);
            }
        }

        findViewById(R.id.addTargetBunchButton).setOnClickListener(this);
        findViewById(R.id.addSourceBunchButton).setOnClickListener(this);
        findViewById(R.id.addDiffBunchButton).setOnClickListener(this);

        _targetBunchesContainer = findViewById(R.id.targetBunchesContainer);
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

    private void addEntry(Correlation.Entry<AlphabetId> entry, ViewGroup container, MutableList<Correlation.Entry<AlphabetId>> entries) {
        getLayoutInflater().inflate(R.layout.agent_editor_correlation_entry, container, true);
        final View view = container.getChildAt(container.getChildCount() - 1);
        bindEntry(view, entry, container, entries);
    }

    private void bindEntry(View view, Correlation.Entry<AlphabetId> entry, ViewGroup container, MutableList<Correlation.Entry<AlphabetId>> entries) {
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

    private static void removeEntry(Correlation.Entry<AlphabetId> entry, ViewGroup container, MutableList<Correlation.Entry<AlphabetId>> entries) {
        final int position = entries.indexOf(entry);
        if (position < 0) {
            throw new AssertionError();
        }

        container.removeViewAt(position);
        entries.removeAt(position);
    }

    private final class AlphabetSelectedListener implements AdapterView.OnItemSelectedListener {

        private final Correlation.Entry<AlphabetId> _entry;

        AlphabetSelectedListener(Correlation.Entry<AlphabetId> entry) {
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

        private final Correlation.Entry<AlphabetId> _entry;

        CorrelationTextWatcher(Correlation.Entry<AlphabetId> entry) {
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
        public AlphabetId getItem(int position) {
            return _alphabets.keyAt(position);
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
            textView.setText(_alphabets.valueAt(position));

            return view;
        }
    }

    private void addBunch(LangbookDbChecker checker, BunchId bunch, ViewGroup container, MutableList<BunchId> bunches) {
        getLayoutInflater().inflate(R.layout.agent_editor_bunch_entry, container, true);
        final View view = container.getChildAt(container.getChildCount() - 1);
        bindBunch(view, checker, bunch, container, bunches);
    }

    private void bindBunch(View view, LangbookDbChecker checker, BunchId bunch, ViewGroup container, MutableList<BunchId> bunches) {
        final TextView textView = view.findViewById(R.id.textView);
        textView.setText(checker.readConceptText(bunch.getConceptId(), _preferredAlphabet));
        view.findViewById(R.id.removeButton).setOnClickListener(v -> removeBunch(container, bunches, bunch));
    }

    private static void removeBunch(ViewGroup container, MutableList<BunchId> bunches, BunchId bunch) {
        final int index = bunches.indexOf(bunch);
        if (index < 0) {
            throw new AssertionError();
        }

        container.removeViewAt(index);
        bunches.removeAt(index);
    }

    private static ImmutableCorrelation<AlphabetId> buildCorrelation(List<Correlation.Entry<AlphabetId>> entries) {
        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        for (Correlation.Entry<AlphabetId> corrEntry : entries) {
            builder.put(corrEntry.alphabet, corrEntry.text);
        }
        return builder.build();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addTargetBunchButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_TARGET_BUNCH);
                break;

            case R.id.addSourceBunchButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_SOURCE_BUNCH);
                break;

            case R.id.addDiffBunchButton:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_DIFF_BUNCH);
                break;

            case R.id.addStartMatcherButton:
                Correlation.Entry<AlphabetId> entry = new Correlation.Entry<>(_alphabets.keyAt(0), null);
                _state.startMatcher.append(entry);
                addEntry(entry, _startMatchersContainer, _state.startMatcher);
                break;

            case R.id.addStartAdderButton:
                entry = new Correlation.Entry<>(_alphabets.keyAt(0), null);
                _state.startAdder.append(entry);
                addEntry(entry, _startAddersContainer, _state.startAdder);
                break;

            case R.id.addEndMatcherButton:
                entry = new Correlation.Entry<>(_alphabets.keyAt(0), null);
                _state.endMatcher.append(entry);
                addEntry(entry, _endMatchersContainer, _state.endMatcher);
                break;

            case R.id.addEndAdderButton:
                entry = new Correlation.Entry<>(_alphabets.keyAt(0), null);
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
                    final ImmutableCorrelation<AlphabetId> startMatcher = buildCorrelation(_state.startMatcher);
                    final ImmutableCorrelation<AlphabetId> startAdder = buildCorrelation(_state.startAdder);
                    final ImmutableCorrelation<AlphabetId> endMatcher = buildCorrelation(_state.endMatcher);
                    final ImmutableCorrelation<AlphabetId> endAdder = buildCorrelation(_state.endAdder);

                    final int rule = (startMatcher.equals(startAdder) && endMatcher.equals(endAdder))? NO_RULE : _state.rule;

                    final int givenAgentId = getAgentId();
                    final LangbookDbManager manager = DbManager.getInstance().getManager();
                    final ImmutableSet<BunchId> targetBunches = _state.targetBunches.toImmutable().toSet();
                    final ImmutableSet<BunchId> sourceBunches = _state.sourceBunches.toImmutable().toSet();
                    final ImmutableSet<BunchId> diffBunches = _state.diffBunches.toImmutable().toSet();
                    if (givenAgentId == 0) {
                        final Integer agentId = manager.addAgent(targetBunches, sourceBunches, diffBunches,
                                startMatcher, startAdder, endMatcher, endAdder, rule);
                        final int message = (agentId != null) ? R.string.newAgentFeedback : R.string.newAgentError;
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        if (agentId != null) {
                            finish();
                        }
                    }
                    else {
                        final boolean success = manager.updateAgent(givenAgentId, targetBunches, sourceBunches, diffBunches,
                                startMatcher, startAdder, endMatcher, endAdder, rule);
                        final int message = success? R.string.updateAgentFeedback : R.string.updateAgentError;
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            finish();
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        if (requestCode == REQUEST_CODE_PICK_TARGET_BUNCH && resultCode == RESULT_OK) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
            if (acceptation == null) {
                throw new AssertionError();
            }

            final BunchId bunch = conceptAsBunchId(manager.conceptFromAcceptation(acceptation));
            _state.targetBunches.append(bunch);
            addBunch(DbManager.getInstance().getManager(), bunch, _targetBunchesContainer, _state.targetBunches);
        }
        else if (requestCode == REQUEST_CODE_PICK_SOURCE_BUNCH && resultCode == RESULT_OK) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
            if (acceptation == null) {
                throw new AssertionError();
            }

            final BunchId concept = conceptAsBunchId(manager.conceptFromAcceptation(acceptation));
            _state.sourceBunches.append(concept);
            addBunch(DbManager.getInstance().getManager(), concept, _sourceBunchesContainer, _state.sourceBunches);
        }
        else if (requestCode == REQUEST_CODE_PICK_DIFF_BUNCH && resultCode == RESULT_OK) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
            if (acceptation == null) {
                throw new AssertionError();
            }

            final BunchId bunch = conceptAsBunchId(manager.conceptFromAcceptation(acceptation));
            _state.diffBunches.append(bunch);
            addBunch(DbManager.getInstance().getManager(), bunch, _diffBunchesContainer, _state.diffBunches);
        }
        else if (requestCode == REQUEST_CODE_PICK_RULE && resultCode == RESULT_OK) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
            if (acceptation == null) {
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
        final ImmutableSet<BunchId> targets = _state.targetBunches.toSet().toImmutable();
        final ImmutableSet<BunchId> sources = _state.sourceBunches.toSet().toImmutable();
        final ImmutableSet<BunchId> diffs = _state.diffBunches.toSet().toImmutable();

        if (targets.anyMatch(bunch -> bunch.isNoBunchForQuiz() || sources.contains(bunch) || diffs.contains(bunch))) {
            return "Invalid target bunch selection";
        }

        if (sources.anyMatch(bunch -> bunch.isNoBunchForQuiz() || targets.contains(bunch) || diffs.contains(bunch))) {
            return "Invalid target bunch selection";
        }

        if (diffs.anyMatch(bunch -> bunch.isNoBunchForQuiz() || targets.contains(bunch) || sources.contains(bunch))) {
            return "Invalid bunch selection";
        }

        final MutableSet<AlphabetId> alphabets = MutableHashSet.empty();
        for (Correlation.Entry<AlphabetId> entry : _state.startMatcher) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in start matcher";
            }
            alphabets.add(entry.alphabet);

            if (TextUtils.isEmpty(entry.text)) {
                return "Start matcher entries cannot be empty";
            }
        }

        alphabets.clear();
        for (Correlation.Entry<AlphabetId> entry : _state.startAdder) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in start adder";
            }
            alphabets.add(entry.alphabet);
        }

        alphabets.clear();
        for (Correlation.Entry<AlphabetId> entry : _state.endMatcher) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in end matcher";
            }
            alphabets.add(entry.alphabet);

            if (TextUtils.isEmpty(entry.text)) {
                return "End matcher entries cannot be empty";
            }
        }

        alphabets.clear();
        for (Correlation.Entry<AlphabetId> entry : _state.endAdder) {
            if (alphabets.contains(entry.alphabet)) {
                return "Unable to duplicate alphabet in end adder";
            }
            alphabets.add(entry.alphabet);
        }

        if (sources.isEmpty() && _state.startMatcher.isEmpty() && _state.endMatcher.isEmpty()) {
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
