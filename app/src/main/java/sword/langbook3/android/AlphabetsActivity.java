package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.database.Database;
import sword.langbook3.android.LangbookReadableDatabase.Conversion;

import static sword.langbook3.android.LangbookReadableDatabase.checkAlphabetCanBeRemoved;
import static sword.langbook3.android.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.readAlphabetsForLanguage;

public final class AlphabetsActivity extends Activity implements DialogInterface.OnClickListener, ListView.OnItemLongClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CODE_NEW_ALPHABET = 1;
    private static final int REQUEST_CODE_NEW_LANGUAGE = 2;
    private static final int REQUEST_CODE_NEW_CONVERSION = 3;

    private interface SavedKeys {
        String STATE = "st";
    }

    public static void open(Context context) {
        final Intent intent = new Intent(context, AlphabetsActivity.class);
        context.startActivity(intent);
    }

    private AlphabetsActivityState _state;

    private AlphabetsAdapter _adapter;
    private ImmutableIntPairMap _conversions;

    private ImmutableIntKeyMap<String> _sourceAlphabetTexts;

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

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }
        else {
            _state = new AlphabetsActivityState();
        }

        updateUi();

        if (_state.shouldShowDeleteConfirmationDialog()) {
            showDeleteConfirmationDialog();
        }

        if (_state.shouldShowLanguageOptionsDialog()) {
            showLanguageOptionsDialog();
        }

        if (_state.shouldShowAlphabetOptionsDialog()) {
            showAlphabetOptionsDialog();
        }

        if (_state.shouldShowSourceAlphabetPickerDialog()) {
            showSourceAlphabetPickerDialog();
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
        final int message = _state.isRemovingAlphabetConfirmationPresent()? R.string.deleteAlphabetConfirmationText : R.string.deleteLanguageConfirmationText;
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.cancelDeleteConfirmation())
                .create().show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (_adapter.getItemViewType(position) == AlphabetsAdapter.ViewTypes.ALPHABET) {
            final int alphabet = _adapter.getItem(position);
            final boolean canBeRemoved = checkAlphabetCanBeRemoved(DbManager.getInstance().getDatabase(), alphabet);
            final boolean isTargetForConversion = _conversions.keySet().contains(alphabet);

            if (canBeRemoved && isTargetForConversion) {
                _state.showAlphabetOptions(alphabet);
                showAlphabetOptionsDialog();
            }
            else if (canBeRemoved) {
                _state.showAlphabetRemovalConfirmation(alphabet);
                showDeleteConfirmationDialog();
                return true;
            }
            else if (isTargetForConversion) {
                ConversionDetailsActivity.open(this, _conversions.get(alphabet), alphabet);
                return true;
            }
        }
        else {
            _state.showLanguageOptions(_adapter.getItem(position));
            showLanguageOptionsDialog();
        }

        return false;
    }

    private void showLanguageOptionsDialog() {
        final String[] items = {"Add alphabet", "Delete language"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        _state.pickAcceptationForAlphabet();
                        addAlphabet();
                    }
                    else if (which == 1) {
                        _state.showLanguageRemovalConfirmation();
                        showDeleteConfirmationDialog();
                    }
                })
                .setOnCancelListener(dialog -> _state.cancelLanguageOptions())
                .create().show();
    }

    private void showAlphabetOptionsDialog() {
        final String[] items = {"Check conversion", "Delete alphabet"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        final int alphabet = _state.cancelAlphabetOptions();
                        ConversionDetailsActivity.open(this, _conversions.get(alphabet), alphabet);
                    }
                    else if (which == 1) {
                        _state.showAlphabetRemovalConfirmation();
                        showDeleteConfirmationDialog();
                    }
                })
                .setOnCancelListener(dialog -> _state.cancelAlphabetOptions())
                .create().show();
    }

    private void showSourceAlphabetPickerDialog() {
        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final Database db = DbManager.getInstance().getDatabase();

        _sourceAlphabetTexts = readAlphabetsForLanguage(db, _state.getNewAlphabetLanguage(), preferredAlphabet);
        final AlphabetAdapter adapter = new AlphabetAdapter(_sourceAlphabetTexts);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.sourceAlphabetPickerDialog)
                .setPositiveButton(R.string.addFieldButton, (d, which) -> {
                    final boolean defineConversion = _state.isDefineConversionChecked();
                    final int sourceAlphabet = _state.getSelectedSourceAlphabet();
                    if (defineConversion) {
                        final int alphabet = _state.startDefiningConversion();
                        ConversionEditorActivity.open(this, REQUEST_CODE_NEW_CONVERSION, sourceAlphabet, alphabet);
                    }
                    else {
                        final int alphabet = _state.cancelSourceAlphabetPicking();
                        addAlphabetCopyingFromSource(alphabet, sourceAlphabet);
                    }
                })
                .setOnCancelListener(d -> _state.cancelSourceAlphabetPicking())
                .create();

        final LayoutInflater inflater = LayoutInflater.from(dialog.getContext());
        final View view = inflater.inflate(R.layout.source_alphabet_picker_dialog, null);
        final Spinner sourceAlphabetSpinner = view.findViewById(R.id.sourceAlphabetSpinner);
        sourceAlphabetSpinner.setAdapter(adapter);
        final int position = _sourceAlphabetTexts.keySet().indexOf(_state.getSelectedSourceAlphabet());
        if (position >= 0) {
            sourceAlphabetSpinner.setSelection(position);
        }
        sourceAlphabetSpinner.setOnItemSelectedListener(this);

        final CheckBox defineConversionCheckBox = view.findViewById(R.id.defineConversionCheckBox);
        if (_state.isDefineConversionChecked()) {
            defineConversionCheckBox.setChecked(true);
        }
        defineConversionCheckBox.setOnCheckedChangeListener(this);

        dialog.setView(view);
        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Database db = DbManager.getInstance().getDatabase();
        if (_state.isRemovingAlphabetConfirmationPresent()) {
            final int alphabet = _state.cancelAlphabetRemoval();

            if (!LangbookDatabase.removeAlphabet(db, alphabet)) {
                throw new AssertionError();
            }

            Toast.makeText(this, R.string.removeAlphabetFeedback, Toast.LENGTH_SHORT).show();
            updateUi();
        }
        else if (_state.isRemovingLanguageConfirmationPresent()) {
            final int language = _state.cancelLanguageRemoval();

            if (LangbookDatabase.removeLanguage(db, language)) {
                Toast.makeText(this, R.string.removeLanguageFeedback, Toast.LENGTH_SHORT).show();
                updateUi();
            }
            else {
                Toast.makeText(this, R.string.removeLanguageKo, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addAlphabet() {
        AcceptationPickerActivity.open(this, REQUEST_CODE_NEW_ALPHABET);
    }

    private void addAlphabetCopyingFromSource(int alphabet, int sourceAlphabet) {
        final Database db = DbManager.getInstance().getDatabase();
        final boolean ok = LangbookDatabase.addAlphabetCopyingFromOther(db, alphabet, sourceAlphabet);
        final int message = ok? R.string.includeAlphabetFeedback : R.string.includeAlphabetKo;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        if (ok) {
            updateUi();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NEW_ALPHABET) {
            final int acceptation = (data != null)? data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0) : 0;
            if (resultCode == RESULT_OK && acceptation != 0) {
                final Database db = DbManager.getInstance().getDatabase();
                final int alphabet = conceptFromAcceptation(db, acceptation);
                _state.showSourceAlphabetPickingState(alphabet);
                showSourceAlphabetPickerDialog();
            }
            else {
                _state.cancelAcceptationForAlphabetPicking();
            }
        }
        else if (requestCode == REQUEST_CODE_NEW_CONVERSION) {
            final ParcelableConversion parcelable = (data != null)? data.getParcelableExtra(ConversionEditorActivity.ResultKeys.CONVERSION) : null;
            final Conversion conversion = (parcelable != null)? parcelable.get() : null;
            if (resultCode == RESULT_OK && conversion != null) {
                final Database db = DbManager.getInstance().getDatabase();
                final boolean ok = LangbookDatabase.addAlphabetAsConversionTarget(db, conversion);
                final int message = ok? R.string.includeAlphabetFeedback : R.string.includeAlphabetKo;
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                _state.completeDefiningConversion();

                if (ok) {
                    updateUi();
                }
            }
            else {
                _state.cancelDefiningConversion();
                showSourceAlphabetPickerDialog();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        _state.setSelectedSourceAlphabet(_sourceAlphabetTexts.keyAt(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing to be done
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        _state.setDefinedConversionChecked(isChecked);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
