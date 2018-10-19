package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.CorrelationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.LangbookDbSchema.CorrelationsTable;
import sword.langbook3.android.LangbookDbSchema.SymbolArraysTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.DbExporter;

import static sword.langbook3.android.AcceptationDetailsActivity.composeCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.LangbookReadableDatabase.readAcceptationsIncludingCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.readAllAlphabets;
import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public final class CorrelationDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private interface ArgKeys {
        String CORRELATION = BundleKeys.CORRELATION;
    }

    public static void open(Context context, int correlationId) {
        Intent intent = new Intent(context, CorrelationDetailsActivity.class);
        intent.putExtra(ArgKeys.CORRELATION, correlationId);
        context.startActivity(intent);
    }

    private int _preferredAlphabet;
    private AcceptationDetailsAdapter _listAdapter;

    private ImmutableIntKeyMap<ImmutableIntKeyMap<String>> readCorrelationsWithSameSymbolArray(SQLiteDatabase db, int correlation, int alphabet) {
        final CorrelationsTable correlations = Tables.correlations;
        final SymbolArraysTable symbolArrays = Tables.symbolArrays;

        final String alphabetField = correlations.columns().get(correlations.getAlphabetColumnIndex()).name();
        final String correlationIdField = correlations.columns().get(correlations.getCorrelationIdColumnIndex()).name();
        final String symbolArrayField = correlations.columns().get(correlations.getSymbolArrayColumnIndex()).name();

        final Cursor cursor = db.rawQuery(
                "SELECT J1." + correlationIdField +
                        ",J2." + alphabetField +
                        ",J3." + symbolArrays.columns().get(symbolArrays.getStrColumnIndex()).name() +
                " FROM " + correlations.name() + " AS J0" +
                        " JOIN " + correlations.name() + " AS J1 ON J0." + symbolArrayField + "=J1." + symbolArrayField +
                        " JOIN " + correlations.name() + " AS J2 ON J1." + correlationIdField + "=J2." + correlationIdField +
                        " JOIN " + symbolArrays.name() + " AS J3 ON J2." + symbolArrayField + "=J3." + idColumnName +
                " WHERE J0." + correlationIdField + "=? AND " +
                        "J0." + alphabetField + "=? AND " +
                        "J0." + alphabetField + "=J1." + alphabetField + " AND " +
                        "J1." + correlationIdField + "!=J0." + correlationIdField +
                " ORDER BY J1." + correlationIdField, new String[] { Integer.toString(correlation), Integer.toString(alphabet) });

        final ImmutableIntKeyMap.Builder<ImmutableIntKeyMap<String>> result = new ImmutableIntKeyMap.Builder<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int corrId = cursor.getInt(0);
                    ImmutableIntKeyMap.Builder<String> corr = new ImmutableIntKeyMap.Builder<>();
                    corr.put(cursor.getInt(1), cursor.getString(2));
                    do {
                        int newCorrId = cursor.getInt(0);
                        if (corrId == newCorrId) {
                            corr.put(cursor.getInt(1), cursor.getString(2));
                        }
                        else {
                            result.put(corrId, corr.build());
                            corr = new ImmutableIntKeyMap.Builder<>();
                            corrId = newCorrId;
                            corr.put(cursor.getInt(1), cursor.getString(2));
                        }
                    } while(cursor.moveToNext());

                    result.put(corrId, corr.build());
                }
            }
            finally {
                cursor.close();
            }
        }

        return result.build();
    }

    private ImmutableList<AcceptationDetailsAdapter.Item> getAdapterItems(int correlationId) {
        final DbManager manager = DbManager.getInstance();
        final DbExporter.Database db = manager.getDatabase();
        final SQLiteDatabase sqliteDb = manager.getReadableDatabase();
        final ImmutableIntKeyMap<String> alphabets = readAllAlphabets(db, _preferredAlphabet);
        final ImmutableIntKeyMap<String> correlation = getCorrelationWithText(db, correlationId);

        final int entryCount = correlation.size();
        final ImmutableList.Builder<AcceptationDetailsAdapter.Item> result = new ImmutableList.Builder<>();
        result.add(new HeaderItem("Displaying details for correlation " + correlationId));
        for (int i = 0; i < entryCount; i++) {
            final String alphabetText = alphabets.get(correlation.keyAt(i));
            final String text = correlation.valueAt(i);
            result.add(new NonNavigableItem(alphabetText + " -> " + text));
        }

        final ImmutableIntKeyMap<String> acceptations = readAcceptationsIncludingCorrelation(db, correlationId, _preferredAlphabet);
        final int acceptationCount = acceptations.size();
        result.add(new HeaderItem("Acceptations where included"));
        for (int i = 0; i < acceptationCount; i++) {
            result.add(new AcceptationNavigableItem(acceptations.keyAt(i), acceptations.valueAt(i), false));
        }

        for (int i = 0; i < entryCount; i++) {
            final int matchingAlphabet = correlation.keyAt(i);
            final ImmutableIntKeyMap<ImmutableIntKeyMap<String>> correlations = readCorrelationsWithSameSymbolArray(sqliteDb, correlationId, matchingAlphabet);
            final int count = correlations.size();
            if (count > 0) {
                result.add(new HeaderItem("Other correlations sharing " + alphabets.get(matchingAlphabet)));
                for (int j = 0; j < count; j++) {
                    final int corrId = correlations.keyAt(j);
                    final ImmutableIntKeyMap<String> corr = correlations.valueAt(j);
                    final StringBuilder sb = new StringBuilder();
                    composeCorrelation(corr, sb);
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

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final int correlationId = getIntent().getIntExtra(ArgKeys.CORRELATION, 0);
        _listAdapter = new AcceptationDetailsAdapter(getAdapterItems(correlationId));
        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(_listAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        _listAdapter.getItem(position).navigate(this);
    }
}
