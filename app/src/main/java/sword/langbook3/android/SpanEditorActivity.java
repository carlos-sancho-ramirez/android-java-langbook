package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableSet;
import sword.collections.MutableIntValueMap;
import sword.langbook3.android.LangbookReadableDatabase.SentenceSpan;
import sword.langbook3.android.db.Database;

import static sword.langbook3.android.LangbookDbInserter.insertSpan;
import static sword.langbook3.android.LangbookDeleter.deleteSpan;
import static sword.langbook3.android.LangbookReadableDatabase.getAcceptationTexts;
import static sword.langbook3.android.LangbookReadableDatabase.getSentenceSpans;
import static sword.langbook3.android.LangbookReadableDatabase.getSentenceSpansWithIds;
import static sword.langbook3.android.LangbookReadableDatabase.isSymbolArrayMerelyASentence;
import static sword.langbook3.android.SentenceEditorActivity.NO_SYMBOL_ARRAY;

public final class SpanEditorActivity extends Activity implements ActionMode.Callback {

    private static final int REQUEST_CODE_PICK_ACCEPTATION = 1;

    private interface ArgKeys {
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
        String TEXT = BundleKeys.TEXT;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    private TextView _sentenceText;
    private ListView _listView;
    private ActionMode _selectionActionMode;

    private SpanEditorActivityState _state = new SpanEditorActivityState();

    static void open(Activity activity, int requestCode, String text) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        activity.startActivityForResult(intent, requestCode);
    }

