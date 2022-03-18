package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.models.CharacterDetailsModel;

import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;
import static sword.langbook3.android.models.CharacterDetailsModel.UNKNOWN_COMPOSITION_TYPE;

public final class CharacterDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_ASSIGN_UNICODE = 1;
    private static final int REQUEST_CODE_CHARACTER_COMPOSITION = 2;

    private CharacterDetailsAdapter _adapter;

    private interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    private interface SavedKeys {
        String CHARACTER = ArgKeys.CHARACTER;
        String DELETE_COMPOSITION_CONFIRMATION_DIALOG_PRESENT = "dccdp";
    }

    public static void open(Context context, CharacterId characterId) {
        Intent intent = new Intent(context, CharacterDetailsActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        context.startActivity(intent);
    }

    private CharacterId _characterId;
    private CharacterDetailsModel<CharacterId, AcceptationId> _model;

    private boolean _showingDeleteCompositionDialog;

    private void updateModelAndUi() {
        _model = DbManager.getInstance().getManager().getCharacterDetails(_characterId);

        if (_model != null) {
            setTitle(CharacterDetailsAdapter.representChar(_model.representation));
            _adapter.setModel(_model);
            invalidateOptionsMenu();
        }
        else {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_details_activity);

        _adapter = new CharacterDetailsAdapter();
        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(_adapter);
        listView.setOnItemClickListener(this);

        if (savedInstanceState != null) {
            _characterId = CharacterIdBundler.read(savedInstanceState, SavedKeys.CHARACTER);
        }

        if (_characterId == null) {
            _characterId = CharacterIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CHARACTER);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (_model != null) {
            final MenuInflater inflater = new MenuInflater(this);
            if (_model.representation.character == INVALID_CHARACTER) {
                inflater.inflate(R.menu.character_details_activity_no_unicode_assigned, menu);
            }

            final int menuRes = (_model.compositionType == UNKNOWN_COMPOSITION_TYPE)?
                    R.menu.character_details_activity_no_composition :
                    R.menu.character_details_activity_with_composition;
            inflater.inflate(menuRes, menu);
        }

        return true;
    }

    private void deleteComposition() {
        if (DbManager.getInstance().getManager().removeCharacterComposition(_characterId)) {
            updateModelAndUi();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteCharacterCompositionConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, (d, w) -> {
                    _showingDeleteCompositionDialog = false;
                    deleteComposition();
                })
                .setOnCancelListener(d -> _showingDeleteCompositionDialog = false)
                .create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemAssignUnicode) {
            UnicodeAssignerActivity.open(this, REQUEST_CODE_ASSIGN_UNICODE, _characterId);
            return true;
        }
        else if (itemId == R.id.menuItemAddCharacterComposition || itemId == R.id.menuItemEditCharacterComposition) {
            CharacterCompositionEditorActivity.open(this, REQUEST_CODE_CHARACTER_COMPOSITION, _characterId);
            return true;
        }
        else if (itemId == R.id.menuItemDeleteCharacterComposition) {
            _showingDeleteCompositionDialog = true;
            showDeleteConfirmationDialog();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Object item = parent.getAdapter().getItem(position);
        if (item instanceof CharacterId) {
            CharacterDetailsActivity.open(this, (CharacterId) item);
        }
        else if (item instanceof AcceptationId) {
            AcceptationDetailsActivity.open(this, (AcceptationId) item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (_showingDeleteCompositionDialog) {
            outState.putBoolean(SavedKeys.DELETE_COMPOSITION_CONFIRMATION_DIALOG_PRESENT, true);
        }

        final CharacterId characterOnIntent = CharacterIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CHARACTER);
        if (!characterOnIntent.equals(_characterId)) {
            CharacterIdBundler.write(outState, SavedKeys.CHARACTER, _characterId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ASSIGN_UNICODE && resultCode == RESULT_OK && data != null) {
            _characterId = CharacterIdBundler.readAsIntentExtra(data, UnicodeAssignerActivity.ResultKeys.MERGED_CHARACTER);
            updateModelAndUi();
        }
    }
}
