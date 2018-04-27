package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public final class WordEditorActivity extends Activity {

    private interface BundleKeys {
        String SEARCH_QUERY = "searchQuery";
    }

    public static void open(Context context, String searchQuery) {
        final Intent intent = new Intent(context, WordEditorActivity.class);
        intent.putExtra(BundleKeys.SEARCH_QUERY, searchQuery);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);
    }
}
