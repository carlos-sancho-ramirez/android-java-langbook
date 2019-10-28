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
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.models.AgentRegister;

import static sword.langbook3.android.SearchActivity.AGENT_QUERY_PREFIX;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;

public final class AgentDetailsActivity extends Activity {

    private static final int REQUEST_CODE_EDIT_AGENT = 1;

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

    private void dumpCorrelation(LangbookChecker checker, int correlationId, SyncCacheIntKeyNonNullValueMap<String> alphabetTexts, StringBuilder sb) {
        ImmutableIntKeyMap<String> matcher = checker.getCorrelationWithText(correlationId);
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
        updateUi();
        _uiJustUpdated = true;
    }

    private void updateUi() {
        final LangbookChecker checker = DbManager.getInstance().getManager();
        _register = checker.getAgentRegister(_agentId);

        final StringBuilder s = new StringBuilder();
        if (_register.targetBunch != NO_BUNCH) {
            DisplayableItem targetResult = checker.readConceptAcceptationAndText(_register.targetBunch, _preferredAlphabet);
            s.append("Target: ").append(targetResult.text).append(" (").append(_register.targetBunch).append(")\n");
        }

        s.append("Source Bunch Set (").append(_register.sourceBunchSetId).append("):");
        for (DisplayableItem r : checker.readBunchSetAcceptationsAndTexts(_register.sourceBunchSetId, _preferredAlphabet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.id).append(')');
        }

        s.append("\nDiff Bunch Set (").append(_register.diffBunchSetId).append("):");
        for (DisplayableItem r : checker.readBunchSetAcceptationsAndTexts(_register.diffBunchSetId, _preferredAlphabet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.id).append(')');
        }

        final SyncCacheIntKeyNonNullValueMap<String> alphabetTexts = new SyncCacheIntKeyNonNullValueMap<>(alphabet -> checker.readConceptText(alphabet, _preferredAlphabet));
        s.append("\nStart Matcher: ").append(_register.startMatcherId);
        dumpCorrelation(checker, _register.startMatcherId, alphabetTexts, s);

        s.append("\nStart Adder: ").append(_register.startAdderId);
        dumpCorrelation(checker, _register.startAdderId, alphabetTexts, s);

        s.append("\nEnd Matcher: ").append(_register.endMatcherId);
        dumpCorrelation(checker, _register.endMatcherId, alphabetTexts, s);

        s.append("\nEnd Adder: ").append(_register.endAdderId);
        dumpCorrelation(checker, _register.endAdderId, alphabetTexts, s);

        if (_register.rule != 0) {
            DisplayableItem ruleResult = checker.readConceptAcceptationAndText(_register.rule, _preferredAlphabet);
            s.append("\nRule: ").append(ruleResult.text).append(" (").append(_register.rule).append(')');
        }

        final TextView tv = findViewById(R.id.textView);
        tv.setText(s.toString());

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
