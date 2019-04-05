package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
    private static final int REQUEST_CODE_NEW_LANGUAGE = 2;

    private interface SavedKeys {
        String ALPHABET_TO_REMOVE = "atr";
        String LANGUAGE_TO_REMOVE = "ltr";
        String NEW_ALPHABET_LANGUAGE = "nal";
        String OPTIONS_FOR_ALPHABET = "ofa";
        String OPTIONS_FOR_LANGUAGE = "ofl";
    }

    public static void open(Context context) {
        final Intent intent = new Intent(context, AlphabetsActivity.class);
        context.startActivity(intent);
    }

    private int _newAlphabetLanguage;
    private int _alphabetToRemove;
    private int _languageToRemove;
    private int _optionsForAlphabet;
    private int _optionsForLanguage;

    private AlphabetsAdapter _adapter;
    private ImmutableIntPairMap _conversions;

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

        _conversions = LangbookReadableDatabase.getConversionsMap(db);
        _adapter = new AlphabetsAdapter(langAlphabetMap, languages, alphabetsBuilder.build());
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
            _languageToRemove = savedInstanceState.getInt(SavedKeys.LANGUAGE_TO_REMOVE);
            _optionsForAlphabet = savedInstanceState.getInt(SavedKeys.OPTIONS_FOR_ALPHABET);
            _optionsForLanguage = savedInstanceState.getInt(SavedKeys.OPTIONS_FOR_LANGUAGE);
        }

        if (_alphabetToRemove != 0 || _languageToRemove != 0) {
            showDeleteConfirmationDialog();
        }

        if (_optionsForLanguage != 0) {
            showLanguageOptionsDialog();
        }

        if (_optionsForAlphabet != 0) {
            showAlphabetOptionsDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.alphabets_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemAdd:
                LanguageAdderActivity.open(this, REQUEST_CODE_NEW_LANGUAGE);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        final int message = (_alphabetToRemove != 0)? R.string.deleteAlphabetConfirmationText : R.string.deleteLanguageConfirmationText;
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> {
                    _alphabetToRemove = 0;
                    _languageToRemove = 0;
                })
                .create().show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (_adapter.getItemViewType(position) == AlphabetsAdapter.ViewTypes.ALPHABET) {
            final int alphabet = _adapter.getItem(position);
            final boolean canBeRemoved = checkAlphabetCanBeRemoved(DbManager.getInstance().getDatabase(), alphabet);
            final boolean isTargetForConversion = _conversions.keySet().contains(alphabet);

            if (canBeRemoved && isTargetForConversion) {
                _optionsForAlphabet = alphabet;
                showAlphabetOptionsDialog();
            }
            else if (canBeRemoved) {
                _alphabetToRemove = alphabet;
                showDeleteConfirmationDialog();
                return true;
            }
            else if (isTargetForConversion) {
                ConversionDetailsActivity.open(this, _conversions.get(alphabet), alphabet);
                return true;
            }
        }
        else {
            _optionsForLanguage = _adapter.getItem(position);
            showLanguageOptionsDialog();
        }

        return false;
    }

    private void showLanguageOptionsDialog() {
        final String[] items = {"Add alphabet", "Delete language"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        final int language = _optionsForLanguage;
                        _optionsForLanguage = 0;

                        addAlphabet(language);
                    }
                    else if (which == 1) {
                        _languageToRemove = _optionsForLanguage;
                        _optionsForLanguage = 0;

                        showDeleteConfirmationDialog();
                    }
                })
                .setOnCancelListener(dialog -> _optionsForLanguage = 0)
                .create().show();
    }

    private void showAlphabetOptionsDialog() {
        final String[] items = {"Check conversion", "Delete alphabet"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        final int alphabet = _optionsForAlphabet;
                        _optionsForAlphabet = 0;

                        ConversionDetailsActivity.open(this, _conversions.get(alphabet), alphabet);
                    }
                    else if (which == 1) {
                        final int alphabet = _optionsForAlphabet;
                        _optionsForAlphabet = 0;

                        _alphabetToRemove = alphabet;
                        showDeleteConfirmationDialog();
                    }
                })
                .setOnCancelListener(dialog -> _optionsForAlphabet = 0)
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (_alphabetToRemove != 0) {
            final int alphabet = _alphabetToRemove;
            _alphabetToRemove = 0;

            if (!LangbookDatabase.removeAlphabet(DbManager.getInstance().getDatabase(), alphabet)) {
                throw new AssertionError();
            }

            Toast.makeText(this, R.string.removeAlphabetFeedback, Toast.LENGTH_SHORT).show();
            updateUi();
        }
        else {
            _languageToRemove = 0;
            Toast.makeText(this, "Unimplemented", Toast.LENGTH_SHORT).show();
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
        outState.putInt(SavedKeys.ALPHABET_TO_REMOVE, _alphabetToRemove);
        outState.putInt(SavedKeys.LANGUAGE_TO_REMOVE, _languageToRemove);
        outState.putInt(SavedKeys.OPTIONS_FOR_ALPHABET, _optionsForAlphabet);
        outState.putInt(SavedKeys.OPTIONS_FOR_LANGUAGE, _optionsForLanguage);
    }
}
