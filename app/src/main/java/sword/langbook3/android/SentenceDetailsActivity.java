package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import sword.collections.ImmutableSet;
import sword.langbook3.android.LangbookReadableDatabase.SentenceSpan;
import sword.langbook3.android.db.Database;

import static sword.langbook3.android.LangbookReadableDatabase.getSentenceSpans;
import static sword.langbook3.android.LangbookReadableDatabase.getStaticAcceptationFromDynamic;
import static sword.langbook3.android.LangbookReadableDatabase.getSymbolArray;

public final class SentenceDetailsActivity extends Activity {

    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_OPEN_ACCEPTATION = 2;

    interface ArgKeys {
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
    }

    static void open(Context context, int symbolArray) {
        final Intent intent = new Intent(context, SentenceDetailsActivity.class);
        intent.putExtra(ArgKeys.SYMBOL_ARRAY, symbolArray);
        context.startActivity(intent);
    }

    private TextView _sentenceTextView;
    private ListView _listView;
    private boolean justCreated;

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
        final Database db = DbManager.getInstance().getDatabase();
        final String text = getSymbolArray(db, symbolArrayId);

        if (text == null) {
            finish();
        }
        else {
            final ImmutableSet<SentenceSpan> spans = getSentenceSpans(db, symbolArrayId);

            final SpannableString string = new SpannableString(text);
            for (SentenceSpan span : spans) {
                final int staticAcceptation = getStaticAcceptationFromDynamic(db, span.acceptation);
                string.setSpan(new ClickableSentenceSpan(staticAcceptation, span.acceptation),
                        span.range.min(), span.range.max() + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            _sentenceTextView.setText(string);
            _sentenceTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_details_activity);

        _sentenceTextView = findViewById(R.id.sentenceText);
        _listView = findViewById(R.id.listView);

        updateSentenceTextView();
        justCreated = true;
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        justCreated = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && !justCreated) {
            updateSentenceTextView();
        }
    }
}
