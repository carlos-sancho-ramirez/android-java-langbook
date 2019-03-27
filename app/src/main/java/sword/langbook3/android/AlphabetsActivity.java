package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.database.Database;

import static sword.langbook3.android.LangbookReadableDatabase.conceptFromAcceptation;

public final class AlphabetsActivity extends Activity {

    private static final int REQUEST_CODE_NEW_ALPHABET = 1;

    private interface SavedKeys {
        String NEW_ALPHABET_LANGUAGE = "nal";
    }

    public static void open(Context context) {
        final Intent intent = new Intent(context, AlphabetsActivity.class);
        context.startActivity(intent);
    }

    private int _newAlphabetLanguage;

    private void updateUi() {
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

        final ImmutableIntPairMap conversions = LangbookReadableDatabase.getConversionsMap(db);
        listView.setAdapter(new AlphabetsAdapter(langAlphabetMap, languages, alphabetsBuilder.build(), conversions,
                this::addAlphabet, pair -> ConversionDetailsActivity.open(this, pair.left, pair.right)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alphabets_activity);

        updateUi();
        if (savedInstanceState != null) {
            _newAlphabetLanguage = savedInstanceState.getInt(SavedKeys.NEW_ALPHABET_LANGUAGE);
        }
    }

    private void addAlphabet(int languageId) {
        _newAlphabetLanguage = languageId;
        AcceptationPickerActivity.open(this, REQUEST_CODE_NEW_ALPHABET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final int acceptation = (data != null)? data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0) : 0;
        if (requestCode == REQUEST_CODE_NEW_ALPHABET && resultCode == RESULT_OK && acceptation != 0) {
            final int language = _newAlphabetLanguage;
            _newAlphabetLanguage = 0;

            final Database db = DbManager.getInstance().getDatabase();
            final boolean ok = LangbookDatabase.addAlphabet(db, conceptFromAcceptation(db, acceptation), language);
            final int message = ok? R.string.includeAlphabetFeedback : R.string.includeAlphabetKo;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            if (ok) {
                updateUi();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SavedKeys.NEW_ALPHABET_LANGUAGE, _newAlphabetLanguage);
    }
}
