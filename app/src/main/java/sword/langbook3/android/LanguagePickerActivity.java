package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.database.Database;

import static sword.langbook3.android.CorrelationPickerActivity.NO_CONCEPT;
import static sword.langbook3.android.db.LangbookReadableDatabase.readAllLanguages;

public final class LanguagePickerActivity extends Activity implements ListView.OnItemClickListener {

    private static final int REQUEST_CODE_NEW_WORD = 1;

    interface ArgKeys {
        String CONCEPT = BundleKeys.CONCEPT;
        String SEARCH_QUERY = BundleKeys.SEARCH_QUERY;
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    public static void open(Activity activity, int requestCode, String searchQuery) {
        final Intent intent = new Intent(activity, LanguagePickerActivity.class);
        intent.putExtra(ArgKeys.SEARCH_QUERY, searchQuery);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, String searchQuery, int concept) {
        final Intent intent = new Intent(activity, LanguagePickerActivity.class);
        intent.putExtra(ArgKeys.CONCEPT, concept);
        intent.putExtra(ArgKeys.SEARCH_QUERY, searchQuery);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_picker_activity);

        final ListView listView = findViewById(R.id.listView);
        final Database db = DbManager.getInstance().getDatabase();
        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        listView.setAdapter(new LanguagePickerAdapter(readAllLanguages(db, preferredAlphabet)));
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int concept = getIntent().getIntExtra(ArgKeys.CONCEPT, NO_CONCEPT);
        final String searchQuery = getIntent().getStringExtra(ArgKeys.SEARCH_QUERY);
        WordEditorActivity.open(this, REQUEST_CODE_NEW_WORD, (int) id, searchQuery, concept);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NEW_WORD && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
