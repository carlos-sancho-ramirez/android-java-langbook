package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.collections.ImmutableSet;
import sword.collections.MutableIntKeyMap;
import sword.collections.SortFunction;
import sword.database.Database;

import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;

public final class ConversionSelectorActivity extends Activity implements ListView.OnItemClickListener {

    public static void open(Context context) {
        Intent intent = new Intent(context, ConversionSelectorActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversion_selector_activity);

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final Database db = DbManager.getInstance().getDatabase();
        final ImmutableSet<ImmutableIntPair> conversions = LangbookReadableDatabase.findConversions(db);

        final MutableIntKeyMap<String> alphabetTexts = MutableIntKeyMap.empty();
        for (ImmutableIntPair conversion : conversions) {
            final int source = conversion.left;
            if (!alphabetTexts.keySet().contains(source)) {
                alphabetTexts.put(source, readConceptText(db, source, preferredAlphabet));
            }

            final int target = conversion.right;
            if (!alphabetTexts.keySet().contains(target)) {
                alphabetTexts.put(target, readConceptText(db, target, preferredAlphabet));
            }
        }

        final SortFunction<ImmutableIntPair> sorting = (a, b) -> a.left < b.left || a.left == b.left && a.right < b.right;
        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new ConversionSelectorAdapter(conversions.sort(sorting), alphabetTexts.toImmutable()));
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ConversionSelectorAdapter adapter = (ConversionSelectorAdapter) parent.getAdapter();
        final ImmutableIntPair pair = adapter.getItem(position);
        ConversionDetailsActivity.open(this, pair.left, pair.right);
    }
}
