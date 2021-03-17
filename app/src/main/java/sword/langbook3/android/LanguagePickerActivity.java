package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdBundler;
import sword.langbook3.android.db.LanguageId;

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

    public static void open(Activity activity, int requestCode, String searchQuery, ConceptId concept) {
        final Intent intent = new Intent(activity, LanguagePickerActivity.class);
        ConceptIdBundler.writeAsIntentExtra(intent, ArgKeys.CONCEPT, concept);
        intent.putExtra(ArgKeys.SEARCH_QUERY, searchQuery);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_picker_activity);

        final ListView listView = findViewById(R.id.listView);
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        listView.setAdapter(new LanguagePickerAdapter(DbManager.getInstance().getManager().readAllLanguages(preferredAlphabet)));
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ConceptId concept = ConceptIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CONCEPT);
        final String searchQuery = getIntent().getStringExtra(ArgKeys.SEARCH_QUERY);
        final LanguageId languageId = ((LanguagePickerAdapter) parent.getAdapter()).getItem(position);
        WordEditorActivity.open(this, REQUEST_CODE_NEW_WORD, languageId, searchQuery, concept);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NEW_WORD && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
