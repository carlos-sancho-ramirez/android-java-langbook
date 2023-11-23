package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;

import sword.langbook3.android.AcceptationDetailsAdapter;
import sword.langbook3.android.CharacterCompositionDefinitionEditorActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.Intentions;
import sword.langbook3.android.QuizSelectorActivity;
import sword.langbook3.android.R;
import sword.langbook3.android.SentenceDetailsActivity;
import sword.langbook3.android.controllers.CharacterCompositionDefinitionEditorController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CharacterCompositionTypeIdManager;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.DisplayableItem;

public final class AcceptationDetailsActivityDelegate<Activity extends ActivityExtensions> extends AbstractAcceptationDetailsActivityDelegate<Activity> implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    public interface ArgKeys extends AbstractAcceptationDetailsActivityDelegate.ArgKeys {
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    private AcceptationDetailsActivityState _state;

    @Override
    boolean canNavigate() {
        return true;
    }

    private void showDeleteFromBunchConfirmationDialog() {
        final String message = _activity.getString(R.string.deleteFromBunchConfirmationText, _state.getDeleteTargetBunch().text);
        _activity.newAlertDialogBuilder()
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteTarget())
                .create().show();
    }

    private void showDeleteAcceptationFromBunchConfirmationDialog() {
        final String message = _activity.getString(R.string.deleteAcceptationFromBunchConfirmationText, _state.getDeleteTargetAcceptation().text);
        _activity.newAlertDialogBuilder()
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteTarget())
                .create().show();
    }

    private void showDeleteConfirmationDialog() {
        _activity.newAlertDialogBuilder()
                .setMessage(R.string.deleteAcceptationConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeletingAcceptation())
                .create().show();
    }

    private void showDeleteDefinitionConfirmationDialog() {
        _activity.newAlertDialogBuilder()
                .setMessage(R.string.deleteDefinitionConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeletingDefinition())
                .create().show();
    }

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        super.onCreate(activity, savedInstanceState);

        _acceptation = AcceptationIdBundler.readAsIntentExtra(_activity.getIntent(), AbstractAcceptationDetailsActivityDelegate.ArgKeys.ACCEPTATION);
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
            _activity.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        if (_model != null) {
            final MenuInflater inflater = activity.newMenuInflater();

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
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemBunchChildrenQuiz) {
            QuizSelectorActivity.open(activity, conceptAsBunchId(_model.getConcept()));
            return true;
        }
        else if (itemId == R.id.menuItemEdit) {
            Intentions.editAcceptation(activity, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemLinkConcept) {
            Intentions.linkAcceptation(activity, REQUEST_CODE_LINKED_ACCEPTATION, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemIncludeAcceptation) {
            Intentions.addAcceptationInBunch(activity, REQUEST_CODE_PICK_ACCEPTATION, conceptAsBunchId(_model.getConcept()));
            return true;
        }
        else if (itemId == R.id.menuItemIncludeInBunch) {
            Intentions.addBunchFromAcceptation(activity, REQUEST_CODE_PICK_BUNCH, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemDeleteAcceptation) {
            _state.setDeletingAcceptation();
            showDeleteConfirmationDialog();
            return true;
        }
        else if (itemId == R.id.menuItemIncludeDefinition) {
            Intentions.addDefinition(activity, REQUEST_CODE_PICK_DEFINITION, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemDeleteDefinition) {
            _state.setDeletingSupertype();
            showDeleteDefinitionConfirmationDialog();
            return true;
        }
        else if (itemId == R.id.menuItemNewSentence) {
            Intentions.addSentence(activity, REQUEST_CODE_CREATE_SENTENCE, _acceptation);
            return true;
        }
        else if (itemId == R.id.menuItemNewAgentAsSource) {
            Intentions.addAgentWithSource(activity, conceptAsBunchId(_model.getConcept()));
            return true;
        }
        else if (itemId == R.id.menuItemNewAgentAsDiff) {
            Intentions.addAgentWithDiff(activity, conceptAsBunchId(_model.getConcept()));
            return true;
        }
        else if (itemId == R.id.menuItemNewAgentAsTarget) {
            Intentions.addAgentWithTarget(activity, conceptAsBunchId(_model.getConcept()));
            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_LINKED_ACCEPTATION) {
                updateModelAndUi();
            }
            else if (requestCode == REQUEST_CODE_PICK_BUNCH) {
                updateModelAndUi();
            }
            else if (requestCode == REQUEST_CODE_PICK_DEFINITION) {
                if (updateModelAndUi()) {
                    activity.invalidateOptionsMenu();
                }
            }
            else if (requestCode == REQUEST_CODE_CREATE_SENTENCE) {
                final SentenceId sentenceId = (data != null)? SentenceIdBundler.readAsIntentExtra(data, SentenceEditorActivityDelegate.ResultKeys.SENTENCE_ID) : null;
                if (sentenceId != null) {
                    SentenceDetailsActivity.open(activity, REQUEST_CODE_CLICK_NAVIGATION, sentenceId);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        _listAdapter.getItem(position).navigate(_activity, REQUEST_CODE_CLICK_NAVIGATION);
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
            CharacterCompositionDefinitionEditorActivity.open(_activity, new CharacterCompositionDefinitionEditorController(CharacterCompositionTypeIdManager.conceptAsCharacterCompositionTypeId(_model.getConcept())));
            return true;
        }

        return false;
    }

    private void deleteAcceptation() {
        if (DbManager.getInstance().getManager().removeAcceptation(_acceptation)) {
            showFeedback(_activity.getString(R.string.deleteAcceptationFeedback));
            _activity.finish();
        }
        else {
            showFeedback(_activity.getString(R.string.unableToDelete));
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
                    _activity.invalidateOptionsMenu();
                }
                showFeedback(_activity.getString(R.string.deleteDefinitionFeedback));
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
                showFeedback(_activity.getString(R.string.deleteFromBunchFeedback, bunchText));
                break;

            case AcceptationDetailsActivityState.IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                final DisplayableItem<AcceptationId> itemToDelete = _state.getDeleteTargetAcceptation();
                final AcceptationId accIdToDelete = itemToDelete.id;
                final String accToDeleteText = itemToDelete.text;
                _state.clearDeleteTarget();

                if (!manager.removeAcceptationFromBunch(conceptAsBunchId(_model.getConcept()), accIdToDelete)) {
                    throw new AssertionError();
                }

                if (updateModelAndUi()) {
                    _activity.invalidateOptionsMenu();
                }

                showFeedback(_activity.getString(R.string.deleteAcceptationFromBunchFeedback, accToDeleteText));
                break;

            default:
                throw new AssertionError("Unable to handle state " + _state.getIntrinsicState());
        }
    }

    @Override
    public void onResume(@NonNull Activity activity) {
        if (DbManager.getInstance().getDatabase().getWriteVersion() != _dbWriteVersion && updateModelAndUi()) {
            activity.invalidateOptionsMenu();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
