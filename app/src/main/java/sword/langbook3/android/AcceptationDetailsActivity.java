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

    private String readLanguage(SQLiteDatabase db, int language) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations; // J0
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J1." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J1." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + strings.getName() + " AS J1 ON J0." + idColumnName + "=J1." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=?",
                new String[] { Integer.toString(language) });

        String text = null;
        try {
            cursor.moveToFirst();
            int firstAlphabet = cursor.getInt(0);
            text = cursor.getString(1);
            while (firstAlphabet != preferredAlphabet && cursor.moveToNext()) {
                if (cursor.getInt(0) == preferredAlphabet) {
                    firstAlphabet = preferredAlphabet;
                    text = cursor.getString(1);
                }
            }
        }
        finally {
            cursor.close();
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
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations; // J0
        final DbManager.AlphabetsTable alphabets = DbManager.Tables.alphabets;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

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
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.BunchConceptsTable bunchConcepts = DbManager.Tables.bunchConcepts;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

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
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.BunchAcceptationsTable bunchAcceptations = DbManager.Tables.bunchAcceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

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
                    final int nullAgentSet = DbManager.Tables.agentSets.nullReference();
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
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.AlphabetsTable alphabets = DbManager.Tables.alphabets;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;
        final DbManager.LanguagesTable languages = DbManager.Tables.languages;

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

        final int dynamicAcceptation;
        final int rule;
        final String ruleText;
        final String text;

        MorphologyResult(int dynamicAcceptation, int rule, String ruleText, String text) {
            this.dynamicAcceptation = dynamicAcceptation;
            this.rule = rule;
            this.ruleText = ruleText;
            this.text = text;
        }
    }

    private MorphologyResult[] readMorphologies(SQLiteDatabase db, int acceptation) {
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;
        final DbManager.RuledConceptsTable ruledConcepts = DbManager.Tables.ruledConcepts;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J2." + idColumnName +
                        ",J4." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J5." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J5." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + acceptations.getName() + " AS J0" +
                        " JOIN " + ruledConcepts.getName() + " AS J1 ON J0." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) + "=J1." + ruledConcepts.getColumnName(ruledConcepts.getConceptColumnIndex()) +
                        " JOIN " + acceptations.getName() + " AS J2 ON J1." + idColumnName + "=J2." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " JOIN " + acceptations.getName() + " AS J4 ON J1." + ruledConcepts.getColumnName(ruledConcepts.getRuleColumnIndex()) + "=J4." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J5 ON J4." + idColumnName + "=J5." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + idColumnName + "=?" +
                        " AND J3." + strings.getColumnName(strings.getMainAcceptationColumnIndex()) + "=?" +
                " ORDER BY J2." + idColumnName,
                new String[] { Integer.toString(acceptation) , Integer.toString(acceptation)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    ArrayList<MorphologyResult> result = new ArrayList<>();

                    int acc = cursor.getInt(0);
                    int rule = cursor.getInt(1);
                    String text = cursor.getString(2);
                    int alphabet = cursor.getInt(3);
                    String ruleText = cursor.getString(4);

                    while (cursor.moveToNext()) {
                        if (cursor.getInt(0) == acc) {
                            if (alphabet != preferredAlphabet && cursor.getInt(3) == preferredAlphabet) {
                                alphabet = preferredAlphabet;
                                ruleText = cursor.getString(4);
                            }
                        }
                        else {
                            result.add(new MorphologyResult(acc, rule, ruleText, text));

                            acc = cursor.getInt(0);
                            rule = cursor.getInt(1);
                            text = cursor.getString(2);
                            alphabet = cursor.getInt(3);
                            ruleText = cursor.getString(4);
                        }
                    }

                    result.add(new MorphologyResult(acc, rule, ruleText, text));
                    return result.toArray(new MorphologyResult[result.size()]);
                }
            }
            finally {
                cursor.close();
            }
        }

        return new MorphologyResult[0];
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

        final LanguageResult languageResult = readLanguageFromAlphabet(db, correlationArray.get(0).keyAt(0));
        sb.append("\n  * Language: ").append(languageResult.text);

        final SparseArray<String> languageStrs = new SparseArray<>();
        languageStrs.put(languageResult.language, languageResult.text);

        final AcceptationResult definition = readDefinition(db, staticAcceptation);
        if (definition != null) {
            sb.append("\n  * Type of: ").append(definition.text);
        }

        final SynonymTranslationResult[] synonymTranslationResults = readSynonymsAndTranslations(db, staticAcceptation);
        boolean synonymFound = false;
        for (SynonymTranslationResult result : synonymTranslationResults) {
            if (result.language == languageResult.language) {
                if (!synonymFound) {
                    sb.append("\n  * Synonyms: ");
                    synonymFound = true;
                }
                else {
                    sb.append(", ");
                }

                sb.append(result.text);
            }
        }

        boolean translationFound = false;
        for (SynonymTranslationResult result : synonymTranslationResults) {
            final int language = result.language;
            if (language != languageResult.language) {
                if (!translationFound) {
                    sb.append("\n  * Translations:");
                    translationFound = true;
                }

                String langStr = languageStrs.get(language);
                if (langStr == null) {
                    langStr = readLanguage(db, language);
                    languageStrs.put(language, langStr);
                }

                sb.append("\n      ").append(langStr).append(" -> ").append(result.text);
            }
        }

        boolean parentBunchFound = false;
        for (BunchInclusionResult result : readBunchesWhereIncluded(db, staticAcceptation)) {
            if (!parentBunchFound) {
                sb.append("\n  * Bunches where included:");
                parentBunchFound = true;
            }

            sb.append("\n      ").append(result.text);

            if (result.dynamic) {
                sb.append(" *");
            }
        }

        boolean morphologyFound = false;
        for (MorphologyResult result : readMorphologies(db, staticAcceptation)) {
            if (!morphologyFound) {
                sb.append("\n  * Morphologies:");
                morphologyFound = true;
            }

            sb.append("\n      ").append(result.ruleText).append(" -> ").append(result.text);
        }

        final TextView tv = findViewById(R.id.textView);
        tv.setText(sb.toString());
    }
}
