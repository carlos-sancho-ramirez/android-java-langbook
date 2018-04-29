package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.IntKeyMap;

public final class CorrelationPickerActivity extends Activity {

    private interface BundleKeys {
        String ALPHABETS = "alphabets";
        String TEXTS = "texts";
    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correlation_picker_activity);
        Toast.makeText(this, getTexts().toString(), Toast.LENGTH_SHORT).show();
    }
}
