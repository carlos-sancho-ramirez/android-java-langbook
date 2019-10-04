package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.models.SentenceSpan;

public final class SentenceDetailsActivity extends Activity implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_OPEN_ACCEPTATION = 2;
    private static final int REQUEST_CODE_OPEN_SENTENCE = 3;
    private static final int REQUEST_CODE_NEW = 4;

    interface ArgKeys {
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
    }

    private interface SavedKeys {
        String DISPLAYING_DELETE_DIALOG = "dd";
    }

    static void open(Activity activity, int requestCode, int symbolArray) {
        final Intent intent = new Intent(activity, SentenceDetailsActivity.class);
        intent.putExtra(ArgKeys.SYMBOL_ARRAY, symbolArray);

        if (requestCode != 0) {
            activity.startActivityForResult(intent, requestCode);
        }
        else {
            activity.startActivity(intent);
        }
    }

    private TextView _sentenceTextView;
    private ListView _listView;
    private boolean _justCreated;

    private boolean _displayingDeleteDialog;

    private class ClickableSentenceSpan extends ClickableSpan {
        private final int staticAcceptation;
        private final int dynamicAcceptation;

        ClickableSentenceSpan(int staticAcceptation, int dynamicAcceptation) {
            this.staticAcceptation = staticAcceptation;
            this.dynamicAcceptation = dynamicAcceptation;
        }

        @Override
        public void onClick(View widget) {
            AcceptationDetailsActivity.open(SentenceDetailsActivity.this,
                    REQUEST_CODE_OPEN_ACCEPTATION, staticAcceptation, dynamicAcceptation, false);
        }
    }

    private int getSymbolArrayId() {
        return getIntent().getIntExtra(ArgKeys.SYMBOL_ARRAY, 0);
    }

    private void updateSentenceTextView() {
        final int symbolArrayId = getSymbolArrayId();
        final LangbookChecker checker = DbManager.getInstance().getManager();
        final String text = checker.getSymbolArray(symbolArrayId);

        if (text == null) {
            finish();
        }
        else {
            final ImmutableSet<SentenceSpan> spans = checker.getSentenceSpans(symbolArrayId);

            final SpannableString string = new SpannableString(text);
            for (SentenceSpan span : spans) {
                final int staticAcceptation = checker.getStaticAcceptationFromDynamic(span.acceptation);
                string.setSpan(new ClickableSentenceSpan(staticAcceptation, span.acceptation),
                        span.range.min(), span.range.max() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            _sentenceTextView.setText(string);
            _sentenceTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void updateOtherSentences() {
        final LangbookChecker checker = DbManager.getInstance().getManager();
        final ImmutableIntSet others = checker.findSentenceIdsMatchingMeaning(getSymbolArrayId());
        final ImmutableIntKeyMap<String> sentences = others.assign(checker::getSymbolArray);
        _listView.setAdapter(new SentenceDetailsAdapter(sentences));
        _listView.setOnItemClickListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_details_activity);

        _sentenceTextView = findViewById(R.id.sentenceText);
        _listView = findViewById(R.id.listView);

        if (savedInstanceState != null) {
            _displayingDeleteDialog = savedInstanceState.getBoolean(SavedKeys.DISPLAYING_DELETE_DIALOG);
        }

        updateSentenceTextView();
        updateOtherSentences();
        _justCreated = true;

        if (_displayingDeleteDialog) {
            showDeleteConfirmationDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.sentence_details_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemEdit:
                SentenceEditorActivity.open(this, REQUEST_CODE_EDIT, getSymbolArrayId());
                return true;
            case R.id.menuItemDelete:
                _displayingDeleteDialog = true;
                showDeleteConfirmationDialog();
                return true;
            case R.id.menuItemLinkSentence:
                SentenceEditorActivity.open(this, REQUEST_CODE_NEW);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SentenceDetailsActivity.open(this, REQUEST_CODE_OPEN_SENTENCE, (int) id);
    }

    @Override
    public void onResume() {
        super.onResume();
        _justCreated = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NEW && resultCode == RESULT_OK) {
            final int pickedSentence = data.getIntExtra(SentenceEditorActivity.ResultKeys.SYMBOL_ARRAY, 0);
            final int thisSentence = getSymbolArrayId();
            if (pickedSentence != 0 && pickedSentence != thisSentence) {
                DbManager.getInstance().getManager().copySentenceMeaning(thisSentence, pickedSentence);
                updateOtherSentences();
            }
        }

        if (resultCode == RESULT_OK && !_justCreated) {
            updateSentenceTextView();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteSentenceConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _displayingDeleteDialog = false)
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (!DbManager.getInstance().getManager().removeSentence(getSymbolArrayId())) {
            throw new AssertionError();
        }

        showFeedback(getString(R.string.deleteSentenceFeedback));
        setResult(RESULT_OK);
        finish();
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle outBundle) {
        super.onSaveInstanceState(outBundle);
        if (_displayingDeleteDialog) {
            outBundle.putBoolean(SavedKeys.DISPLAYING_DELETE_DIALOG, true);
        }
    }
}
