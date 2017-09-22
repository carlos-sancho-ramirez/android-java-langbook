package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseIntArray;

import java.util.HashMap;
import java.util.Map;

import static sword.langbook3.android.DbManager.idColumnName;

public class RuleTableActivity extends Activity {

    private static final class BundleKeys {
        static final String ACCEPTATION = "acc";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int dynamicAcceptation) {
        Intent intent = new Intent(context, RuleTableActivity.class);
        intent.putExtra(BundleKeys.ACCEPTATION, dynamicAcceptation);
        context.startActivity(intent);
    }

    private static final class TableCellRef {

        final int acceptation;
        final int rule;

        TableCellRef(int acceptation, int rule) {
            this.acceptation = acceptation;
            this.rule = rule;
        }

        @Override
        public int hashCode() {
            return acceptation * 31 + rule;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof TableCellRef)) {
                return false;
            }

            final TableCellRef that = (TableCellRef) other;
            return acceptation == that.acceptation && rule == that.rule;
        }
    }

    private Map<TableCellRef, String> readTableContent(int dynamicAcceptation) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.RuledConceptsTable ruledConcepts = DbManager.Tables.ruledConcepts;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();

        final Cursor cursor = db.rawQuery("SELECT" +
                        " J4." + strings.getColumnName(strings.getMainAcceptationColumnIndex()) +
                        ",J2." + ruledConcepts.getColumnName(ruledConcepts.getRuleColumnIndex()) +
                        ",J4." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J4." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J4." + strings.getColumnName(strings.getMainStringColumnIndex()) +
                        " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + ruledConcepts.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + idColumnName +
                        " JOIN " + ruledConcepts.getName() + " AS J2 ON J1." + ruledConcepts.getColumnName(ruledConcepts.getConceptColumnIndex()) + "=J2." + ruledConcepts.getColumnName(ruledConcepts.getConceptColumnIndex()) +
                        " JOIN " + acceptations.getName() + " AS J3 ON J2." + idColumnName + "=J3." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J4 ON J3." + idColumnName + "=J4." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " WHERE J0." + idColumnName + "=?" +
                        " ORDER BY J4." + strings.getColumnName(strings.getMainAcceptationColumnIndex()) +
                        ",J2." + ruledConcepts.getColumnName(ruledConcepts.getRuleColumnIndex()) +
                        ",J4." + strings.getColumnName(strings.getStringAlphabetColumnIndex()),
                new String[] { Integer.toString(dynamicAcceptation) });

        final HashMap<TableCellRef, String> tableMap = new HashMap<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    TableCellRef cellRef = new TableCellRef(cursor.getInt(0), cursor.getInt(1));
                    int alphabet = cursor.getInt(2);
                    String text = cursor.getString(3);
                    String mainText = cursor.getString(4);

                    while (cursor.moveToNext()) {
                        if (cursor.getInt(0) == cellRef.acceptation && cursor.getInt(1) == cellRef.rule) {
                            if (alphabet != preferredAlphabet && cursor.getInt(2) == preferredAlphabet) {
                                alphabet = preferredAlphabet;
                                text = cursor.getString(3);
                            }
                        }
                        else {
                            TableCellRef rawRef = new TableCellRef(cellRef.acceptation, 0);
                            if (!tableMap.containsKey(rawRef)) {
                                tableMap.put(rawRef, mainText);
                            }

                            tableMap.put(cellRef, text);

                            cellRef = new TableCellRef(cursor.getInt(0), cursor.getInt(1));
                            alphabet = cursor.getInt(2);
                            text = cursor.getString(3);
                            mainText = cursor.getString(4);
                        }
                    }

                    TableCellRef rawRef = new TableCellRef(cellRef.acceptation, 0);
                    if (!tableMap.containsKey(rawRef)) {
                        tableMap.put(rawRef, mainText);
                    }

                    tableMap.put(cellRef, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        return tableMap;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rule_table_activity);

        final int dynAcc = getIntent().getIntExtra(BundleKeys.ACCEPTATION, 0);
        Map<TableCellRef, String> tableContent = readTableContent(dynAcc);

        SparseIntArray acceptationSet = new SparseIntArray();
        SparseIntArray ruleSet = new SparseIntArray();

        for (TableCellRef ref : tableContent.keySet()) {
            acceptationSet.put(ref.acceptation, 0);
            ruleSet.put(ref.rule, 0);
        }

        final int acceptationCount = acceptationSet.size();
        int[] acceptations = new int[acceptationCount];
        for (int i = 0; i < acceptationCount; i++) {
            acceptations[i] = acceptationSet.keyAt(i);
        }

        final int ruleCount = ruleSet.size();
        int[] rules = new int[ruleCount];
        for (int i = 0; i < ruleCount; i++) {
            rules[i] = ruleSet.keyAt(i);
        }

        String[] content = new String[acceptationCount * ruleCount];
        for (int i = 0; i < acceptationCount; i++) {
            for (int j = 0; j < ruleCount; j++) {
                content[i * ruleCount + j] = tableContent.get(new TableCellRef(acceptations[i], rules[j]));
            }
        }

        final RuleTableView view = findViewById(R.id.ruleTable);
        view.setValues(ruleCount, content);
    }
}
