package sword.langbook3.android;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.List;
import sword.collections.SortUtils;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.database.DbExporter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbValue;

abstract class SearchActivity extends Activity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    static final String AGENT_QUERY_PREFIX = "Agent ";
    private static final int REQUEST_CODE_NEW_ACCEPTATION = 1;

    private ListView _listView;
    private SearchResultAdapter _listAdapter;
    private String _query;

    interface ArgKeys {
        String TEXT = BundleKeys.TEXT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        _listView = findViewById(R.id.listView);
        _listView.setOnItemClickListener(this);

        findViewById(R.id.addWordButton).setOnClickListener(this);
        _query = getIntent().getStringExtra(ArgKeys.TEXT);

        prepareSearchField(findViewById(R.id.searchField));
    }

    private void prepareSearchField(EditText searchField) {
        if (_query != null) {
            searchField.setText(_query);
        }

        if (isQueryModifiable()) {
            searchField.addTextChangedListener(this);
        }
        else {
            searchField.setEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSearchResults();
    }

    private void updateSearchResults() {
        if (TextUtils.isEmpty(_query)) {
            updateSearchResults(noQueryResults());
        }
        else {
            queryAllResults();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Nothing to be done
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // Nothing to be done
    }

    @Override
    public void afterTextChanged(Editable editable) {
        _query = editable.toString();
        updateSearchResults();
    }

    ImmutableList<SearchResult> noQueryResults() {
        return ImmutableList.empty();
    }

    boolean isQueryModifiable() {
        return true;
    }

    int getSearchRestrictionType() {
        return DbQuery.RestrictionStringTypes.STARTS_WITH;
    }

    boolean includeAgentsAsResult() {
        return false;
    }

    private ImmutableList<SearchResult> querySearchResults(DbExporter.Database db) {
        final StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringColumnIndex(), new DbQuery.Restriction(new DbStringValue(_query),
                        getSearchRestrictionType()))
                .select(
                        table.getStringColumnIndex(),
                        table.getMainStringColumnIndex(),
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        final ImmutableList.Builder<SearchResult> builder = new ImmutableList.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String str = row.get(0).toText();
                final String mainStr = row.get(1).toText();
                final int acc = row.get(2).toInt();
                final int dynAcc = row.get(3).toInt();

                builder.add(new SearchResult(str, mainStr, SearchResult.Types.ACCEPTATION, acc, dynAcc));
            }
        }

        return builder.build().sort((a, b) -> !a.isDynamic() && b.isDynamic() || a.isDynamic() == b.isDynamic() && SortUtils.compareCharSequenceByUnicode(a.getStr(), b.getStr()));
    }

    private ImmutableIntSet getAgentIds() {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (List<DbValue> row : DbManager.getInstance().attach(query)) {
            builder.add(row.get(0).toInt());
        }
        return builder.build();
    }

    private ImmutableList<SearchResult> agentSearchResults() {
        final ImmutableList.Builder<SearchResult> builder = new ImmutableList.Builder<>();
        for (int agentId : getAgentIds()) {
            String str = AGENT_QUERY_PREFIX + agentId;
            builder.add(new SearchResult(str, str, SearchResult.Types.AGENT, agentId, 0));
        }

        return builder.build();
    }

    private boolean possibleString(String str) {
        return str != null && _query != null && ((str.length() > _query.length())?
                str.toLowerCase().startsWith(_query.toLowerCase()) :
                _query.toLowerCase().startsWith(str.toLowerCase()));
    }

    final void queryAllResults() {
        ImmutableList<SearchResult> results = querySearchResults(DbManager.getInstance().getDatabase());
        if (includeAgentsAsResult() && _query != null && possibleString(AGENT_QUERY_PREFIX)) {
            results = results.appendAll(agentSearchResults().filter(entry -> possibleString(entry.getStr())));
        }

        updateSearchResults(results);
    }

    private void updateSearchResults(ImmutableList<SearchResult> results) {
        _listAdapter = new SearchResultAdapter(results);
        _listView.setAdapter(_listAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        SearchResult item = _listAdapter.getItem(position);
        switch (item.getType()) {
            case SearchResult.Types.ACCEPTATION:
                onAcceptationSelected(item.getId(), item.getAuxiliarId());
                break;

            case SearchResult.Types.AGENT:
                AgentDetailsActivity.open(this, item.getId());
                break;

            default:
                throw new AssertionError();
        }
    }

    abstract void onAcceptationSelected(int staticAcceptation, int dynamicAcceptation);
    abstract void openLanguagePicker(int requestCode, String query);

    @Override
    public void onClick(View v) {
        openLanguagePicker(REQUEST_CODE_NEW_ACCEPTATION, _query);
    }
}
