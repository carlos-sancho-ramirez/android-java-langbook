package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.activities.delegates.CharacterCompositionEditorActivityDelegate.TOKEN_END_CHARACTER;
import static sword.langbook3.android.activities.delegates.CharacterCompositionEditorActivityDelegate.TOKEN_START_CHARACTER;
import static sword.langbook3.android.activities.delegates.CharacterCompositionEditorActivityDelegate.TOKEN_START_STRING;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.SortUtils;
import sword.langbook3.android.AboutActivity;
import sword.langbook3.android.AcceptationDetailsActivity;
import sword.langbook3.android.AlphabetsActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.Intentions;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.QuizSelectorActivity;
import sword.langbook3.android.R;
import sword.langbook3.android.SearchResultAdapter;
import sword.langbook3.android.SettingsActivity;
import sword.langbook3.android.WelcomeActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.SearchResult;

public final class MainSearchActivityDelegate<Activity extends ActivityExtensions> extends SearchActivityDelegate<Activity> {

    private ImmutableMap<RuleId, String> _ruleTexts;
    private int _dbWriteVersion;

    private boolean isAnyLanguagePresent() {
        return DbManager.getInstance().getManager().isAnyLanguagePresent();
    }

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        super.onCreate(activity, savedInstanceState);

        if (savedInstanceState == null && !isAnyLanguagePresent()) {
            WelcomeActivity.open(activity, REQUEST_CODE_WELCOME);
        }
    }

    void onAcceptationSelected(AcceptationId acceptation) {
        DbManager.getInstance().getManager().updateSearchHistory(acceptation);
        AcceptationDetailsActivity.open(_activity, acceptation);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        activity.newMenuInflater().inflate(R.menu.main_search_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menuItemCheckAlphabets) {
            AlphabetsActivity.open(activity);
            return true;
        }
        else if (itemId == R.id.menuItemQuiz) {
            if (isAnyLanguagePresent()) {
                QuizSelectorActivity.open(activity, null);
            }
            else {
                activity.showToast(R.string.anyLanguageRequired);
            }
            return true;
        }
        else if (itemId == R.id.menuItemNewAgent) {
            if (isAnyLanguagePresent()) {
                Intentions.addAgent(activity);
            }
            else {
                activity.showToast(R.string.anyLanguageRequired);
            }
            return true;
        }
        else if (itemId == R.id.menuItemSettings) {
            SettingsActivity.open(activity, REQUEST_CODE_OPEN_SETTINGS);
            return true;
        }
        else if (itemId == R.id.menuItemAbout) {
            AboutActivity.open(activity);
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
        if (isAnyLanguagePresent()) {
            Intentions.addAcceptation(_activity, REQUEST_CODE_NEW_ACCEPTATION, query);
        }
        else {
            _activity.showToast(R.string.anyLanguageRequired);
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK || requestCode == REQUEST_CODE_OPEN_SETTINGS) {
            _ruleTexts = null;
        }

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_NEW_ACCEPTATION) {
            final AcceptationId acceptationId = AcceptationIdBundler.readAsIntentExtra(data, LanguagePickerActivityDelegate.ResultKeys.ACCEPTATION);
            if (acceptationId != null) {
                final EditText searchField = activity.findViewById(R.id.searchField);
                if (searchField.getText().length() > 0) {
                    DbManager.getInstance().getManager().updateSearchHistory(acceptationId);
                }
                AcceptationDetailsActivity.open(activity, acceptationId);
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
    ImmutableList<SearchResult<CharacterId, Object>> queryCharacterResults(String query) {
        if (!SortUtils.isEmpty(query)) {
            final String dbQuery = (query.charAt(0) == TOKEN_START_CHARACTER)? query.substring(1) : query;
            return DbManager.getInstance().getManager().searchCharacterTokens(dbQuery, str -> TOKEN_START_STRING + str + TOKEN_END_CHARACTER);
        }
        else {
            return ImmutableList.empty();
        }
    }

    @Override
    ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationAndRulesFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }
}
