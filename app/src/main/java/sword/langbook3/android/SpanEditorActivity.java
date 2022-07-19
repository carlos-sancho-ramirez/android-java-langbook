package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.IntValueMap;
import sword.collections.MutableIntValueMap;
import sword.collections.MutableIntValueSortedMap;
import sword.langbook3.android.controllers.SpanEditorController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class SpanEditorActivity extends Activity implements ActionMode.Callback {

    public static final int REQUEST_CODE_PICK_ACCEPTATION = 1;

    private interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    interface ResultKeys {
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    private TextView _sentenceText;
    private ListView _listView;

    private final Presenter _presenter = new DefaultPresenter(this);
    private Controller _controller;
    private State _state;

    public static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithConcept(Activity activity, int requestCode, String text, ConceptId concept) {
        open(activity, requestCode, new SpanEditorController(text, null, concept, null));
    }

    static void openWithAcceptation(Activity activity, int requestCode, String text, AcceptationId acceptation) {
        open(activity, requestCode, new SpanEditorController(text, acceptation, null, null));
    }

    static void openWithSentenceId(Activity activity, int requestCode, String text, SentenceId sentenceId) {
        open(activity, requestCode, new SpanEditorController(text, null, null, sentenceId));
    }

    private SpannableString getRichText() {
        final SpannableString string = new SpannableString(_controller.getText());
        final int highlightColor = getResources().getColor(R.color.agentDynamicTextColor);

        final MutableIntValueMap<SentenceSpan<AcceptationId>> spans = _state.getSpans();
        final int spanCount = _state.getSpans().size();
        for (int spanIndex = 0; spanIndex < spanCount; spanIndex++) {
            if (spans.valueAt(spanIndex) != 0) {
                final SentenceSpan<AcceptationId> span = spans.keyAt(spanIndex);
                string.setSpan(new ForegroundColorSpan(highlightColor), span.range.min(), span.range.max() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }

        return string;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.span_editor_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        if (savedInstanceState == null) {
            _state = new State();
            _controller.setup(_state);
        }
        else {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        _sentenceText = findViewById(R.id.sentenceText);
        _sentenceText.setText(getRichText());
        _sentenceText.setCustomSelectionActionModeCallback(this);

        _listView = findViewById(R.id.listView);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        getMenuInflater().inflate(R.menu.span_editor_selection_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Nothing to be done
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.menuItemAddSpan) {
            addSpan();
            mode.finish();
            return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Nothing to be done
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.span_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemConfirm) {
            _controller.complete(_presenter, _state);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void addSpan() {
        final int start = _sentenceText.getSelectionStart();
        final int end = _sentenceText.getSelectionEnd();

        final ImmutableIntRange range = new ImmutableIntRange(start, end - 1);
        final String query = _sentenceText.getText().toString().substring(start, end);

        final Controller.MutableState innerState = new Controller.MutableState() {

            @Override
            public IntValueMap<SentenceSpan<AcceptationId>> getSpans() {
                return _state.getSpans();
            }

            @Override
            public void putSpan(SentenceSpan<AcceptationId> key) {
                _state.putSpan(key);
                _sentenceText.setText(getRichText());
                setAdapter();
            }

            @Override
            public void setSelection(ImmutableIntRange range) {
                _state.setSelection(range);
            }

            @Override
            public ImmutableIntRange getSelection() {
                return _state.getSelection();
            }
        };

        _state.setSelection(range);
        _controller.pickAcceptation(_presenter, innerState, query);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _controller.onActivityResult(this, requestCode, resultCode, data, _state);
    }

    private void setAdapter() {
        _listView.setAdapter(new SpanEditorAdapter(_controller.getText(), _state.getSpans(), map -> _sentenceText.setText(getRichText())));
    }

    @Override
    public void onResume() {
        super.onResume();
        setAdapter();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    public static final class State implements Controller.MutableState, Parcelable {
        private final MutableIntValueMap<SentenceSpan<AcceptationId>> _spans = MutableIntValueSortedMap.empty((a, b) -> a.range.min() < b.range.min());
        private ImmutableIntRange _selection;

        @Override
        public ImmutableIntRange getSelection() {
            return _selection;
        }

        @Override
        public void putSpan(SentenceSpan<AcceptationId> key) {
            _spans.put(key, 1);
        }

        @Override
        public void setSelection(ImmutableIntRange range) {
            if (range != null && range.min() < 0) {
                throw new IllegalArgumentException();
            }

            _selection = range;
        }

        @Override
        public MutableIntValueMap<SentenceSpan<AcceptationId>> getSpans() {
            return _spans;
        }

        public static final Creator<State> CREATOR = new Creator<State>() {
            @Override
            public State createFromParcel(Parcel in) {
                final State state = new State();
                final int start = in.readInt();
                if (start >= 0) {
                    final int end = in.readInt();
                    state.setSelection(new ImmutableIntRange(start, end));
                }

                sentenceSpanSetFromParcel(state.getSpans(), in);
                return state;
            }

            @Override
            public State[] newArray(int size) {
                return new State[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (_selection != null) {
                dest.writeInt(_selection.min());
                dest.writeInt(_selection.max());
            }
            else {
                dest.writeInt(-1);
            }

            writeSentenceSpanSetToParcel(dest);
        }

        public static void writeSpanToParcel(@NonNull SentenceSpan<AcceptationId> span, @NonNull Parcel dest) {
            dest.writeInt(span.range.min());
            dest.writeInt(span.range.max());
            AcceptationIdParceler.write(dest, span.acceptation);
        }

        @NonNull
        public static SentenceSpan<AcceptationId> spanFromParcel(@NonNull Parcel in) {
            final int start = in.readInt();
            final int end = in.readInt();
            final AcceptationId acc = AcceptationIdParceler.read(in);
            return new SentenceSpan<>(new ImmutableIntRange(start, end), acc);
        }

        private static void sentenceSpanSetFromParcel(MutableIntValueMap<SentenceSpan<AcceptationId>> builder, Parcel in) {
            final int size = in.readInt();
            for (int i = 0; i < size; i++) {
                builder.put(spanFromParcel(in), in.readInt());
            }
        }

        private void writeSentenceSpanSetToParcel(@NonNull Parcel dest) {
            final int size = _spans.size();
            dest.writeInt(size);
            for (int i = 0; i < size; i++) {
                writeSpanToParcel(_spans.keyAt(i), dest);
                dest.writeInt(_spans.valueAt(i));
            }
        }
    }

    public interface Controller extends Parcelable {
        @NonNull
        String getText();
        void setup(@NonNull MutableState state);
        void pickAcceptation(@NonNull Presenter presenter, @NonNull MutableState state, @NonNull String query);
        void complete(@NonNull Presenter presenter, @NonNull State state);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, @NonNull MutableState state);

        interface State {
            ImmutableIntRange getSelection();
            IntValueMap<SentenceSpan<AcceptationId>> getSpans();
        }

        interface MutableState extends State {
            void putSpan(SentenceSpan<AcceptationId> key);
            void setSelection(ImmutableIntRange range);
        }
    }
}