    static void open(Activity activity, int requestCode, String text, int symbolArrayId) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        intent.putExtra(ArgKeys.SYMBOL_ARRAY, symbolArrayId);
        activity.startActivityForResult(intent, requestCode);
    }

    private String getText() {
        return getIntent().getStringExtra(ArgKeys.TEXT);
    }

    private SpannableString getRichText() {
        final SpannableString string = new SpannableString(getText());
        final int highlightColor = getResources().getColor(R.color.agentDynamicTextColor);

        final MutableIntValueMap<SentenceSpan> spans = _state.getSpans();
        final int spanCount = _state.getSpans().size();
        for (int spanIndex = 0; spanIndex < spanCount; spanIndex++) {
            if (spans.valueAt(spanIndex) != 0) {
                final SentenceSpan span = spans.keyAt(spanIndex);
                string.setSpan(new ForegroundColorSpan(highlightColor), span.range.min(), span.range.max() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }

        return string;
    }

    private int getSymbolArrayId() {
        return getIntent().getIntExtra(ArgKeys.SYMBOL_ARRAY, NO_SYMBOL_ARRAY);
    }

    private void insertInitialSpans(int symbolArrayId) {
        final Database db = DbManager.getInstance().getDatabase();
        final String sentence = getText();
        final ImmutableSet<SentenceSpan> spans = getSentenceSpans(db, symbolArrayId);
        final MutableIntValueMap<SentenceSpan> builder = _state.getSpans();
        for (SentenceSpan span : spans) {
            final ImmutableIntKeyMap<String> texts = getAcceptationTexts(db, span.acceptation);
            final int mapSize = texts.size();
            int index = 0;
            int mapIndex;
            for (mapIndex = 0; mapIndex < mapSize; mapIndex++) {
                final String text = texts.valueAt(mapIndex);
                index = sentence.indexOf(text);
                if (index >= 0) {
                    break;
                }
            }

            if (mapIndex < mapSize) {
                final ImmutableIntRange range = new ImmutableIntRange(index, index + texts.valueAt(mapIndex).length() - 1);
                final SentenceSpan newSpan = range.equals(span.range)? span : new SentenceSpan(range, span.acceptation);
                builder.put(newSpan, 1);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.span_editor_activity);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        _sentenceText = findViewById(R.id.sentenceText);
        _sentenceText.setText(getRichText());
        _sentenceText.setCustomSelectionActionModeCallback(this);

        _listView = findViewById(R.id.listView);

        final int symbolArrayId = getSymbolArrayId();
        if (symbolArrayId != NO_SYMBOL_ARRAY) {
            insertInitialSpans(symbolArrayId);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        _selectionActionMode = mode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        getMenuInflater().inflate(R.menu.span_editor_selection_actions, menu);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemAddSpan:
                addSpan();
                final ActionMode actionMode = _selectionActionMode;
                _selectionActionMode = null;
                actionMode.finish();
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
        switch (item.getItemId()) {
            case R.id.menuItemConfirm:
                evaluateSpans();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void evaluateSpans() {
        final ImmutableSet<SentenceSpan> spans = _state.getSpans().keySet().toImmutable();
        if (spans.isEmpty()) {
            Toast.makeText(this, R.string.spanEditorNoSpanPresentError, Toast.LENGTH_SHORT).show();
        }
        else {
            final String newText = getText();
            final Database db = DbManager.getInstance().getDatabase();
            final int symbolArrayId = getSymbolArrayId();
            if (symbolArrayId == NO_SYMBOL_ARRAY) {
                final int newSymbolArray = LangbookDatabase.obtainSymbolArray(db, newText);
                for (SentenceSpan span : spans) {
                    insertSpan(db, newSymbolArray, span.range, span.acceptation);
                }

                Toast.makeText(this, R.string.includeSentenceFeedback, Toast.LENGTH_SHORT).show();
            }
            else {
                if (isSymbolArrayMerelyASentence(db, symbolArrayId)) {
                    if (!LangbookDatabase.updateSymbolArray(db, symbolArrayId, newText)) {
                        throw new AssertionError();
                    }

                    final ImmutableIntValueMap<SentenceSpan> dbSpanMap = getSentenceSpansWithIds(db, symbolArrayId);
                    final ImmutableSet<SentenceSpan> dbSpanSet = dbSpanMap.keySet();
                    for (SentenceSpan span : dbSpanSet.filterNot(spans::contains)) {
                        if (!deleteSpan(db, dbSpanMap.get(span))) {
                            throw new AssertionError();
                        }
                    }

                    for (SentenceSpan span : spans.filterNot(dbSpanSet::contains)) {
                        insertSpan(db, symbolArrayId, span.range, span.acceptation);
                    }
                }
                else {
                    final int newSymbolArray = LangbookDatabase.obtainSymbolArray(db, newText);
                    for (int spanId : getSentenceSpansWithIds(db, symbolArrayId)) {
                        if (!deleteSpan(db, spanId)) {
                            throw new AssertionError();
                        }
                    }

                    for (SentenceSpan span : spans) {
                        insertSpan(db, newSymbolArray, span.range, span.acceptation);
                    }
                }

                Toast.makeText(this, R.string.updateSentenceFeedback, Toast.LENGTH_SHORT).show();
            }

            setResult(RESULT_OK);
            finish();
        }
    }

    private void addSpan() {
        final int start = _sentenceText.getSelectionStart();
        final int end = _sentenceText.getSelectionEnd();
        if (start >= 0 && start < end - 1) {
            final ImmutableIntRange range = new ImmutableIntRange(start, end - 1);
            final String query = _sentenceText.getText().toString().substring(start, end);
            _state.setSelection(range);
            FixedTextAcceptationPickerActivity.open(this, REQUEST_CODE_PICK_ACCEPTATION, query);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCEPTATION && resultCode == RESULT_OK && data != null) {
            final int acceptation = data.getIntExtra(FixedTextAcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
            if (acceptation != 0) {
                _state.getSpans().put(new SentenceSpan(_state.getSelection(), acceptation), 1);
                _sentenceText.setText(getRichText());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        _listView.setAdapter(new SpanEditorAdapter(getText(), _state.getSpans(), map -> {
            _sentenceText.setText(getRichText());
        }));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
