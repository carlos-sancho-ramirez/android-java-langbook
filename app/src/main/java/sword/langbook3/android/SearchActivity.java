package sword.langbook3.android;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import sword.collections.ImmutableList;
import sword.database.DbQuery;
import sword.langbook3.android.models.SearchResult;

abstract class SearchActivity extends Activity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {

    static final String AGENT_QUERY_PREFIX = "Agent ";

    static final int REQUEST_CODE_WELCOME = 1;
    static final int REQUEST_CODE_NEW_ACCEPTATION = 2;
    static final int REQUEST_CODE_NEW_AGENT = 3;
    static final int REQUEST_CODE_OPEN_SETTINGS = 4;

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
            searchField.setOnTouchListener((v, event) -> {
                final int paddingRight = ((TextView) v).getTotalPaddingRight();
                if (event.getAction() == MotionEvent.ACTION_UP && event.getX() >= v.getWidth() - paddingRight) {
                    searchField.setText(null);
                    return true;
                }
                return false;
            });
        }
        else {
            searchField.setEnabled(false);
            searchField.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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

    private ImmutableList<SearchResult> agentSearchResults() {
        return DbManager.getInstance().getManager().getAgentIds().map(agentId -> {
            final String str = AGENT_QUERY_PREFIX + agentId;
            return new SearchResult(str, str, SearchResult.Types.AGENT, agentId, 0);
        });
    }

    private boolean possibleString(String str) {
        return str != null && _query != null && ((str.length() > _query.length())?
                str.toLowerCase().startsWith(_query.toLowerCase()) :
                _query.toLowerCase().startsWith(str.toLowerCase()));
    }

    ImmutableList<SearchResult> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationFromText(query, getSearchRestrictionType());
    }

    final void queryAllResults() {
        ImmutableList<SearchResult> results = queryAcceptationResults(_query);
        if (includeAgentsAsResult() && _query != null && possibleString(AGENT_QUERY_PREFIX)) {
            results = results.appendAll(agentSearchResults().filter(entry -> possibleString(entry.getStr())));
        }

        updateSearchResults(results);
    }

    SearchResultAdapter createAdapter(ImmutableList<SearchResult> results) {
        return new SearchResultAdapter(results, null);
    }

    private void updateSearchResults(ImmutableList<SearchResult> results) {
        _listAdapter = createAdapter(results);
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
