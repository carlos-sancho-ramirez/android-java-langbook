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

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntProcedure;
import sword.collections.MutableList;
import sword.langbook3.android.collections.ImmutableListUtils;
import sword.langbook3.android.collections.ListUtils;
import sword.langbook3.android.collections.MinimumSizeArrayLengthFunction;
import sword.langbook3.android.collections.Supplier;
import sword.langbook3.android.collections.TraversableUtils;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdParceler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;
import sword.langbook3.android.db.BunchIdSetParceler;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.db.RuleIdParceler;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class AgentEditorActivity extends Activity {

    public static final int REQUEST_CODE_DEFINE_START_ADDER = 1;
    public static final int REQUEST_CODE_DEFINE_END_ADDER = 2;
    public static final int REQUEST_CODE_PICK_TARGET_BUNCH = 3;
    public static final int REQUEST_CODE_PICK_SOURCE_BUNCH = 4;
    public static final int REQUEST_CODE_PICK_DIFF_BUNCH = 5;
    public static final int REQUEST_CODE_PICK_RULE = 6;

    private interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    public static void open(@NonNull Context context, @NonNull Controller controller) {
        ensureNonNull(context, controller);
        final Intent intent = new Intent(context, AgentEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        context.startActivity(intent);
    }

    public static final class State implements Controller.MutableState, Parcelable {
        private ImmutableSet<Object> _targetBunches = ImmutableHashSet.empty();
        private ImmutableSet<Object> _sourceBunches = ImmutableHashSet.empty();
        private ImmutableSet<Object> _diffBunches = ImmutableHashSet.empty();
        private ImmutableList<Correlation.Entry<AlphabetId>> _startMatcher = ImmutableList.empty();
        private ImmutableCorrelationArray<AlphabetId> _startAdder = ImmutableCorrelationArray.empty();
        private ImmutableList<Correlation.Entry<AlphabetId>> _endMatcher = ImmutableList.empty();
        private ImmutableCorrelationArray<AlphabetId> _endAdder = ImmutableCorrelationArray.empty();
        private Object _rule;

        private void writeBunchToParcel(Parcel dest, Object item) {
            if (item instanceof AcceptationDefinition) {
                final AcceptationDefinition definition = (AcceptationDefinition) item;
                CorrelationArrayParceler.write(dest, definition.correlationArray);
                BunchIdSetParceler.write(dest, definition.bunchSet);
            }
            else {
                BunchIdParceler.write(dest, (BunchId) item);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(_targetBunches.size());
            dest.writeInt(_sourceBunches.size());
            dest.writeInt(_diffBunches.size());

            final ImmutableList<Object> bunches = ImmutableListUtils.appendAll(
                    ImmutableListUtils.appendAll(_targetBunches.toList().toImmutable(), _sourceBunches), _diffBunches);

            int typeBitCount = 0;
            int typeFlags = 0;
            int first = 0;
            for (Object bunch : bunches) {
                if (typeBitCount == 32) {
                    dest.writeInt(typeFlags);

                    final int oldFirst = first;
                    first += 32;
                    for (int index = oldFirst; index < first; index++) {
                        writeBunchToParcel(dest, bunches.valueAt(index));
                    }

                    typeBitCount = 0;
                    typeFlags = 0;
                }

                if (bunch instanceof AcceptationDefinition) {
                    typeFlags |= 1 << typeBitCount;
                }
                typeBitCount++;
            }
            dest.writeInt(typeFlags);

            for (int index = first; index < first + typeBitCount; index++) {
                writeBunchToParcel(dest, bunches.valueAt(index));
            }

            dest.writeInt(_startMatcher.size());
            for (Correlation.Entry<AlphabetId> entry : _startMatcher) {
                AlphabetIdParceler.write(dest, entry.alphabet);
                dest.writeString(entry.text);
            }
            CorrelationArrayParceler.write(dest, _startAdder);

            dest.writeInt(_endMatcher.size());
            for (Correlation.Entry<AlphabetId> entry : _endMatcher) {
                AlphabetIdParceler.write(dest, entry.alphabet);
                dest.writeString(entry.text);
            }
            CorrelationArrayParceler.write(dest, _endAdder);

            dest.writeInt((_rule instanceof RuleId)? 0 : (_rule instanceof AcceptationDefinition)? 1 : 2);
            if (_rule instanceof AcceptationDefinition) {
                final AcceptationDefinition definition = (AcceptationDefinition) _rule;
                CorrelationArrayParceler.write(dest, definition.correlationArray);
                BunchIdSetParceler.write(dest, definition.bunchSet);
            }
            else if (_rule instanceof RuleId) {
                RuleIdParceler.write(dest, (RuleId) _rule);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<State> CREATOR = new Creator<State>() {
            private Object readItemFromParcel(Parcel in, boolean isDefinition) {
                if (isDefinition) {
                    final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(in);
                    final ImmutableSet<BunchId> bunchSet = BunchIdSetParceler.read(in);
                    return new AcceptationDefinition(correlationArray, bunchSet);
                }
                else {
                    return BunchIdParceler.read(in);
                }
            }

            @Override
            public State createFromParcel(Parcel in) {
                final State state = new State();
                final int targetBunchCount = in.readInt();
                final int sourceBunchCount = in.readInt();
                final int diffBunchCount = in.readInt();
                final int totalBunchCount = targetBunchCount + sourceBunchCount + diffBunchCount;
                final MutableList<Object> bunches = MutableList.empty(new MinimumSizeArrayLengthFunction(totalBunchCount));

                int index = 0;
                while (index < totalBunchCount) {
                    int typeFlags = in.readInt();
                    final int first = index;
                    for (index = first; index < totalBunchCount && index < first + 32; index++) {
                        bunches.append(readItemFromParcel(in, (typeFlags & 1) != 0));
                        typeFlags >>>= 1;
                    }
                }

                if (targetBunchCount > 0) {
                    state._targetBunches = ListUtils.slice(bunches, new ImmutableIntRange(0, targetBunchCount - 1)).toSet().toImmutable();
                }

                if (sourceBunchCount > 0) {
                    state._sourceBunches = ListUtils.slice(bunches, new ImmutableIntRange(targetBunchCount, sourceBunchCount + targetBunchCount - 1)).toSet().toImmutable();
                }

                if (diffBunchCount > 0) {
                    state._diffBunches = ListUtils.slice(bunches, new ImmutableIntRange(sourceBunchCount + targetBunchCount, totalBunchCount - 1)).toSet().toImmutable();
                }

                final int startMatcherSize = in.readInt();
                final ImmutableList.Builder<Correlation.Entry<AlphabetId>> startMatcherBuilder = new ImmutableList.Builder<>(new MinimumSizeArrayLengthFunction(startMatcherSize));
                for (int i = 0; i < startMatcherSize; i++) {
                    final AlphabetId alphabet = AlphabetIdParceler.read(in);
                    final String text = in.readString();
                    startMatcherBuilder.append(new Correlation.Entry<>(alphabet, text));
                }
                state._startMatcher = startMatcherBuilder.build();
                state._startAdder = CorrelationArrayParceler.read(in);

                final int endMatcherSize = in.readInt();
                final ImmutableList.Builder<Correlation.Entry<AlphabetId>> endMatcherBuilder = new ImmutableList.Builder<>(new MinimumSizeArrayLengthFunction(startMatcherSize));
                for (int i = 0; i < endMatcherSize; i++) {
                    final AlphabetId alphabet = AlphabetIdParceler.read(in);
                    final String text = in.readString();
                    endMatcherBuilder.append(new Correlation.Entry<>(alphabet, text));
                }
                state._endMatcher = endMatcherBuilder.build();
                state._endAdder = CorrelationArrayParceler.read(in);

                final int ruleState = in.readInt();
                if (ruleState == 0) {
                    state._rule = RuleIdParceler.read(in);
                }
                else if (ruleState == 1) {
                    final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(in);
                    final ImmutableSet<BunchId> bunchSet = BunchIdSetParceler.read(in);
                    state._rule = new AcceptationDefinition(correlationArray, bunchSet);
                }

                return state;
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };

        @NonNull
        @Override
        public ImmutableSet<Object> getTargetBunches() {
            return _targetBunches;
        }

        @NonNull
        @Override
        public ImmutableSet<Object> getSourceBunches() {
            return _sourceBunches;
        }

        @NonNull
        @Override
        public ImmutableSet<Object> getDiffBunches() {
            return _diffBunches;
        }

        @NonNull
        @Override
        public ImmutableList<Correlation.Entry<AlphabetId>> getStartMatcher() {
            return _startMatcher;
        }

        @NonNull
        @Override
        public ImmutableCorrelationArray<AlphabetId> getStartAdder() {
            return _startAdder;
        }

        @NonNull
        @Override
        public ImmutableList<Correlation.Entry<AlphabetId>> getEndMatcher() {
            return _endMatcher;
        }

        @NonNull
        @Override
        public ImmutableCorrelationArray<AlphabetId> getEndAdder() {
            return _endAdder;
        }

        @Override
        public Object getRule() {
            return _rule;
        }

        @Override
        public void setTargetBunches(@NonNull ImmutableSet<Object> bunches) {
            ensureNonNull(bunches);
            ensureValidArguments(TraversableUtils.allMatch(bunches, bunch -> bunch instanceof BunchId || bunch instanceof AcceptationDefinition));
            _targetBunches = bunches;
        }

        @Override
        public void setSourceBunches(@NonNull ImmutableSet<Object> bunches) {
            ensureNonNull(bunches);
            ensureValidArguments(TraversableUtils.allMatch(bunches, bunch -> bunch instanceof BunchId || bunch instanceof AcceptationDefinition));
            _sourceBunches = bunches;
        }

        @Override
        public void setDiffBunches(@NonNull ImmutableSet<Object> bunches) {
            ensureNonNull(bunches);
            ensureValidArguments(TraversableUtils.allMatch(bunches, bunch -> bunch instanceof BunchId || bunch instanceof AcceptationDefinition));
            _diffBunches = bunches;
        }

        @Override
        public void setStartMatcher(@NonNull ImmutableList<Correlation.Entry<AlphabetId>> matcher) {
            ensureNonNull(matcher);
            _startMatcher = matcher;
        }

        @Override
        public void setStartAdder(@NonNull ImmutableCorrelationArray<AlphabetId> adder) {
            ensureNonNull(adder);
            _startAdder = adder;
        }

        @Override
        public void setEndMatcher(@NonNull ImmutableList<Correlation.Entry<AlphabetId>> matcher) {
            ensureNonNull(matcher);
            _endMatcher = matcher;
        }

        @Override
        public void setEndAdder(@NonNull ImmutableCorrelationArray<AlphabetId> adder) {
            ensureNonNull(adder);
            _endAdder = adder;
        }

        @Override
        public void setRule(Object rule) {
            ensureValidArguments(rule == null || rule instanceof RuleId || rule instanceof AcceptationDefinition);
            _rule = rule;
        }
    }

    private Controller _controller;
    private final Presenter _presenter = new DefaultPresenter(this);
    private State _state;

    private AlphabetId _preferredAlphabet;
    private ImmutableMap<AlphabetId, String> _alphabets;
    private boolean _enabledFlagAndRuleFields;

    private LinearLayout _targetBunchesContainer;
    private LinearLayout _sourceBunchesContainer;
    private LinearLayout _diffBunchesContainer;
    private LinearLayout _startMatchersContainer;
    private LinearLayout _startAdderEntry;
    private LinearLayout _endMatchersContainer;
    private LinearLayout _endAdderEntry;

    private void updateBunchSet(
            @NonNull LangbookDbChecker checker,
            @NonNull ViewGroup container,
            @NonNull Supplier<ImmutableSet<Object>> bunchesSupplier,
            @NonNull IntProcedure remover) {
        final ImmutableSet<Object> bunches = bunchesSupplier.supply();
        final int currentBunchViewCount = container.getChildCount();
        final int stateBunchCount = bunches.size();

        for (int i = currentBunchViewCount - 1; i >= stateBunchCount; i--) {
            container.removeViewAt(i);
        }

        for (int i = 0; i < stateBunchCount; i++) {
            final Object bunch = bunches.valueAt(i);
            if (i < currentBunchViewCount) {
                bindBunch(container.getChildAt(i), checker, bunch, () -> removeBunch(container, bunchesSupplier, bunch, remover));
            }
            else {
                addBunch(checker, bunch, container, bunchesSupplier, remover);
            }
        }
    }

    private void updateMatcherEntries(
            @NonNull ViewGroup container,
            @NonNull Supplier<ImmutableList<Correlation.Entry<AlphabetId>>> entriesSupplier,
            @NonNull IntProcedure remover) {
        final ImmutableList<Correlation.Entry<AlphabetId>> entries = entriesSupplier.supply();
        final int currentEntryViewCount = container.getChildCount();
        final int stateEntryCount = entries.size();

        for (int i = currentEntryViewCount - 1; i >= stateEntryCount; i--) {
            container.removeViewAt(i);
        }

        for (int i = 0; i < stateEntryCount; i++) {
            final Correlation.Entry<AlphabetId> entry = entries.get(i);
            if (i < currentEntryViewCount) {
                bindEntry(container.getChildAt(i), entry, () -> removeEntry(container, entriesSupplier, entry, remover));
            }
            else {
                addEntry(entry, container, entriesSupplier, remover);
            }
        }
    }

    private void setStateValues() {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();

        updateBunchSet(checker, _targetBunchesContainer, _state::getTargetBunches, index -> _state.setTargetBunches(_state.getTargetBunches().removeAt(index)));
        updateBunchSet(checker, _sourceBunchesContainer, _state::getSourceBunches, index -> _state.setSourceBunches(_state.getSourceBunches().removeAt(index)));
        updateBunchSet(checker, _diffBunchesContainer, _state::getDiffBunches, index -> _state.setDiffBunches(_state.getDiffBunches().removeAt(index)));

        updateMatcherEntries(_startMatchersContainer, _state::getStartMatcher, index -> _state.setStartMatcher(_state.getStartMatcher().removeAt(index)));
        bindAdder(_startAdderEntry, _state.getStartAdder());
        updateMatcherEntries(_endMatchersContainer, _state::getEndMatcher, index -> _state.setEndMatcher(_state.getEndMatcher().removeAt(index)));
        bindAdder(_endAdderEntry, _state.getEndAdder());

        if (_state.getStartMatcher().anyMatch(entry -> !TextUtils.isEmpty(entry.text)) || !_state.getStartAdder().isEmpty() ||
                _state.getEndMatcher().anyMatch(entry -> !TextUtils.isEmpty(entry.text)) || !_state.getEndAdder().isEmpty()) {
            enableFlagAndRuleFields();
        }

        final TextView textView = findViewById(R.id.ruleText);
        final Object rule = _state.getRule();
        final String ruleText;
        if (rule instanceof RuleId) {
            ruleText = checker.readConceptText(((RuleId) rule).getConceptId(), _preferredAlphabet);
        }
        else if (rule instanceof AcceptationDefinition) {
            ruleText = ((AcceptationDefinition) rule).correlationArray.getDisplayableText(_preferredAlphabet);
        }
        else {
            ruleText = null;
        }

        textView.setText(ruleText);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_editor_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        _alphabets = checker.readAllAlphabets(_preferredAlphabet);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }
        else {
            _state = new State();
            _controller.setup(_state);
        }

        findViewById(R.id.addTargetBunchButton).setOnClickListener(v -> _controller.pickTargetBunch(_presenter));
        findViewById(R.id.addSourceBunchButton).setOnClickListener(v -> _controller.pickSourceBunch(_presenter));
        findViewById(R.id.addDiffBunchButton).setOnClickListener(v -> _controller.pickDiffBunch(_presenter));

        _targetBunchesContainer = findViewById(R.id.targetBunchesContainer);
        _sourceBunchesContainer = findViewById(R.id.sourceBunchesContainer);
        _diffBunchesContainer = findViewById(R.id.diffBunchesContainer);

        _startMatchersContainer = findViewById(R.id.startMatchersContainer);
        _startAdderEntry = findViewById(R.id.startAdderEntry);
        findViewById(R.id.startAdderRemoveButton).setOnClickListener(v -> {
            _state.setStartAdder(ImmutableCorrelationArray.empty());
            bindAdder(_startAdderEntry, ImmutableCorrelationArray.empty());
        });
        _endMatchersContainer = findViewById(R.id.endMatchersContainer);
        _endAdderEntry = findViewById(R.id.endAdderEntry);
        findViewById(R.id.endAdderRemoveButton).setOnClickListener(v -> {
            _state.setEndAdder(ImmutableCorrelationArray.empty());
            bindAdder(_endAdderEntry, ImmutableCorrelationArray.empty());
        });

        findViewById(R.id.addStartMatcherButton).setOnClickListener(v -> {
            final Correlation.Entry<AlphabetId> entry = new Correlation.Entry<>(_alphabets.keyAt(0), null);
            _state.setStartMatcher(_state.getStartMatcher().append(entry));
            addEntry(entry, _startMatchersContainer, _state::getStartMatcher, index -> _state.setStartMatcher(_state.getStartMatcher().removeAt(index)));
        });

        findViewById(R.id.addStartAdderButton).setOnClickListener(v -> _controller.defineStartAdder(_presenter));
        findViewById(R.id.addEndMatcherButton).setOnClickListener(v -> {
            final Correlation.Entry<AlphabetId> entry = new Correlation.Entry<>(_alphabets.keyAt(0), null);
            _state.setEndMatcher(_state.getEndMatcher().append(entry));
            addEntry(entry, _endMatchersContainer, _state::getEndMatcher, index -> _state.setEndMatcher(_state.getEndMatcher().removeAt(index)));
        });
        findViewById(R.id.addEndAdderButton).setOnClickListener(v -> _controller.defineEndAdder(_presenter));

        findViewById(R.id.ruleChangeButton).setOnClickListener(v -> _controller.pickRule(_presenter));
        findViewById(R.id.saveButton).setOnClickListener(v -> _controller.complete(_presenter, _state));

        setStateValues();
    }

    private void addEntry(
            @NonNull Correlation.Entry<AlphabetId> entry,
            @NonNull ViewGroup container,
            @NonNull Supplier<ImmutableList<Correlation.Entry<AlphabetId>>> entriesSupplier,
            @NonNull IntProcedure remover) {
        getLayoutInflater().inflate(R.layout.agent_editor_correlation_entry, container, true);
        final View view = container.getChildAt(container.getChildCount() - 1);
        bindEntry(view, entry, () -> removeEntry(container, entriesSupplier, entry, remover));
    }

    private void bindEntry(
            @NonNull View view,
            @NonNull Correlation.Entry<AlphabetId> entry,
            @NonNull Runnable remover) {
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

        view.findViewById(R.id.removeButton).setOnClickListener(v -> remover.run());
    }

    private static void removeEntry(
            @NonNull ViewGroup container,
            @NonNull Supplier<ImmutableList<Correlation.Entry<AlphabetId>>> entriesSupplier,
            @NonNull Correlation.Entry<AlphabetId> entry,
            @NonNull IntProcedure remover) {
        final int position = entriesSupplier.supply().indexOf(entry);
        if (position < 0) {
            throw new AssertionError();
        }

        container.removeViewAt(position);
        remover.apply(position);
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

    private void addBunch(
            @NonNull LangbookDbChecker checker,
            @NonNull Object item,
            @NonNull ViewGroup container,
            @NonNull Supplier<ImmutableSet<Object>> bunchesSupplier,
            @NonNull IntProcedure remover) {
        getLayoutInflater().inflate(R.layout.agent_editor_bunch_entry, container, true);
        final View view = container.getChildAt(container.getChildCount() - 1);
        bindBunch(view, checker, item, () -> removeBunch(container, bunchesSupplier, item, remover));
    }

    private void bindBunch(@NonNull View view, @NonNull LangbookDbChecker checker, @NonNull Object bunch, @NonNull Runnable remover) {
        final TextView textView = view.findViewById(R.id.textView);
        final String text;
        if (bunch instanceof BunchId) {
            text = checker.readConceptText(((BunchId) bunch).getConceptId(), _preferredAlphabet);
        }
        else {
            final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
            text = ((AcceptationDefinition) bunch).correlationArray.getDisplayableText(preferredAlphabet);
        }
        textView.setText(text);
        view.findViewById(R.id.removeButton).setOnClickListener(v -> remover.run());
    }

    private void bindAdder(View view, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        if (correlationArray.isEmpty()) {
            view.setVisibility(View.GONE);
        }
        else {
            final String text = CorrelationPickerAdapter.toPlainText(correlationArray, ImmutableHashSet.empty());
            view.<TextView>findViewById(R.id.textView).setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }

    private static void removeBunch(
            @NonNull ViewGroup container,
            @NonNull Supplier<ImmutableSet<Object>> bunchesSupplier,
            @NonNull Object bunch,
            @NonNull IntProcedure remover) {
        final int index = bunchesSupplier.supply().indexOf(bunch);
        if (index < 0) {
            throw new AssertionError();
        }

        container.removeViewAt(index);
        remover.apply(index);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Controller.MutableState innerState = new Controller.MutableState() {
            @Override
            public void setTargetBunches(@NonNull ImmutableSet<Object> bunches) {
                final ImmutableSet<Object> newBunches = bunches.filterNot(_state.getTargetBunches()::contains);
                _state.setTargetBunches(bunches);
                for (Object item : newBunches) {
                    addBunch(DbManager.getInstance().getManager(), item, _targetBunchesContainer, _state::getTargetBunches, index -> _state.setTargetBunches(_state.getTargetBunches().removeAt(index)));
                }
            }

            @Override
            public void setSourceBunches(@NonNull ImmutableSet<Object> bunches) {
                final ImmutableSet<Object> newBunches = bunches.filterNot(_state.getSourceBunches()::contains);
                _state.setSourceBunches(bunches);
                for (Object item : newBunches) {
                    addBunch(DbManager.getInstance().getManager(), item, _sourceBunchesContainer, _state::getSourceBunches, index -> _state.setSourceBunches(_state.getSourceBunches().removeAt(index)));
                }
            }

            @Override
            public void setDiffBunches(@NonNull ImmutableSet<Object> bunches) {
                final ImmutableSet<Object> newBunches = bunches.filterNot(_state.getDiffBunches()::contains);
                _state.setDiffBunches(bunches);
                for (Object item : newBunches) {
                    addBunch(DbManager.getInstance().getManager(), item, _diffBunchesContainer, _state::getDiffBunches, index -> _state.setDiffBunches(_state.getDiffBunches().removeAt(index)));
                }
            }

            @Override
            public void setStartMatcher(@NonNull ImmutableList<Correlation.Entry<AlphabetId>> matcher) {
                _state.setStartMatcher(matcher);
            }

            @Override
            public void setStartAdder(@NonNull ImmutableCorrelationArray<AlphabetId> adder) {
                _state.setStartAdder(adder);
                bindAdder(_startAdderEntry, adder);
                enableFlagAndRuleFields();
            }

            @Override
            public void setEndMatcher(@NonNull ImmutableList<Correlation.Entry<AlphabetId>> matcher) {
                _state.setEndMatcher(matcher);
            }

            @Override
            public void setEndAdder(@NonNull ImmutableCorrelationArray<AlphabetId> adder) {
                _state.setEndAdder(adder);
                bindAdder(_endAdderEntry, adder);
                enableFlagAndRuleFields();
            }

            @Override
            public void setRule(Object rule) {
                _state.setRule(rule);
                final String text;
                if (rule instanceof RuleId) {
                    text = DbManager.getInstance().getManager().readConceptText(((RuleId) rule).getConceptId(), _preferredAlphabet);
                }
                else {
                    text = ((AcceptationDefinition) rule).correlationArray.getDisplayableText(_preferredAlphabet);
                }

                final TextView textView = findViewById(R.id.ruleText);
                textView.setText(text);
            }

            @NonNull
            @Override
            public ImmutableSet<Object> getTargetBunches() {
                return _state.getTargetBunches();
            }

            @NonNull
            @Override
            public ImmutableSet<Object> getSourceBunches() {
                return _state.getSourceBunches();
            }

            @NonNull
            @Override
            public ImmutableSet<Object> getDiffBunches() {
                return _state.getDiffBunches();
            }

            @NonNull
            @Override
            public ImmutableList<Correlation.Entry<AlphabetId>> getStartMatcher() {
                return _state.getStartMatcher();
            }

            @NonNull
            @Override
            public ImmutableCorrelationArray<AlphabetId> getStartAdder() {
                return _state.getStartAdder();
            }

            @NonNull
            @Override
            public ImmutableList<Correlation.Entry<AlphabetId>> getEndMatcher() {
                return _state.getEndMatcher();
            }

            @NonNull
            @Override
            public ImmutableCorrelationArray<AlphabetId> getEndAdder() {
                return _state.getEndAdder();
            }

            @Override
            public Object getRule() {
                return _state.getRule();
            }
        };

        _controller.onActivityResult(this, requestCode, resultCode, data, innerState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    public interface Controller extends Parcelable {
        void setup(@NonNull MutableState state);
        void pickTargetBunch(@NonNull Presenter presenter);
        void pickSourceBunch(@NonNull Presenter presenter);
        void pickDiffBunch(@NonNull Presenter presenter);
        void defineStartAdder(@NonNull Presenter presenter);
        void defineEndAdder(@NonNull Presenter presenter);
        void pickRule(@NonNull Presenter presenter);
        void complete(@NonNull Presenter presenter, @NonNull State state);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, @NonNull MutableState state);

        interface State {
            @NonNull
            ImmutableSet<Object> getTargetBunches();

            @NonNull
            ImmutableSet<Object> getSourceBunches();

            @NonNull
            ImmutableSet<Object> getDiffBunches();

            @NonNull
            ImmutableList<Correlation.Entry<AlphabetId>> getStartMatcher();

            @NonNull
            ImmutableCorrelationArray<AlphabetId> getStartAdder();

            @NonNull
            ImmutableList<Correlation.Entry<AlphabetId>> getEndMatcher();

            @NonNull
            ImmutableCorrelationArray<AlphabetId> getEndAdder();

            Object getRule();
        }

        interface MutableState extends State {
            void setTargetBunches(@NonNull ImmutableSet<Object> bunches);
            void setSourceBunches(@NonNull ImmutableSet<Object> bunches);
            void setDiffBunches(@NonNull ImmutableSet<Object> bunches);

            void setStartMatcher(@NonNull ImmutableList<Correlation.Entry<AlphabetId>> matcher);
            void setStartAdder(@NonNull ImmutableCorrelationArray<AlphabetId> adder);
            void setEndMatcher(@NonNull ImmutableList<Correlation.Entry<AlphabetId>> matcher);
            void setEndAdder(@NonNull ImmutableCorrelationArray<AlphabetId> adder);

            void setRule(Object rule);
        }
    }
}
