package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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

    private static final int REQUEST_CODE_OPEN_ACCEPTATION = 1;

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

    private void updateSentenceTextView() {
        final int symbolArrayId = getIntent().getIntExtra(ArgKeys.SYMBOL_ARRAY, 0);
        final Database db = DbManager.getInstance().getDatabase();
        final String text = getSymbolArray(db, symbolArrayId);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_details_activity);

        _sentenceTextView = findViewById(R.id.sentenceText);
        _listView = findViewById(R.id.listView);

        updateSentenceTextView();
    }
}
