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

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

    static final int NO_SENTENCE_ID = 0;

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

    static void openWithSentenceId(Activity activity, int requestCode, int sentenceId) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.SENTENCE_ID, sentenceId);
        activity.startActivityForResult(intent, requestCode);
    }

    private EditText _textField;

    private int getSentenceId() {
        return getIntent().getIntExtra(ArgKeys.SENTENCE_ID, NO_SENTENCE_ID);
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

        final int sentenceId = getSentenceId();
        if (sentenceId != NO_SENTENCE_ID) {
            _textField.setText(DbManager.getInstance().getManager().getSentenceText(sentenceId));
        }
    }

    @Override
    public void onClick(View v) {
        openSpanEditor();
    }

    private void openSpanEditor() {
        final int sentenceId = getSentenceId();
        final AcceptationId acceptation = getAcceptationId();
        final String text = _textField.getText().toString();

        if (acceptation != null) {
            SpanEditorActivity.openWithAcceptation(this, REQUEST_CODE_ADD_SPAN, text, acceptation);
        }
        else if (sentenceId == NO_SENTENCE_ID) {
            SpanEditorActivity.openWithConcept(this, REQUEST_CODE_ADD_SPAN, text, getConcept());
        }
        else {
            SpanEditorActivity.openWithSentenceId(this, REQUEST_CODE_ADD_SPAN, text, sentenceId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final int sentenceId = (data != null)? data.getIntExtra(SpanEditorActivity.ResultKeys.SENTENCE_ID, 0) : 0;
            if (sentenceId != 0) {
                final Intent intent = new Intent();
                intent.putExtra(ResultKeys.SENTENCE_ID, sentenceId);
                setResult(RESULT_OK, intent);
            }
            else {
                setResult(RESULT_OK);
            }
            finish();
        }
    }
}
