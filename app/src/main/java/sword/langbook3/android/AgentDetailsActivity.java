package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.MutableIntKeyMap;
import sword.langbook3.android.LangbookDbSchema.AgentsTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbDeleteQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

import static sword.langbook3.android.AcceptationDetailsActivity.preferredAlphabet;
import static sword.langbook3.android.LangbookDatabase.obtainAgentSet;
import static sword.langbook3.android.LangbookDeleter.deleteAgentSet;
import static sword.langbook3.android.LangbookDeleter.deleteBunchAcceptationsForAgentSet;
import static sword.langbook3.android.LangbookDeleter.deleteRuledAcceptation;
import static sword.langbook3.android.LangbookDeleter.deleteStringQueriesForDynamicAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.getAllAgentSetsContaining;
import static sword.langbook3.android.LangbookReadableDatabase.getAllRuledAcceptationsForAgent;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.LangbookReadableDatabase.readBunchSetAcceptationsAndTexts;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptAcceptationAndText;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;
import static sword.langbook3.android.QuizSelectorActivity.NO_BUNCH;

public final class AgentDetailsActivity extends Activity {

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

    int _agentId;

    boolean _deleteDialogPresent;

    int _targetBunch;
    int _sourceBunchSet;
    int _diffBunchSet;
    int _matcher;
    int _adder;
    int _rule;

    void readAgent() {
        final AgentsTable table = Tables.agents; // J0
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), _agentId)
                .select(table.getTargetBunchColumnIndex(),
                        table.getSourceBunchSetColumnIndex(),
                        table.getDiffBunchSetColumnIndex(),
                        table.getMatcherColumnIndex(),
                        table.getAdderColumnIndex(),
                        table.getRuleColumnIndex());
        final DbResult.Row row = DbManager.getInstance().selectSingleRow(query);
        _targetBunch = row.get(0).toInt();
        _sourceBunchSet = row.get(1).toInt();
        _diffBunchSet = row.get(2).toInt();
        _matcher = row.get(3).toInt();
        _adder = row.get(4).toInt();
        _rule = row.get(5).toInt();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_details_activity);

        if (!getIntent().hasExtra(ArgKeys.AGENT)) {
            throw new IllegalArgumentException("agent identifier not provided");
        }

        _agentId = getIntent().getIntExtra(ArgKeys.AGENT, 0);
        if (savedInstanceState != null) {
            _deleteDialogPresent = savedInstanceState.getBoolean(SavedKeys.DELETE_DIALOG_PRESENT);
        }

        final Database db = DbManager.getInstance().getDatabase();
        readAgent();

        final StringBuilder s = new StringBuilder("Agent #").append(_agentId);
        if (_targetBunch != NO_BUNCH) {
            DisplayableItem targetResult = readConceptAcceptationAndText(db, _targetBunch, preferredAlphabet);
            s.append("\nTarget: ").append(targetResult.text).append(" (").append(_targetBunch).append(')');
        }

        s.append("\nSource Bunch Set (").append(_sourceBunchSet).append("):");
        for (DisplayableItem r : readBunchSetAcceptationsAndTexts(db, _sourceBunchSet, preferredAlphabet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.id).append(')');
        }

        s.append("\nDiff Bunch Set (").append(_diffBunchSet).append("):");
        for (DisplayableItem r : readBunchSetAcceptationsAndTexts(db, _diffBunchSet, preferredAlphabet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.id).append(')');
        }

        final MutableIntKeyMap<String> alphabetTexts = MutableIntKeyMap.empty();
        s.append("\nMatcher: ").append(_matcher);
        ImmutableIntKeyMap<String> matcher = getCorrelationWithText(db, _matcher);
        for (int i = 0; i < matcher.size(); i++) {
            final int alphabet = matcher.keyAt(i);
            String alphabetText = alphabetTexts.get(alphabet, null);
            if (alphabetText == null) {
                alphabetText = readConceptText(db, alphabet, preferredAlphabet);
                alphabetTexts.put(alphabet, alphabetText);
            }
            s.append("\n  * ").append(alphabetText).append(" -> ").append(matcher.valueAt(i));
        }

        s.append("\nAdder: ").append(_adder);
        ImmutableIntKeyMap<String> adder = getCorrelationWithText(db, _adder);
        for (int i = 0; i < adder.size(); i++) {
            final int alphabet = adder.keyAt(i);
            String alphabetText = alphabetTexts.get(alphabet, null);
            if (alphabetText == null) {
                alphabetText = readConceptText(db, alphabet, preferredAlphabet);
                alphabetTexts.put(alphabet, alphabetText);
            }
            s.append("\n  * ").append(alphabetText).append(" -> ").append(adder.valueAt(i));
        }

        if (_rule != 0) {
            DisplayableItem ruleResult = readConceptAcceptationAndText(db, _rule, preferredAlphabet);
            s.append("\nRule: ").append(ruleResult.text).append(" (").append(_rule).append(')');
        }

        final TextView tv = findViewById(R.id.textView);
        tv.setText(s.toString());

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
        switch (item.getItemId()) {
            case R.id.menuItemDeleteAgent:
                _deleteDialogPresent = true;
                showDeleteConfirmationDialog();
                return true;

            default:
        }

        return super.onOptionsItemSelected(item);
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
        // This implementation has lot of holes.
        // 1. It is assuming that there is no chained agents
        // 2. It is assuming that agents sets only contains a single agent.
        // TODO: Improve this logic once it is centralised and better defined
        final DbManager manager = DbManager.getInstance();
        final Database db = manager.getDatabase();

        final ImmutableIntKeyMap<ImmutableIntSet> agentSets = getAllAgentSetsContaining(db, _agentId);
        final ImmutableIntPairMap.Builder agentSetMapBuilder = new ImmutableIntPairMap.Builder();
        final ImmutableIntSetBuilder removableAgentSetsBuilder = new ImmutableIntSetBuilder();
        for (IntKeyMap.Entry<ImmutableIntSet> entry : agentSets.entries()) {
            final int setId = obtainAgentSet(db, entry.value().remove(_agentId));
            if (setId == 0) {
                removableAgentSetsBuilder.add(entry.key());
            }
            else {
                agentSetMapBuilder.put(entry.key(), setId);
            }
        }

        if (!agentSetMapBuilder.build().isEmpty()) {
            Toast.makeText(this, "Unimplemented: Multiple agents", Toast.LENGTH_LONG).show();
            return;
        }

        for (int setId : removableAgentSetsBuilder.build()) {
            if (!deleteBunchAcceptationsForAgentSet(db, setId)) {
                throw new AssertionError();
            }

            if (!deleteAgentSet(db, setId)) {
                throw new AssertionError();
            }
        }

        final ImmutableIntSet ruledAcceptations = getAllRuledAcceptationsForAgent(db, _agentId);
        for (int ruleAcceptation : ruledAcceptations) {
            if (!deleteStringQueriesForDynamicAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            if (!deleteRuledAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }
        }

        if (!LangbookDeleter.deleteAgent(db, _agentId)) {
            throw new AssertionError();
        }

        showFeedback(getString(R.string.deleteAgentFeedback));
        finish();
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
