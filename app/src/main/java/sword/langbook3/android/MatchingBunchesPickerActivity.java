package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableMap;
import sword.collections.Map;
import sword.langbook3.android.db.AlphabetId;

public final class MatchingBunchesPickerActivity extends Activity implements View.OnClickListener {

    interface ArgKeys {
        String ALPHABETS = BundleKeys.ALPHABETS;
        String TEXTS = BundleKeys.TEXTS;
    }

    interface ResultKeys {
        String BUNCH_SET = BundleKeys.BUNCH_SET;
    }

    private MatchingBunchesPickerAdapter _adapter;

    public static void open(Activity activity, int requestCode, Map<AlphabetId, String> texts) {
        final int mapSize = texts.size();
        final int[] alphabets = new int[mapSize];
        final String[] str = new String[mapSize];

        for (int i = 0; i < mapSize; i++) {
            alphabets[i] = texts.keyAt(i).key;
            str[i] = texts.valueAt(i);
        }

        final Intent intent = new Intent(activity, MatchingBunchesPickerActivity.class);
        intent.putExtra(ArgKeys.ALPHABETS, alphabets);
        intent.putExtra(ArgKeys.TEXTS, str);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableMap<AlphabetId, String> getTexts() {
        final Bundle extras = getIntent().getExtras();
        final int[] alphabets = extras.getIntArray(ArgKeys.ALPHABETS);
        final String[] texts = extras.getStringArray(ArgKeys.TEXTS);

        if (alphabets == null || texts == null || alphabets.length != texts.length) {
            throw new AssertionError();
        }

        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        for (int i = 0; i < alphabets.length; i++) {
            builder.put(new AlphabetId(alphabets[i]), texts[i]);
        }

        return builder.build();
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
