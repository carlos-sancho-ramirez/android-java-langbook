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

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;

public final class AlphabetsActivity extends Activity implements DialogInterface.OnClickListener, ListView.OnItemLongClickListener {

    private static final int REQUEST_CODE_NEW_ALPHABET = 1;
    private static final int REQUEST_CODE_NEW_LANGUAGE = 2;

    private interface SavedKeys {
        String STATE = "st";
    }

    public static void open(Context context) {
        final Intent intent = new Intent(context, AlphabetsActivity.class);
        context.startActivity(intent);
    }

    private AlphabetsActivityState _state;
    private boolean _uiJustUpdated;

    private AlphabetsAdapter _adapter;
    private ImmutableMap<AlphabetId, AlphabetId> _conversions;

    private void updateUi() {
        final ListView listView = findViewById(R.id.listView);

        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<LanguageId, String> languages = checker.readAllLanguages(preferredAlphabet);
        final ImmutableMap<LanguageId, ImmutableMap<AlphabetId, String>> alphabetsPerLanguage =
                languages.keySet().assign(lang -> checker.readAlphabetsForLanguage(lang, preferredAlphabet));

        final ImmutableMap<LanguageId, ImmutableSet<AlphabetId>> langAlphabetMap = alphabetsPerLanguage.map(ImmutableMap::keySet);
        final ImmutableMap.Builder<AlphabetId, String> alphabetsBuilder = new ImmutableHashMap.Builder<>();
        for (ImmutableMap<AlphabetId, String> alphabets : alphabetsPerLanguage) {
            final int size = alphabets.size();
            for (int i = 0; i < size; i++) {
                alphabetsBuilder.put(alphabets.keyAt(i), alphabets.valueAt(i));
            }
        }

        _conversions = checker.getConversionsMap();
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
        _uiJustUpdated = true;

        if (_state.shouldShowDeleteConfirmationDialog()) {
            showDeleteConfirmationDialog();
        }

        if (_state.shouldShowLanguageOptionsDialog()) {
            showLanguageOptionsDialog();
        }

        if (_state.shouldShowAlphabetOptionsDialog()) {
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
        if (item.getItemId() == R.id.menuItemAdd) {
            Intentions.addLanguage(this, REQUEST_CODE_NEW_LANGUAGE);
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
        final Object rawItem = _adapter.getItem(position);
        if (rawItem instanceof AlphabetId) {
            final AlphabetId alphabet = (AlphabetId) rawItem;
            final boolean canBeRemoved = DbManager.getInstance().getManager().checkAlphabetCanBeRemoved(alphabet);
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
            final LanguageId languageId = (LanguageId) rawItem;
            _state.showLanguageOptions(languageId);
            showLanguageOptionsDialog();
        }

        return false;
    }

    private void showLanguageOptionsDialog() {
        final String addAlphabetOption = getString(R.string.addAlphabetOption);
        final String deleteLanguageOption = getString(R.string.deleteLanguageOption);
        final String[] items = {
                addAlphabetOption,
                deleteLanguageOption
        };

        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        Intentions.addAlphabet(this, REQUEST_CODE_NEW_ALPHABET, _state.startAddAlphabetIntention());
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
        final String checkConversionOption = getString(R.string.checkConversionOption);
        final String deleteAlphabetOption = getString(R.string.deleteAlphabetOption);
        final String[] items = {
                checkConversionOption,
                deleteAlphabetOption
        };

        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        final AlphabetId alphabet = _state.cancelAlphabetOptions();
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

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        if (_state.isRemovingAlphabetConfirmationPresent()) {
            final AlphabetId alphabet = _state.cancelAlphabetRemoval();

            if (!manager.removeAlphabet(alphabet)) {
                throw new AssertionError();
            }

            Toast.makeText(this, R.string.removeAlphabetFeedback, Toast.LENGTH_SHORT).show();
            updateUi();
        }
        else if (_state.isRemovingLanguageConfirmationPresent()) {
            final LanguageId language = _state.cancelLanguageRemoval();

            if (manager.removeLanguage(language)) {
                Toast.makeText(this, R.string.removeLanguageFeedback, Toast.LENGTH_SHORT).show();
                updateUi();
            }
            else {
                Toast.makeText(this, R.string.removeLanguageKo, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && !_uiJustUpdated && (requestCode == REQUEST_CODE_NEW_ALPHABET || requestCode == REQUEST_CODE_NEW_LANGUAGE)) {
            updateUi();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        _uiJustUpdated = false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
