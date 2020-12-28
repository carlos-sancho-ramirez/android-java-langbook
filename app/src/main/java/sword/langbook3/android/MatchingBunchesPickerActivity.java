package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.CorrelationBundler;
import sword.langbook3.android.db.ImmutableCorrelation;

public final class MatchingBunchesPickerActivity extends Activity implements View.OnClickListener {

    interface ArgKeys {
        String CORRELATION_MAP = BundleKeys.CORRELATION_MAP;
    }

    interface ResultKeys {
        String BUNCH_SET = BundleKeys.BUNCH_SET;
    }

    private MatchingBunchesPickerAdapter _adapter;

    public static void open(Activity activity, int requestCode, Correlation<AlphabetId> texts) {
        final Intent intent = new Intent(activity, MatchingBunchesPickerActivity.class);
        CorrelationBundler.writeAsIntentExtra(intent, ArgKeys.CORRELATION_MAP, texts);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableCorrelation<AlphabetId> getTexts() {
        return CorrelationBundler.readAsIntentExtra(getIntent(), ArgKeys.CORRELATION_MAP).toImmutable();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.matching_bunches_picker_activity);

        final ListView listView = findViewById(R.id.listView);
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableIntKeyMap<String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(getTexts(), preferredAlphabet);

        if (bunches.isEmpty()) {
            final Intent intent = new Intent();
            intent.putExtra(ResultKeys.BUNCH_SET, new int[0]);
            setResult(RESULT_OK, intent);
            finish();
        }
        else {
            _adapter = new MatchingBunchesPickerAdapter(bunches);
            listView.setAdapter(_adapter);

            findViewById(R.id.nextButton).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        final ImmutableIntSet bunchSet = _adapter.getCheckedBunches();
        final int[] bunchArray = new int[bunchSet.size()];
        int index = 0;
        for (int bunch : bunchSet) {
            bunchArray[index++] = bunch;
        }

        final Intent intent = new Intent();
        intent.putExtra(ResultKeys.BUNCH_SET, bunchArray);
        setResult(RESULT_OK, intent);
        finish();
    }
}
