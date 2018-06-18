package sword.langbook3.android;

import android.content.Intent;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import sword.collections.ImmutableList;
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

import static sword.langbook3.android.LangbookDatabase.updateSearchHistory;

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
            case R.id.menuItemQuiz:
                QuizSelectorActivity.open(this, QuizSelectorActivity.NO_BUNCH);
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
        final LangbookDbSchema.SearchHistoryTable history = LangbookDbSchema.Tables.searchHistory;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final int offset = history.columns().size();
        final DbQuery query = new DbQuery.Builder(history)
                .join(strings, history.getAcceptation(), strings.getDynamicAcceptationColumnIndex())
                .select(history.getAcceptation(),
                        offset + strings.getMainAcceptationColumnIndex(),
                        offset + strings.getStringColumnIndex(),
                        offset + strings.getMainStringColumnIndex());

        final MutableIntSet acceptations = MutableIntSet.empty();
        final ImmutableList.Builder<SearchResult> builder = new ImmutableList.Builder<>();

        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            final int acceptation = row.get(0).toInt();
            if (!acceptations.contains(acceptation)) {
                acceptations.add(acceptation);
                builder.add(new SearchResult(row.get(2).toText(), row.get(3).toText(), SearchResult.Types.ACCEPTATION, row.get(1).toInt(), acceptation));
            }
        }

        return builder.build();
    }
}
