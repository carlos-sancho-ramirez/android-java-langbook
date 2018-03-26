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

import sword.langbook3.android.LangbookDbSchema.AcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.AgentsTable;
import sword.langbook3.android.LangbookDbSchema.BunchSetsTable;
import sword.langbook3.android.LangbookDbSchema.CorrelationsTable;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.SymbolArraysTable;
import sword.langbook3.android.LangbookDbSchema.Tables;

import static sword.langbook3.android.AcceptationDetailsActivity.preferredAlphabet;
import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public class AgentDetailsActivity extends Activity {

    private static final class BundleKeys {
        static final String AGENT = "a";
    }

    public static void open(Context context, int agent) {
        Intent intent = new Intent(context, AgentDetailsActivity.class);
        intent.putExtra(BundleKeys.AGENT, agent);
        context.startActivity(intent);
    }

    int _agentId;

    int _targetBunch;
    int _sourceBunchSet;
    int _diffBunchSet;
    int _matcher;
    int _adder;
    int _rule;

    int readAgent(SQLiteDatabase db) {
        final AgentsTable agents = Tables.agents; // J0
        Cursor cursor = db.rawQuery(
                "SELECT " + agents.getColumnName(agents.getTargetBunchColumnIndex()) +
                        "," + agents.getColumnName(agents.getSourceBunchSetColumnIndex()) +
                        "," + agents.getColumnName(agents.getDiffBunchSetColumnIndex()) +
                        "," + agents.getColumnName(agents.getMatcherColumnIndex()) +
                        "," + agents.getColumnName(agents.getAdderColumnIndex()) +
                        "," + agents.getColumnName(agents.getRuleColumnIndex()) +
                        " FROM " + agents.name() +
                        " WHERE " + idColumnName + "=?",
                new String[] { Integer.toString(_agentId) });

        int concept = 0;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    _targetBunch = cursor.getInt(0);
                    _sourceBunchSet = cursor.getInt(1);
                    _diffBunchSet = cursor.getInt(2);
                    _matcher = cursor.getInt(3);
                    _adder = cursor.getInt(4);
                    _rule = cursor.getInt(5);
                }
            }
            finally {
                cursor.close();
            }
        }

        return concept;
    }

    private static final class BunchInclusionResult {

        final int acceptation;
        final String text;

        BunchInclusionResult(int acceptation, String text) {
            this.acceptation = acceptation;
            this.text = text;
        }
    }

    private BunchInclusionResult readBunch(SQLiteDatabase db, int bunch) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final StringQueriesTable strings = Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + idColumnName +
                        ",J1." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J1." + strings.getColumnName(strings.getStringColumnIndex()) +
                        " FROM " + acceptations.name() + " AS J0" +
                        " JOIN " + strings.name() + " AS J1 ON J0." + idColumnName + "=J1." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " WHERE J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=?",
                new String[] { Integer.toString(bunch) });

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int acc = cursor.getInt(0);
                    int alphabet = cursor.getInt(1);
                    String text = cursor.getString(2);
                    while (cursor.moveToNext()) {
                        if (alphabet != preferredAlphabet && cursor.getInt(1) == preferredAlphabet) {
                            acc = cursor.getInt(0);
                            text = cursor.getString(2);
                            alphabet = preferredAlphabet;
                        }
                    }

                    return new BunchInclusionResult(acc, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        // Agents applying morphologies usually have null targets. This is why, it is possible to reach this code.
        return null;
    }

    private BunchInclusionResult[] readBunchSet(SQLiteDatabase db, int bunchSet) {
        final BunchSetsTable bunchSets = Tables.bunchSets;
        final AcceptationsTable acceptations = Tables.acceptations;
        final StringQueriesTable strings = Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + bunchSets.getColumnName(bunchSets.getBunchColumnIndex()) +
                        ",J1." + idColumnName +
                        ",J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                        " FROM " + bunchSets.name() + " AS J0" +
                        " JOIN " + acceptations.name() + " AS J1 ON J0." + bunchSets.getColumnName(bunchSets.getBunchColumnIndex()) + "=J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.name() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " WHERE J0." + bunchSets.getColumnName(bunchSets.getSetIdColumnIndex()) + "=?",
                new String[] { Integer.toString(bunchSet) });

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    ArrayList<BunchInclusionResult> result = new ArrayList<>();

                    int bunch = cursor.getInt(0);
                    int acc = cursor.getInt(1);
                    int alphabet = cursor.getInt(2);
                    String text = cursor.getString(3);
                    while (cursor.moveToNext()) {
                        if (bunch == cursor.getInt(0)) {
                            if (alphabet != preferredAlphabet && cursor.getInt(2) == preferredAlphabet) {
                                acc = cursor.getInt(1);
                                text = cursor.getString(3);
                                alphabet = preferredAlphabet;
                            }
                        }
                        else {
                            result.add(new BunchInclusionResult(acc, text));

                            bunch = cursor.getInt(0);
                            acc = cursor.getInt(1);
                            alphabet = cursor.getInt(2);
                            text = cursor.getString(3);
                        }
                    }

                    result.add(new BunchInclusionResult(acc, text));
                    return result.toArray(new BunchInclusionResult[result.size()]);
                }
            }
            finally {
                cursor.close();
            }
        }

        return new BunchInclusionResult[0];
    }

    private SparseArray<String> readCorrelation(SQLiteDatabase db, int correlationId) {
        final CorrelationsTable correlations = Tables.correlations;
        final SymbolArraysTable symbolArrays = Tables.symbolArrays;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + correlations.getColumnName(correlations.getAlphabetColumnIndex()) +
                        ",J1." + symbolArrays.getColumnName(symbolArrays.getStrColumnIndex()) +
                        " FROM " + correlations.name() + " AS J0" +
                        " JOIN " + symbolArrays.name() + " AS J1 ON J0." + correlations.getColumnName(correlations.getSymbolArrayColumnIndex()) + "=J1." + idColumnName +
                        " WHERE J0." + correlations.getColumnName(correlations.getCorrelationIdColumnIndex()) + "=?",
                new String[] { Integer.toString(correlationId) });

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_details_activity);

        if (!getIntent().hasExtra(BundleKeys.AGENT)) {
            throw new IllegalArgumentException("agent identifier not provided");
        }

        _agentId = getIntent().getIntExtra(BundleKeys.AGENT, 0);
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();
        readAgent(db);

        final StringBuilder s = new StringBuilder("Agent #").append(_agentId);
        BunchInclusionResult targetResult = readBunch(db, _targetBunch);
        if (targetResult != null) {
            s.append("\nTarget: ").append(targetResult.text).append(" (").append(_targetBunch).append(')');
        }

        s.append("\nSource Bunch Set (").append(_sourceBunchSet).append("):");
        for (BunchInclusionResult r : readBunchSet(db, _sourceBunchSet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.acceptation).append(')');
        }

        s.append("\nDiff Bunch Set (").append(_diffBunchSet).append("):");
        for (BunchInclusionResult r : readBunchSet(db, _diffBunchSet)) {
            s.append("\n  * ").append(r.text).append(" (").append(r.acceptation).append(')');
        }

        s.append("\nMatcher: ").append(_matcher);
        SparseArray<String> matcher = readCorrelation(db, _matcher);
        for (int i = 0; i < matcher.size(); i++) {
            s.append("\n  * ").append(matcher.keyAt(i)).append(" -> ").append(matcher.valueAt(i));
        }

        s.append("\nAdder: ").append(_adder);
        SparseArray<String> adder = readCorrelation(db, _adder);
        for (int i = 0; i < adder.size(); i++) {
            s.append("\n  * ").append(adder.keyAt(i)).append(" -> ").append(adder.valueAt(i));
        }

        s.append("\nRule:");
        BunchInclusionResult ruleResult = readBunch(db, _rule);
        if (ruleResult != null) {
            s.append(' ').append(ruleResult.text);
        }
        else {
            s.append(" -");
        }
        s.append(" (").append(_rule).append(')');

        final TextView tv = findViewById(R.id.textView);
        tv.setText(s.toString());
    }
}
