package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;

import sword.langbook3.android.AcceptationDetailsActivity;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.CharacterCompositionEditorActivity;
import sword.langbook3.android.CharacterDetailsActivity;
import sword.langbook3.android.CharacterDetailsAdapter;
import sword.langbook3.android.CharacterRepresentationUpdaterActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.CharacterDetailsModel;

public final class CharacterDetailsActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements AdapterView.OnItemClickListener {
    private static final int REQUEST_CODE_ASSIGN_UNICODE = 1;
    private static final int REQUEST_CODE_CHARACTER_COMPOSITION = 2;

    private CharacterDetailsAdapter _adapter;

    public interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    private interface SavedKeys {
        String CHARACTER = ArgKeys.CHARACTER;
        String DELETE_COMPOSITION_CONFIRMATION_DIALOG_PRESENT = "dccdp";
    }

    private Activity _activity;
    private AlphabetId _preferredAlphabet;
    private CharacterId _characterId;
    private CharacterDetailsModel<CharacterId, AcceptationId> _model;

    private boolean _showingDeleteCompositionDialog;

    private void updateModelAndUi() {
        _model = DbManager.getInstance().getManager().getCharacterDetails(_characterId, _preferredAlphabet);

        if (_model != null) {
            _activity.setTitle(CharacterDetailsAdapter.representChar(_model.representation));
            _adapter.setModel(_model);
            _activity.invalidateOptionsMenu();
        }
        else {
            _activity.finish();
        }
    }

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.character_details_activity);

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _adapter = new CharacterDetailsAdapter();
        final ListView listView = activity.findViewById(R.id.listView);
        listView.setAdapter(_adapter);
        listView.setOnItemClickListener(this);

        if (savedInstanceState != null) {
            _characterId = CharacterIdBundler.read(savedInstanceState, SavedKeys.CHARACTER);
        }

        if (_characterId == null) {
            _characterId = CharacterIdBundler.readAsIntentExtra(_activity.getIntent(), ArgKeys.CHARACTER);
        }
        updateModelAndUi();

        if (savedInstanceState != null) {
            _showingDeleteCompositionDialog = savedInstanceState.getBoolean(SavedKeys.DELETE_COMPOSITION_CONFIRMATION_DIALOG_PRESENT);
        }

        if (_showingDeleteCompositionDialog) {
            showDeleteConfirmationDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        if (_model != null) {
            final MenuInflater inflater = activity.newMenuInflater();
            if (_model.representation.character == INVALID_CHARACTER) {
                inflater.inflate(R.menu.character_details_activity_no_unicode_assigned, menu);
            }

            final int menuRes = (_model.compositionType == null)?
                    R.menu.character_details_activity_no_composition :
                    R.menu.character_details_activity_with_composition;
            inflater.inflate(menuRes, menu);

            return true;
        }

        return false;
    }

    private void deleteComposition() {
        if (DbManager.getInstance().getManager().removeCharacterComposition(_characterId)) {
            updateModelAndUi();
        }
    }

    private void showDeleteConfirmationDialog() {
        _activity.newAlertDialogBuilder()
                .setMessage(R.string.deleteCharacterCompositionConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, (d, w) -> {
                    _showingDeleteCompositionDialog = false;
                    deleteComposition();
                })
                .setOnCancelListener(d -> _showingDeleteCompositionDialog = false)
                .create().show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemUpdateRepresentation) {
            CharacterRepresentationUpdaterActivity.open(activity, REQUEST_CODE_ASSIGN_UNICODE, _characterId);
            return true;
        }
        else if (itemId == R.id.menuItemAddCharacterComposition || itemId == R.id.menuItemEditCharacterComposition) {
            CharacterCompositionEditorActivity.open(activity, REQUEST_CODE_CHARACTER_COMPOSITION, _characterId);
            return true;
        }
        else if (itemId == R.id.menuItemDeleteCharacterComposition) {
            _showingDeleteCompositionDialog = true;
            showDeleteConfirmationDialog();
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Object item = parent.getAdapter().getItem(position);
        if (item instanceof CharacterId) {
            CharacterDetailsActivity.open(_activity, (CharacterId) item);
        }
        else if (item instanceof AcceptationId) {
            AcceptationDetailsActivity.open(_activity, (AcceptationId) item);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        if (_showingDeleteCompositionDialog) {
            outState.putBoolean(SavedKeys.DELETE_COMPOSITION_CONFIRMATION_DIALOG_PRESENT, true);
        }

        final CharacterId characterOnIntent = CharacterIdBundler.readAsIntentExtra(activity.getIntent(), ArgKeys.CHARACTER);
        if (!characterOnIntent.equals(_characterId)) {
            CharacterIdBundler.write(outState, SavedKeys.CHARACTER, _characterId);
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ASSIGN_UNICODE && resultCode == RESULT_OK && data != null) {
            _characterId = CharacterIdBundler.readAsIntentExtra(data, CharacterRepresentationUpdaterActivityDelegate.ResultKeys.MERGED_CHARACTER);
            updateModelAndUi();
        }
    }
}
