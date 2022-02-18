package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.CorrelationIdBundler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.models.CorrelationDetailsModel;

import static sword.langbook3.android.collections.StringUtils.stringToCharList;

public final class CorrelationDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_CODE_CLICK_NAVIGATION = 1;

    private interface ArgKeys {
        String CORRELATION = BundleKeys.CORRELATION;
    }

    public static void open(Activity activity, int requestCode, CorrelationId correlationId) {
        Intent intent = new Intent(activity, CorrelationDetailsActivity.class);
        CorrelationIdBundler.writeAsIntentExtra(intent, ArgKeys.CORRELATION, correlationId);
        activity.startActivityForResult(intent, requestCode);
    }

    private CorrelationId _correlationId;
    private CorrelationDetailsModel<AlphabetId, CharacterId, CorrelationId, AcceptationId> _model;
    private AcceptationDetailsAdapter _listAdapter;

    private boolean _justLoaded;

    private static String composeCorrelationString(ImmutableCorrelation<AlphabetId> correlation) {
        return correlation.reduce((a, b) -> a + '/' + b);
    }

    private ImmutableList<AcceptationDetailsAdapter.Item> getAdapterItems() {
        final int entryCount = _model.correlation.size();
        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> result = new ImmutableList.Builder<>();
        result.add(new HeaderItem("Displaying details for correlation " + _correlationId));
        final ImmutableMap<Character, CharacterId> characters = _model.characters;
        for (int i = 0; i < entryCount; i++) {
            final String alphabetText = _model.alphabets.get(_model.correlation.keyAt(i));
            final String text = _model.correlation.valueAt(i);
            final String itemText = alphabetText + " -> " + text;
            final Character charMatching = stringToCharList(text).findFirst(characters::containsKey, null);
            final AcceptationDetailsAdapter.Item item = (charMatching == null)? new NonNavigableItem(itemText) :
                    new AcceptationDetailsAdapter.CharacterCompositionNavigableItem(characters.get(charMatching), itemText);
            result.add(item);
        }

        final int acceptationCount = _model.acceptations.size();
        result.add(new HeaderItem("Acceptations where included"));
        for (int i = 0; i < acceptationCount; i++) {
            result.add(new AcceptationNavigableItem(_model.acceptations.keyAt(i), _model.acceptations.valueAt(i), false));
        }

        for (int i = 0; i < entryCount; i++) {
            final AlphabetId matchingAlphabet = _model.correlation.keyAt(i);
            final ImmutableSet<CorrelationId> matchingCorrelations = _model.relatedCorrelationsByAlphabet.get(matchingAlphabet);
            final int count = matchingCorrelations.size();
            if (count > 0) {
                result.add(new HeaderItem("Other correlations sharing " + _model.alphabets.get(matchingAlphabet)));
                for (CorrelationId corrId : matchingCorrelations) {
                    final ImmutableCorrelation<AlphabetId> corr = _model.relatedCorrelations.get(corrId);
                    result.add(new CorrelationNavigableItem(corrId, composeCorrelationString(corr)));
                }
            }
        }

        return result.build();
    }

    private void updateModelAndUi() {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _model = DbManager.getInstance().getManager().getCorrelationDetails(_correlationId, preferredAlphabet);

        if (_model != null) {
            _justLoaded = true;
            setTitle(getString(R.string.correlationDetailsActivityTitle, composeCorrelationString(_model.correlation)));
            _listAdapter = new AcceptationDetailsAdapter(this, REQUEST_CODE_CLICK_NAVIGATION, getAdapterItems());
            final ListView listView = findViewById(R.id.listView);
            listView.setAdapter(_listAdapter);
            listView.setOnItemClickListener(this);
        }
        else {
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correlation_details_activity);

        _correlationId = CorrelationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CORRELATION);
        updateModelAndUi();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        _listAdapter.getItem(position).navigate(this, REQUEST_CODE_CLICK_NAVIGATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CLICK_NAVIGATION && !_justLoaded) {
            updateModelAndUi();
        }
    }

    @Override
    public void onStart() {
        _justLoaded = false;
        super.onStart();
    }
}
