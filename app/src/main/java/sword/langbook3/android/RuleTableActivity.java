package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;

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

    private int _columnCount;
    private TableCellValue[] _tableCellValues;

    public static void open(Context context, int dynamicAcceptation) {
        Intent intent = new Intent(context, RuleTableActivity.class);
        intent.putExtra(BundleKeys.ACCEPTATION, dynamicAcceptation);
        context.startActivity(intent);
    }

    private static final class TableCellRef {

        final int bunchSet;
        final int rule;

        TableCellRef(int agent, int rule) {
            this.bunchSet = agent;
            this.rule = rule;
        }

        @Override
        public int hashCode() {
            return bunchSet * 31 + rule;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof TableCellRef)) {
                return false;
            }

            final TableCellRef that = (TableCellRef) other;
            return bunchSet == that.bunchSet && rule == that.rule;
        }
    }

    private static final class TableCellValue {

        final int staticAcceptation;
        final int dynamicAcceptation;
        final String text;

        TableCellValue(int staticAcceptation, int dynamicAcceptation, String text) {
            this.staticAcceptation = staticAcceptation;
            this.dynamicAcceptation = dynamicAcceptation;
            this.text = text;
        }
    }

    private String readAcceptationText(int acceptation) {
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;
        SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        final Cursor cursor = db.rawQuery("SELECT " + strings.getColumnName(strings.getStringColumnIndex()) +
                        " FROM " + strings.getName() + " WHERE " +
                        strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) + "=?",
                new String[]{Integer.toString(acceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }

        throw new AssertionError();
    }

    private Map<TableCellRef, TableCellValue> readTableContent(int dynamicAcceptation) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.AgentsTable agents = DbManager.Tables.agents;
        final DbManager.RuledConceptsTable ruledConcepts = DbManager.Tables.ruledConcepts;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();

        final Cursor cursor = db.rawQuery("SELECT" +
                        " J4." + agents.getColumnName(agents.getSourceBunchSetColumnIndex()) +
                        ",J3." + agents.getColumnName(agents.getRuleColumnIndex()) +
                        ",J6." + idColumnName +
                        ",J7." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J7." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J7." + strings.getColumnName(strings.getMainAcceptationColumnIndex()) +
                    " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + ruledConcepts.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + idColumnName +
                        " JOIN " + ruledConcepts.getName() + " AS J2 ON J1." + ruledConcepts.getColumnName(ruledConcepts.getConceptColumnIndex()) + "=J2." + ruledConcepts.getColumnName(ruledConcepts.getConceptColumnIndex()) +
                        " JOIN " + agents.getName() + " AS J3 ON J2." + ruledConcepts.getColumnName(ruledConcepts.getAgentColumnIndex()) + "=J3." + idColumnName +
                        " JOIN " + agents.getName() + " AS J4 ON J3." + agents.getColumnName(agents.getRuleColumnIndex()) + "=J4." + agents.getColumnName(agents.getRuleColumnIndex()) +
                        " JOIN " + ruledConcepts.getName() + " AS J5 ON J4." + idColumnName + "=J5." + ruledConcepts.getColumnName(ruledConcepts.getAgentColumnIndex()) +
                        " JOIN " + acceptations.getName() + " AS J6 ON J5." + idColumnName + "=J6." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J7 ON J6." + idColumnName + "=J7." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                    " WHERE J0." + idColumnName + "=?" +
                        " ORDER BY J4." + agents.getColumnName(agents.getSourceBunchSetColumnIndex()) +
                        ",J6." + idColumnName +
                        ",J3." + agents.getColumnName(agents.getRuleColumnIndex()) +
                        ",J7." + strings.getColumnName(strings.getStringAlphabetColumnIndex()),
                new String[] { Integer.toString(dynamicAcceptation) });

        final HashMap<TableCellRef, TableCellValue> tableMap = new HashMap<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    TableCellRef cellRef = new TableCellRef(cursor.getInt(0), cursor.getInt(1));
                    int dynAcc = cursor.getInt(2);
                    int alphabet = cursor.getInt(3);
                    String text = cursor.getString(4);
                    int staAcc = cursor.getInt(5);

                    while (cursor.moveToNext()) {
                        if (cursor.getInt(0) == cellRef.bunchSet && cursor.getInt(1) == cellRef.rule) {
                            if (alphabet != preferredAlphabet && cursor.getInt(2) == preferredAlphabet) {
                                alphabet = preferredAlphabet;
                                text = cursor.getString(3);
                            }
                        }
                        else {
                            tableMap.put(cellRef, new TableCellValue(staAcc, dynAcc, text));

                            cellRef = new TableCellRef(cursor.getInt(0), cursor.getInt(1));
                            dynAcc = cursor.getInt(2);
                            alphabet = cursor.getInt(3);
                            text = cursor.getString(4);
                            staAcc = cursor.getInt(5);
                        }
                    }

                    tableMap.put(cellRef, new TableCellValue(staAcc, dynAcc, text));
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
        Map<TableCellRef, TableCellValue> tableContent = readTableContent(dynAcc);

        SparseArray<String> acceptationSet = new SparseArray<>();
        SparseArray<String> ruleSet = new SparseArray<>();

        SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        for (Map.Entry<TableCellRef, TableCellValue> entry : tableContent.entrySet()) {
            final TableCellRef ref = entry.getKey();
            if (acceptationSet.get(ref.bunchSet) == null) {
                acceptationSet.put(ref.bunchSet, readAcceptationText(entry.getValue().staticAcceptation));
            }

            if (ruleSet.get(ref.rule) == null) {
                ruleSet.put(ref.rule, AcceptationDetailsActivity.readConceptText(db, ref.rule));
            }
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

        final int columnCount = ruleCount + 1;
        String[] texts = new String[(acceptationCount + 1) * columnCount];
        TableCellValue[] content = new TableCellValue[(acceptationCount + 1) * (ruleCount + 1)];
        for (int i = 0; i < ruleCount; i++) {
            texts[i + 1] = ruleSet.get(rules[i]);
        }

        for (int i = 0; i < acceptationCount; i++) {
            texts[(i + 1) * columnCount] = acceptationSet.get(acceptations[i]);
            for (int j = 0; j < ruleCount; j++) {
                final int index = (i + 1) * columnCount + j + 1;
                final TableCellRef ref = new TableCellRef(acceptations[i], rules[j]);
                final TableCellValue value = tableContent.get(ref);
                texts[index] = (value != null)? value.text : null;
                content[index] = value;
            }
        }

        _columnCount = columnCount;
        _tableCellValues = content;

        final RuleTableView view = findViewById(R.id.ruleTable);
        view.setValues(columnCount, texts);
    }
}