package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import sword.langbook3.android.controllers.AcceptationPickerController;
import sword.langbook3.android.controllers.CharacterCompositionDefinitionEditorController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CharacterCompositionTypeIdManager;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;
import sword.langbook3.android.models.DisplayableItem;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;

public final class AcceptationDetailsActivity extends AbstractAcceptationDetailsActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    private interface SavedKeys {
        String STATE = "cSt";
    }

    public static void open(@NonNull Context context, @NonNull AcceptationId acceptation) {
        Intent intent = new Intent(context, AcceptationDetailsActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        context.startActivity(intent);
    }

    public static void open(@NonNull Activity activity, int requestCode, @NonNull AcceptationId acceptation) {
        Intent intent = new Intent(activity, AcceptationDetailsActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    private AcceptationDetailsActivityState _state;

    @Override
    boolean canNavigate() {
        return true;
    }

    private void showDeleteFromBunchConfirmationDialog() {
        final String message = getString(R.string.deleteFromBunchConfirmationText, _state.getDeleteTargetBunch().text);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteTarget())
                .create().show();
    }

    private void showDeleteAcceptationFromBunchConfirmationDialog() {
        final String message = getString(R.string.deleteAcceptationFromBunchConfirmationText, _state.getDeleteTargetAcceptation().text);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteTarget())
                .create().show();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteAcceptationConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeletingAcceptation())
                .create().show();
    }

    private void showDeleteDefinitionConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteDefinitionConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeletingDefinition())
                .create().show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _acceptation = AcceptationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ACCEPTATION);
        if (_acceptation == null) {
            throw new IllegalArgumentException("acceptation not provided");
        }

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        if (_state == null) {
            _state = new AcceptationDetailsActivityState();
        }

        if (updateModelAndUi()) {
            _listView.setOnItemClickListener(this);
            _listView.setOnItemLongClickListener(this);

            switch (_state.getIntrinsicState()) {
                case AcceptationDetailsActivityState.IntrinsicStates.DELETE_ACCEPTATION:
                    showDeleteConfirmationDialog();
                    break;

                case AcceptationDetailsActivityState.IntrinsicStates.DELETE_DEFINITION:
                    showDeleteDefinitionConfirmationDialog();
                    break;

                case AcceptationDetailsActivityState.IntrinsicStates.DELETING_FROM_BUNCH:
                    showDeleteFromBunchConfirmationDialog();
                    break;

                case AcceptationDetailsActivityState.IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                    showDeleteAcceptationFromBunchConfirmationDialog();
                    break;
            }
        }
        else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (_model != null) {
            final MenuInflater inflater = new MenuInflater(this);

            inflater.inflate(R.menu.acceptation_details_activity_edit, menu);

            if (_shouldShowBunchChildrenQuizMenuOption) {
                inflater.inflate(R.menu.acceptation_details_activity_bunch_children_quiz, menu);
            }

            inflater.inflate(R.menu.acceptation_details_activity_link_options, menu);

            if (_hasDefinition) {
                inflater.inflate(R.menu.acceptation_details_activity_delete_definition, menu);
            }
            else {
                inflater.inflate(R.menu.acceptation_details_activity_include_definition, menu);
            }

            inflater.inflate(R.menu.acceptation_details_activity_common_actions, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemBunchChildrenQuiz) {
            QuizSelectorActivity.open(this, conceptAsBunchId(_model.getConcept()));
            return true;
        }
        else if (itemId == R.id.menuItemEdit) {
            Intentions.editAcceptation(this, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemLinkConcept) {
            Intentions.linkAcceptation(this, REQUEST_CODE_LINKED_ACCEPTATION, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemIncludeAcceptation) {
            AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_ACCEPTATION, new AcceptationPickerController(null));
            return true;
        }
        else if (itemId == R.id.menuItemIncludeInBunch) {
            AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_BUNCH, new AcceptationPickerController(null));
            return true;
        }
        else if (itemId == R.id.menuItemDeleteAcceptation) {
            _state.setDeletingAcceptation();
            showDeleteConfirmationDialog();
            return true;
        }
        else if (itemId == R.id.menuItemIncludeDefinition) {
            Intentions.addDefinition(this, REQUEST_CODE_PICK_DEFINITION, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemDeleteDefinition) {
            _state.setDeletingSupertype();
            showDeleteDefinitionConfirmationDialog();
            return true;
        }
        else if (itemId == R.id.menuItemNewSentence) {
            SentenceEditorActivity.openWithAcceptation(this, REQUEST_CODE_CREATE_SENTENCE, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemNewAgentAsSource) {
            Intentions.addAgentWithSource(this, conceptAsBunchId(_model.getConcept()));
            return true;
        }
        else if (itemId == R.id.menuItemNewAgentAsDiff) {
            Intentions.addAgentWithDiff(this, conceptAsBunchId(_model.getConcept()));
            return true;
        }
        else if (itemId == R.id.menuItemNewAgentAsTarget) {
            Intentions.addAgentWithTarget(this, conceptAsBunchId(_model.getConcept()));
            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            if (requestCode == REQUEST_CODE_LINKED_ACCEPTATION) {
                updateModelAndUi();
            }
            else if (requestCode == REQUEST_CODE_PICK_ACCEPTATION) {
                final AcceptationId pickedAcceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
                final int message = manager.addAcceptationInBunch(conceptAsBunchId(_model.getConcept()), pickedAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_BUNCH) {
                final AcceptationId pickedAcceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
                final BunchId pickedBunch = (pickedAcceptation != null)? conceptAsBunchId(manager.conceptFromAcceptation(pickedAcceptation)) : null;
                final int message = manager.addAcceptationInBunch(pickedBunch, _acceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_DEFINITION) {
                if (updateModelAndUi()) {
                    invalidateOptionsMenu();
                }
            }
            else if (requestCode == REQUEST_CODE_CREATE_SENTENCE) {
                final SentenceId sentenceId = (data != null)? SentenceIdBundler.readAsIntentExtra(data, SentenceEditorActivity.ResultKeys.SENTENCE_ID) : null;
                if (sentenceId != null) {
                    SentenceDetailsActivity.open(this, REQUEST_CODE_CLICK_NAVIGATION, sentenceId);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        _listAdapter.getItem(position).navigate(this, REQUEST_CODE_CLICK_NAVIGATION);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // For now, as the only valuable action per item is 'delete', no
        // contextual menu is displayed and confirmation dialog is prompted directly.
        //
        // This routine may be valuable while there is only one enabled item.
        // For multiple, the action mode for the list view should be enabled
        // instead to apply the same action to multiple items at the same time.
        final AcceptationDetailsAdapter.Item item = _listAdapter.getItem(position);
        if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED) {
            AcceptationDetailsAdapter.AcceptationNavigableItem it = (AcceptationDetailsAdapter.AcceptationNavigableItem) item;
            if (!it.isDynamic()) {
                final BunchId bunch = conceptAsBunchId(DbManager.getInstance().getManager().conceptFromAcceptation(it.getId()));
                _state.setDeleteBunchTarget(new DisplayableItem<>(bunch, it.getText()));
                showDeleteFromBunchConfirmationDialog();
            }
            return true;
        }
        else if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED) {
            AcceptationDetailsAdapter.AcceptationNavigableItem it = (AcceptationDetailsAdapter.AcceptationNavigableItem) item;
            if (!it.isDynamic()) {
                _state.setDeleteAcceptationFromBunch(new DisplayableItem<>(it.getId(), it.getText().toString()));
                showDeleteAcceptationFromBunchConfirmationDialog();
            }
            return true;
        }
        else if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.CHARACTER_COMPOSITION_DEFINITION) {
            // TODO: Display edition confirmation dialog if the composition definition is in use, as it can break things
            CharacterCompositionDefinitionEditorActivity.open(this, new CharacterCompositionDefinitionEditorController(CharacterCompositionTypeIdManager.conceptAsCharacterCompositionTypeId(_model.getConcept())));
            return true;
        }

        return false;
    }

    private void deleteAcceptation() {
        if (DbManager.getInstance().getManager().removeAcceptation(_acceptation)) {
            showFeedback(getString(R.string.deleteAcceptationFeedback));
            finish();
        }
        else {
            showFeedback(getString(R.string.unableToDelete));
            _state.clearDeletingAcceptation();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        switch (_state.getIntrinsicState()) {
            case AcceptationDetailsActivityState.IntrinsicStates.DELETE_ACCEPTATION:
                deleteAcceptation();
                break;

            case AcceptationDetailsActivityState.IntrinsicStates.DELETE_DEFINITION:
                _state.clearDeletingDefinition();
                if (!manager.removeDefinition(_model.getConcept())) {
                    throw new AssertionError();
                }

                if (updateModelAndUi()) {
                    invalidateOptionsMenu();
                }
                showFeedback(getString(R.string.deleteDefinitionFeedback));
                break;

            case AcceptationDetailsActivityState.IntrinsicStates.DELETING_FROM_BUNCH:
                final DisplayableItem<BunchId> item = _state.getDeleteTargetBunch();
                final BunchId bunch = item.id;
                final String bunchText = item.text;
                _state.clearDeleteTarget();

                if (!manager.removeAcceptationFromBunch(bunch, _acceptation)) {
                    throw new AssertionError();
                }

                updateModelAndUi();
                showFeedback(getString(R.string.deleteFromBunchFeedback, bunchText));
                break;

            case AcceptationDetailsActivityState.IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                final DisplayableItem<AcceptationId> itemToDelete = _state.getDeleteTargetAcceptation();
                final AcceptationId accIdToDelete = itemToDelete.id;
                final String accToDeleteText = itemToDelete.text;
                _state.clearDeleteTarget();

                if (!manager.removeAcceptationFromBunch(conceptAsBunchId(_model.getConcept()), accIdToDelete)) {
                    throw new AssertionError();
                }

                updateModelAndUi();
                showFeedback(getString(R.string.deleteAcceptationFromBunchFeedback, accToDeleteText));
                break;

            default:
                throw new AssertionError("Unable to handle state " + _state.getIntrinsicState());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (DbManager.getInstance().getDatabase().getWriteVersion() != _dbWriteVersion && updateModelAndUi()) {
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
