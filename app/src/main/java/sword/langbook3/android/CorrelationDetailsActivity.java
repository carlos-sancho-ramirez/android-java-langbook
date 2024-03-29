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
import sword.langbook3.android.models.CorrelationDetails2;

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
    private CorrelationDetails2<AlphabetId, CharacterId, CorrelationId, AcceptationId> _model;
    private AcceptationDetailsAdapter _listAdapter;

    private boolean _justLoaded;

    private static String composeCorrelationString(ImmutableCorrelation<AlphabetId> correlation) {
        return correlation.reduce((a, b) -> a + '/' + b);
    }

    private ImmutableList<AcceptationDetailsAdapter.Item> getAdapterItems() {
        final ImmutableCorrelation<AlphabetId> correlation = _model.getCorrelation();
        final int entryCount = correlation.size();

        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> result = new ImmutableList.Builder<>();
        result.add(new HeaderItem(getString(R.string.correlationDetailsIdEntry, _correlationId)));
        final ImmutableMap<Character, CharacterId> characters = _model.getCharacters();
        final ImmutableMap<AlphabetId, String> alphabets = _model.getAlphabets();
        for (int i = 0; i < entryCount; i++) {
            final String alphabetText = alphabets.get(correlation.keyAt(i));
            final String text = correlation.valueAt(i);
            final String itemText = alphabetText + " -> " + text;
            final Character charMatching = stringToCharList(text).findFirst(characters::containsKey, null);
            final AcceptationDetailsAdapter.Item item = (charMatching == null)? new NonNavigableItem(itemText) :
                    new AcceptationDetailsAdapter.CharacterPickerNavigableItem(text, itemText);
            result.add(item);
        }

        final ImmutableMap<AcceptationId, String> acceptations = _model.getAcceptations();
        final int acceptationCount = acceptations.size();
        result.add(new HeaderItem(getString(R.string.characterCompositionAcceptationsHeader)));
        for (int i = 0; i < acceptationCount; i++) {
            result.add(new AcceptationNavigableItem(acceptations.keyAt(i), acceptations.valueAt(i), false));
        }

        final ImmutableMap<AlphabetId, ImmutableSet<CorrelationId>> relatedCorrelationsByAlphabet = _model.getRelatedCorrelationsByAlphabet();
        final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> relatedCorrelations = _model.getRelatedCorrelations();
        for (int i = 0; i < entryCount; i++) {
            final AlphabetId matchingAlphabet = correlation.keyAt(i);
            final ImmutableSet<CorrelationId> matchingCorrelations = relatedCorrelationsByAlphabet.get(matchingAlphabet);
            final int count = matchingCorrelations.size();
            if (count > 0) {
                result.add(new HeaderItem(getString(R.string.correlationDetailsPartiallySharedHeader, alphabets.get(matchingAlphabet))));
                for (CorrelationId corrId : matchingCorrelations) {
                    final ImmutableCorrelation<AlphabetId> corr = relatedCorrelations.get(corrId);
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
            setTitle(getString(R.string.correlationDetailsActivityTitle, composeCorrelationString(_model.getCorrelation())));
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
