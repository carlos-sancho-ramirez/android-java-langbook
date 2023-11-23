package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;

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

import androidx.annotation.NonNull;

import sword.collections.ImmutableSet;
import sword.langbook3.android.AcceptationDetailsActivity;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.Intentions;
import sword.langbook3.android.R;
import sword.langbook3.android.SentenceDetailsActivity;
import sword.langbook3.android.SentenceDetailsAdapter;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;

public final class SentenceDetailsActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements DialogInterface.OnClickListener, AdapterView.OnItemClickListener {
    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_OPEN_ACCEPTATION = 2;
    private static final int REQUEST_CODE_OPEN_SENTENCE = 3;
    private static final int REQUEST_CODE_NEW = 4;

    public interface ArgKeys {
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    private interface SavedKeys {
        String DISPLAYING_DELETE_DIALOG = "dd";
    }

    private Activity _activity;
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
            AcceptationDetailsActivity.open(_activity, REQUEST_CODE_OPEN_ACCEPTATION, acceptation);
        }
    }

    private SentenceId getSentenceId() {
        return SentenceIdBundler.readAsIntentExtra(_activity.getIntent(), ArgKeys.SENTENCE_ID);
    }

    private void updateSentenceTextView() {
        final String text = _model.text;
        if (text == null) {
            _activity.finish();
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
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.sentence_details_activity);

        _sentenceTextView = activity.findViewById(R.id.sentenceText);
        _listView = activity.findViewById(R.id.listView);

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
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        activity.getMenuInflater().inflate(R.menu.sentence_details_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemEdit) {
            Intentions.editSentence(activity, REQUEST_CODE_EDIT, getSentenceId());
            return true;
        }
        else if (itemId == R.id.menuItemDelete) {
            _displayingDeleteDialog = true;
            showDeleteConfirmationDialog();
            return true;
        }
        else if (itemId == R.id.menuItemLinkSentence) {
            Intentions.addSynonymSentence(activity, REQUEST_CODE_NEW, _model.concept);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SentenceDetailsActivity.open(_activity, REQUEST_CODE_OPEN_SENTENCE, _adapter.getSentenceIdAt(position));
    }

    @Override
    public void onResume(@NonNull Activity activity) {
        _justCreated = false;
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && !_justCreated) {
            updateModelAndUi();
        }
    }

    private void showDeleteConfirmationDialog() {
        _activity.newAlertDialogBuilder()
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

        showFeedback(_activity.getString(R.string.deleteSentenceFeedback));
        _activity.setResult(RESULT_OK);
        _activity.finish();
    }

    private void showFeedback(String message) {
        _activity.showToast(message);
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outBundle) {
        if (_displayingDeleteDialog) {
            outBundle.putBoolean(SavedKeys.DISPLAYING_DELETE_DIALOG, true);
        }
    }
}
