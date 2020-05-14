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

import sword.collections.ImmutableSet;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

public final class SentenceDetailsActivity extends Activity implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_OPEN_ACCEPTATION = 2;
    private static final int REQUEST_CODE_OPEN_SENTENCE = 3;
    private static final int REQUEST_CODE_NEW = 4;

    interface ArgKeys {
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    private interface SavedKeys {
        String DISPLAYING_DELETE_DIALOG = "dd";
    }

    static void open(Activity activity, int requestCode, int sentenceId) {
        final Intent intent = new Intent(activity, SentenceDetailsActivity.class);
        intent.putExtra(ArgKeys.SENTENCE_ID, sentenceId);

        if (requestCode != 0) {
            activity.startActivityForResult(intent, requestCode);
        }
        else {
            activity.startActivity(intent);
        }
    }

    private TextView _sentenceTextView;
    private ListView _listView;
    private SentenceDetailsModel _model;
    private boolean _justCreated;

    private boolean _displayingDeleteDialog;

    private final class ClickableSentenceSpan extends ClickableSpan {
        private final int staticAcceptation;

        ClickableSentenceSpan(int staticAcceptation) {
            this.staticAcceptation = staticAcceptation;
        }

        @Override
        public void onClick(View widget) {
            AcceptationDetailsActivity.open(SentenceDetailsActivity.this,
                    REQUEST_CODE_OPEN_ACCEPTATION, staticAcceptation, false);
        }
    }

    private int getSentenceId() {
        return getIntent().getIntExtra(ArgKeys.SENTENCE_ID, 0);
    }

    private void updateSentenceTextView() {
        final String text = _model.text;
        if (text == null) {
            finish();
        }
        else {
            final ImmutableSet<SentenceSpan> spans = _model.spans;

            final SpannableString string = new SpannableString(text);
            final LangbookChecker checker = DbManager.getInstance().getManager();
            for (SentenceSpan span : spans) {
                final int staticAcceptation = checker.getStaticAcceptationFromDynamic(span.acceptation);
                string.setSpan(new ClickableSentenceSpan(staticAcceptation),
                        span.range.min(), span.range.max() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            _sentenceTextView.setText(string);
            _sentenceTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void updateOtherSentences() {
        _listView.setAdapter(new SentenceDetailsAdapter(_model.sameMeaningSentences));
        _listView.setOnItemClickListener(this);
    }

    private void updateModelAndUi() {
        _model = DbManager.getInstance().getManager().getSentenceDetails(getSentenceId());
        updateSentenceTextView();
        updateOtherSentences();
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

        updateModelAndUi();
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
                SentenceEditorActivity.openWithSentenceId(this, REQUEST_CODE_EDIT, getSentenceId());
                return true;
            case R.id.menuItemDelete:
                _displayingDeleteDialog = true;
                showDeleteConfirmationDialog();
                return true;
            case R.id.menuItemLinkSentence:
                SentenceEditorActivity.openWithConcept(this, REQUEST_CODE_NEW, _model.concept);
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
        if (resultCode == RESULT_OK && !_justCreated) {
            updateModelAndUi();
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
        if (!DbManager.getInstance().getManager().removeSentence(getSentenceId())) {
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
