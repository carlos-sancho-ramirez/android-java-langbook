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
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.langbook3.android.AcceptationDetailsActivityState.IntrinsicStates;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.AgentNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationArrayItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.RuleNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.SentenceNavigableItem;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.AcceptationDetailsModel.InvolvedAgentResultFlags;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.SynonymTranslationResult;

public final class AcceptationDetailsActivity extends Activity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_CREATE_SENTENCE = 1;
    private static final int REQUEST_CODE_CLICK_NAVIGATION = 2;
    private static final int REQUEST_CODE_EDIT = 3;
    private static final int REQUEST_CODE_LINKED_ACCEPTATION = 4;
    private static final int REQUEST_CODE_PICK_ACCEPTATION = 5;
    private static final int REQUEST_CODE_PICK_BUNCH = 6;
    private static final int REQUEST_CODE_PICK_DEFINITION = 7;

    private interface ArgKeys {
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
        String CONFIRM_ONLY = BundleKeys.CONFIRM_ONLY;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    interface ResultKeys {
        String STATIC_ACCEPTATION = BundleKeys.ACCEPTATION;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
    }

    private int _preferredAlphabet;
    private int _staticAcceptation;
    private AcceptationDetailsModel _model;
    private boolean _confirmOnly;

    private boolean _hasDefinition;

    private AcceptationDetailsActivityState _state;

    private boolean _shouldShowBunchChildrenQuizMenuOption;
    private ListView _listView;
    private AcceptationDetailsAdapter _listAdapter;

    public static void open(Context context, int staticAcceptation, int dynamicAcceptation) {
        Intent intent = new Intent(context, AcceptationDetailsActivity.class);
        intent.putExtra(ArgKeys.STATIC_ACCEPTATION, staticAcceptation);
        intent.putExtra(ArgKeys.DYNAMIC_ACCEPTATION, dynamicAcceptation);
        context.startActivity(intent);
    }

    public static void open(Activity activity, int requestCode, int staticAcceptation, int dynamicAcceptation, boolean confirmOnly) {
        Intent intent = new Intent(activity, AcceptationDetailsActivity.class);
        intent.putExtra(ArgKeys.STATIC_ACCEPTATION, staticAcceptation);
        intent.putExtra(ArgKeys.DYNAMIC_ACCEPTATION, dynamicAcceptation);

        if (confirmOnly) {
            intent.putExtra(ArgKeys.CONFIRM_ONLY, true);
        }

        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableList<AcceptationDetailsAdapter.Item> getAdapterItems() {
        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> result = new ImmutableList.Builder<>();

        final ImmutableIntSet commonAlphabets = _model.correlationIds
                .map((int id) -> _model.correlations.get(id).keySet())
                .reduce((set1, set2) -> set1.filter(set2::contains), new ImmutableIntSetCreator().build());
        if (commonAlphabets.size() > 1) {
            final int mainAlphabet = commonAlphabets.valueAt(0);
            final int pronunciationAlphabet = commonAlphabets.valueAt(1);
            result.add(new CorrelationArrayItem(_model.correlationIds, _model.correlations, mainAlphabet, pronunciationAlphabet));
        }

        result.add(new HeaderItem(getString(R.string.accDetailsSectionSummary, _staticAcceptation)));
        result.add(new NonNavigableItem(getString(R.string.accDetailsSectionLanguage) + ": " + _model.language.text));

        _hasDefinition = _model.baseConceptAcceptationId != 0;
        if (_hasDefinition) {
            String baseText = getString(R.string.accDetailsSectionDefinition) + ": " + _model.baseConceptText;
            String complementsText = _model.definitionComplementTexts.reduce((a, b) -> a + ", " + b, null);
            String definitionText = (complementsText != null)? baseText + " (" + complementsText + ")" : baseText;
            result.add(new AcceptationNavigableItem(_model.baseConceptAcceptationId, definitionText, false));
            _hasDefinition = true;
        }

        boolean subTypeFound = false;
        for (ImmutableIntKeyMap.Entry<String> subtype : _model.subtypes.entries()) {
            if (!subTypeFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSubtypes)));
                subTypeFound = true;
            }

            result.add(new AcceptationNavigableItem(subtype.key(), subtype.value(), false));
        }

        boolean synonymFound = false;
        for (IntKeyMap.Entry<SynonymTranslationResult> entry : _model.synonymsAndTranslations.entries()) {
            if (entry.value().language == _model.language.id) {
                if (!synonymFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionSynonyms)));
                    synonymFound = true;
                }

                result.add(new AcceptationNavigableItem(entry.key(), entry.value().text, false));
            }
        }

        boolean translationFound = false;
        for (IntKeyMap.Entry<SynonymTranslationResult> entry : _model.synonymsAndTranslations.entries()) {
            final int language = entry.value().language;
            if (language != _model.language.id) {
                if (!translationFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionTranslations)));
                    translationFound = true;
                }

                final String langStr = _model.languageTexts.get(language, null);
                result.add(new AcceptationNavigableItem(entry.key(), "" + langStr + " -> " + entry.value().text, false));
            }
        }

        boolean acceptationSharingCorrelationArrayFound = false;
        for (int acc : _model.acceptationsSharingCorrelationArray) {
            if (!acceptationSharingCorrelationArrayFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingCorrelationArray)));
                acceptationSharingCorrelationArrayFound = true;
            }

            result.add(new AcceptationNavigableItem(acc, _model.getTitle(_preferredAlphabet), false));
        }

        boolean parentBunchFound = false;
        for (DynamizableResult r : _model.bunchesWhereAcceptationIsIncluded) {
            if (!parentBunchFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionBunchesWhereIncluded)));
                parentBunchFound = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED,
                    r.id, r.text, r.dynamic));
        }

        boolean morphologyFound = false;
        final ImmutableList<MorphologyResult> morphologyResults = _model.morphologies;
        for (MorphologyResult r : morphologyResults) {
            if (!morphologyFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionMorphologies)));
                morphologyFound = true;
            }

            final String ruleText = r.rules.map(_model.ruleTexts::get).reduce((a, b) -> a + " + " + b);
            result.add(new RuleNavigableItem(r.dynamicAcceptation, ruleText + " -> " + r.text));
        }

        boolean bunchChildFound = false;
        for (DynamizableResult r : _model.bunchChildren) {
            if (!bunchChildFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsInThisBunch)));
                bunchChildFound = true;
                _shouldShowBunchChildrenQuizMenuOption = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED, r.id, r.text, r.dynamic));
        }

        boolean sentenceFound = false;
        final int sampleSentenceCount = _model.sampleSentences.size();
        for (int i = 0; i < sampleSentenceCount; i++) {
            final int symbolArray = _model.sampleSentences.keyAt(i);
            final String sentence = _model.sampleSentences.valueAt(i);

            if (!sentenceFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSampleSentences)));
                sentenceFound = true;
            }

            result.add(new SentenceNavigableItem(symbolArray, sentence));
        }

        boolean agentFound = false;
        for (IntPairMap.Entry entry : _model.involvedAgents.entries()) {
            if (!agentFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionInvolvedAgents)));
                agentFound = true;
            }

            final StringBuilder s = new StringBuilder("Agent #");
            s.append(entry.key()).append(" (");
            final int flags = entry.value();
            s.append(((flags & InvolvedAgentResultFlags.target) != 0)? 'T' : '-');
            s.append(((flags & InvolvedAgentResultFlags.source) != 0)? 'S' : '-');
            s.append(((flags & InvolvedAgentResultFlags.diff) != 0)? 'D' : '-');
            s.append(((flags & InvolvedAgentResultFlags.rule) != 0)? 'R' : '-');
            s.append(((flags & InvolvedAgentResultFlags.processed) != 0)? 'P' : '-');
            s.append(')');

            result.add(new AgentNavigableItem(entry.key(), s.toString()));
        }

        for (IntPairMap.Entry entry : _model.agentRules.entries()) {
            if (!agentFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionInvolvedAgents)));
                agentFound = true;
            }

            final String text = "Agent #" + entry.key() + " (" + _model.ruleTexts.get(entry.value()) + ')';
            result.add(new AgentNavigableItem(entry.key(), text));
        }

        return result.build();
    }

    private boolean updateModelAndUi() {
        _model = DbManager.getInstance().getManager().getAcceptationsDetails(_staticAcceptation, _preferredAlphabet);
        if (_model != null) {
            setTitle(_model.getTitle(_preferredAlphabet));
            _listAdapter = new AcceptationDetailsAdapter(this, REQUEST_CODE_CLICK_NAVIGATION, getAdapterItems());
            _listView.setAdapter(_listAdapter);
            return true;
        }
        else {
            finish();
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceptation_details_activity);

        if (!getIntent().hasExtra(ArgKeys.STATIC_ACCEPTATION)) {
            throw new IllegalArgumentException("staticAcceptation not provided");
        }

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        if (_state == null) {
            _state = new AcceptationDetailsActivityState();
        }

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _staticAcceptation = getIntent().getIntExtra(ArgKeys.STATIC_ACCEPTATION, 0);
        _confirmOnly = getIntent().getBooleanExtra(ArgKeys.CONFIRM_ONLY, false);

        _listView = findViewById(R.id.listView);
        if (updateModelAndUi()) {
            if (!_confirmOnly) {
                _listView.setOnItemClickListener(this);
                _listView.setOnItemLongClickListener(this);
            }

            switch (_state.getIntrinsicState()) {
                case IntrinsicStates.DELETE_ACCEPTATION:
                    showDeleteConfirmationDialog();
                    break;

                case IntrinsicStates.DELETE_DEFINITION:
                    showDeleteDefinitionConfirmationDialog();
                    break;

                case IntrinsicStates.DELETING_FROM_BUNCH:
                    showDeleteFromBunchConfirmationDialog();
                    break;

                case IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                    showDeleteAcceptationFromBunchConfirmationDialog();
                    break;

                case IntrinsicStates.LINKING_CONCEPT:
                    showLinkModeSelectorDialog();
                    break;
            }
        }
        else {
            finish();
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
            AcceptationNavigableItem it = (AcceptationNavigableItem) item;
            if (!it.isDynamic()) {
                final int bunch = DbManager.getInstance().getManager().conceptFromAcceptation(it.getId());
                _state.setDeleteBunchTarget(new DisplayableItem(bunch, it.getText()));
                showDeleteFromBunchConfirmationDialog();
            }
            return true;
        }
        else if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED) {
            AcceptationNavigableItem it = (AcceptationNavigableItem) item;
            if (!it.isDynamic()) {
                _state.setDeleteAcceptationFromBunch(new DisplayableItem(it.getId(), it.getText().toString()));
                showDeleteAcceptationFromBunchConfirmationDialog();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (_model != null) {
            final MenuInflater inflater = new MenuInflater(this);

            if (_confirmOnly) {
                inflater.inflate(R.menu.acceptation_details_activity_confirm, menu);
            }
            else {
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
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemBunchChildrenQuiz:
                QuizSelectorActivity.open(this, _model.concept);
                return true;

            case R.id.menuItemEdit:
                WordEditorActivity.open(this, REQUEST_CODE_EDIT, _staticAcceptation);
                return true;

            case R.id.menuItemLinkConcept:
                AcceptationPickerActivity.open(this, REQUEST_CODE_LINKED_ACCEPTATION, _model.concept);
                return true;

            case R.id.menuItemIncludeAcceptation:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_ACCEPTATION);
                return true;

            case R.id.menuItemIncludeInBunch:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_BUNCH);
                return true;

            case R.id.menuItemDeleteAcceptation:
                _state.setDeletingAcceptation();
                showDeleteConfirmationDialog();
                return true;

            case R.id.menuItemIncludeDefinition:
                DefinitionEditorActivity.open(this, REQUEST_CODE_PICK_DEFINITION);
                return true;

            case R.id.menuItemDeleteDefinition:
                _state.setDeletingSupertype();
                showDeleteDefinitionConfirmationDialog();
                return true;

            case R.id.menuItemConfirm:
                final Intent intent = new Intent();
                intent.putExtra(ResultKeys.STATIC_ACCEPTATION, _staticAcceptation);
                intent.putExtra(ResultKeys.DYNAMIC_ACCEPTATION, getIntent().getIntExtra(ArgKeys.DYNAMIC_ACCEPTATION, 0));
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;

            case R.id.menuItemNewSentence:
                SentenceEditorActivity.openWithStaticAcceptation(this, REQUEST_CODE_CREATE_SENTENCE, _staticAcceptation);
                return true;
        }

        return false;
    }

    private void showDeleteFromBunchConfirmationDialog() {
        final String message = getString(R.string.deleteFromBunchConfirmationText, _state.getDeleteTarget().text);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteTarget())
                .create().show();
    }

    private void showDeleteAcceptationFromBunchConfirmationDialog() {
        final String message = getString(R.string.deleteAcceptationFromBunchConfirmationText, _state.getDeleteTarget().text);
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
    public void onClick(DialogInterface dialog, int which) {
        final LangbookManager manager = DbManager.getInstance().getManager();
        switch (_state.getIntrinsicState()) {
            case IntrinsicStates.DELETE_ACCEPTATION:
                deleteAcceptation();
                break;

            case IntrinsicStates.DELETE_DEFINITION:
                _state.clearDeletingDefinition();
                if (!manager.removeDefinition(_model.concept)) {
                    throw new AssertionError();
                }

                if (updateModelAndUi()) {
                    invalidateOptionsMenu();
                }
                showFeedback(getString(R.string.deleteDefinitionFeedback));
                break;

            case IntrinsicStates.LINKING_CONCEPT:
                if (_state.getDialogCheckedOption() == 0) {
                    final boolean ok = manager.shareConcept(_state.getLinkedAcceptation(), _model.concept);
                    showFeedback(ok? "Concept shared" : "Unable to shared concept");
                }
                else {
                    manager.duplicateAcceptationWithThisConcept(_state.getLinkedAcceptation(), _model.concept);
                    showFeedback("Acceptation linked");
                }
                _state.clearLinkedAcceptation();
                updateModelAndUi();
                break;

            case IntrinsicStates.DELETING_FROM_BUNCH:
                final DisplayableItem item = _state.getDeleteTarget();
                final int bunch = item.id;
                final String bunchText = item.text;
                _state.clearDeleteTarget();

                if (!manager.removeAcceptationFromBunch(bunch, _staticAcceptation)) {
                    throw new AssertionError();
                }

                updateModelAndUi();
                showFeedback(getString(R.string.deleteFromBunchFeedback, bunchText));
                break;

            case IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                final DisplayableItem itemToDelete = _state.getDeleteTarget();
                final int accIdToDelete = itemToDelete.id;
                final String accToDeleteText = itemToDelete.text;
                _state.clearDeleteTarget();

                if (!manager.removeAcceptationFromBunch(_model.concept, accIdToDelete)) {
                    throw new AssertionError();
                }

                updateModelAndUi();
                showFeedback(getString(R.string.deleteAcceptationFromBunchFeedback, accToDeleteText));
                break;

            default:
                throw new AssertionError("Unable to handle state " + _state.getIntrinsicState());
        }
    }

    private void deleteAcceptation() {
        if (DbManager.getInstance().getManager().removeAcceptation(_staticAcceptation)) {
            showFeedback(getString(R.string.deleteAcceptationFeedback));
            finish();
        }
        else {
            showFeedback(getString(R.string.unableToDelete));
            _state.clearDeletingAcceptation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CLICK_NAVIGATION) {
            updateModelAndUi();
        }
        else if (resultCode == RESULT_OK) {
            final LangbookManager manager = DbManager.getInstance().getManager();
            if (requestCode == REQUEST_CODE_LINKED_ACCEPTATION) {
                final boolean usedConcept = data
                        .getBooleanExtra(AcceptationPickerActivity.ResultKeys.CONCEPT_USED, false);
                if (!usedConcept) {
                    _state.setLinkedAcceptation(data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0));
                    showLinkModeSelectorDialog();
                }
                else {
                    updateModelAndUi();
                }
            }
            else if (requestCode == REQUEST_CODE_PICK_ACCEPTATION) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                final int message = manager.addAcceptationInBunch(_model.concept, pickedAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                updateModelAndUi();
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_BUNCH) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                final int pickedBunch = (pickedAcceptation != 0)? manager.conceptFromAcceptation(pickedAcceptation) : 0;
                final int message = manager.addAcceptationInBunch(pickedBunch, _staticAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                updateModelAndUi();
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_DEFINITION) {
                final DefinitionEditorActivity.State values = data.getParcelableExtra(DefinitionEditorActivity.ResultKeys.VALUES);
                manager.addDefinition(values.baseConcept, _model.concept, values.complements);
                showFeedback(getString(R.string.includeSupertypeOk));
                if (updateModelAndUi()) {
                    invalidateOptionsMenu();
                }
            }
            else if (requestCode == REQUEST_CODE_EDIT || requestCode == REQUEST_CODE_CREATE_SENTENCE) {
                updateModelAndUi();
            }
        }
    }

    private void showLinkModeSelectorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.linkDialogTitle)
                .setPositiveButton(R.string.linkDialogButton, this)
                .setSingleChoiceItems(R.array.linkDialogOptions, 0, this::onLinkDialogChoiceChecked)
                .setOnCancelListener(dialog -> _state.clearLinkedAcceptation())
                .create().show();
    }

    private void onLinkDialogChoiceChecked(DialogInterface dialog, int which) {
        _state.setDialogCheckedOption(which);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
