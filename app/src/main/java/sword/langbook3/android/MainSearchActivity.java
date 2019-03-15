package sword.langbook3.android;

import android.content.Intent;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import sword.collections.ImmutableList;

import static sword.langbook3.android.LangbookDatabase.updateSearchHistory;
import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.LangbookReadableDatabase.getSearchHistory;

public final class MainSearchActivity extends SearchActivity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    void onAcceptationSelected(int staticAcceptation, int dynamicAcceptation) {
        updateSearchHistory(DbManager.getInstance().getDatabase(), dynamicAcceptation);
        AcceptationDetailsActivity.open(this, staticAcceptation, dynamicAcceptation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.main_search_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemCheckConversions:
                ConversionSelectorActivity.open(this);
                return true;

            case R.id.menuItemQuiz:
                QuizSelectorActivity.open(this, NO_BUNCH);
                return true;

            case R.id.menuItemNewAgent:
                AgentEditorActivity.open(this);
                return true;

            case R.id.menuItemSettings:
                SettingsActivity.open(this);
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
    void openLanguagePicker(int requestCode, String query) {
        LanguagePickerActivity.open(this, requestCode, query);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final int acceptationId = data.getIntExtra(LanguagePickerActivity.ResultKeys.ACCEPTATION, 0);
            if (acceptationId != 0) {
                AcceptationDetailsActivity.open(this, acceptationId, acceptationId);
            }
        }
    }

    @Override
    ImmutableList<SearchResult> noQueryResults() {
        return getSearchHistory(DbManager.getInstance().getDatabase());
    }
}
