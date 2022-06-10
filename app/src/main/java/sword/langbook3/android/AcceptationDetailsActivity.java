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
import sword.collections.IntValueMap;
import sword.collections.Map;
import sword.langbook3.android.AcceptationDetailsActivityState.IntrinsicStates;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.AgentNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationArrayItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.SentenceNavigableItem;
import sword.langbook3.android.controllers.AcceptationPickerController;
import sword.langbook3.android.controllers.CharacterCompositionDefinitionEditorController;
import sword.langbook3.android.controllers.WordEditorController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CharacterCompositionTypeIdManager;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdBundler;
import sword.langbook3.android.models.AcceptationDetails.InvolvedAgentResultFlags;
import sword.langbook3.android.models.AcceptationDetails2;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.IdTextPairResult;
import sword.langbook3.android.models.IdentifiableResult;
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

    public interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private AlphabetId _preferredAlphabet;
    private AcceptationId _acceptation;
    private AcceptationDetails2<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> _model;
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

        final ImmutableList<CorrelationId> correlationIds = _model.getCorrelationIds();
        final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlations = _model.getCorrelations();
        final ImmutableSet<AlphabetId> commonAlphabets = correlationIds
                .map(id -> correlations.get(id).keySet())
                .reduce((set1, set2) -> set1.filter(set2::contains), ImmutableHashSet.empty());
        if (commonAlphabets.size() > 1) {
            final AlphabetId mainAlphabet = commonAlphabets.valueAt(0);
            final AlphabetId pronunciationAlphabet = commonAlphabets.valueAt(1);
            result.add(new CorrelationArrayItem(correlationIds, correlations, mainAlphabet, pronunciationAlphabet, !_confirmOnly));
        }

        final IdTextPairResult<LanguageId> language = _model.getLanguage();
        result.add(new HeaderItem(getString(R.string.accDetailsSectionSummary, _acceptation)));
        result.add(new NonNavigableItem(getString(R.string.accDetailsSectionLanguage) + ": " + language.text));

        final AcceptationId baseConceptAcceptationId = _model.getBaseConceptAcceptationId();
        _hasDefinition = baseConceptAcceptationId != null;
        if (_hasDefinition) {
            String baseText = getString(R.string.accDetailsSectionDefinition) + ": " + _model.getBaseConceptText();
            String complementsText = _model.getDefinitionComplementTexts().reduce((a, b) -> a + ", " + b, null);
            String definitionText = (complementsText != null)? baseText + " (" + complementsText + ")" : baseText;
            result.add(new AcceptationNavigableItem(baseConceptAcceptationId, definitionText, false));
            _hasDefinition = true;
        }

        final CharacterCompositionDefinitionRegister characterCompositionDefinitionRegister = _model.getCharacterCompositionDefinitionRegister();
        if (characterCompositionDefinitionRegister != null) {
            result.add(new HeaderItem(getString(R.string.accDetailsSectionCharacterCompositionDefinition)));
            result.add(new AcceptationDetailsAdapter.CharacterCompositionDefinitionItem(characterCompositionDefinitionRegister));
        }

        final String agentTextPrefix = "Agent #";
        final AcceptationId originalAcceptationId = _model.getOriginalAcceptationId();
        final ImmutableMap<RuleId, String> ruleTexts = _model.getRuleTexts();
        if (originalAcceptationId != null) {
            final String text = getString(R.string.accDetailsSectionOrigin) + ": " + _model.getOriginalAcceptationText();
            result.add(new AcceptationNavigableItem(originalAcceptationId, text, false));

            final String ruleText = getString(R.string.accDetailsSectionAppliedRule) + ": " + ruleTexts.get(_model.getAppliedRuleId());
            result.add(new AcceptationNavigableItem(_model.getAppliedRuleAcceptationId(), ruleText, false));

            final AgentId appliedAgentId = _model.getAppliedAgentId();
            final String agentText = getString(R.string.accDetailsSectionAppliedAgent) + ": " + agentTextPrefix + appliedAgentId;
            result.add(new AgentNavigableItem(appliedAgentId, agentText));
        }

        boolean subTypeFound = false;
        for (ImmutableMap.Entry<AcceptationId, String> subtype : _model.getSubtypes().entries()) {
            if (!subTypeFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSubtypes)));
                subTypeFound = true;
            }

            result.add(new AcceptationNavigableItem(subtype.key(), subtype.value(), false));
        }

        final ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> synonymsAndTranslations = _model.getSynonymsAndTranslations();
        boolean synonymFound = false;
        for (Map.Entry<AcceptationId, SynonymTranslationResult<LanguageId>> entry : synonymsAndTranslations.entries()) {
            if (language.id.equals(entry.value().language)) {
                if (!synonymFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionSynonyms)));
                    synonymFound = true;
                }

                result.add(new AcceptationNavigableItem(entry.key(), entry.value().text, entry.value().dynamic));
            }
        }

        boolean translationFound = false;
        for (Map.Entry<AcceptationId, SynonymTranslationResult<LanguageId>> entry : synonymsAndTranslations.entries()) {
            final LanguageId entryLanguage = entry.value().language;
            if (!language.id.equals(entryLanguage)) {
                if (!translationFound) {
                    result.add(new HeaderItem(getString(R.string.accDetailsSectionTranslations)));
                    translationFound = true;
                }

                final String langStr = _model.getLanguageTexts().get(entryLanguage, null);
                result.add(new AcceptationNavigableItem(entry.key(), "" + langStr + " -> " + entry.value().text, entry.value().dynamic));
            }
        }

        final ImmutableSet<AlphabetId> alphabets = _model.getTexts().keySet();
        final ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> acceptationsSharingTexts = _model.getAcceptationsSharingTexts();
        boolean acceptationSharingCorrelationArrayFound = false;
        final ImmutableSet<AcceptationId> accsSharingCorrelationArray = acceptationsSharingTexts.filter(alphabets::equalSet).keySet();
        for (AcceptationId acc : accsSharingCorrelationArray) {
            if (!acceptationSharingCorrelationArrayFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingCorrelationArray)));
                acceptationSharingCorrelationArrayFound = true;
            }

            result.add(new AcceptationNavigableItem(acc, _model.getTitle(_preferredAlphabet), false));
        }

        boolean acceptationSharingTextsFound = false;
        for (AcceptationId acc : acceptationsSharingTexts.keySet().filterNot(accsSharingCorrelationArray::contains)) {
            if (!acceptationSharingTextsFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsSharingTexts)));
                acceptationSharingTextsFound = true;
            }

            final String text = _model.getAcceptationsSharingTextsDisplayableTexts().get(acc);
            result.add(new AcceptationNavigableItem(acc, text, false));
        }

        boolean parentBunchFound = false;
        for (DynamizableResult<AcceptationId> r : _model.getBunchesWhereAcceptationIsIncluded()) {
            if (!parentBunchFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionBunchesWhereIncluded)));
                parentBunchFound = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED,
                    r.id, r.text, r.dynamic));
        }

        boolean morphologyFound = false;
        final ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> derivedAcceptations = _model.getDerivedAcceptations();
        final ImmutableMap<AgentId, RuleId> agentRules = _model.getAgentRules();
        final int derivedAcceptationsCount = derivedAcceptations.size();
        for (int i = 0; i < derivedAcceptationsCount; i++) {
            final AcceptationId accId = derivedAcceptations.keyAt(i);
            final IdentifiableResult<AgentId> r = derivedAcceptations.valueAt(i);
            if (!morphologyFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionDerivedAcceptations)));
                morphologyFound = true;
            }

            final String ruleText = ruleTexts.get(agentRules.get(r.id));
            result.add(new AcceptationNavigableItem(accId, ruleText + " -> " + r.text, true));
        }

        _shouldShowBunchChildrenQuizMenuOption = false;
        boolean bunchChildFound = false;
        for (DynamizableResult<AcceptationId> r : _model.getBunchChildren()) {
            if (!bunchChildFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionAcceptationsInThisBunch)));
                bunchChildFound = true;
                _shouldShowBunchChildrenQuizMenuOption = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED, r.id, r.text, r.dynamic));
        }

        final ImmutableMap<SentenceId, String> sampleSentences = _model.getSampleSentences();
        boolean sentenceFound = false;
        final int sampleSentenceCount = sampleSentences.size();
        for (int i = 0; i < sampleSentenceCount; i++) {
            final SentenceId sentenceId = sampleSentences.keyAt(i);
            final String sentence = sampleSentences.valueAt(i);

            if (!sentenceFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionSampleSentences)));
                sentenceFound = true;
            }

            result.add(new SentenceNavigableItem(sentenceId, sentence));
        }

        boolean agentFound = false;
        for (IntValueMap.Entry<AgentId> entry : _model.getInvolvedAgents().entries()) {
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

        for (Map.Entry<AgentId, RuleId> entry : agentRules.entries()) {
            if (!agentFound) {
                result.add(new HeaderItem(getString(R.string.accDetailsSectionInvolvedAgents)));
                agentFound = true;
            }

            final String text = "Agent #" + entry.key() + " (" + ruleTexts.get(entry.value()) + ')';
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
        else if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.CHARACTER_COMPOSITION_DEFINITION) {
            // TODO: Display edition confirmation dialog if the composition definition is in use, as it can break things
            CharacterCompositionDefinitionEditorActivity.open(this, new CharacterCompositionDefinitionEditorController(CharacterCompositionTypeIdManager.conceptAsCharacterCompositionTypeId(_model.getConcept())));
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
                QuizSelectorActivity.open(this, conceptAsBunchId(_model.getConcept()));
                return true;

            case R.id.menuItemEdit:
                WordEditorActivity.open(this, new WordEditorController(null, null, _acceptation, null, null, null, true));
                return true;

            case R.id.menuItemLinkConcept:
                AcceptationPickerActivity.open(this, REQUEST_CODE_LINKED_ACCEPTATION, new AcceptationPickerController(_model.getConcept()));
                return true;

            case R.id.menuItemIncludeAcceptation:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_ACCEPTATION, new AcceptationPickerController(null));
                return true;

            case R.id.menuItemIncludeInBunch:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_BUNCH, new AcceptationPickerController(null));
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
                AgentEditorActivity.openWithSource(this, conceptAsBunchId(_model.getConcept()));
                return true;

            case R.id.menuItemNewAgentAsDiff:
                AgentEditorActivity.openWithDiff(this, conceptAsBunchId(_model.getConcept()));
                return true;

            case R.id.menuItemNewAgentAsTarget:
                AgentEditorActivity.openWithTarget(this, conceptAsBunchId(_model.getConcept()));
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
                if (!manager.removeDefinition(_model.getConcept())) {
                    throw new AssertionError();
                }

                if (updateModelAndUi()) {
                    invalidateOptionsMenu();
                }
                showFeedback(getString(R.string.deleteDefinitionFeedback));
                break;

            case IntrinsicStates.LINKING_CONCEPT:
                if (_state.getDialogCheckedOption() == 0) {
                    final boolean ok = manager.shareConcept(_state.getLinkedAcceptation(), _model.getConcept());
                    showFeedback(ok? "Concept shared" : "Unable to shared concept");
                }
                else {
                    manager.duplicateAcceptationWithThisConcept(_state.getLinkedAcceptation(), _model.getConcept());
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
                final DefinitionEditorActivity.State values = data.getParcelableExtra(DefinitionEditorActivity.ResultKeys.VALUES);
                manager.addDefinition(values.baseConcept, _model.getConcept(), values.complements);
                showFeedback(getString(R.string.includeSupertypeOk));
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
