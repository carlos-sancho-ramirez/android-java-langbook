package sword.langbook3.android;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.SearchResult;

public final class MainSearchActivity extends SearchActivity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    private ImmutableMap<RuleId, String> _ruleTexts;
    private int _dbWriteVersion;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && !DbManager.getInstance().getManager().isAnyLanguagePresent()) {
            WelcomeActivity.open(this, REQUEST_CODE_WELCOME);
        }
    }

    void onAcceptationSelected(AcceptationId acceptation) {
        DbManager.getInstance().getManager().updateSearchHistory(acceptation);
        AcceptationDetailsActivity.open(this, acceptation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.main_search_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemCheckAlphabets:
                AlphabetsActivity.open(this);
                return true;

            case R.id.menuItemQuiz:
                QuizSelectorActivity.open(this, null);
                return true;

            case R.id.menuItemNewAgent:
                AgentEditorActivity.open(this);
                return true;

            case R.id.menuItemSettings:
                SettingsActivity.open(this, REQUEST_CODE_OPEN_SETTINGS);
                return true;

            case R.id.menuItemAbout:
                AboutActivity.open(this);
                return true;
        }

        return false;
    }

    @Override
    boolean includeAgentsAsResult() {
        return true;
    }

    @Override
    void openLanguagePicker(String query) {
        LanguagePickerActivity.open(this, REQUEST_CODE_NEW_ACCEPTATION, query);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK || requestCode == REQUEST_CODE_OPEN_SETTINGS) {
            _ruleTexts = null;
        }

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_NEW_ACCEPTATION) {
            final AcceptationId acceptationId = AcceptationIdBundler.readAsIntentExtra(data, LanguagePickerActivity.ResultKeys.ACCEPTATION);
            if (acceptationId != null) {
                final EditText searchField = findViewById(R.id.searchField);
                if (searchField.getText().length() > 0) {
                    DbManager.getInstance().getManager().updateSearchHistory(acceptationId);
                }
                AcceptationDetailsActivity.open(this, acceptationId);
            }
        }
    }

    @Override
    ImmutableList<SearchResult<AcceptationId, RuleId>> noQueryResults() {
        return DbManager.getInstance().getManager().getSearchHistory();
    }

    @Override
    SearchResultAdapter createAdapter(ImmutableList<SearchResult> results) {
        final int dbWriteVersion = DbManager.getInstance().getDatabase().getWriteVersion();
        if (_ruleTexts == null || dbWriteVersion != _dbWriteVersion) {
            final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
            _ruleTexts = DbManager.getInstance().getManager().readAllRules(preferredAlphabet);
            _dbWriteVersion = dbWriteVersion;
        }

        return new SearchResultAdapter(results, _ruleTexts);
    }

    @Override
    ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationAndRulesFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }
}
