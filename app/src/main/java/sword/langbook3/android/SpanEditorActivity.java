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
import sword.collections.IntValueMap;
import sword.collections.MutableIntValueMap;
import sword.database.Database;
import sword.langbook3.android.db.LangbookDatabase;
import sword.langbook3.android.models.SentenceSpan;

import static sword.langbook3.android.SentenceEditorActivity.NO_SYMBOL_ARRAY;
import static sword.langbook3.android.db.LangbookDatabase.removeSpan;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationTexts;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceSpans;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSentenceSpansWithIds;
import static sword.langbook3.android.db.LangbookReadableDatabase.isSymbolArrayMerelyASentence;
import static sword.langbook3.android.db.LangbookReadableDatabase.readTextAndDynamicAcceptationsMapFromStaticAcceptation;

public final class SpanEditorActivity extends Activity implements ActionMode.Callback {

    private static final int REQUEST_CODE_PICK_ACCEPTATION = 1;

    private interface ArgKeys {
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
        String TEXT = BundleKeys.TEXT;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    interface ResultKeys {
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
    }

    private TextView _sentenceText;
    private ListView _listView;

    private SpanEditorActivityState _state;

    static void open(Activity activity, int requestCode, String text) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithStaticAcceptation(Activity activity, int requestCode, String text, int staticAcceptation) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        intent.putExtra(ArgKeys.STATIC_ACCEPTATION, staticAcceptation);
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

    private int getStaticAcceptationId() {
        return getIntent().getIntExtra(ArgKeys.STATIC_ACCEPTATION, 0);
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

    private void insertSuggestedSpans(int staticAcceptation) {
        final Database db = DbManager.getInstance().getDatabase();
        final ImmutableIntValueMap<String> map = readTextAndDynamicAcceptationsMapFromStaticAcceptation(db, staticAcceptation);
        final String text = getText();
        for (IntValueMap.Entry<String> entry : map.entries()) {
            final int index = text.indexOf(entry.key());
            if (index >= 0) {
                final ImmutableIntRange range = new ImmutableIntRange(index, index + entry.key().length() - 1);
                _state.getSpans().put(new SentenceSpan(range, entry.value()), 1);
                return;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.span_editor_activity);

        if (savedInstanceState == null) {
            _state = new SpanEditorActivityState();

            final int symbolArrayId = getSymbolArrayId();
            final int staticAcceptation = getStaticAcceptationId();
            if (symbolArrayId != NO_SYMBOL_ARRAY) {
                insertInitialSpans(symbolArrayId);
            }
            else if (staticAcceptation != 0) {
                insertSuggestedSpans(staticAcceptation);
            }
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
            evaluateSpans();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
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
            final int newSymbolArray;
            if (symbolArrayId == NO_SYMBOL_ARRAY) {
                newSymbolArray = LangbookDatabase.obtainSymbolArray(db, newText);
                for (SentenceSpan span : spans) {
                    if (!LangbookDatabase.addSpan(db, newSymbolArray, span.range, span.acceptation)) {
                        throw new AssertionError();
                    }
                }

                Toast.makeText(this, R.string.includeSentenceFeedback, Toast.LENGTH_SHORT).show();
            }
            else {
                if (isSymbolArrayMerelyASentence(db, symbolArrayId)) {
                    newSymbolArray = symbolArrayId;
                    if (!LangbookDatabase.updateSymbolArray(db, symbolArrayId, newText)) {
                        throw new AssertionError();
                    }

                    final ImmutableIntValueMap<SentenceSpan> dbSpanMap = getSentenceSpansWithIds(db, symbolArrayId);
                    final ImmutableSet<SentenceSpan> dbSpanSet = dbSpanMap.keySet();
                    for (SentenceSpan span : dbSpanSet.filterNot(spans::contains)) {
                        if (!removeSpan(db, dbSpanMap.get(span))) {
                            throw new AssertionError();
                        }
                    }

                    for (SentenceSpan span : spans.filterNot(dbSpanSet::contains)) {
                        if (!LangbookDatabase.addSpan(db, symbolArrayId, span.range, span.acceptation)) {
                            throw new AssertionError();
                        }
                    }
                }
                else {
                    newSymbolArray = LangbookDatabase.obtainSymbolArray(db, newText);
                    for (int spanId : getSentenceSpansWithIds(db, symbolArrayId)) {
                        if (!removeSpan(db, spanId)) {
                            throw new AssertionError();
                        }
                    }

                    for (SentenceSpan span : spans) {
                        if (!LangbookDatabase.addSpan(db, newSymbolArray, span.range, span.acceptation)) {
                            throw new AssertionError();
                        }
                    }
                }

                Toast.makeText(this, R.string.updateSentenceFeedback, Toast.LENGTH_SHORT).show();
            }

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(ResultKeys.SYMBOL_ARRAY, newSymbolArray);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    private void addSpan() {
        final int start = _sentenceText.getSelectionStart();
        final int end = _sentenceText.getSelectionEnd();

        final ImmutableIntRange range = new ImmutableIntRange(start, end - 1);
        final String query = _sentenceText.getText().toString().substring(start, end);
        _state.setSelection(range);
        FixedTextAcceptationPickerActivity.open(this, REQUEST_CODE_PICK_ACCEPTATION, query);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCEPTATION && resultCode == RESULT_OK && data != null) {
            final int dynamicAcc = data.getIntExtra(FixedTextAcceptationPickerActivity.ResultKeys.DYNAMIC_ACCEPTATION, 0);
            if (dynamicAcc != 0) {
                _state.getSpans().put(new SentenceSpan(_state.getSelection(), dynamicAcc), 1);
                _sentenceText.setText(getRichText());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        _listView.setAdapter(new SpanEditorAdapter(getText(), _state.getSpans(), map -> _sentenceText.setText(getRichText())));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
