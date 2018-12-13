package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.langbook3.android.LangbookReadableDatabase.AgentRegister;
import sword.langbook3.android.db.Database;

import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.LangbookReadableDatabase.getAgentRegister;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.LangbookReadableDatabase.readBunchSetAcceptationsAndTexts;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptAcceptationAndText;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;
import static sword.langbook3.android.SearchActivity.AGENT_QUERY_PREFIX;

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

    private int _preferredAlphabet;
    int _agentId;

    boolean _deleteDialogPresent;

    AgentRegister _register;

    private void dumpCorrelation(Database db, int correlationId, SyncCacheIntKeyNonNullValueMap<String> alphabetTexts, StringBuilder sb) {
        ImmutableIntKeyMap<String> matcher = getCorrelationWithText(db, correlationId);
        for (int i = 0; i < matcher.size(); i++) {
            final int alphabet = matcher.keyAt(i);
            final String alphabetText = alphabetTexts.get(alphabet);
            sb.append("\n  * ").append(alphabetText).append(" -> ").append(matcher.valueAt(i));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_details_activity);

        if (!getIntent().hasExtra(ArgKeys.AGENT)) {
            throw new IllegalArgumentException("agent identifier not provided");
        }

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _agentId = getIntent().getIntExtra(ArgKeys.AGENT, 0);
        if (savedInstanceState != null) {
            _deleteDialogPresent = savedInstanceState.getBoolean(SavedKeys.DELETE_DIALOG_PRESENT);
        }

        setTitle(AGENT_QUERY_PREFIX + '#' + _agentId);

        final Database db = DbManager.getInstance().getDatabase();
        _register = getAgentRegister(DbManager.getInstance().getDatabase(), _agentId);

        final StringBuilder s = new StringBuilder();
        if (_register.targetBunch != NO_BUNCH) {
            DisplayableItem targetResult = readConceptAcceptationAndText(db, _register.targetBunch, _preferredAlphabet);
            s.append("Target: ").append(targetResult.text).append(" (").append(_register.targetBunch).append(")\n");
        }

        s.append("Source Bunch Set (").append(_register.sourceBunchSetId).append("):");
        for (DisplayableItem r : readBunchSetAcceptationsAndTexts(db, _register.sourceBunchSetId, _preferredAlphabet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.id).append(')');
        }

        s.append("\nDiff Bunch Set (").append(_register.diffBunchSetId).append("):");
        for (DisplayableItem r : readBunchSetAcceptationsAndTexts(db, _register.diffBunchSetId, _preferredAlphabet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.id).append(')');
        }

        final SyncCacheIntKeyNonNullValueMap<String> alphabetTexts = new SyncCacheIntKeyNonNullValueMap<>(alphabet -> readConceptText(db, alphabet, _preferredAlphabet));
        s.append("\nStart Matcher: ").append(_register.startMatcherId);
        dumpCorrelation(db, _register.startMatcherId, alphabetTexts, s);

        s.append("\nStart Adder: ").append(_register.startAdderId);
        dumpCorrelation(db, _register.startAdderId, alphabetTexts, s);

        s.append("\nEnd Matcher: ").append(_register.endMatcherId);
        dumpCorrelation(db, _register.endMatcherId, alphabetTexts, s);

        s.append("\nEnd Adder: ").append(_register.endAdderId);
        dumpCorrelation(db, _register.endAdderId, alphabetTexts, s);

        if (_register.rule != 0) {
            DisplayableItem ruleResult = readConceptAcceptationAndText(db, _register.rule, _preferredAlphabet);
            s.append("\nRule: ").append(ruleResult.text).append(" (").append(_register.rule).append(')');
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
        LangbookDatabase.removeAgent(DbManager.getInstance().getDatabase(), _agentId);
        showFeedback(getString(R.string.deleteAgentFeedback));
        finish();
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
