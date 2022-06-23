package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdSetParceler;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class DefinitionEditorActivity extends Activity {

    public static final int REQUEST_CODE_PICK_BASE = 1;
    public static final int REQUEST_CODE_PICK_COMPLEMENT = 2;

    private interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String STATE = "state";
    }

    public static void open(Activity activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = new Intent(activity, DefinitionEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private final Presenter _presenter = new DefaultPresenter(this);
    private Controller _controller;
    private State _state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.definition_editor_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.setTitle(this);
        _state = (savedInstanceState == null)? new State() :
                savedInstanceState.getParcelable(SavedKeys.STATE);

        findViewById(R.id.baseConceptChangeButton).setOnClickListener(v -> _controller.pickBaseConcept(_presenter));
        findViewById(R.id.complementsAddButton).setOnClickListener(v -> _controller.pickComplement(_presenter, _state));
        findViewById(R.id.saveButton).setOnClickListener(v -> _controller.complete(_presenter, _state));

        updateUi();
    }

    private String getStateItemText(Object item) {
        if (item instanceof AcceptationId) {
            final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
            return DbManager.getInstance().getManager().getAcceptationDisplayableText((AcceptationId) item, preferredAlphabet);
        }
        else if (item instanceof Controller.AcceptationDefinition) {
            final ImmutableCorrelation<AlphabetId> correlation = ((Controller.AcceptationDefinition) item).correlationArray.concatenateTexts();

            final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
            return correlation.containsKey(preferredAlphabet)? correlation.get(preferredAlphabet) : correlation.valueAt(0);
        }

        return null;
    }
    private void updateUi() {
        final TextView baseConceptTextView = findViewById(R.id.baseConceptText);
        baseConceptTextView.setText(getStateItemText(_state.getBase()));

        final LinearLayout complementsPanel = findViewById(R.id.complementsPanel);
        complementsPanel.removeAllViews();

        final LayoutInflater inflater = LayoutInflater.from(this);
        for (Object complementItem : _state.getComplements()) {
            inflater.inflate(R.layout.definition_editor_complement_entry, complementsPanel, true);
            final TextView textView = complementsPanel.getChildAt(complementsPanel.getChildCount() - 1).findViewById(R.id.text);
            textView.setText(getStateItemText(complementItem));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Controller.MutableState innerState = new Controller.MutableState() {
            @Override
            public void setBase(@NonNull Object item) {
                _state.setBase(item);
                updateUi();
            }

            @Override
            public void addComplement(@NonNull Object item) {
                _state.addComplement(item);
                updateUi();
            }

            @Override
            public Object getBase() {
                return _state.getBase();
            }

            @NonNull
            @Override
            public ImmutableSet<Object> getComplements() {
                return _state.getComplements();
            }
        };
        _controller.onActivityResult(this, requestCode, resultCode, data, innerState);
    }

    @Override
    public void onSaveInstanceState(Bundle outBundle) {
        outBundle.putParcelable(SavedKeys.STATE, _state);
    }

    public static final class State implements Controller.MutableState, Parcelable {
        private Object _base;

        @NonNull
        private ImmutableSet<Object> _complements = ImmutableHashSet.empty();

        @Override
        public Object getBase() {
            return _base;
        }

        @NonNull
        @Override
        public ImmutableSet<Object> getComplements() {
            return _complements;
        }

        @Override
        public void setBase(@NonNull Object item) {
            ensureValidArguments(item instanceof AcceptationId || item instanceof Controller.AcceptationDefinition);
            _base = item;
        }

        @Override
        public void addComplement(@NonNull Object item) {
            ensureValidArguments(item instanceof AcceptationId || item instanceof Controller.AcceptationDefinition);
            _complements = _complements.add(item);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        private void writeItemToParcel(Parcel dest, Object item) {
            if (item instanceof Controller.AcceptationDefinition) {
                final Controller.AcceptationDefinition definition = (Controller.AcceptationDefinition) item;
                CorrelationArrayParceler.write(dest, definition.correlationArray);
                BunchIdSetParceler.write(dest, definition.bunchSet);
            }
            else {
                AcceptationIdParceler.write(dest, (AcceptationId) item);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            final int complementCount = _complements.size();
            dest.writeInt(complementCount);

            int typeBitCount = 1;
            int typeFlags = (_base instanceof Controller.AcceptationDefinition)? 1 : 0;

            int first = -1;
            for (Object complement : _complements) {
                if (typeBitCount == 32) {
                    dest.writeInt(typeFlags);

                    final int oldFirst = first;
                    first += 32;
                    for (int index = oldFirst; index < first; index++) {
                        final Object item = (index == -1)? _base : _complements.valueAt(index);
                        writeItemToParcel(dest, item);
                    }

                    typeBitCount = 0;
                    typeFlags = 0;
                }

                if (complement instanceof Controller.AcceptationDefinition) {
                    typeFlags |= 1 << typeBitCount;
                }
                typeBitCount++;
            }
            dest.writeInt(typeFlags);

            for (int index = first; index < first + typeBitCount; index++) {
                final Object item = (index == -1)? _base : _complements.valueAt(index);
                writeItemToParcel(dest, item);
            }
        }

        public static final Creator<State> CREATOR = new Creator<State>() {
            private Object readItemFromParcel(Parcel in, boolean isDefinition) {
                if (isDefinition) {
                    final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(in);
                    final ImmutableSet<BunchId> bunchSet = BunchIdSetParceler.read(in);
                    return new Controller.AcceptationDefinition(correlationArray, bunchSet);
                }
                else {
                    return AcceptationIdParceler.read(in);
                }
            }

            @Override
            public State createFromParcel(Parcel in) {
                final State state = new State();
                int complementCount = in.readInt();
                int index = -1;
                while (index < complementCount) {
                    int typeFlags = in.readInt();
                    final int first = index;
                    for (index = first; index < complementCount && index < first + 32; index++) {
                        final Object item = readItemFromParcel(in, (typeFlags & 1) != 0);

                        if (index == -1) {
                            state._base = item;
                        }
                        else {
                            state._complements = state._complements.add(item);
                        }

                        typeFlags >>>= 1;
                    }
                }

                return state;
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };
    }

    public interface Controller extends Parcelable {
        void setTitle(@NonNull Activity activity);
        void pickBaseConcept(@NonNull Presenter presenter);
        void pickComplement(@NonNull Presenter presenter, @NonNull State state);
        void complete(@NonNull Presenter presenter, @NonNull State state);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, @NonNull MutableState state);

        final class AcceptationDefinition {
            public final ImmutableCorrelationArray<AlphabetId> correlationArray;
            public final ImmutableSet<BunchId> bunchSet;

            public AcceptationDefinition(
                    @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray,
                    @NonNull ImmutableSet<BunchId> bunchSet) {
                ensureNonNull(correlationArray, bunchSet);
                this.correlationArray = correlationArray;
                this.bunchSet = bunchSet;
            }

            @Override
            public int hashCode() {
                return correlationArray.hashCode();
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                if (!(obj instanceof AcceptationDefinition)) {
                    return false;
                }
                else if (obj == this) {
                    return true;
                }

                final AcceptationDefinition that = (AcceptationDefinition) obj;
                return correlationArray.equals(that.correlationArray) && bunchSet.equalSet(that.bunchSet);
            }
        }

        interface State {
            Object getBase();

            @NonNull
            ImmutableSet<Object> getComplements();
        }

        interface MutableState extends State {
            void setBase(@NonNull Object item);
            void addComplement(@NonNull Object item);
        }
    }
}
