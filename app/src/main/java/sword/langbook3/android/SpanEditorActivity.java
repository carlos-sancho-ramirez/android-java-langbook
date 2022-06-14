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

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntList;
import sword.collections.MutableIntValueMap;
import sword.collections.Traverser;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdBundler;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;
import sword.langbook3.android.models.SentenceSpan;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

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

    static void openWithConcept(Activity activity, int requestCode, String text, ConceptId concept) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        ConceptIdBundler.writeAsIntentExtra(intent, ArgKeys.CONCEPT, concept);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithAcceptation(Activity activity, int requestCode, String text, AcceptationId acceptation) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithSentenceId(Activity activity, int requestCode, String text, SentenceId sentenceId) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        SentenceIdBundler.writeAsIntentExtra(intent, ArgKeys.SENTENCE_ID, sentenceId);
        activity.startActivityForResult(intent, requestCode);
    }

    private String getText() {
        return getIntent().getStringExtra(ArgKeys.TEXT);
    }

    private SpannableString getRichText() {
        final SpannableString string = new SpannableString(getText());
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

    private SentenceId getSentenceId() {
        return SentenceIdBundler.readAsIntentExtra(getIntent(), ArgKeys.SENTENCE_ID);
    }

    private AcceptationId getAcceptationId() {
        return AcceptationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ACCEPTATION);
    }

    private ConceptId getConcept() {
        return ConceptIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CONCEPT);
    }

    // We should prevent having sentences without neither spans nor other sentence sharing the same meaning,
    // as it will be not possible to reference them within the app.
    private boolean shouldAllowNoSpans() {
        return getSentenceId() != null || getConcept() != null;
    }

    private void insertInitialSpans(SentenceId sentenceId) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final String sentence = getText();
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = checker.getSentenceSpans(sentenceId);
        final MutableIntValueMap<SentenceSpan<AcceptationId>> builder = _state.getSpans();
        for (SentenceSpan<AcceptationId> span : spans) {
            final ImmutableList<CorrelationId> correlationIds = checker.getAcceptationCorrelationArray(span.acceptation);
            final Traverser<CorrelationId> traverser = correlationIds.iterator();

            final ImmutableCorrelation<AlphabetId> firstCorrelation = checker.getCorrelationWithText(traverser.next());
            final MutableHashSet<ImmutableIntRange> matchingRanges = MutableHashSet.empty();
            for (String correlationText : firstCorrelation.toSet()) {
                final MutableIntList matchingIndexes = MutableIntList.empty();
                int index = 0;
                while (index >= 0) {
                    index = sentence.indexOf(correlationText, index);
                    if (index >= 0) {
                        matchingIndexes.append(index);
                        index++;
                    }
                }
                matchingRanges.addAll(matchingIndexes.map(start -> new ImmutableIntRange(start, start + correlationText.length() - 1)));
            }

            while (traverser.hasNext() && !matchingRanges.isEmpty()) {
                final ImmutableSet<String> correlationTexts = checker.getCorrelationWithText(traverser.next()).toSet();
                for (ImmutableIntRange range : matchingRanges.donate()) {
                    for (String correlationText : correlationTexts) {
                        if (sentence.substring(range.max() + 1).startsWith(correlationText)) {
                            matchingRanges.add(new ImmutableIntRange(range.min(), range.max() + correlationText.length()));
                        }
                    }
                }
            }

            if (matchingRanges.size() == 1) {
                final ImmutableIntRange range = matchingRanges.valueAt(0);
                final SentenceSpan<AcceptationId> newSpan = range.equals(span.range)? span : new SentenceSpan<>(range, span.acceptation);
                builder.put(newSpan, 1);
            }
        }
    }

    private void insertSuggestedSpans(AcceptationId acceptation) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableMap<String, AcceptationId> map = checker.readTextAndDynamicAcceptationsMapFromAcceptation(acceptation);
        final String text = getText();
        for (Map.Entry<String, AcceptationId> entry : map.entries()) {
            final int index = text.indexOf(entry.key());
            if (index >= 0) {
                final ImmutableIntRange range = new ImmutableIntRange(index, index + entry.key().length() - 1);
                _state.getSpans().put(new SentenceSpan<>(range, entry.value()), 1);
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

            final SentenceId sentenceId = getSentenceId();
            final AcceptationId acceptation = getAcceptationId();
            if (sentenceId != null) {
                insertInitialSpans(sentenceId);
            }
            else if (acceptation != null) {
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
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = _state.getSpans().filter(v -> v != 0).keySet().toImmutable();

        if (spans.isEmpty() && !shouldAllowNoSpans()) {
            Toast.makeText(this, R.string.spanEditorNoSpanPresentError, Toast.LENGTH_SHORT).show();
        }
        else {
            final String newText = getText();
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final SentenceId sentenceId = getSentenceId();
            if (sentenceId == null) {
                ConceptId concept = getConcept();
                if (concept == null) {
                    concept = manager.getNextAvailableConceptId();
                }

                final SentenceId newSentenceId = manager.addSentence(concept, newText, spans);
                Toast.makeText(this, R.string.includeSentenceFeedback, Toast.LENGTH_SHORT).show();

                final Intent intent = new Intent();
                SentenceIdBundler.writeAsIntentExtra(intent, ResultKeys.SENTENCE_ID, newSentenceId);
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
        Intentions.addSentenceSpan(this, REQUEST_CODE_PICK_ACCEPTATION, query);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCEPTATION && resultCode == RESULT_OK && data != null) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, BundleKeys.ACCEPTATION);
            ensureNonNull(acceptation);
            _state.getSpans().put(new SentenceSpan<>(_state.getSelection(), acceptation), 1);
            _sentenceText.setText(getRichText());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        _listView.setAdapter(new SpanEditorAdapter(getText(), _state.getSpans(), map -> _sentenceText.setText(getRichText())));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
