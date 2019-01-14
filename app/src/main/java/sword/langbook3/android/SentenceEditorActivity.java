package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import sword.database.Database;

import static sword.langbook3.android.LangbookReadableDatabase.getSymbolArray;

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

    static final int NO_SYMBOL_ARRAY = 0;

    private static final int REQUEST_CODE_ADD_SPAN = 1;

    interface ArgKeys {
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
    }

    interface ResultKeys {
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
    }

    static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    static void open(Activity activity, int requestCode, int symbolArrayId) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.SYMBOL_ARRAY, symbolArrayId);
        activity.startActivityForResult(intent, requestCode);
    }

    private EditText _textField;

    private int getSymbolArrayId() {
        return getIntent().getIntExtra(ArgKeys.SYMBOL_ARRAY, NO_SYMBOL_ARRAY);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_editor_activity);

        findViewById(R.id.nextButton).setOnClickListener(this);
        _textField = findViewById(R.id.textField);

        final int symbolArrayId = getSymbolArrayId();
        if (symbolArrayId != NO_SYMBOL_ARRAY) {
            final Database db = DbManager.getInstance().getDatabase();
            final String text = getSymbolArray(db, symbolArrayId);
            _textField.setText(text);
        }
    }

    @Override
    public void onClick(View v) {
        final int symbolArrayId = getSymbolArrayId();
        final String text = _textField.getText().toString();

        if (symbolArrayId == NO_SYMBOL_ARRAY) {
            SpanEditorActivity.open(this, REQUEST_CODE_ADD_SPAN, text);
        }
        else {
            SpanEditorActivity.open(this, REQUEST_CODE_ADD_SPAN, text, symbolArrayId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            final Intent resultIntent = new Intent();
            resultIntent.putExtra(ResultKeys.SYMBOL_ARRAY, data.getIntExtra(SpanEditorActivity.ResultKeys.SYMBOL_ARRAY, 0));
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}
