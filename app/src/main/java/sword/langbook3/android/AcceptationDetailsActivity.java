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

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.Map;
import sword.langbook3.android.AcceptationDetailsActivityState.IntrinsicStates;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.AgentNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationArrayItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.SentenceNavigableItem;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.AcceptationDetailsModel.InvolvedAgentResultFlags;
import sword.langbook3.android.models.DerivedAcceptationResult;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.SynonymTranslationResult;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;

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

    private AlphabetId _preferredAlphabet;
    private AcceptationId _acceptation;
    private AcceptationDetailsModel<LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId> _model;
    private int _dbWriteVersion;
    private boolean _confirmOnly;

    private boolean _hasDefinition;

    private AcceptationDetailsActivityState _state;

    private boolean _shouldShowBunchChildrenQuizMenuOption;
    private ListView _listView;
    private AcceptationDetailsAdapter _listAdapter;

    public static void open(Context context, AcceptationId acceptation) {
        Intent intent = new Intent(context, AcceptationDetailsActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        context.startActivity(intent);
    }

    public static void open(Activity activity, int requestCode, AcceptationId acceptation, boolean confirmOnly) {
        Intent intent = new Intent(activity, AcceptationDetailsActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);

        if (confirmOnly) {
            intent.putExtra(ArgKeys.CONFIRM_ONLY, true);
        }

        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableList<AcceptationDetailsAdapter.Item> getAdapterItems() {
        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> result = new ImmutableList.Builder<>();

        final ImmutableSet<AlphabetId> commonAlphabets = _model.correlationIds
                .map(id -> _model.correlations.get(id).keySet())
                .reduce((set1, set2) -> set1.filter(set2::contains), ImmutableHashSet.empty());
        if (commonAlphabets.size() > 1) {
            final AlphabetId mainAlphabet = commonAlphabets.valueAt(0);
            final AlphabetId pronunciationAlphabet = commonAlphabets.valueAt(1);
            result.add(new CorrelationArrayItem(_model.correlationIds, _model.correlations, mainAlphabet, pronunciationAlphabet, !_confirmOnly));
        }

        result.add(new HeaderItem(getString(R.string.accDetailsSectionSummary, _acceptation)));
        result.add(new NonNavigableItem(getString(R.string.accDetailsSectionLanguage) + ": " + _model.language.text));

        _hasDefinition = _model.baseConceptAcceptationId != null;
        if (_hasDefinition) {
            String baseText = getString(R.string.accDetailsSectionDefinition) + ": " + _model.baseConceptText;
            String complementsText = _model.definitionComplementTexts.reduce((a, b) -> a + ", " + b, null);
            String definitionText = (complementsText != null)? baseText + " (" + complementsText + ")" : baseText;
            result.add(new AcceptationNavigableItem(_model.baseConceptAcceptationId, definitionText, false));
            _hasDefinition = true;
        }

        final String agentTextPrefix = "Agent #";
        if (_model.originalAcceptationId != null) {
            final String text = getString(R.string.accDetailsSectionOrigin) + ": " + _model.originalAcceptationText;
            result.add(new AcceptationNavigableItem(_model.originalAcceptationId, text, false));

            final String ruleText = getString(R.string.accDetailsSectionAppliedRule) + ": " + _model.ruleTexts.get(_model.appliedRuleId);
            result.add(new AcceptationNavigableItem(_model.appliedRuleAcceptationId, ruleText, false));

            final String agentText = getString(R.string.accDetailsSectionAppliedAgent) + ": " + agentTextPrefix + _model.appliedAgentId;
            result.add(new AgentNavigableItem(_model.appliedAgentId, agentText));
        }

        boolean subTypeFound = false;
        for (ImmutableMap.Entry<AcceptationId, String> subtype : _model.subtypes.entries()) {
            if (!subTypeFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSubtypes)));
                subTypeFound = true;
            }

            result.add(new AcceptationNavigableItem(subtype.key(), subtype.value(), false));
        }

        boolean synonymFound = false;
        for (Map.Entry<AcceptationId, SynonymTranslationResult<LanguageId>> entry : _model.synonymsAndTranslations.entries()) {
            if (_model.language.id.equals(entry.value().language)) {
                if (!synonymFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionSynonyms)));
                    synonymFound = true;
                }

                result.add(new AcceptationNavigableItem(entry.key(), entry.value().text, entry.value().dynamic));
            }
        }

        boolean translationFound = false;
        for (Map.Entry<AcceptationId, SynonymTranslationResult<LanguageId>> entry : _model.synonymsAndTranslations.entries()) {
            final LanguageId language = entry.value().language;
            if (!_model.language.id.equals(language)) {
                if (!translationFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionTranslations)));
                    translationFound = true;
                }

                final String langStr = _model.languageTexts.get(language, null);
                result.add(new AcceptationNavigableItem(entry.key(), "" + langStr + " -> " + entry.value().text, entry.value().dynamic));
            }
        }

        final ImmutableSet<AlphabetId> alphabets = _model.texts.keySet();
        boolean acceptationSharingCorrelationArrayFound = false;
        final ImmutableSet<AcceptationId> accsSharingCorrelationArray = _model.acceptationsSharingTexts.filter(alphabets::equalSet).keySet();
        for (AcceptationId acc : accsSharingCorrelationArray) {
            if (!acceptationSharingCorrelationArrayFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingCorrelationArray)));
                acceptationSharingCorrelationArrayFound = true;
            }

            result.add(new AcceptationNavigableItem(acc, _model.getTitle(_preferredAlphabet), false));
        }

        boolean acceptationSharingTextsFound = false;
        for (AcceptationId acc : _model.acceptationsSharingTexts.keySet().filterNot(accsSharingCorrelationArray::contains)) {
            if (!acceptationSharingTextsFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingTexts)));
                acceptationSharingTextsFound = true;
            }

            final String text = _model.acceptationsSharingTextsDisplayableTexts.get(acc);
            result.add(new AcceptationNavigableItem(acc, text, false));
        }

        boolean parentBunchFound = false;
        for (DynamizableResult<AcceptationId> r : _model.bunchesWhereAcceptationIsIncluded) {
            if (!parentBunchFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionBunchesWhereIncluded)));
                parentBunchFound = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED,
                    r.id, r.text, r.dynamic));
        }

        boolean morphologyFound = false;
        final ImmutableMap<AcceptationId, DerivedAcceptationResult> derivedAcceptations = _model.derivedAcceptations;
        final int derivedAcceptationsCount = derivedAcceptations.size();
        for (int i = 0; i < derivedAcceptationsCount; i++) {
            final AcceptationId accId = derivedAcceptations.keyAt(i);
            final DerivedAcceptationResult r = derivedAcceptations.valueAt(i);
            if (!morphologyFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionDerivedAcceptations)));
                morphologyFound = true;
            }

            final String ruleText = _model.ruleTexts.get(_model.agentRules.get(r.agent));
            result.add(new AcceptationNavigableItem(accId, ruleText + " -> " + r.text, true));
        }

        _shouldShowBunchChildrenQuizMenuOption = false;
        boolean bunchChildFound = false;
        for (DynamizableResult<AcceptationId> r : _model.bunchChildren) {
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
            final int sentenceId = _model.sampleSentences.keyAt(i);
            final String sentence = _model.sampleSentences.valueAt(i);

            if (!sentenceFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSampleSentences)));
                sentenceFound = true;
            }

            result.add(new SentenceNavigableItem(sentenceId, sentence));
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

        for (IntKeyMap.Entry<RuleId> entry : _model.agentRules.entries()) {
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
        _acceptation = AcceptationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ACCEPTATION);
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
                final BunchId bunch = conceptAsBunchId(DbManager.getInstance().getManager().conceptFromAcceptation(it.getId()));
                _state.setDeleteBunchTarget(new DisplayableItem<>(bunch, it.getText()));
                showDeleteFromBunchConfirmationDialog();
            }
            return true;
        }
        else if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED) {
            AcceptationNavigableItem it = (AcceptationNavigableItem) item;
            if (!it.isDynamic()) {
                _state.setDeleteAcceptationFromBunch(new DisplayableItem<>(it.getId(), it.getText().toString()));
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
                QuizSelectorActivity.open(this, conceptAsBunchId(_model.concept));
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
                AcceptationIdBundler.writeAsIntentExtra(intent, ResultKeys.ACCEPTATION, _acceptation);
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;

            case R.id.menuItemNewSentence:
                SentenceEditorActivity.openWithAcceptation(this, REQUEST_CODE_CREATE_SENTENCE, _acceptation);
                return true;

            case R.id.menuItemNewAgentAsSource:
                AgentEditorActivity.openWithSource(this, conceptAsBunchId(_model.concept));
                return true;

            case R.id.menuItemNewAgentAsDiff:
                AgentEditorActivity.openWithDiff(this, conceptAsBunchId(_model.concept));
                return true;

            case R.id.menuItemNewAgentAsTarget:
                AgentEditorActivity.openWithTarget(this, conceptAsBunchId(_model.concept));
                return true;
        }

        return false;
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
    public void onClick(DialogInterface dialog, int which) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
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

            case IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                final DisplayableItem<AcceptationId> itemToDelete = _state.getDeleteTargetAcceptation();
                final AcceptationId accIdToDelete = itemToDelete.id;
                final String accToDeleteText = itemToDelete.text;
                _state.clearDeleteTarget();

                if (!manager.removeAcceptationFromBunch(conceptAsBunchId(_model.concept), accIdToDelete)) {
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
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            if (requestCode == REQUEST_CODE_LINKED_ACCEPTATION) {
                final boolean usedConcept = data
                        .getBooleanExtra(AcceptationPickerActivity.ResultKeys.CONCEPT_USED, false);
                if (!usedConcept) {
                    _state.setLinkedAcceptation(AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.DYNAMIC_ACCEPTATION));
                    showLinkModeSelectorDialog();
                }
            }
            else if (requestCode == REQUEST_CODE_PICK_ACCEPTATION) {
                final AcceptationId pickedAcceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
                final int message = manager.addAcceptationInBunch(conceptAsBunchId(_model.concept), pickedAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_BUNCH) {
                final AcceptationId pickedAcceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
                final BunchId pickedBunch = (pickedAcceptation != null)? conceptAsBunchId(manager.conceptFromAcceptation(pickedAcceptation)) : null;
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
