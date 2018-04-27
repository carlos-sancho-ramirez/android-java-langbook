package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public final class WordEditorActivity extends Activity {

    private static final int REQUEST_CODE_LANGUAGE_PICKER = 1;

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

        if (savedInstanceState == null) {
            LanguagePickerActivity.open(this, REQUEST_CODE_LANGUAGE_PICKER);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LANGUAGE_PICKER) {
            if (resultCode == RESULT_OK) {
                final int language = data.getIntExtra(LanguagePickerActivity.ResultKeys.LANGUAGE, 0);
                Toast.makeText(this, "Selected language " + language, Toast.LENGTH_SHORT).show();
            }
            else {
                finish();
            }
        }
    }
}
