package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.database.Database;

public final class AlphabetsActivity extends Activity {

    public static void open(Context context) {
        final Intent intent = new Intent(context, AlphabetsActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alphabets_activity);

        final ListView listView = findViewById(R.id.listView);

        final Database db = DbManager.getInstance().getDatabase();
        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableIntKeyMap<String> languages = LangbookReadableDatabase.readAllLanguages(db, preferredAlphabet);
        final ImmutableIntKeyMap<ImmutableIntKeyMap<String>> alphabetsPerLanguage = languages.keySet().assign(lang -> LangbookReadableDatabase.readAlphabetsForLanguage(db, lang, preferredAlphabet));

        final ImmutableIntKeyMap<ImmutableIntSet> langAlphabetMap = alphabetsPerLanguage.map(ImmutableIntKeyMap::keySet);
        final ImmutableIntKeyMap.Builder<String> alphabetsBuilder = new ImmutableIntKeyMap.Builder<>();
        for (ImmutableIntKeyMap<String> alphabets : alphabetsPerLanguage) {
            final int size = alphabets.size();
            for (int i = 0; i < size; i++) {
                alphabetsBuilder.put(alphabets.keyAt(i), alphabets.valueAt(i));
            }
        }

        listView.setAdapter(new AlphabetsAdapter(langAlphabetMap, languages, alphabetsBuilder.build()));
    }
}
