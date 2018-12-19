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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntRange;
import sword.langbook3.android.LangbookReadableDatabase.SentenceSpan;
import sword.langbook3.android.db.Database;

import static sword.langbook3.android.LangbookDbInserter.insertSpan;

public final class SpanEditorActivity extends Activity implements ActionMode.Callback, AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_PICK_ACCEPTATION = 1;

    private interface ArgKeys {
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

    private String getText() {
        return getIntent().getStringExtra(ArgKeys.TEXT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.span_editor_activity);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        _sentenceText = findViewById(R.id.sentenceText);
        _sentenceText.setText(getText());
        _sentenceText.setCustomSelectionActionModeCallback(this);

        _listView = findViewById(R.id.listView);
        _listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        _listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final SentenceSpan span = _state.getSpans().valueAt(position);
        final SpannableString string = new SpannableString(getText());
        final int highlightColor = getResources().getColor(R.color.agentDynamicTextColor);
        string.setSpan(new ForegroundColorSpan(highlightColor), span.range.min(), span.range.max() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        _sentenceText.setText(string);
        _listView.setSelection(position);
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
        if (_state.getSpans().isEmpty()) {
            Toast.makeText(this, R.string.spanEditorNoSpanPresentError, Toast.LENGTH_SHORT).show();
        }
        else {
            final Database db = DbManager.getInstance().getDatabase();
            final int symbolArray = LangbookDatabase.obtainSymbolArray(db, getText());
            for (SentenceSpan span : _state.getSpans()) {
                insertSpan(db, symbolArray, span.range, span.acceptation);
            }

            Toast.makeText(this, R.string.includeSentenceFeedback, Toast.LENGTH_SHORT).show();
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
                _state.composeSpanWithCurrentSelection(acceptation);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        _listView.setAdapter(new SpanEditorAdapter(getText(), _state.getSpans()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
