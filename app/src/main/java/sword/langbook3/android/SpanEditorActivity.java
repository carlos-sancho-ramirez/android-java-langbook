package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public final class SpanEditorActivity extends Activity implements ActionMode.Callback {

    private static final int REQUEST_CODE_PICK_ACCEPTATION = 1;

    private interface ArgKeys {
        String TEXT = BundleKeys.TEXT;
    }

    private TextView _sentenceText;
    private ActionMode _selectionActionMode;

    static void open(Activity activity, int requestCode, String text) {
        final Intent intent = new Intent(activity, SpanEditorActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.span_editor_activity);

        _sentenceText = findViewById(R.id.sentenceText);
        _sentenceText.setText(getIntent().getStringExtra(ArgKeys.TEXT));
        _sentenceText.setCustomSelectionActionModeCallback(this);
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
                Toast.makeText(this, "Confirmed", Toast.LENGTH_SHORT).show();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void addSpan() {
        final int start = _sentenceText.getSelectionStart();
        final int end = _sentenceText.getSelectionEnd();
        if (start >= 0 && start < end) {
            final String query = _sentenceText.getText().toString().substring(start, end);
            FixedTextAcceptationPickerActivity.open(this, REQUEST_CODE_PICK_ACCEPTATION, query);
        }
    }
}
