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

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.models.AgentRegister;

import static sword.langbook3.android.SearchActivity.AGENT_QUERY_PREFIX;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;

public final class AgentDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_CLICK_NAVIGATION = 1;
    private static final int REQUEST_CODE_EDIT_AGENT = 2;

    private interface ArgKeys {
        String AGENT = BundleKeys.AGENT;
    }

    private interface SavedKeys {
        String DELETE_DIALOG_PRESENT = "ddp";
    }

    public static void open(Context context, int agent) {
        Intent intent = new Intent(context, AgentDetailsActivity.class);
        intent.putExtra(ArgKeys.AGENT, agent);
        context.startActivity(intent);
    }

    private int _preferredAlphabet;
    int _agentId;

    boolean _deleteDialogPresent;

    AgentRegister _register;
    private boolean _uiJustUpdated;

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
        _agentId = getIntent().getIntExtra(ArgKeys.AGENT, 0);
        if (savedInstanceState != null) {
            _deleteDialogPresent = savedInstanceState.getBoolean(SavedKeys.DELETE_DIALOG_PRESENT);
        }

        setTitle(AGENT_QUERY_PREFIX + '#' + _agentId);
        updateUi();
        _uiJustUpdated = true;
    }

    private static void addCorrelationSection(LangbookChecker checker, String title, int correlationId, SyncCacheIntKeyNonNullValueMap<String> alphabetTexts, ImmutableList.Builder<AcceptationDetailsAdapter.Item> builder) {
        boolean headerAdded = false;
        ImmutableIntKeyMap<String> matcher = checker.getCorrelationWithText(correlationId);
        for (int i = 0; i < matcher.size(); i++) {
            if (!headerAdded) {
                headerAdded = true;
                builder.add(new AcceptationDetailsAdapter.HeaderItem(title));
            }

            final int alphabet = matcher.keyAt(i);
            final String alphabetText = alphabetTexts.get(alphabet);
            final String text = alphabetText + " -> " + matcher.valueAt(i);
            builder.add(new AcceptationDetailsAdapter.NonNavigableItem(text));
        }
    }

    private void updateUi() {
        final LangbookChecker checker = DbManager.getInstance().getManager();
        _register = checker.getAgentRegister(_agentId);

        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> builder = new ImmutableList.Builder<>();
        if (_register.targetBunch != NO_BUNCH) {
            builder.add(new AcceptationDetailsAdapter.HeaderItem("Target bunches"));
            DisplayableItem targetResult = checker.readConceptAcceptationAndText(_register.targetBunch, _preferredAlphabet);
            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(targetResult.id, targetResult.text, false));
        }

        boolean headerAdded = false;
        for (DisplayableItem r : checker.readBunchSetAcceptationsAndTexts(_register.sourceBunchSetId, _preferredAlphabet)) {
            if (!headerAdded) {
                headerAdded = true;
                builder.add(new AcceptationDetailsAdapter.HeaderItem("Source bunches"));
            }

            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(r.id, r.text, false));
        }

        headerAdded = false;
        for (DisplayableItem r : checker.readBunchSetAcceptationsAndTexts(_register.diffBunchSetId, _preferredAlphabet)) {
            if (!headerAdded) {
                headerAdded = true;
                builder.add(new AcceptationDetailsAdapter.HeaderItem("Diff bunches"));
            }

            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(r.id, r.text, false));
        }

        final SyncCacheIntKeyNonNullValueMap<String> alphabetTexts = new SyncCacheIntKeyNonNullValueMap<>(alphabet -> checker.readConceptText(alphabet, _preferredAlphabet));

        addCorrelationSection(checker, "Start Matcher", _register.startMatcherId, alphabetTexts, builder);
        addCorrelationSection(checker, "Start Adder", _register.startAdderId, alphabetTexts, builder);
        addCorrelationSection(checker, "End Matcher", _register.endMatcherId, alphabetTexts, builder);
        addCorrelationSection(checker, "End Adder", _register.endAdderId, alphabetTexts, builder);

        if (_register.rule != 0) {
            builder.add(new AcceptationDetailsAdapter.HeaderItem("Applying rules"));

            final DisplayableItem ruleResult = checker.readConceptAcceptationAndText(_register.rule, _preferredAlphabet);
            builder.add(new AcceptationDetailsAdapter.AcceptationNavigableItem(ruleResult.id, ruleResult.text, false));
        }

        _listView.setAdapter(new AcceptationDetailsAdapter(this, REQUEST_CODE_CLICK_NAVIGATION, builder.build()));

        if (_deleteDialogPresent) {
            showDeleteConfirmationDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDIT_AGENT && resultCode == RESULT_OK && !_uiJustUpdated) {
            updateUi();
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
        switch (item.getItemId()) {
            case R.id.menuItemEdit:
                AgentEditorActivity.open(this, REQUEST_CODE_EDIT_AGENT, _agentId);
                return true;

            case R.id.menuItemDeleteAgent:
                _deleteDialogPresent = true;
                showDeleteConfirmationDialog();
                return true;

            default:
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        _uiJustUpdated = false;
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
