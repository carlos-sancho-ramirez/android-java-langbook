package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdBundler;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_ADD_SPAN = 1;

    interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CONCEPT = BundleKeys.CONCEPT;
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    interface ResultKeys {
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    static void openWithConcept(Activity activity, int requestCode, ConceptId concept) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        ConceptIdBundler.writeAsIntentExtra(intent, ArgKeys.CONCEPT, concept);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithAcceptation(Activity activity, int requestCode, AcceptationId acceptation) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithSentenceId(Activity activity, int requestCode, SentenceId sentenceId) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        SentenceIdBundler.writeAsIntentExtra(intent, ArgKeys.SENTENCE_ID, sentenceId);
        activity.startActivityForResult(intent, requestCode);
    }

    private EditText _textField;

    private SentenceId getSentenceId() {
        return SentenceIdBundler.readAsIntentExtra(getIntent(), ArgKeys.SENTENCE_ID);
    }

    private AcceptationId getAcceptationId() {
        return AcceptationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ACCEPTATION);
    }

    private ConceptId getConcept() {
        return ConceptIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CONCEPT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_editor_activity);

        findViewById(R.id.nextButton).setOnClickListener(this);
        _textField = findViewById(R.id.textField);
        _textField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                openSpanEditor();
                return true;
            }

            return false;
        });

        final SentenceId sentenceId = getSentenceId();
        if (sentenceId != null) {
            _textField.setText(DbManager.getInstance().getManager().getSentenceText(sentenceId));
        }
    }

    @Override
    public void onClick(View v) {
        openSpanEditor();
    }

    private void openSpanEditor() {
        final SentenceId sentenceId = getSentenceId();
        final AcceptationId acceptation = getAcceptationId();
        final String text = _textField.getText().toString();

        if (acceptation != null) {
            SpanEditorActivity.openWithAcceptation(this, REQUEST_CODE_ADD_SPAN, text, acceptation);
        }
        else if (sentenceId == null) {
            SpanEditorActivity.openWithConcept(this, REQUEST_CODE_ADD_SPAN, text, getConcept());
        }
        else {
            SpanEditorActivity.openWithSentenceId(this, REQUEST_CODE_ADD_SPAN, text, sentenceId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final SentenceId sentenceId = (data != null)? SentenceIdBundler.readAsIntentExtra(data, SpanEditorActivity.ResultKeys.SENTENCE_ID) : null;
            if (sentenceId != null) {
                final Intent intent = new Intent();
                SentenceIdBundler.writeAsIntentExtra(intent, ResultKeys.SENTENCE_ID, sentenceId);
                setResult(RESULT_OK, intent);
            }
            else {
                setResult(RESULT_OK);
            }
            finish();
        }
    }
}
