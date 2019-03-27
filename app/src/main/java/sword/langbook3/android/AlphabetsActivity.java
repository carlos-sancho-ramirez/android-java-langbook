package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.database.Database;

import static sword.langbook3.android.LangbookReadableDatabase.checkAlphabetCanBeRemoved;
import static sword.langbook3.android.LangbookReadableDatabase.conceptFromAcceptation;

public final class AlphabetsActivity extends Activity implements DialogInterface.OnClickListener, ListView.OnItemLongClickListener {

    private static final int REQUEST_CODE_NEW_ALPHABET = 1;

    private interface SavedKeys {
        String ALPHABET_TO_REMOVE = "atr";
        String NEW_ALPHABET_LANGUAGE = "nal";
    }

    public static void open(Context context) {
        final Intent intent = new Intent(context, AlphabetsActivity.class);
        context.startActivity(intent);
    }

    private int _newAlphabetLanguage;
    private int _alphabetToRemove;

    private AlphabetsAdapter _adapter;

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
        _adapter = new AlphabetsAdapter(langAlphabetMap, languages, alphabetsBuilder.build(), conversions,
                this::addAlphabet, pair -> ConversionDetailsActivity.open(this, pair.left, pair.right));
        listView.setAdapter(_adapter);
        listView.setOnItemLongClickListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alphabets_activity);

        updateUi();
        if (savedInstanceState != null) {
            _newAlphabetLanguage = savedInstanceState.getInt(SavedKeys.NEW_ALPHABET_LANGUAGE);
            _alphabetToRemove = savedInstanceState.getInt(SavedKeys.ALPHABET_TO_REMOVE);
        }

        if (_alphabetToRemove != 0) {
            showDeleteConfirmationDialog();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteAcceptationConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _alphabetToRemove = 0)
                .create().show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (_adapter.getItemViewType(position) == AlphabetsAdapter.ViewTypes.ALPHABET) {
            final int alphabet = _adapter.getItem(position);
            if (checkAlphabetCanBeRemoved(DbManager.getInstance().getDatabase(), alphabet)) {
                _alphabetToRemove = alphabet;
                showDeleteConfirmationDialog();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final int alphabet = _alphabetToRemove;
        _alphabetToRemove = 0;

        if (!LangbookDatabase.removeAlphabet(DbManager.getInstance().getDatabase(), alphabet)) {
            throw new AssertionError();
        }

        Toast.makeText(this, R.string.removeAlphabetFeedback, Toast.LENGTH_SHORT).show();
        updateUi();
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
        outState.putInt(SavedKeys.ALPHABET_TO_REMOVE, _alphabetToRemove);
    }
}
