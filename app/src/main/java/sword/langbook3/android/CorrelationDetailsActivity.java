package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;

import static sword.langbook3.android.LangbookReadableDatabase.getCorrelationDetails;

public final class CorrelationDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private interface ArgKeys {
        String CORRELATION = BundleKeys.CORRELATION;
    }

    public static void open(Context context, int correlationId) {
        Intent intent = new Intent(context, CorrelationDetailsActivity.class);
        intent.putExtra(ArgKeys.CORRELATION, correlationId);
        context.startActivity(intent);
    }

    private int _correlationId;
    private CorrelationDetailsModel _model;
    private AcceptationDetailsAdapter _listAdapter;

    private ImmutableList<AcceptationDetailsAdapter.Item> getAdapterItems() {
        final int entryCount = _model.correlation.size();
        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> result = new ImmutableList.Builder<>();
        result.add(new HeaderItem("Displaying details for correlation " + _correlationId));
        for (int i = 0; i < entryCount; i++) {
            final String alphabetText = _model.alphabets.get(_model.correlation.keyAt(i));
            final String text = _model.correlation.valueAt(i);
            result.add(new NonNavigableItem(alphabetText + " -> " + text));
        }

        final int acceptationCount = _model.acceptations.size();
        result.add(new HeaderItem("Acceptations where included"));
        for (int i = 0; i < acceptationCount; i++) {
            result.add(new AcceptationNavigableItem(_model.acceptations.keyAt(i), _model.acceptations.valueAt(i), false));
        }

        for (int i = 0; i < entryCount; i++) {
            final int matchingAlphabet = _model.correlation.keyAt(i);
            final ImmutableIntSet matchingCorrelations = _model.relatedCorrelationsByAlphabet.get(matchingAlphabet);
            final int count = matchingCorrelations.size();
            if (count > 0) {
                result.add(new HeaderItem("Other correlations sharing " + _model.alphabets.get(matchingAlphabet)));
                for (int corrId : matchingCorrelations) {
                    final ImmutableIntKeyMap<String> corr = _model.relatedCorrelations.get(corrId);

                    final StringBuilder sb = new StringBuilder();
                    final int correlationSize = corr.size();
                    for (int j = 0; j < correlationSize; j++) {
                        if (j != 0) {
                            sb.append('/');
                        }

                        sb.append(corr.valueAt(j));
                    }

                    result.add(new CorrelationNavigableItem(corrId, sb.toString()));
                }
            }
        }

        return result.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correlation_details_activity);

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _correlationId = getIntent().getIntExtra(ArgKeys.CORRELATION, 0);
        _model = getCorrelationDetails(DbManager.getInstance().getDatabase(), _correlationId, preferredAlphabet);

        if (_model != null) {
            _listAdapter = new AcceptationDetailsAdapter(getAdapterItems());
            final ListView listView = findViewById(R.id.listView);
            listView.setAdapter(_listAdapter);
            listView.setOnItemClickListener(this);
        }
        else {
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        _listAdapter.getItem(position).navigate(this);
    }
}
