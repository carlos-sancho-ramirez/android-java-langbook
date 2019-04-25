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
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.List;
import sword.database.Database;
import sword.database.DbInsertQuery;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.langbook3.android.AcceptationDetailsActivityState.IntrinsicStates;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.AgentNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationArrayItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.RuleNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.SentenceNavigableItem;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.IdentifiableResult;
import sword.langbook3.android.db.LangbookDatabase;
import sword.langbook3.android.db.LangbookDbSchema.AcceptationsTable;
import sword.langbook3.android.db.LangbookDbSchema.AgentsTable;
import sword.langbook3.android.db.LangbookDbSchema.AlphabetsTable;
import sword.langbook3.android.db.LangbookDbSchema.BunchAcceptationsTable;
import sword.langbook3.android.db.LangbookDbSchema.BunchConceptsTable;
import sword.langbook3.android.db.LangbookDbSchema.BunchSetsTable;
import sword.langbook3.android.db.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.db.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.db.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.db.LangbookReadableDatabase.InvolvedAgentResultFlags;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.SynonymTranslationResult;

import static sword.langbook3.android.db.LangbookDatabase.addAcceptationInBunch;
import static sword.langbook3.android.db.LangbookDatabase.removeAcceptationFromBunch;
import static sword.langbook3.android.db.LangbookDbInserter.insertBunchConcept;
import static sword.langbook3.android.db.LangbookDeleter.deleteBunchConceptForConcept;
import static sword.langbook3.android.db.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationsDetails;

