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

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntValueMap;
import sword.collections.MutableIntValueMap;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.SentenceSpan;

import static sword.langbook3.android.SentenceEditorActivity.NO_SENTENCE_ID;

public final class SpanEditorActivity extends Activity implements ActionMode.Callback {

    private static final int REQUEST_CODE_PICK_ACCEPTATION = 1;

    private interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CONCEPT = BundleKeys.CONCEPT;
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
        String TEXT = BundleKeys.TEXT;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    interface ResultKeys {
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    private TextView _sentenceText;
    private ListView _listView;

    private SpanEditorActivityState _state;

    static void openWithConcept(Activity activity, int requestCode, String text, int concept) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        intent.putExtra(ArgKeys.CONCEPT, concept);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithAcceptation(Activity activity, int requestCode, String text, int acceptation) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithSentenceId(Activity activity, int requestCode, String text, int sentenceId) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        intent.putExtra(ArgKeys.SENTENCE_ID, sentenceId);
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

    private int getSentenceId() {
        return getIntent().getIntExtra(ArgKeys.SENTENCE_ID, NO_SENTENCE_ID);
    }

    private int getAcceptationId() {
        return getIntent().getIntExtra(ArgKeys.ACCEPTATION, 0);
    }

    private int getConcept() {
        return getIntent().getIntExtra(ArgKeys.CONCEPT, 0);
    }

    // We should prevent having sentences without neither spans nor other sentence sharing the same meaning,
    // as it will be not possible to reference them within the app.
    private boolean shouldAllowNoSpans() {
        return getSentenceId() != NO_SENTENCE_ID || getConcept() != 0;
    }

    private void insertInitialSpans(int sentenceId) {
        final LangbookChecker checker = DbManager.getInstance().getManager();
        final String sentence = getText();
        final ImmutableSet<SentenceSpan> spans = checker.getSentenceSpans(sentenceId);
        final MutableIntValueMap<SentenceSpan> builder = _state.getSpans();
        for (SentenceSpan span : spans) {
            final ImmutableMap<AlphabetId, String> texts = checker.getAcceptationTexts(span.acceptation);
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

    private void insertSuggestedSpans(int acceptation) {
        final LangbookChecker checker = DbManager.getInstance().getManager();
        final ImmutableIntValueMap<String> map = checker.readTextAndDynamicAcceptationsMapFromAcceptation(acceptation);
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

            final int sentenceId = getSentenceId();
            final int acceptation = getAcceptationId();
            if (sentenceId != NO_SENTENCE_ID) {
                insertInitialSpans(sentenceId);
            }
            else if (acceptation != 0) {
                insertSuggestedSpans(acceptation);
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
        final ImmutableSet<SentenceSpan> spans = _state.getSpans().filter(v -> v != 0).keySet().toImmutable();

        if (spans.isEmpty() && !shouldAllowNoSpans()) {
            Toast.makeText(this, R.string.spanEditorNoSpanPresentError, Toast.LENGTH_SHORT).show();
        }
        else {
            final String newText = getText();
            final LangbookManager manager = DbManager.getInstance().getManager();
            final int sentenceId = getSentenceId();
            if (sentenceId == NO_SENTENCE_ID) {
                int concept = getConcept();
                if (concept == 0) {
                    concept = manager.getMaxConcept() + 1;
                }

                final int newSentenceId = manager.addSentence(concept, newText, spans);
                Toast.makeText(this, R.string.includeSentenceFeedback, Toast.LENGTH_SHORT).show();

                final Intent intent = new Intent();
                intent.putExtra(ResultKeys.SENTENCE_ID, newSentenceId);
                setResult(RESULT_OK, intent);
            }
            else {
                if (!manager.updateSentenceTextAndSpans(sentenceId, newText, spans)) {
                    throw new AssertionError();
                }

                Toast.makeText(this, R.string.updateSentenceFeedback, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
            }

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
