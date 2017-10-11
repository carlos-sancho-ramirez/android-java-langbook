package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;

import static sword.langbook3.android.DbManager.idColumnName;

public class CorrelationDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final class BundleKeys {
        static final String CORRELATION = "cId";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    private static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int correlationId) {
        Intent intent = new Intent(context, CorrelationDetailsActivity.class);
        intent.putExtra(BundleKeys.CORRELATION, correlationId);
        context.startActivity(intent);
    }

    private AcceptationDetailsAdapter _listAdapter;

    private SparseArray<String> readCorrelation(SQLiteDatabase db, int correlation) {
        final DbManager.CorrelationsTable correlations = DbManager.Tables.correlations;
        final DbManager.SymbolArraysTable symbolArrays = DbManager.Tables.symbolArrays;

        Cursor cursor = db.rawQuery(
                "SELECT J0." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) +
                        ",J1." + symbolArrays.getColumnName(symbolArrays.getStrColumnIndex()) +
                " FROM " + correlations.getName() + " AS J0" +
                        " JOIN " + symbolArrays.getName() + " AS J1 ON J0." + correlations.getColumnName(correlations.getSymbolArrayColumnIndex()) + "=J1." + idColumnName +
                        " WHERE J0." + correlations.getColumnName(correlations.getCorrelationIdColumnIndex()) + "=?", new String[] { Integer.toString(correlation) });

        final SparseArray<String> result = new SparseArray<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        result.put(cursor.getInt(0), cursor.getString(1));
                    } while(cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private SparseArray<String> readAcceptationsIncludingCorrelation(SQLiteDatabase db, int correlation) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.CorrelationArraysTable correlationArrays = DbManager.Tables.correlationArrays;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT J1." + idColumnName +
                        ",J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + correlationArrays.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + correlationArrays.getColumnName(correlationArrays.getArrayIdColumnIndex()) + "=J1." + acceptations.getColumnName(acceptations.getCorrelationArrayColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + correlationArrays.getColumnName(correlationArrays.getCorrelationColumnIndex()) + "=?" +
                " ORDER BY J1." + idColumnName, new String[] { Integer.toString(correlation) });

        final SparseArray<String> result = new SparseArray<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int acc = cursor.getInt(0);
                    int alphabet = cursor.getInt(1);
                    String text = cursor.getString(2);
                    do {
                        int newAcc = cursor.getInt(0);
                        if (acc == newAcc) {
                            if (alphabet != preferredAlphabet && cursor.getInt(1) == preferredAlphabet) {
                                alphabet = preferredAlphabet;
                                text = cursor.getString(2);
                            }
                        }
                        else {
                            result.put(acc, text);
                            acc = newAcc;
                            alphabet = cursor.getInt(1);
                            text = cursor.getString(2);
                        }
                    } while(cursor.moveToNext());

                    result.put(acc, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private AcceptationDetailsAdapter.Item[] getAdapterItems(int correlationId) {
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        final SparseArray<String> alphabets = QuizSelectionActivity.readAllAlphabets(db);
        final SparseArray<String> correlation = readCorrelation(db, correlationId);

        final int entryCount = correlation.size();
        final ArrayList<AcceptationDetailsAdapter.Item> result = new ArrayList<>();
        result.add(new HeaderItem("Displaying details for correlation " + correlationId));
        for (int i = 0; i < entryCount; i++) {
            final String alphabetText = alphabets.get(correlation.keyAt(i));
            final String text = correlation.valueAt(i);
            result.add(new NonNavigableItem(alphabetText + " -> " + text));
        }

        final SparseArray<String> acceptations = readAcceptationsIncludingCorrelation(db, correlationId);
        final int acceptationCount = acceptations.size();
        result.add(new HeaderItem("Acceptations where included"));
        for (int i = 0; i < acceptationCount; i++) {
            result.add(new AcceptationNavigableItem(acceptations.keyAt(i), acceptations.valueAt(i), false));
        }

        return result.toArray(new AcceptationDetailsAdapter.Item[result.size()]);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correlation_details_activity);

        final int correlationId = getIntent().getIntExtra(BundleKeys.CORRELATION, 0);
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
