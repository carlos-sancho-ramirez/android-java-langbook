package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

import static sword.langbook3.android.AcceptationDetailsActivity.preferredAlphabet;

public final class LanguagePickerActivity extends Activity implements ListView.OnItemClickListener {

    public interface ResultKeys {
        String LANGUAGE = "lang";
    }

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, LanguagePickerActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    private static ImmutableIntKeyMap<String> readLanguages() {
        final LangbookDbSchema.LanguagesTable languages = LangbookDbSchema.Tables.languages;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable stringQueries = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = languages.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(languages)
                .join(acceptations, languages.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(stringQueries, accOffset + acceptations.getIdColumnIndex(), stringQueries.getDynamicAcceptationColumnIndex())
                .select(
                        languages.getIdColumnIndex(),
                        strOffset + stringQueries.getStringAlphabetColumnIndex(),
                        strOffset + stringQueries.getStringColumnIndex()
                );

        MutableIntSet foundLanguages = MutableIntSet.empty();
        MutableIntKeyMap<String> result = MutableIntKeyMap.empty();

        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            final int lang = row.get(0).toInt();
            final int alphabet = row.get(1).toInt();

            if (alphabet == preferredAlphabet || !foundLanguages.contains(lang)) {
                foundLanguages.add(lang);
                result.put(lang, row.get(2).toText());
            }
        }

        return result.toImmutable();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_picker_activity);

        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new LanguagePickerAdapter(readLanguages()));
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent();
        intent.putExtra(ResultKeys.LANGUAGE, (int) id);
        setResult(RESULT_OK, intent);
        finish();
    }
}
