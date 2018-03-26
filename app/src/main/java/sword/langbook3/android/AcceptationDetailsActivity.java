package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import sword.langbook3.android.AcceptationDetailsAdapter.AcceptationNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.AgentNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.HeaderItem;
import sword.langbook3.android.AcceptationDetailsAdapter.NonNavigableItem;
import sword.langbook3.android.AcceptationDetailsAdapter.RuleNavigableItem;
import sword.langbook3.android.LangbookDbSchema.AcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.AgentSetsTable;
import sword.langbook3.android.LangbookDbSchema.AgentsTable;
import sword.langbook3.android.LangbookDbSchema.AlphabetsTable;
import sword.langbook3.android.LangbookDbSchema.BunchAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.BunchConceptsTable;
import sword.langbook3.android.LangbookDbSchema.BunchSetsTable;
import sword.langbook3.android.LangbookDbSchema.CorrelationArraysTable;
import sword.langbook3.android.LangbookDbSchema.CorrelationsTable;
import sword.langbook3.android.LangbookDbSchema.LanguagesTable;
import sword.langbook3.android.LangbookDbSchema.RuledAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.SymbolArraysTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;

import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public class AcceptationDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final class BundleKeys {
        static final String STATIC_ACCEPTATION = "sa";
        static final String DYNAMIC_ACCEPTATION = "da";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = 4;

    private int _concept;
    private boolean _shouldShowBunchChildrenQuizMenuOption;
    private AcceptationDetailsAdapter _listAdapter;

    public static void open(Context context, int staticAcceptation, int dynamicAcceptation) {
        Intent intent = new Intent(context, AcceptationDetailsActivity.class);
        intent.putExtra(BundleKeys.STATIC_ACCEPTATION, staticAcceptation);
        intent.putExtra(BundleKeys.DYNAMIC_ACCEPTATION, dynamicAcceptation);
        context.startActivity(intent);
    }

    static final class CorrelationHolder {
        final int id;
        final SparseArray<String> texts;

        CorrelationHolder(int id, SparseArray<String> texts) {
            this.id = id;
            this.texts = texts;
        }
    }

    private List<CorrelationHolder> readCorrelationArray(SQLiteDatabase db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations; // J0
        final CorrelationArraysTable correlationArrays = Tables.correlationArrays; // J1
        final CorrelationsTable correlations = Tables.correlations; // J2
        final SymbolArraysTable symbolArrays = Tables.symbolArrays; // J3

        Cursor cursor = db.rawQuery(
                "SELECT" +
                    " J1." + correlationArrays.getColumnName(correlationArrays.getArrayPositionColumnIndex()) +
                    ",J2." + correlations.getColumnName(correlations.getCorrelationIdColumnIndex()) +
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

        final ArrayList<CorrelationHolder> result = new ArrayList<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    SparseArray<String> corr = new SparseArray<>();
                    int pos = cursor.getInt(0);
                    int correlationId = cursor.getInt(1);
                    if (pos != result.size()) {
                        throw new AssertionError("Expected position " + result.size() + ", but it was " + pos);
                    }

                    corr.put(cursor.getInt(2), cursor.getString(3));

                    while(cursor.moveToNext()) {
                        int newPos = cursor.getInt(0);
                        if (newPos != pos) {
                            result.add(new CorrelationHolder(correlationId, corr));
                            correlationId = cursor.getInt(1);
                            corr = new SparseArray<>();
                        }
                        pos = newPos;

                        if (newPos != result.size()) {
                            throw new AssertionError("Expected position " + result.size() + ", but it was " + pos);
                        }
                        corr.put(cursor.getInt(2), cursor.getString(3));
                    }
                    result.add(new CorrelationHolder(correlationId, corr));
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    static int readConcept(SQLiteDatabase db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations; // J0
        Cursor cursor = db.rawQuery(
                "SELECT " + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " FROM " + acceptations.getName() +
                        " WHERE " + idColumnName + "=?",
                new String[] { Integer.toString(acceptation) });

        int concept = 0;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    concept = cursor.getInt(0);
                }
            }
            finally {
                cursor.close();
            }
        }

        return concept;
    }

    static String readConceptText(SQLiteDatabase db, int concept) {
        final AcceptationsTable acceptations = Tables.acceptations; // J0
        final StringQueriesTable strings = Tables.stringQueries;

        final int j1Offset = acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(strings, acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(j1Offset + strings.getStringAlphabetColumnIndex(), j1Offset + strings.getStringColumnIndex());

        final DbResult result = DbManager.getInstance().attach(query).iterator();
        String text;
        try {
            DbResult.Row row = result.next();
            int firstAlphabet = row.get(0).toInt();
            text = row.get(1).toText();
            while (firstAlphabet != preferredAlphabet && result.hasNext()) {
                row = result.next();
                if (row.get(0).toInt() == preferredAlphabet) {
                    firstAlphabet = preferredAlphabet;
                    text = row.get(1).toText();
                }
            }
        }
        finally {
            result.close();
        }

        return text;
    }

    private static final class LanguageResult {

        final int acceptation;
        final int language;
        final String text;

        LanguageResult(int acceptation, int language, String text) {
            this.acceptation = acceptation;
            this.language = language;
            this.text = text;
        }
    }

    private LanguageResult readLanguageFromAlphabet(SQLiteDatabase db, int alphabet) {
        final AcceptationsTable acceptations = Tables.acceptations; // J0
        final AlphabetsTable alphabets = Tables.alphabets;
        final StringQueriesTable strings = Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                    " J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                    ",J1." + idColumnName +
                    ",J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                    ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + alphabets.getName() + " AS J0" +
                    " JOIN " + acceptations.getName() + " AS J1 ON J0." + alphabets.getColumnName(alphabets.getLanguageColumnIndex()) + "=J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                    " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                    " WHERE J0." + idColumnName + "=?",
                new String[] { Integer.toString(alphabet) });

        int lang = -1;
        int langAcc = -1;
        String text = null;
        try {
            cursor.moveToFirst();
            lang = cursor.getInt(0);
            langAcc = cursor.getInt(1);
            int firstAlphabet = cursor.getInt(2);
            text = cursor.getString(3);
            while (firstAlphabet != preferredAlphabet && cursor.moveToNext()) {
                if (cursor.getInt(2) == preferredAlphabet) {
                    lang = cursor.getInt(0);
                    langAcc = cursor.getInt(1);
                    firstAlphabet = preferredAlphabet;
                    text = cursor.getString(3);
                }
            }
        }
        finally {
            cursor.close();
        }

        return new LanguageResult(langAcc, lang, text);
    }

    private static final class AcceptationResult {

        final int acceptation;
        final String text;

        AcceptationResult(int acceptation, String text) {
            this.acceptation = acceptation;
            this.text = text;
        }
    }

    private AcceptationResult readDefinition(SQLiteDatabase db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final BunchConceptsTable bunchConcepts = Tables.bunchConcepts;
        final StringQueriesTable strings = Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J2." + idColumnName +
                        ",J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                    " JOIN " + bunchConcepts.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + bunchConcepts.getColumnName(bunchConcepts.getConceptColumnIndex()) +
                    " JOIN " + acceptations.getName() + " AS J2 ON J1." + bunchConcepts.getColumnName(bunchConcepts.getBunchColumnIndex()) + "=J2." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                    " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + idColumnName + "=?",
                new String[] { Integer.toString(acceptation) });

        AcceptationResult result = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int acc = cursor.getInt(0);
                    int firstAlphabet = cursor.getInt(1);
                    String text = cursor.getString(2);
                    while (firstAlphabet != preferredAlphabet && cursor.moveToNext()) {
                        if (cursor.getInt(1) == preferredAlphabet) {
                            acc = cursor.getInt(0);
                            text = cursor.getString(2);
                            break;
                        }
                    }

                    result = new AcceptationResult(acc, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private AcceptationResult[] readSubTypes(SQLiteDatabase db, int acceptation, int language) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final AlphabetsTable alphabets = Tables.alphabets;
        final BunchConceptsTable bunchConcepts = Tables.bunchConcepts;
        final StringQueriesTable strings = Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J2." + idColumnName +
                        ",J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + bunchConcepts.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + bunchConcepts.getColumnName(bunchConcepts.getBunchColumnIndex()) +
                        " JOIN " + acceptations.getName() + " AS J2 ON J1." + bunchConcepts.getColumnName(bunchConcepts.getConceptColumnIndex()) + "=J2." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " JOIN " + alphabets.getName() + " AS J4 ON J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) + "=J4." + idColumnName +
                " WHERE J0." + idColumnName + "=?" +
                        " AND J4." + alphabets.getColumnName(alphabets.getLanguageColumnIndex()) + "=?" +
                " ORDER BY J2." + idColumnName,
                new String[] { Integer.toString(acceptation), Integer.toString(language) });

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final ArrayList<AcceptationResult> result = new ArrayList<>();
                    int acc = cursor.getInt(0);
                    int alphabet = cursor.getInt(1);
                    String text = cursor.getString(2);

                    while (cursor.moveToNext()) {
                        if (cursor.getInt(0) == acc) {
                            if (alphabet != preferredAlphabet && cursor.getInt(1) == preferredAlphabet) {
                                alphabet = preferredAlphabet;
                                text = cursor.getString(2);
                            }
                        }
                        else {
                            result.add(new AcceptationResult(acc, text));

                            acc = cursor.getInt(0);
                            alphabet = cursor.getInt(1);
                            text = cursor.getString(2);
                        }
                    }

                    result.add(new AcceptationResult(acc, text));
                    return result.toArray(new AcceptationResult[result.size()]);
                }
            }
            finally {
                cursor.close();
            }
        }

        return new AcceptationResult[0];
    }

    private static final class BunchInclusionResult {

        final int acceptation;
        final boolean dynamic;
        final String text;

        BunchInclusionResult(int acceptation, boolean dynamic, String text) {
            this.acceptation = acceptation;
            this.dynamic = dynamic;
            this.text = text;
        }
    }

    private BunchInclusionResult[] readBunchesWhereIncluded(SQLiteDatabase db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final StringQueriesTable strings = Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) +
                        ",J1." + idColumnName +
                        ",J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAgentSetColumnIndex()) +
                " FROM " + bunchAcceptations.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) + "=J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " WHERE J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) + "=?",
                new String[] { Integer.toString(acceptation) });

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final int nullAgentSet = Tables.agentSets.nullReference();
                    ArrayList<BunchInclusionResult> result = new ArrayList<>();

                    int bunch = cursor.getInt(0);
                    int acc = cursor.getInt(1);
                    int firstAlphabet = cursor.getInt(2);
                    String text = cursor.getString(3);
                    int agentSet = cursor.getInt(4);
                    while (cursor.moveToNext()) {
                        if (firstAlphabet != preferredAlphabet && cursor.getInt(2) == preferredAlphabet) {
                            acc = cursor.getInt(1);
                            text = cursor.getString(3);
                            firstAlphabet = preferredAlphabet;
                        }

                        if (bunch != cursor.getInt(0)) {
                            result.add(new BunchInclusionResult(acc, agentSet != nullAgentSet, text));

                            bunch = cursor.getInt(0);
                            agentSet = cursor.getInt(4);
                            acc = cursor.getInt(1);
                            firstAlphabet = cursor.getInt(2);
                            text = cursor.getString(3);
                        }
                    }

                    result.add(new BunchInclusionResult(acc, agentSet != nullAgentSet, text));
                    return result.toArray(new BunchInclusionResult[result.size()]);
                }
            }
            finally {
                cursor.close();
            }
        }

        return new BunchInclusionResult[0];
    }

    private static final class BunchChildResult {

        final int acceptation;
        final boolean dynamic;
        final String text;

        BunchChildResult(int acceptation, boolean dynamic, String text) {
            this.acceptation = acceptation;
            this.dynamic = dynamic;
            this.text = text;
        }
    }

    private BunchChildResult[] readBunchChildren(SQLiteDatabase db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final StringQueriesTable strings = Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J1." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) +
                        ",J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J1." + bunchAcceptations.getColumnName(bunchAcceptations.getAgentSetColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + bunchAcceptations.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + bunchAcceptations.getColumnName(bunchAcceptations.getBunchColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J2 ON J1." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + idColumnName + "=?" +
                " ORDER BY J1." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()),
                new String[] { Integer.toString(acceptation) });

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    final int nullAgentSet = Tables.agentSets.nullReference();
                    ArrayList<BunchChildResult> result = new ArrayList<>();

                    int acc = cursor.getInt(0);
                    int alphabet = cursor.getInt(1);
                    String text = cursor.getString(2);
                    int agentSet = cursor.getInt(3);
                    while (cursor.moveToNext()) {
                        if (acc == cursor.getInt(0)) {
                            if (alphabet != preferredAlphabet && cursor.getInt(2) == preferredAlphabet) {
                                alphabet = preferredAlphabet;
                                text = cursor.getString(2);
                            }
                        }
                        else {
                            result.add(new BunchChildResult(acc, agentSet != nullAgentSet, text));

                            acc = cursor.getInt(0);
                            alphabet = cursor.getInt(1);
                            text = cursor.getString(2);
                            agentSet = cursor.getInt(3);
                        }
                    }

                    result.add(new BunchChildResult(acc, agentSet != nullAgentSet, text));
                    return result.toArray(new BunchChildResult[result.size()]);
                }
            }
            finally {
                cursor.close();
            }
        }

        return new BunchChildResult[0];
    }

    private static final class SynonymTranslationResult {

        final int acceptation;
        final int language;
        final String text;

        SynonymTranslationResult(int acceptation, int language, String text) {
            this.acceptation = acceptation;
            this.language = language;
            this.text = text;
        }
    }

    private SynonymTranslationResult[] readSynonymsAndTranslations(SQLiteDatabase db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final AlphabetsTable alphabets = Tables.alphabets;
        final StringQueriesTable strings = Tables.stringQueries;
        final LanguagesTable languages = Tables.languages;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J1." + idColumnName +
                        ",J4." + idColumnName +
                        ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " JOIN " + alphabets.getName() + " AS J3 ON J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) + "=J3." + idColumnName +
                        " JOIN " + languages.getName() + " AS J4 ON J3." + alphabets.getColumnName(alphabets.getLanguageColumnIndex()) + "=J4." + idColumnName +
                " WHERE J0." + idColumnName + "=?" +
                        " AND J0." + acceptations.getColumnName(acceptations.getWordColumnIndex()) + "!=J1." + acceptations.getColumnName(acceptations.getWordColumnIndex()) +
                        " AND J3." + idColumnName + "=J4." + languages.getColumnName(languages.getMainAlphabetColumnIndex()),
                new String[] { Integer.toString(acceptation) });

        SynonymTranslationResult[] result = null;
        if (cursor != null) {
            try {
                result = new SynonymTranslationResult[cursor.getCount()];
                if (cursor.moveToFirst()) {
                    int index = 0;
                    do {
                        result[index++] = new SynonymTranslationResult(cursor.getInt(0),
                                cursor.getInt(1), cursor.getString(2));
                    } while (cursor.moveToNext());
                }
            }
            finally {
                cursor.close();
            }
        }
        else {
            result = new SynonymTranslationResult[0];
        }

        return result;
    }

    private static final class MorphologyResult {

        final int agent;
        final int dynamicAcceptation;
        final int rule;
        final String ruleText;
        final String text;

        MorphologyResult(int agent, int dynamicAcceptation, int rule, String ruleText, String text) {
            this.agent = agent;
            this.dynamicAcceptation = dynamicAcceptation;
            this.rule = rule;
            this.ruleText = ruleText;
            this.text = text;
        }
    }

    private MorphologyResult[] readMorphologies(SQLiteDatabase db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final AgentsTable agents = Tables.agents;
        final StringQueriesTable strings = Tables.stringQueries;
        final RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + idColumnName +
                        ",J3." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        ",J1." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J1." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J4." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J4." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J2." + idColumnName +
                " FROM " + ruledAcceptations.getName() + " AS J0" +
                        " JOIN " + strings.getName() + " AS J1 ON J0." + idColumnName + "=J1." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " JOIN " + agents.getName() + " AS J2 ON J0." + ruledAcceptations.getColumnName(ruledAcceptations.getAgentColumnIndex()) + "=J2." + idColumnName +
                        " JOIN " + acceptations.getName() + " AS J3 ON J2." + agents.getColumnName(agents.getRuleColumnIndex()) + "=J3." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J4 ON J3." + idColumnName + "=J4." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + ruledAcceptations.getColumnName(ruledAcceptations.getAcceptationColumnIndex()) + "=?" +
                        " AND J1." + strings.getColumnName(strings.getMainAcceptationColumnIndex()) + "=?" +
                " ORDER BY J0." + idColumnName,
                new String[] { Integer.toString(acceptation) , Integer.toString(acceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    ArrayList<MorphologyResult> result = new ArrayList<>();

                    int acc = cursor.getInt(0);
                    int rule = cursor.getInt(1);
                    int alphabet = cursor.getInt(2);
                    String text = cursor.getString(3);
                    int ruleAlphabet = cursor.getInt(4);
                    String ruleText = cursor.getString(5);
                    int agent = cursor.getInt(6);

                    while (cursor.moveToNext()) {
                        if (cursor.getInt(0) == acc) {
                            if (alphabet != preferredAlphabet && cursor.getInt(2) == preferredAlphabet) {
                                alphabet = preferredAlphabet;
                                text = cursor.getString(3);
                            }

                            if (ruleAlphabet != preferredAlphabet && cursor.getInt(4) == preferredAlphabet) {
                                ruleAlphabet = preferredAlphabet;
                                ruleText = cursor.getString(5);
                            }
                        }
                        else {
                            result.add(new MorphologyResult(agent, acc, rule, ruleText, text));

                            acc = cursor.getInt(0);
                            rule = cursor.getInt(1);
                            alphabet = cursor.getInt(2);
                            text = cursor.getString(3);
                            ruleAlphabet = cursor.getInt(4);
                            ruleText = cursor.getString(5);
                            agent = cursor.getInt(6);
                        }
                    }

                    result.add(new MorphologyResult(agent, acc, rule, ruleText, text));
                    return result.toArray(new MorphologyResult[result.size()]);
                }
            }
            finally {
                cursor.close();
            }
        }

        return new MorphologyResult[0];
    }

    private static class InvolvedAgentResult {

        interface Flags {
            int target = 1;
            int source = 2;
            int diff = 4;
            int rule = 8;
            int processed = 16;
        }

        final int agentId;
        final int flags;

        InvolvedAgentResult(int agentId, int flags) {
            this.agentId = agentId;
            this.flags = flags;
        }
    }

    private int[] readAgentsWhereAccIsTarget(SQLiteDatabase db, int staticAcceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final AgentsTable agents = Tables.agents;

        final Cursor cursor = db.rawQuery(" SELECT J1." + idColumnName +
                " FROM " + acceptations.getName() + " AS J0" +
                " JOIN " + agents.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + agents.getColumnName(agents.getTargetBunchColumnIndex()) +
                " WHERE J0." + idColumnName + "=?",
        new String[] { Integer.toString(staticAcceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int[] result = new int[cursor.getCount()];
                    int index = 0;
                    do {
                        result[index++] = cursor.getInt(0);
                    } while (cursor.moveToNext());

                    return result;
                }
            }
            finally {
                cursor.close();
            }
        }

        return new int[0];
    }

    private int[] readAgentsWhereAccIsSource(SQLiteDatabase db, int staticAcceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final AgentsTable agents = Tables.agents;
        final BunchSetsTable bunchSets = Tables.bunchSets;

        final Cursor cursor = db.rawQuery(" SELECT J2." + idColumnName +
                " FROM " + acceptations.getName() + " AS J0" +
                " JOIN " + bunchSets.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + bunchSets.getColumnName(bunchSets.getBunchColumnIndex()) +
                " JOIN " + agents.getName() + " AS J2 ON J1." + bunchSets.getColumnName(bunchSets.getSetIdColumnIndex()) + "=J2." + agents.getColumnName(agents.getSourceBunchSetColumnIndex()) +
                " WHERE J0." + idColumnName + "=?",
                new String[] { Integer.toString(staticAcceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int[] result = new int[cursor.getCount()];
                    int index = 0;
                    do {
                        result[index++] = cursor.getInt(0);
                    } while (cursor.moveToNext());

                    return result;
                }
            }
            finally {
                cursor.close();
            }
        }

        return new int[0];
    }

    private int[] readAgentsWhereAccIsRule(SQLiteDatabase db, int staticAcceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final AgentsTable agents = Tables.agents;

        final Cursor cursor = db.rawQuery(" SELECT J1." + idColumnName +
                        " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + agents.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + agents.getColumnName(agents.getRuleColumnIndex()) +
                        " WHERE J0." + idColumnName + "=?",
                new String[] { Integer.toString(staticAcceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int[] result = new int[cursor.getCount()];
                    int index = 0;
                    do {
                        result[index++] = cursor.getInt(0);
                    } while (cursor.moveToNext());

                    return result;
                }
            }
            finally {
                cursor.close();
            }
        }

        return new int[0];
    }

    private int[] readAgentsWhereAccIsProcessed(SQLiteDatabase db, int staticAcceptation) {
        final AgentsTable agents = Tables.agents;
        final BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final AgentSetsTable agentSets = Tables.agentSets;

        final Cursor cursor = db.rawQuery(" SELECT J1." + agentSets.getColumnName(agentSets.getAgentColumnIndex()) +
                        " FROM " + bunchAcceptations.getName() + " AS J0" +
                        " JOIN " + agentSets.getName() + " AS J1 ON J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAgentSetColumnIndex()) + "=J1." + agentSets.getColumnName(agentSets.getSetIdColumnIndex()) +
                        " WHERE J0." + bunchAcceptations.getColumnName(bunchAcceptations.getAcceptationColumnIndex()) + "=?",
                new String[] { Integer.toString(staticAcceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    ArrayList<Integer> result = new ArrayList<>();
                    do {
                        final int id = cursor.getInt(0);
                        if (id != agents.nullReference()) {
                            result.add(id);
                        }
                    } while (cursor.moveToNext());

                    int[] intResult = new int[result.size()];
                    int index = 0;
                    for (int value : result) {
                        intResult[index++] = value;
                    }

                    return intResult;
                }
            }
            finally {
                cursor.close();
            }
        }

        return new int[0];
    }

    private InvolvedAgentResult[] readInvolvedAgents(SQLiteDatabase db, int staticAcceptation) {
        final SparseIntArray flags = new SparseIntArray();

        for (int agentId : readAgentsWhereAccIsTarget(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId) | InvolvedAgentResult.Flags.target);
        }

        for (int agentId : readAgentsWhereAccIsSource(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId) | InvolvedAgentResult.Flags.source);
        }

        // TODO: Diff not implemented as right now it is impossible

        for (int agentId : readAgentsWhereAccIsRule(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId) | InvolvedAgentResult.Flags.rule);
        }

        for (int agentId : readAgentsWhereAccIsProcessed(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId) | InvolvedAgentResult.Flags.processed);
        }

        final int count = flags.size();
        final InvolvedAgentResult[] result = new InvolvedAgentResult[count];
        for (int i = 0; i < count; i++) {
            result[i] = new InvolvedAgentResult(flags.keyAt(i), flags.valueAt(i));
        }

        return result;
    }

    private class CorrelationSpan extends ClickableSpan {

        final int id;
        final int start;
        final int end;

        CorrelationSpan(int id, int start, int end) {
            if (start < 0 || end < start) {
                throw new IllegalArgumentException();
            }

            this.id = id;
            this.start = start;
            this.end = end;
        }

        @Override
        public void onClick(View view) {
            CorrelationDetailsActivity.open(AcceptationDetailsActivity.this, id);
        }
    }

    static void composeCorrelation(SparseArray<String> correlation, StringBuilder sb) {
        final int correlationSize = correlation.size();
        for (int i = 0; i < correlationSize; i++) {
            if (i != 0) {
                sb.append('/');
            }

            sb.append(correlation.valueAt(i));
        }
    }

    private AcceptationDetailsAdapter.Item[] getAdapterItems(int staticAcceptation) {
        SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();

        final ArrayList<AcceptationDetailsAdapter.Item> result = new ArrayList<>();
        result.add(new HeaderItem("Displaying details for acceptation " + staticAcceptation));

        final StringBuilder sb = new StringBuilder("Correlation: ");
        List<CorrelationHolder> correlationArray = readCorrelationArray(db, staticAcceptation);
        List<CorrelationSpan> correlationSpans = new ArrayList<>();
        for (int i = 0; i < correlationArray.size(); i++) {
            if (i != 0) {
                sb.append(" - ");
            }

            final CorrelationHolder holder = correlationArray.get(i);
            final SparseArray<String> correlation = holder.texts;
            final int correlationSize = correlation.size();
            int startIndex = -1;
            if (correlationSize > 1) {
                startIndex = sb.length();
            }

            composeCorrelation(correlation, sb);

            if (startIndex >= 0) {
                correlationSpans.add(new CorrelationSpan(holder.id, startIndex, sb.length()));
            }
        }

        SpannableString spannableCorrelations = new SpannableString(sb.toString());
        for (CorrelationSpan span : correlationSpans) {
            spannableCorrelations.setSpan(span, span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        result.add(new NonNavigableItem(spannableCorrelations));

        final LanguageResult languageResult = readLanguageFromAlphabet(db, correlationArray.get(0).texts.keyAt(0));
        result.add(new NonNavigableItem("Language: " + languageResult.text));

        final SparseArray<String> languageStrs = new SparseArray<>();
        languageStrs.put(languageResult.language, languageResult.text);

        final AcceptationResult definition = readDefinition(db, staticAcceptation);
        if (definition != null) {
            result.add(new AcceptationNavigableItem(definition.acceptation, "Type of: " + definition.text, false));
        }

        boolean subTypeFound = false;
        for (AcceptationResult subType : readSubTypes(db, staticAcceptation, languageResult.language)) {
            if (!subTypeFound) {
                result.add(new HeaderItem("Subtypes"));
                subTypeFound = true;
            }

            result.add(new AcceptationNavigableItem(subType.acceptation, subType.text, false));
        }

        final SynonymTranslationResult[] synonymTranslationResults = readSynonymsAndTranslations(db, staticAcceptation);
        boolean synonymFound = false;
        for (SynonymTranslationResult r : synonymTranslationResults) {
            if (r.language == languageResult.language) {
                if (!synonymFound) {
                    result.add(new HeaderItem("Synonyms"));
                    synonymFound = true;
                }

                result.add(new AcceptationNavigableItem(r.acceptation, r.text, false));
            }
        }

        boolean translationFound = false;
        for (SynonymTranslationResult r : synonymTranslationResults) {
            final int language = r.language;
            if (language != languageResult.language) {
                if (!translationFound) {
                    result.add(new HeaderItem("Translations"));
                    translationFound = true;
                }

                String langStr = languageStrs.get(language);
                if (langStr == null) {
                    langStr = readConceptText(db, language);
                    languageStrs.put(language, langStr);
                }

                result.add(new AcceptationNavigableItem(r.acceptation, "" + langStr + " -> " + r.text, false));
            }
        }

        boolean parentBunchFound = false;
        for (BunchInclusionResult r : readBunchesWhereIncluded(db, staticAcceptation)) {
            if (!parentBunchFound) {
                result.add(new HeaderItem("Bunches where included"));
                parentBunchFound = true;
            }

            result.add(new AcceptationNavigableItem(r.acceptation, r.text, r.dynamic));
        }

        boolean morphologyFound = false;
        MorphologyResult[] morphologyResults = readMorphologies(db, staticAcceptation);
        for (MorphologyResult r : morphologyResults) {
            if (!morphologyFound) {
                result.add(new HeaderItem("Morphologies"));
                morphologyFound = true;
            }

            result.add(new RuleNavigableItem(r.dynamicAcceptation, r.ruleText + " -> " + r.text));
        }

        boolean bunchChildFound = false;
        for (BunchChildResult r : readBunchChildren(db, staticAcceptation)) {
            if (!bunchChildFound) {
                result.add(new HeaderItem("Acceptations included in this bunch"));
                bunchChildFound = true;
                _shouldShowBunchChildrenQuizMenuOption = true;
            }

            result.add(new AcceptationNavigableItem(r.acceptation, r.text, r.dynamic));
        }

        boolean agentFound = false;
        for (InvolvedAgentResult r : readInvolvedAgents(db, staticAcceptation)) {
            if (!agentFound) {
                result.add(new HeaderItem("Involved agents"));
                agentFound = true;
            }

            final StringBuilder s = new StringBuilder("Agent #");
            s.append(r.agentId).append(" (");
            s.append(((r.flags & InvolvedAgentResult.Flags.target) != 0)? 'T' : '-');
            s.append(((r.flags & InvolvedAgentResult.Flags.source) != 0)? 'S' : '-');
            s.append(((r.flags & InvolvedAgentResult.Flags.diff) != 0)? 'D' : '-');
            s.append(((r.flags & InvolvedAgentResult.Flags.rule) != 0)? 'R' : '-');
            s.append(((r.flags & InvolvedAgentResult.Flags.processed) != 0)? 'P' : '-');
            s.append(')');

            result.add(new AgentNavigableItem(r.agentId, s.toString()));
        }

        for (MorphologyResult r : morphologyResults) {
            if (!agentFound) {
                result.add(new HeaderItem("Involved agents"));
                agentFound = true;
            }

            final StringBuilder s = new StringBuilder("Agent #");
            s.append(r.agent).append(" (").append(r.ruleText).append(')');
            result.add(new AgentNavigableItem(r.agent, s.toString()));
        }

        return result.toArray(new AcceptationDetailsAdapter.Item[result.size()]);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceptation_details_activity);

        if (!getIntent().hasExtra(BundleKeys.STATIC_ACCEPTATION)) {
            throw new IllegalArgumentException("staticAcceptation not provided");
        }

        final int staticAcceptation = getIntent().getIntExtra(BundleKeys.STATIC_ACCEPTATION, 0);
        _concept = readConcept(DbManager.getInstance().getReadableDatabase(), staticAcceptation);
        if (_concept != 0) {
            _listAdapter = new AcceptationDetailsAdapter(getAdapterItems(staticAcceptation));

            ListView listView = findViewById(R.id.listView);
            listView.setAdapter(_listAdapter);
            listView.setOnItemClickListener(this);
        }
        else {
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        _listAdapter.getItem(position).navigate(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (_shouldShowBunchChildrenQuizMenuOption) {
            new MenuInflater(this).inflate(R.menu.acceptation_details_activity_bunch_children_quiz, menu);
            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemBunchChildrenQuiz:
                QuizSelectorActivity.open(this, _concept);
                return true;
        }

        return false;
    }
}
