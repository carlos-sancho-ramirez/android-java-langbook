package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static sword.langbook3.android.DbManager.idColumnName;

public class AcceptationDetailsActivity extends Activity {

    private static final class BundleKeys {
        static final String STATIC_ACCEPTATION = "sa";
        static final String DYNAMIC_ACCEPTATION = "da";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    private static final int preferredAlphabet = 4;

    public static void open(Context context, int staticAcceptation, int dynamicAcceptation) {
        Intent intent = new Intent(context, AcceptationDetailsActivity.class);
        intent.putExtra(BundleKeys.STATIC_ACCEPTATION, staticAcceptation);
        intent.putExtra(BundleKeys.DYNAMIC_ACCEPTATION, dynamicAcceptation);
        context.startActivity(intent);
    }

    private List<SparseArray<String>> readCorrelationArray(SQLiteDatabase db, int acceptation) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations; // J0
        final DbManager.CorrelationArraysTable correlationArrays = DbManager.Tables.correlationArrays; // J1
        final DbManager.CorrelationsTable correlations = DbManager.Tables.correlations; // J2
        final DbManager.SymbolArraysTable symbolArrays = DbManager.Tables.symbolArrays; // J3
        final DbManager.AlphabetsTable alphabets = DbManager.Tables.alphabets;
        final DbManager.LanguagesTable languages = DbManager.Tables.languages;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                    " J1." + correlationArrays.getColumnName(correlationArrays.getArrayPositionColumnIndex()) +
                    ",J2." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) +
                    ",J3." + symbolArrays.getColumnName(symbolArrays.getStrColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                    " JOIN " + correlationArrays.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getCorrelationArrayColumnIndex()) + "=J1." + correlationArrays.getColumnName(correlationArrays.getArrayIdColumnIndex()) +
                    " JOIN " + correlations.getName() + " AS J2 ON J1." + correlationArrays.getColumnName(correlationArrays.getCorrelationColumnIndex()) + "=J2." + correlations.getColumnName(correlations.getCorrelationIdColumnIndex()) +
                    " JOIN " + symbolArrays.getName() + " AS J3 ON J2." + correlations.getColumnName(correlations.getSymbolArrayColumnIndex()) + "=J3." + idColumnName +
                " WHERE J0." + idColumnName + "=?" +
                " ORDER BY" +
                    " J1." + correlationArrays.getColumnName(correlationArrays.getArrayPositionColumnIndex()) +
                    ",J2." + correlations.getColumnName(correlations.getAlphabetColumnIndex())
                , new String[] { Integer.toString(acceptation) });

        final ArrayList<SparseArray<String>> result = new ArrayList<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    SparseArray<String> corr = new SparseArray<>();
                    int pos = cursor.getInt(0);
                    if (pos != result.size()) {
                        throw new AssertionError("Expected position " + result.size() + ", but it was " + pos);
                    }

                    corr.put(cursor.getInt(1), cursor.getString(2));

                    while(cursor.moveToNext()) {
                        int newPos = cursor.getInt(0);
                        if (newPos != pos) {
                            result.add(corr);
                            corr = new SparseArray<>();
                        }
                        pos = newPos;

                        if (newPos != result.size()) {
                            throw new AssertionError("Expected position " + result.size() + ", but it was " + pos);
                        }
                        corr.put(cursor.getInt(1), cursor.getString(2));
                    }
                    result.add(corr);
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private String readLanguage(SQLiteDatabase db, int alphabet) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations; // J0
        final DbManager.AlphabetsTable alphabets = DbManager.Tables.alphabets;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                    " J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                    ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + alphabets.getName() + " AS J0" +
                    " JOIN " + acceptations.getName() + " AS J1 ON J0." + alphabets.getColumnName(alphabets.getLanguageColumnIndex()) + "=J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                    " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                    " WHERE J0." + idColumnName + "=?",
                new String[] { Integer.toString(alphabet) });

        int firstAlphabet = -1;
        String text = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    firstAlphabet = cursor.getInt(0);
                    text = cursor.getString(1);
                    while (firstAlphabet != preferredAlphabet && cursor.moveToNext()) {
                        if (cursor.getInt(0) == preferredAlphabet) {
                            firstAlphabet = preferredAlphabet;
                            text = cursor.getString(1);
                        }
                    }
                }
            }
            finally {
                cursor.close();
            }
        }

        return text;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceptation_details_activity);

        if (!getIntent().hasExtra(BundleKeys.STATIC_ACCEPTATION)) {
            throw new IllegalArgumentException("staticAcceptation not provided");
        }

        final int staticAcceptation = getIntent().getIntExtra(BundleKeys.STATIC_ACCEPTATION, 0);

        DbManager dbManager = new DbManager(this);
        SQLiteDatabase db = dbManager.getReadableDatabase();

        final StringBuilder sb = new StringBuilder("Displaying details for acceptation ")
                .append(staticAcceptation)
                .append("\n  * Correlation: ");

        List<SparseArray<String>> correlationArray = readCorrelationArray(db, staticAcceptation);
        for (int i = 0; i < correlationArray.size(); i++) {
            if (i != 0) {
                sb.append(" - ");
            }

            final SparseArray<String> correlation = correlationArray.get(i);
            for (int j = 0; j < correlation.size(); j++) {
                if (j != 0) {
                    sb.append('/');
                }

                sb.append(correlation.valueAt(j));
            }
        }

        sb.append("\n  * Language: ").append(readLanguage(db, correlationArray.get(0).keyAt(0)));

        final TextView tv = findViewById(R.id.textView);
        tv.setText(sb.toString());
    }
}
