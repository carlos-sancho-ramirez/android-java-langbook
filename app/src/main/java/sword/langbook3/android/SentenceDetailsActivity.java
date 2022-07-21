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

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;
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

    static void open(Activity activity, int requestCode, SentenceId sentenceId) {
        final Intent intent = new Intent(activity, SentenceDetailsActivity.class);
        SentenceIdBundler.writeAsIntentExtra(intent, ArgKeys.SENTENCE_ID, sentenceId);

        if (requestCode != 0) {
            activity.startActivityForResult(intent, requestCode);
        }
        else {
            activity.startActivity(intent);
        }
    }

    private TextView _sentenceTextView;
    private SentenceDetailsAdapter _adapter;
    private ListView _listView;
    private SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> _model;
    private boolean _justCreated;

    private boolean _displayingDeleteDialog;

    private final class ClickableSentenceSpan extends ClickableSpan {
        private final AcceptationId acceptation;

        ClickableSentenceSpan(AcceptationId acceptation) {
            this.acceptation = acceptation;
        }

        @Override
        public void onClick(@NonNull View widget) {
            AcceptationDetailsActivity.open(SentenceDetailsActivity.this,
                    REQUEST_CODE_OPEN_ACCEPTATION, acceptation);
        }
    }

    private SentenceId getSentenceId() {
        return SentenceIdBundler.readAsIntentExtra(getIntent(), ArgKeys.SENTENCE_ID);
    }

    private void updateSentenceTextView() {
        final String text = _model.text;
        if (text == null) {
            finish();
        }
        else {
            final ImmutableSet<SentenceSpan<AcceptationId>> spans = _model.spans;

            final SpannableString string = new SpannableString(text);
            for (SentenceSpan<AcceptationId> span : spans) {
                string.setSpan(new ClickableSentenceSpan(span.acceptation),
                        span.range.min(), span.range.max() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            _sentenceTextView.setText(string);
            _sentenceTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void updateOtherSentences() {
        _adapter = new SentenceDetailsAdapter(_model.sameMeaningSentences);
        _listView.setAdapter(_adapter);
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
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemEdit) {
            Intentions.editSentence(this, REQUEST_CODE_EDIT, getSentenceId());
            return true;
        }
        else if (itemId == R.id.menuItemDelete) {
            _displayingDeleteDialog = true;
            showDeleteConfirmationDialog();
            return true;
        }
        else if (itemId == R.id.menuItemLinkSentence) {
            Intentions.addSynonymSentence(this, REQUEST_CODE_NEW, _model.concept);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SentenceDetailsActivity.open(this, REQUEST_CODE_OPEN_SENTENCE, _adapter.getSentenceIdAt(position));
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
