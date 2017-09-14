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

    private LanguageResult readLanguage(SQLiteDatabase db, int alphabet) {
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

        final LanguageResult languageResult = readLanguage(db, correlationArray.get(0).keyAt(0));
        sb.append("\n  * Language: ").append(languageResult.text);

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
            if (result.language != languageResult.language) {
                if (!translationFound) {
                    sb.append("\n  * Translations:");
                    translationFound = true;
                }

                sb.append("\n      ").append(result.language).append(" -> ").append(result.text);
            }
        }

        final TextView tv = findViewById(R.id.textView);
        tv.setText(sb.toString());
    }
}
