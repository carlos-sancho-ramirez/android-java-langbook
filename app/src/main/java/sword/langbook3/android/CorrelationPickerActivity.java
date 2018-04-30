package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntResultFunction;

public final class CorrelationPickerActivity extends Activity {

    private interface BundleKeys {
        String ALPHABETS = "alphabets";
        String TEXTS = "texts";
    }

    private ListView _listView;

    public static void open(Activity activity, int requestCode, IntKeyMap<String> texts) {
        final int mapSize = texts.size();
        final int[] alphabets = new int[mapSize];
        final String[] str = new String[mapSize];

        for (int i = 0; i < mapSize; i++) {
            alphabets[i] = texts.keyAt(i);
            str[i] = texts.valueAt(i);
        }

        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(BundleKeys.ALPHABETS, alphabets);
        intent.putExtra(BundleKeys.TEXTS, str);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableIntKeyMap<String> getTexts() {
        final Bundle extras = getIntent().getExtras();
        final int[] alphabets = extras.getIntArray(BundleKeys.ALPHABETS);
        final String[] texts = extras.getStringArray(BundleKeys.TEXTS);

        if (alphabets == null || texts == null || alphabets.length != texts.length) {
            throw new AssertionError();
        }

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        for (int i = 0; i < alphabets.length; i++) {
            builder.put(alphabets[i], texts[i]);
        }

        return builder.build();
    }

    private void checkPossibleCorrelationArraysRecursive(
            ImmutableSet.Builder<ImmutableList<ImmutableIntKeyMap<String>>> builder,
            ImmutableIntKeyMap<String> remaining,
            ImmutableIntKeyMap<String> left,
            ImmutableIntKeyMap<String> right) {
        final int remainingSize = remaining.size();
        if (remainingSize == 0) {
            for (ImmutableList<ImmutableIntKeyMap<String>> array : checkPossibleCorrelationArrays(right)) {
                builder.add(array.prepend(left));
            }
        }
        else {
            final int firstAlphabet = remaining.keyAt(0);
            final String firstText = remaining.valueAt(0);

            // TODO: Change this to global.skip(1) when available
            final ImmutableIntKeyMap.Builder<String> tailBuilder = new ImmutableIntKeyMap.Builder<>();
            for (int i = 1; i < remainingSize; i++) {
                tailBuilder.put(remaining.keyAt(i), remaining.valueAt(i));
            }
            final ImmutableIntKeyMap<String> tail = tailBuilder.build();

            final int firstTextSize = firstText.length();
            for (int i = 1; i < firstTextSize; i++) {
                final ImmutableIntKeyMap<String> newLeft = left.put(firstAlphabet, firstText.substring(0, i));
                final ImmutableIntKeyMap<String> newRight = right.put(firstAlphabet, firstText.substring(i));
                checkPossibleCorrelationArraysRecursive(builder, tail, newLeft, newRight);
            }
        }
    }

    private ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> checkPossibleCorrelationArrays(ImmutableIntKeyMap<String> global) {
        final int globalSize = global.size();
        final IntResultFunction<String> lengthFunc = text -> (text == null)? 0 : text.length();
        final ImmutableIntPairMap lengths = global.mapValues(lengthFunc);
        if (globalSize == 0 || lengths.anyMatch(length -> length <= 0)) {
            return ImmutableSet.empty();
        }

        final ImmutableSet.Builder<ImmutableList<ImmutableIntKeyMap<String>>> builder = new ImmutableSet.Builder<>();
        builder.add(new ImmutableList.Builder<ImmutableIntKeyMap<String>>().add(global).build());

        if (globalSize > 1) {
            checkPossibleCorrelationArraysRecursive(builder, global, ImmutableIntKeyMap.empty(), ImmutableIntKeyMap.empty());
        }
        return builder.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correlation_picker_activity);

        final ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> set = checkPossibleCorrelationArrays(getTexts());
        _listView = findViewById(R.id.listView);
        _listView.setAdapter(new CorrelationPickerAdapter(set));
    }
}
