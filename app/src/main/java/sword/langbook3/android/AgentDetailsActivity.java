package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableList;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AgentId;
import sword.langbook3.android.db.AgentIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchSetId;
import sword.langbook3.android.db.CorrelationArrayId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.DisplayableItem;

import static sword.langbook3.android.SearchActivity.AGENT_QUERY_PREFIX;

public final class AgentDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_CLICK_NAVIGATION = 1;

    private interface ArgKeys {
        String AGENT = BundleKeys.AGENT;
    }

    private interface SavedKeys {
        String DELETE_DIALOG_PRESENT = "ddp";
    }

    public static void open(Context context, AgentId agent) {
        Intent intent = new Intent(context, AgentDetailsActivity.class);
        AgentIdBundler.writeAsIntentExtra(intent, ArgKeys.AGENT, agent);
        context.startActivity(intent);
    }

    private AlphabetId _preferredAlphabet;
    AgentId _agentId;

    boolean _deleteDialogPresent;

    AgentRegister<CorrelationId, CorrelationArrayId, BunchSetId, RuleId> _register;
    private int _dbWriteVersion;

    private ListView _listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_activity);

        _listView = findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);

        if (!getIntent().hasExtra(ArgKeys.AGENT)) {
            throw new IllegalArgumentException("agent identifier not provided");
        }

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _agentId = AgentIdBundler.readAsIntentExtra(getIntent(), ArgKeys.AGENT);
        if (savedInstanceState != null) {
            _deleteDialogPresent = savedInstanceState.getBoolean(SavedKeys.DELETE_DIALOG_PRESENT);
        }

        setTitle(AGENT_QUERY_PREFIX + '#' + _agentId);
        updateUi();
    }

    private static void addCorrelationSection(LangbookDbChecker checker, String title, CorrelationId correlationId, SyncCacheMap<AlphabetId, String> alphabetTexts, ImmutableList.Builder<AcceptationDetailsAdapter.Item> builder) {
        boolean headerAdded = false;
        final ImmutableCorrelation<AlphabetId> matcher = checker.getCorrelationWithText(correlationId);
        for (int i = 0; i < matcher.size(); i++) {
            if (!headerAdded) {
                headerAdded = true;
                builder.add(new AcceptationDetailsAdapter.HeaderItem(title));
            }

            final AlphabetId alphabet = matcher.keyAt(i);
            final String alphabetText = alphabetTexts.get(alphabet);
            final String text = alphabetText + " -> " + matcher.valueAt(i);
            builder.add(new AcceptationDetailsAdapter.NonNavigableItem(text));
        }
    }

    private static void addCorrelationArraySection(LangbookDbChecker checker, String title, CorrelationArrayId correlationArrayId, ImmutableList.Builder<AcceptationDetailsAdapter.Item> builder) {
        final ImmutableCorrelationArray<AlphabetId> correlationArray = checker.getCorrelationArrayWithText(correlationArrayId).left;
        if (!correlationArray.isEmpty()) {
            builder.add(new AcceptationDetailsAdapter.HeaderItem(title));

            final String text = CorrelationPickerAdapter.toPlainText(correlationArray, ImmutableHashSet.empty());
            builder.add(new AcceptationDetailsAdapter.NonNavigableItem(text));
        }
    }

    private void updateUi() {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        _register = checker.getAgentRegister(_agentId);

        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> builder = new ImmutableList.Builder<>();
        boolean headerAdded = false;
        for (DisplayableItem<AcceptationId> r : checker.readBunchSetAcceptationsAndTexts(_register.targetBunchSetId, _preferredAlphabet)) {
            if (!headerAdded) {
                headerAdded = true;
                builder.add(new AcceptationDetailsAdapter.HeaderItem(getString(R.string.agentTargetBunchesHeader)));
            }

            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(r.id, r.text, false));
        }

        headerAdded = false;
        for (DisplayableItem<AcceptationId> r : checker.readBunchSetAcceptationsAndTexts(_register.sourceBunchSetId, _preferredAlphabet)) {
            if (!headerAdded) {
                headerAdded = true;
                builder.add(new AcceptationDetailsAdapter.HeaderItem(getString(R.string.agentSourceBunchesHeader)));
            }

            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(r.id, r.text, false));
        }

        headerAdded = false;
        for (DisplayableItem<AcceptationId> r : checker.readBunchSetAcceptationsAndTexts(_register.diffBunchSetId, _preferredAlphabet)) {
            if (!headerAdded) {
                headerAdded = true;
                builder.add(new AcceptationDetailsAdapter.HeaderItem(getString(R.string.agentDiffBunchesHeader)));
            }

            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(r.id, r.text, false));
        }

        final SyncCacheMap<AlphabetId, String> alphabetTexts = new SyncCacheMap<>(alphabet -> checker.readConceptText(alphabet.getConceptId(), _preferredAlphabet));

        addCorrelationSection(checker, getString(R.string.agentStartMatcherHeader), _register.startMatcherId, alphabetTexts, builder);
        addCorrelationArraySection(checker, getString(R.string.agentStartAdderHeader), _register.startAdderId, builder);
        addCorrelationSection(checker, getString(R.string.agentEndMatcherHeader), _register.endMatcherId, alphabetTexts, builder);
        addCorrelationArraySection(checker, getString(R.string.agentEndAdderHeader), _register.endAdderId, builder);

        if (_register.rule != null) {
            builder.add(new AcceptationDetailsAdapter.HeaderItem(getString(R.string.agentRuleHeader)));

            final DisplayableItem<AcceptationId> ruleResult = checker.readConceptAcceptationAndText(_register.rule.getConceptId(), _preferredAlphabet);
            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(ruleResult.id, ruleResult.text, false));
        }

        _dbWriteVersion = DbManager.getInstance().getDatabase().getWriteVersion();
        _listView.setAdapter(new AcceptationDetailsAdapter(this, REQUEST_CODE_CLICK_NAVIGATION, builder.build()));

        if (_deleteDialogPresent) {
            showDeleteConfirmationDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.agent_details_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemEdit) {
            Intentions.editAgent(this, _agentId);
            return true;
        }
        else if (itemId == R.id.menuItemDeleteAgent) {
            _deleteDialogPresent = true;
            showDeleteConfirmationDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        if (DbManager.getInstance().getDatabase().getWriteVersion() != _dbWriteVersion) {
            updateUi();
        }

        super.onResume();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final AcceptationDetailsAdapter.Item item = (AcceptationDetailsAdapter.Item) parent.getAdapter().getItem(position);
        item.navigate(this, REQUEST_CODE_CLICK_NAVIGATION);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (_deleteDialogPresent) {
            outState.putBoolean(SavedKeys.DELETE_DIALOG_PRESENT, true);
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteAgentConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, (dialog, which) -> {
                    _deleteDialogPresent = false;
                    deleteAgent();
                })
                .setOnCancelListener(dialog -> _deleteDialogPresent = false)
                .create().show();
    }

    private void deleteAgent() {
        DbManager.getInstance().getManager().removeAgent(_agentId);
        showFeedback(getString(R.string.deleteAgentFeedback));
        finish();
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