public final class AcceptationDetailsActivity extends Activity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_CREATE_SENTENCE = 1;
    private static final int REQUEST_CODE_EDIT = 2;
    private static final int REQUEST_CODE_LINKED_ACCEPTATION = 3;
    private static final int REQUEST_CODE_PICK_ACCEPTATION = 4;
    private static final int REQUEST_CODE_PICK_BUNCH = 5;
    private static final int REQUEST_CODE_PICK_SUPERTYPE = 6;

    private interface ArgKeys {
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
        String CONFIRM_ONLY = BundleKeys.CONFIRM_ONLY;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private int _preferredAlphabet;
    private int _staticAcceptation;
    private AcceptationDetailsModel _model;
    private boolean _confirmOnly;

    private IdentifiableResult _definition;

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

        result.add(new HeaderItem("Displaying details for acceptation " + _staticAcceptation));
        result.add(new NonNavigableItem("Language: " + _model.language.text));

        for (IntKeyMap.Entry<String> entry : _model.supertypes.entries()) {
            _definition = new IdentifiableResult(entry.key(), entry.value());
            result.add(new AcceptationNavigableItem(entry.key(), "Type of: " + _definition.text, false));
        }

        boolean subTypeFound = false;
        for (ImmutableIntKeyMap.Entry<String> subtype : _model.subtypes.entries()) {
            if (!subTypeFound) {
                result.add(new HeaderItem("Subtypes"));
                subTypeFound = true;
            }

            result.add(new AcceptationNavigableItem(subtype.key(), subtype.value(), false));
        }

        boolean synonymFound = false;
        for (IntKeyMap.Entry<SynonymTranslationResult> entry : _model.synonymsAndTranslations.entries()) {
            if (entry.value().language == _model.language.id) {
                if (!synonymFound) {
                    result.add(new HeaderItem("Synonyms"));
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
                    result.add(new HeaderItem("Translations"));
                    translationFound = true;
                }

                final String langStr = _model.languageTexts.get(language, null);
                result.add(new AcceptationNavigableItem(entry.key(), "" + langStr + " -> " + entry.value().text, false));
            }
        }

        boolean parentBunchFound = false;
        for (DynamizableResult r : _model.bunchesWhereAcceptationIsIncluded) {
            if (!parentBunchFound) {
                result.add(new HeaderItem("Bunches where included"));
                parentBunchFound = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED,
                    r.id, r.text, r.dynamic));
        }

        boolean morphologyFound = false;
        final ImmutableList<MorphologyResult> morphologyResults = _model.morphologies;
        for (MorphologyResult r : morphologyResults) {
            if (!morphologyFound) {
                result.add(new HeaderItem("Morphologies"));
                morphologyFound = true;
            }

            result.add(new RuleNavigableItem(r.dynamicAcceptation, r.ruleText + " -> " + r.text));
        }

        boolean bunchChildFound = false;
        for (DynamizableResult r : _model.bunchChildren) {
            if (!bunchChildFound) {
                result.add(new HeaderItem("Acceptations included in this bunch"));
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
                result.add(new HeaderItem("Sample sentences"));
                sentenceFound = true;
            }

            result.add(new SentenceNavigableItem(symbolArray, sentence));
        }

        boolean agentFound = false;
        for (IntPairMap.Entry entry : _model.involvedAgents.entries()) {
            if (!agentFound) {
                result.add(new HeaderItem("Involved agents"));
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

        for (MorphologyResult r : morphologyResults) {
            if (!agentFound) {
                result.add(new HeaderItem("Involved agents"));
                agentFound = true;
            }

            final String text = "Agent #" + r.agent + " (" + r.ruleText + ')';
            result.add(new AgentNavigableItem(r.agent, text));
        }

        return result.build();
    }

    private boolean updateModelAndUi() {
        final Database db = DbManager.getInstance().getDatabase();
        _model = getAcceptationsDetails(db, _staticAcceptation, _preferredAlphabet);
        if (_model != null) {
            setTitle(_model.getTitle(_preferredAlphabet));
            _listAdapter = new AcceptationDetailsAdapter(getAdapterItems());
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

                case IntrinsicStates.DELETE_SUPERTYPE:
                    showDeleteSupertypeConfirmationDialog();
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
        _listAdapter.getItem(position).navigate(this);
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
                final int bunch = conceptFromAcceptation(DbManager.getInstance().getDatabase(), it.getId());
                _state.setDeleteBunchTarget(new DisplayableItem(bunch, it.getText().toString()));
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

                if (_definition == null) {
                    inflater.inflate(R.menu.acceptation_details_activity_include_supertype, menu);
                }
                else {
                    inflater.inflate(R.menu.acceptation_details_activity_delete_supertype, menu);
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

            case R.id.menuItemIncludeSupertype:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_SUPERTYPE);
                return true;

            case R.id.menuItemDeleteSupertype:
                _state.setDeletingSupertype();
                showDeleteSupertypeConfirmationDialog();
                return true;

            case R.id.menuItemConfirm:
                final Intent intent = new Intent();
                intent.putExtra(ResultKeys.ACCEPTATION, _staticAcceptation);
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;

            case R.id.menuItemNewSentence:
                SentenceEditorActivity.open(this, REQUEST_CODE_CREATE_SENTENCE);
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

    private void showDeleteSupertypeConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteSupertypeConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeletingSupertype())
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Database db = DbManager.getInstance().getDatabase();
        switch (_state.getIntrinsicState()) {
            case IntrinsicStates.DELETE_ACCEPTATION:
                deleteAcceptation();
                break;

            case IntrinsicStates.DELETE_SUPERTYPE:
                _state.clearDeletingSupertype();
                if (!deleteBunchConceptForConcept(db, _model.concept)) {
                    throw new AssertionError();
                }

                if (updateModelAndUi()) {
                    invalidateOptionsMenu();
                }
                showFeedback(getString(R.string.deleteSupertypeFeedback));
                break;

            case IntrinsicStates.LINKING_CONCEPT:
                if (_state.getDialogCheckedOption() == 0) {
                    // Sharing concept
                    showFeedback(shareConcept()? "Concept shared" : "Unable to shared concept");
                }
                else {
                    // Duplicate concepts
                    duplicateAcceptationWithThisConcept();
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

                if (!removeAcceptationFromBunch(db, bunch, _staticAcceptation)) {
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

                if (!removeAcceptationFromBunch(db, _model.concept, accIdToDelete)) {
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
        if (!LangbookDatabase.removeAcceptation(DbManager.getInstance().getDatabase(), _staticAcceptation)) {
            throw new AssertionError();
        }

        showFeedback(getString(R.string.deleteAcceptationFeedback));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Database db = DbManager.getInstance().getDatabase();
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
                final int message = addAcceptationInBunch(db, _model.concept, pickedAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                updateModelAndUi();
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_BUNCH) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                final int pickedBunch = (pickedAcceptation != 0)? conceptFromAcceptation(db, pickedAcceptation) : 0;
                final int message = addAcceptationInBunch(db, pickedBunch, _staticAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                updateModelAndUi();
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_SUPERTYPE) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                final int pickedConcept = (pickedAcceptation != 0)? conceptFromAcceptation(db, pickedAcceptation) : 0;
                insertBunchConcept(db, pickedConcept, _model.concept);
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

    private int getLinkedConcept() {
        final AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), _state.getLinkedAcceptation())
                .select(table.getConceptColumnIndex());
        return DbManager.getInstance().selectSingleRow(query).get(0).toInt();
    }

    private ImmutableIntSet getImmutableConcepts() {
        final AlphabetsTable table = Tables.alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getLanguageColumnIndex());

        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        for (List<DbValue> row : DbManager.getInstance().attach(query)) {
            builder.add(row.get(0).toInt());
            builder.add(row.get(1).toInt());
        }

        return builder.build();
    }

    private static void updateBunchConceptConcepts(int oldConcept, int newConcept) {
        final BunchConceptsTable table = Tables.bunchConcepts;
        final Database db = DbManager.getInstance().getDatabase();

        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldConcept)
                .put(table.getBunchColumnIndex(), newConcept)
                .build();
        db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        db.update(query);
    }

    private static void updateBunchAcceptationConcepts(int oldConcept, int newConcept) {
        final BunchAcceptationsTable table = Tables.bunchAcceptations;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldConcept)
                .put(table.getBunchColumnIndex(), newConcept)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateQuestionRules(int oldRule, int newRule) {
        final QuestionFieldSets table = Tables.questionFieldSets;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateQuizBunches(int oldBunch, int newBunch) {
        final QuizDefinitionsTable table = Tables.quizDefinitions;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateBunchSetBunches(int oldBunch, int newBunch) {
        final BunchSetsTable table = Tables.bunchSets;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateAgentRules(int oldRule, int newRule) {
        final AgentsTable table = Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateAgentTargetBunches(int oldBunch, int newBunch) {
        final AgentsTable table = Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getTargetBunchColumnIndex(), oldBunch)
                .put(table.getTargetBunchColumnIndex(), newBunch)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateAcceptationConcepts(int oldConcept, int newConcept) {
        final AcceptationsTable table = Tables.acceptations;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private boolean shareConcept() {
        final int linkedConcept = getLinkedConcept();

        final ImmutableIntSet immutableConcepts = getImmutableConcepts();
        if (immutableConcepts.contains(linkedConcept)) {
            return false;
        }

        final int oldConcept = _model.concept;
        if (oldConcept == 0 || linkedConcept == 0) {
            throw new AssertionError();
        }

        updateBunchConceptConcepts(oldConcept, linkedConcept);
        updateBunchAcceptationConcepts(oldConcept, linkedConcept);
        updateQuestionRules(oldConcept, linkedConcept);
        updateQuizBunches(oldConcept, linkedConcept);
        updateBunchSetBunches(oldConcept, linkedConcept);
        updateAgentRules(oldConcept, linkedConcept);
        updateAgentTargetBunches(oldConcept, linkedConcept);
        updateAcceptationConcepts(oldConcept, linkedConcept);
        return true;
    }

    private void duplicateAcceptationWithThisConcept() {
        final int concept = _model.concept;
        if (concept == 0) {
            throw new AssertionError();
        }

        final int linkedAcceptation = _state.getLinkedAcceptation();
        final AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), linkedAcceptation)
                .select(table.getCorrelationArrayColumnIndex());

        final Database db = DbManager.getInstance().getDatabase();
        final DbResult result = db.select(query);
        final List<DbValue> row = result.next();
        if (result.hasNext()) {
            throw new AssertionError();
        }

        final int correlationArray = row.get(0).toInt();

        final DbInsertQuery insertQuery = new DbInsertQuery.Builder(table)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();

        final int newAccId = db.insert(insertQuery);

        final StringQueriesTable strings = Tables.stringQueries;
        final DbQuery stringsQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), linkedAcceptation)
                .where(strings.getMainAcceptationColumnIndex(), linkedAcceptation)
                .select(strings.getStringAlphabetColumnIndex(), strings.getStringColumnIndex(), strings.getMainStringColumnIndex());

        final ImmutableIntList.Builder alphabetsBuilder = new ImmutableIntList.Builder();
        final ImmutableList.Builder<String> stringsBuilder = new ImmutableList.Builder<>();
        final ImmutableList.Builder<String> mainStringsBuilder = new ImmutableList.Builder<>();

        for (List<DbValue> r : DbManager.getInstance().attach(stringsQuery)) {
            alphabetsBuilder.add(r.get(0).toInt());
            stringsBuilder.add(r.get(1).toText());
            mainStringsBuilder.add(r.get(2).toText());
        }

        final ImmutableIntList alphabets = alphabetsBuilder.build();
        final ImmutableList<String> strs = stringsBuilder.build();
        final ImmutableList<String> mainStrs = mainStringsBuilder.build();
        final int length = alphabets.size();

        for (int i = 0; i < length; i++) {
            final DbInsertQuery iQuery = new DbInsertQuery.Builder(strings)
                    .put(strings.getDynamicAcceptationColumnIndex(), newAccId)
                    .put(strings.getMainAcceptationColumnIndex(), newAccId)
                    .put(strings.getStringAlphabetColumnIndex(), alphabets.valueAt(i))
                    .put(strings.getStringColumnIndex(), strs.valueAt(i))
                    .put(strings.getMainStringColumnIndex(), mainStrs.valueAt(i))
                    .build();

            if (db.insert(iQuery) == null) {
                throw new AssertionError();
            }
        }
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
