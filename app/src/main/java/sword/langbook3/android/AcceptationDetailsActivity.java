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
import sword.langbook3.android.AcceptationDetailsAdapter.SentenceNavigableItem;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.AcceptationDetailsModel.InvolvedAgentResultFlags;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.SynonymTranslationResult;

public final class AcceptationDetailsActivity extends Activity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_CLICK_NAVIGATION = 1;
    private static final int REQUEST_CODE_CREATE_SENTENCE = 2;
    private static final int REQUEST_CODE_LINKED_ACCEPTATION = 3;
    private static final int REQUEST_CODE_PICK_ACCEPTATION = 4;
    private static final int REQUEST_CODE_PICK_BUNCH = 5;
    private static final int REQUEST_CODE_PICK_DEFINITION = 6;

    private interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CONFIRM_ONLY = BundleKeys.CONFIRM_ONLY;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private int _preferredAlphabet;
    private int _acceptation;
    private AcceptationDetailsModel _model;
    private int _dbWriteVersion;
    private boolean _confirmOnly;

    private boolean _hasDefinition;

    private AcceptationDetailsActivityState _state;

    private boolean _shouldShowBunchChildrenQuizMenuOption;
    private ListView _listView;
    private AcceptationDetailsAdapter _listAdapter;

    public static void open(Context context, int acceptation) {
        Intent intent = new Intent(context, AcceptationDetailsActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);
        context.startActivity(intent);
    }

    public static void open(Activity activity, int requestCode, int acceptation, boolean confirmOnly) {
        Intent intent = new Intent(activity, AcceptationDetailsActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);

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

        result.add(new HeaderItem(getString(R.string.accDetailsSectionSummary, _acceptation)));
        result.add(new NonNavigableItem(getString(R.string.accDetailsSectionLanguage) + ": " + _model.language.text));

        _hasDefinition = _model.baseConceptAcceptationId != 0;
        if (_hasDefinition) {
            String baseText = getString(R.string.accDetailsSectionDefinition) + ": " + _model.baseConceptText;
            String complementsText = _model.definitionComplementTexts.reduce((a, b) -> a + ", " + b, null);
            String definitionText = (complementsText != null)? baseText + " (" + complementsText + ")" : baseText;
            result.add(new AcceptationNavigableItem(_model.baseConceptAcceptationId, definitionText, false));
            _hasDefinition = true;
        }

        final String agentTextPrefix = "Agent #";
        if (_model.originalAcceptationId != 0) {
            final String text = getString(R.string.accDetailsSectionOrigin) + ": " + _model.originalAcceptationText;
            result.add(new AcceptationNavigableItem(_model.originalAcceptationId, text, false));

            final String ruleText = getString(R.string.accDetailsSectionAppliedRule) + ": " + _model.ruleTexts.get(_model.appliedRuleId);
            result.add(new AcceptationNavigableItem(_model.appliedRuleAcceptationId, ruleText, false));

            final String agentText = getString(R.string.accDetailsSectionAppliedAgent) + ": " + agentTextPrefix + _model.appliedAgentId;
            result.add(new AgentNavigableItem(_model.appliedAgentId, agentText));
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

                result.add(new AcceptationNavigableItem(entry.key(), entry.value().text, entry.value().dynamic));
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
                result.add(new AcceptationNavigableItem(entry.key(), "" + langStr + " -> " + entry.value().text, entry.value().dynamic));
            }
        }

        boolean morphologyLinkedAcceptationFound = false;
        for (IntKeyMap.Entry<ImmutableIntKeyMap<String>> entry : _model.morphologyLinkedAcceptations.entries()) {
            final int dynAcc = entry.key();
            final String morphStr = _model.morphologies.findFirst(morph -> morph.dynamicAcceptation == dynAcc, null).text;
            for (IntKeyMap.Entry<String> innerEntry : entry.value().entries()) {
                if (!morphologyLinkedAcceptationFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionMorphologyLinkedAcceptations)));
                    morphologyLinkedAcceptationFound = true;
                }

                result.add(new AcceptationNavigableItem(innerEntry.key(), "" + morphStr + " -> " + innerEntry.value(), false));
            }
        }

        final ImmutableIntSet alphabets = _model.texts.keySet();
        boolean acceptationSharingCorrelationArrayFound = false;
        final ImmutableIntSet accsSharingCorrelationArray = _model.acceptationsSharingTexts.filter(alphabets::equalSet).keySet();
        for (int acc : accsSharingCorrelationArray) {
            if (!acceptationSharingCorrelationArrayFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingCorrelationArray)));
                acceptationSharingCorrelationArrayFound = true;
            }

            result.add(new AcceptationNavigableItem(acc, _model.getTitle(_preferredAlphabet), false));
        }

        boolean acceptationSharingTextsFound = false;
        for (int acc : _model.acceptationsSharingTexts.keySet().filterNot(accsSharingCorrelationArray::contains)) {
            if (!acceptationSharingTextsFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingTexts)));
                acceptationSharingTextsFound = true;
            }

            final String text = _model.texts.get(_model.acceptationsSharingTexts.get(acc).valueAt(0));
            result.add(new AcceptationNavigableItem(acc, text, false));
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

            final String ruleText = r.rules.reverse().map(_model.ruleTexts::get).reduce((a, b) -> a + " + " + b);
            result.add(new AcceptationNavigableItem(r.dynamicAcceptation, ruleText + " -> " + r.text, true));
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

            final StringBuilder s = new StringBuilder(agentTextPrefix);
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
        _model = DbManager.getInstance().getManager().getAcceptationsDetails(_acceptation, _preferredAlphabet);
        _dbWriteVersion = DbManager.getInstance().getDatabase().getWriteVersion();
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
        setContentView(R.layout.list_activity);

        if (!getIntent().hasExtra(ArgKeys.ACCEPTATION)) {
            throw new IllegalArgumentException("acceptation not provided");
        }

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        if (_state == null) {
            _state = new AcceptationDetailsActivityState();
        }

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _acceptation = getIntent().getIntExtra(ArgKeys.ACCEPTATION, 0);
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
                WordEditorActivity.open(this, _acceptation);
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
                intent.putExtra(ResultKeys.ACCEPTATION, _acceptation);
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;

            case R.id.menuItemNewSentence:
                SentenceEditorActivity.openWithStaticAcceptation(this, REQUEST_CODE_CREATE_SENTENCE, _acceptation);
                return true;

            case R.id.menuItemNewAgentAsSource:
                AgentEditorActivity.openWithSource(this, _model.concept);
                return true;

            case R.id.menuItemNewAgentAsDiff:
                AgentEditorActivity.openWithDiff(this, _model.concept);
                return true;

            case R.id.menuItemNewAgentAsTarget:
                AgentEditorActivity.openWithTarget(this, _model.concept);
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

                if (!manager.removeAcceptationFromBunch(bunch, _acceptation)) {
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

    @Override
    public void onResume() {
        super.onResume();

        if (DbManager.getInstance().getDatabase().getWriteVersion() != _dbWriteVersion) {
            updateModelAndUi();
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final LangbookManager manager = DbManager.getInstance().getManager();
            if (requestCode == REQUEST_CODE_LINKED_ACCEPTATION) {
                final boolean usedConcept = data
                        .getBooleanExtra(AcceptationPickerActivity.ResultKeys.CONCEPT_USED, false);
                if (!usedConcept) {
                    _state.setLinkedAcceptation(data.getIntExtra(AcceptationPickerActivity.ResultKeys.DYNAMIC_ACCEPTATION, 0));
                    showLinkModeSelectorDialog();
                }
            }
            else if (requestCode == REQUEST_CODE_PICK_ACCEPTATION) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, 0);
                final int message = manager.addAcceptationInBunch(_model.concept, pickedAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_BUNCH) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION, 0);
                final int pickedBunch = (pickedAcceptation != 0)? manager.conceptFromAcceptation(pickedAcceptation) : 0;
                final int message = manager.addAcceptationInBunch(pickedBunch, _acceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
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
            else if (requestCode == REQUEST_CODE_CREATE_SENTENCE) {
                final int sentenceId = (data != null)? data.getIntExtra(SentenceEditorActivity.ResultKeys.SENTENCE_ID, 0) : 0;
                if (sentenceId != 0) {
                    SentenceDetailsActivity.open(this, REQUEST_CODE_CLICK_NAVIGATION, sentenceId);
                }
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
