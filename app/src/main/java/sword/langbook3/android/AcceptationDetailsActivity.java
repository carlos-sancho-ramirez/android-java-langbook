package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.langbook3.android.AcceptationDetailsActivityState.IntrinsicStates;
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
import sword.langbook3.android.LangbookDbSchema.LanguagesTable;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.RuledAcceptationsTable;
import sword.langbook3.android.LangbookDbSchema.StringQueriesTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.LangbookReadableDatabase.BunchInclusionResult;
import sword.langbook3.android.LangbookReadableDatabase.IdentifiableResult;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbExporter;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbUpdateQuery;

import static sword.langbook3.android.LangbookDatabase.addAcceptationInBunch;
import static sword.langbook3.android.LangbookDatabase.removeAcceptationFromBunch;
import static sword.langbook3.android.LangbookDbInserter.insertBunchConcept;
import static sword.langbook3.android.LangbookDeleter.deleteBunchConceptForConcept;
import static sword.langbook3.android.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.getAcceptationCorrelations;
import static sword.langbook3.android.LangbookReadableDatabase.readAcceptationText;
import static sword.langbook3.android.LangbookReadableDatabase.readBunchesWhereAcceptationIsIncluded;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;
import static sword.langbook3.android.LangbookReadableDatabase.readLanguageFromAlphabet;
import static sword.langbook3.android.LangbookReadableDatabase.readSubtypesFromAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.readSupertypeFromAcceptation;
import static sword.langbook3.android.db.DbIdColumn.idColumnName;

public final class AcceptationDetailsActivity extends Activity implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_EDIT = 1;
    private static final int REQUEST_CODE_LINKED_ACCEPTATION = 2;
    private static final int REQUEST_CODE_PICK_ACCEPTATION = 3;
    private static final int REQUEST_CODE_PICK_BUNCH = 4;
    private static final int REQUEST_CODE_PICK_SUPERTYPE = 5;

    private interface ArgKeys {
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
        String DYNAMIC_ACCEPTATION = BundleKeys.DYNAMIC_ACCEPTATION;
        String CONFIRM_ONLY = BundleKeys.CONFIRM_ONLY;
    }

    private interface SavedKeys {
        String STATE = "cSt";
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private int _preferredAlphabet;
    private int _staticAcceptation;
    private int _concept;
    private boolean _confirmOnly;

    private IdentifiableResult _definition;

    private AcceptationDetailsActivityState _state;

    private boolean _shouldShowBunchChildrenQuizMenuOption;
    private ListView _listView;
    private AcceptationDetailsAdapter _listAdapter;

    public static void open(Context context, int staticAcceptation, int dynamicAcceptation) {
        Intent intent = new Intent(context, AcceptationDetailsActivity.class);
        intent.putExtra(ArgKeys.STATIC_ACCEPTATION, staticAcceptation);
        intent.putExtra(ArgKeys.DYNAMIC_ACCEPTATION, dynamicAcceptation);
        context.startActivity(intent);
    }

    public static void open(Activity activity, int requestCode, int staticAcceptation, int dynamicAcceptation, boolean confirmOnly) {
        Intent intent = new Intent(activity, AcceptationDetailsActivity.class);
        intent.putExtra(ArgKeys.STATIC_ACCEPTATION, staticAcceptation);
        intent.putExtra(ArgKeys.DYNAMIC_ACCEPTATION, dynamicAcceptation);

        if (confirmOnly) {
            intent.putExtra(ArgKeys.CONFIRM_ONLY, true);
        }

        activity.startActivityForResult(intent, requestCode);
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
                        " J1." + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name() +
                        ",J2." + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() +
                        ",J2." + strings.columns().get(strings.getStringColumnIndex()).name() +
                        ",J1." + bunchAcceptations.columns().get(bunchAcceptations.getAgentSetColumnIndex()).name() +
                " FROM " + acceptations.name() + " AS J0" +
                        " JOIN " + bunchAcceptations.name() + " AS J1 ON J0." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() + "=J1." + bunchAcceptations.columns().get(bunchAcceptations.getBunchColumnIndex()).name() +
                        " JOIN " + strings.name() + " AS J2 ON J1." + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name() + "=J2." + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() +
                " WHERE J0." + idColumnName + "=?" +
                " ORDER BY J1." + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name(),
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
                            if (alphabet != _preferredAlphabet && cursor.getInt(2) == _preferredAlphabet) {
                                alphabet = _preferredAlphabet;
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

    private ImmutableList<SynonymTranslationResult> readSynonymsAndTranslations(DbExporter.Database db, int acceptation) {
        final AcceptationsTable acceptations = Tables.acceptations;
        final AlphabetsTable alphabets = Tables.alphabets;
        final StringQueriesTable strings = Tables.stringQueries;
        final LanguagesTable languages = Tables.languages;

        final int accOffset = acceptations.columns().size();
        final int stringsOffset = accOffset * 2;
        final int alphabetsOffset = stringsOffset + strings.columns().size();
        final int languagesOffset = alphabetsOffset + alphabets.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, acceptations.columns().size() + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, stringsOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .join(languages, alphabetsOffset + alphabets.getLanguageColumnIndex(), languages.getIdColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .whereColumnValueMatch(alphabetsOffset + alphabets.getIdColumnIndex(), languagesOffset + languages.getMainAlphabetColumnIndex())
                .select(accOffset + acceptations.getIdColumnIndex(),
                        languagesOffset + languages.getIdColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex());

        final DbResult result = db.select(query);
        final ImmutableList.Builder<SynonymTranslationResult> builder = new ImmutableList.Builder<>(result.getRemainingRows());
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int accId = row.get(0).toInt();
                if (accId != acceptation) {
                    builder.add(new SynonymTranslationResult(accId,
                            row.get(1).toInt(), row.get(2).toText()));
                }
            }
        } finally {
            result.close();
        }

        return builder.build();
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
                        ",J3." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() +
                        ",J1." + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() +
                        ",J1." + strings.columns().get(strings.getStringColumnIndex()).name() +
                        ",J4." + strings.columns().get(strings.getStringAlphabetColumnIndex()).name() +
                        ",J4." + strings.columns().get(strings.getStringColumnIndex()).name() +
                        ",J2." + idColumnName +
                " FROM " + ruledAcceptations.name() + " AS J0" +
                        " JOIN " + strings.name() + " AS J1 ON J0." + idColumnName + "=J1." + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() +
                        " JOIN " + agents.name() + " AS J2 ON J0." + ruledAcceptations.columns().get(ruledAcceptations.getAgentColumnIndex()).name() + "=J2." + idColumnName +
                        " JOIN " + acceptations.name() + " AS J3 ON J2." + agents.columns().get(agents.getRuleColumnIndex()).name() + "=J3." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() +
                        " JOIN " + strings.name() + " AS J4 ON J3." + idColumnName + "=J4." + strings.columns().get(strings.getDynamicAcceptationColumnIndex()).name() +
                " WHERE J0." + ruledAcceptations.columns().get(ruledAcceptations.getAcceptationColumnIndex()).name() + "=?" +
                        " AND J1." + strings.columns().get(strings.getMainAcceptationColumnIndex()).name() + "=?" +
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
                            if (alphabet != _preferredAlphabet && cursor.getInt(2) == _preferredAlphabet) {
                                alphabet = _preferredAlphabet;
                                text = cursor.getString(3);
                            }

                            if (ruleAlphabet != _preferredAlphabet && cursor.getInt(4) == _preferredAlphabet) {
                                ruleAlphabet = _preferredAlphabet;
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
                " FROM " + acceptations.name() + " AS J0" +
                " JOIN " + agents.name() + " AS J1 ON J0." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() + "=J1." + agents.columns().get(agents.getTargetBunchColumnIndex()).name() +
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
                " FROM " + acceptations.name() + " AS J0" +
                " JOIN " + bunchSets.name() + " AS J1 ON J0." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() + "=J1." + bunchSets.columns().get(bunchSets.getBunchColumnIndex()).name() +
                " JOIN " + agents.name() + " AS J2 ON J1." + bunchSets.columns().get(bunchSets.getSetIdColumnIndex()).name() + "=J2." + agents.columns().get(agents.getSourceBunchSetColumnIndex()).name() +
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
                        " FROM " + acceptations.name() + " AS J0" +
                        " JOIN " + agents.name() + " AS J1 ON J0." + acceptations.columns().get(acceptations.getConceptColumnIndex()).name() + "=J1." + agents.columns().get(agents.getRuleColumnIndex()).name() +
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

        final Cursor cursor = db.rawQuery(" SELECT J1." + agentSets.columns().get(agentSets.getAgentColumnIndex()).name() +
                        " FROM " + bunchAcceptations.name() + " AS J0" +
                        " JOIN " + agentSets.name() + " AS J1 ON J0." + bunchAcceptations.columns().get(bunchAcceptations.getAgentSetColumnIndex()).name() + "=J1." + agentSets.columns().get(agentSets.getSetIdColumnIndex()).name() +
                        " WHERE J0." + bunchAcceptations.columns().get(bunchAcceptations.getAcceptationColumnIndex()).name() + "=?",
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
        final MutableIntPairMap flags = MutableIntPairMap.empty();

        for (int agentId : readAgentsWhereAccIsTarget(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResult.Flags.target);
        }

        for (int agentId : readAgentsWhereAccIsSource(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResult.Flags.source);
        }

        // TODO: Diff not implemented as right now it is impossible

        for (int agentId : readAgentsWhereAccIsRule(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResult.Flags.rule);
        }

        for (int agentId : readAgentsWhereAccIsProcessed(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResult.Flags.processed);
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

    static void composeCorrelation(ImmutableIntKeyMap<String> correlation, StringBuilder sb) {
        final int correlationSize = correlation.size();
        for (int i = 0; i < correlationSize; i++) {
            if (i != 0) {
                sb.append('/');
            }

            sb.append(correlation.valueAt(i));
        }
    }

    private AcceptationDetailsAdapter.Item[] getAdapterItems(int staticAcceptation) {
        final DbManager dbManager = DbManager.getInstance();
        final DbExporter.Database db = dbManager.getDatabase();
        SQLiteDatabase sqliteDb = DbManager.getInstance().getReadableDatabase();

        final ArrayList<AcceptationDetailsAdapter.Item> result = new ArrayList<>();
        result.add(new HeaderItem("Displaying details for acceptation " + staticAcceptation));

        final StringBuilder sb = new StringBuilder("Correlation: ");
        ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntKeyMap<String>>> correlationResultPair = getAcceptationCorrelations(db, staticAcceptation);
        ImmutableList.Builder<CorrelationSpan> correlationSpansBuilder = new ImmutableList.Builder<>();
        final int correlationArrayLength = correlationResultPair.left.size();
        for (int i = 0; i < correlationArrayLength; i++) {
            if (i != 0) {
                sb.append(" - ");
            }

            final int correlationId = correlationResultPair.left.get(i);
            final ImmutableIntKeyMap<String> correlation = correlationResultPair.right.get(correlationId);
            final int correlationSize = correlation.size();
            int startIndex = -1;
            if (correlationSize > 1) {
                startIndex = sb.length();
            }

            composeCorrelation(correlation, sb);

            if (startIndex >= 0) {
                correlationSpansBuilder.add(new CorrelationSpan(correlationId, startIndex, sb.length()));
            }
        }

        SpannableString spannableCorrelations = new SpannableString(sb.toString());
        for (CorrelationSpan span : correlationSpansBuilder.build()) {
            spannableCorrelations.setSpan(span, span.start, span.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        result.add(new NonNavigableItem(spannableCorrelations));

        final int givenAlphabet = correlationResultPair.right.get(correlationResultPair.left.get(0)).keyAt(0);
        final IdentifiableResult languageResult = readLanguageFromAlphabet(db, givenAlphabet, _preferredAlphabet);
        result.add(new NonNavigableItem("Language: " + languageResult.text));

        final MutableIntKeyMap<String> languageStrs = MutableIntKeyMap.empty();
        languageStrs.put(languageResult.id, languageResult.text);

        _definition = readSupertypeFromAcceptation(db, staticAcceptation, _preferredAlphabet);
        if (_definition != null) {
            result.add(new AcceptationNavigableItem(_definition.id, "Type of: " + _definition.text, false));
        }

        boolean subTypeFound = false;
        for (ImmutableIntKeyMap.Entry<String> subtype : readSubtypesFromAcceptation(db, staticAcceptation, _preferredAlphabet).entries()) {
            if (!subTypeFound) {
                result.add(new HeaderItem("Subtypes"));
                subTypeFound = true;
            }

            result.add(new AcceptationNavigableItem(subtype.key(), subtype.value(), false));
        }

        final ImmutableList<SynonymTranslationResult> synonymTranslationResults =
                readSynonymsAndTranslations(DbManager.getInstance().getDatabase(), staticAcceptation);
        boolean synonymFound = false;
        for (SynonymTranslationResult r : synonymTranslationResults) {
            if (r.language == languageResult.id) {
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
            if (language != languageResult.id) {
                if (!translationFound) {
                    result.add(new HeaderItem("Translations"));
                    translationFound = true;
                }

                String langStr = languageStrs.get(language, null);
                if (langStr == null) {
                    langStr = readConceptText(DbManager.getInstance().getDatabase(), language, _preferredAlphabet);
                    languageStrs.put(language, langStr);
                }

                result.add(new AcceptationNavigableItem(r.acceptation, "" + langStr + " -> " + r.text, false));
            }
        }

        boolean parentBunchFound = false;
        for (BunchInclusionResult r : readBunchesWhereAcceptationIsIncluded(db, staticAcceptation, _preferredAlphabet)) {
            if (!parentBunchFound) {
                result.add(new HeaderItem("Bunches where included"));
                parentBunchFound = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED,
                    r.acceptation, r.text, r.dynamic));
        }

        boolean morphologyFound = false;
        MorphologyResult[] morphologyResults = readMorphologies(sqliteDb, staticAcceptation);
        for (MorphologyResult r : morphologyResults) {
            if (!morphologyFound) {
                result.add(new HeaderItem("Morphologies"));
                morphologyFound = true;
            }

            result.add(new RuleNavigableItem(r.dynamicAcceptation, r.ruleText + " -> " + r.text));
        }

        boolean bunchChildFound = false;
        for (BunchChildResult r : readBunchChildren(sqliteDb, staticAcceptation)) {
            if (!bunchChildFound) {
                result.add(new HeaderItem("Acceptations included in this bunch"));
                bunchChildFound = true;
                _shouldShowBunchChildrenQuizMenuOption = true;
            }

            result.add(new AcceptationNavigableItem(AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED, r.acceptation, r.text, r.dynamic));
        }

        boolean agentFound = false;
        for (InvolvedAgentResult r : readInvolvedAgents(sqliteDb, staticAcceptation)) {
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

    private void updateAdapter() {
        _listAdapter = new AcceptationDetailsAdapter(getAdapterItems(_staticAcceptation));
        _listView.setAdapter(_listAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceptation_details_activity);

        if (!getIntent().hasExtra(ArgKeys.STATIC_ACCEPTATION)) {
            throw new IllegalArgumentException("staticAcceptation not provided");
        }

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        if (_state == null) {
            _state = new AcceptationDetailsActivityState();
        }

        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        _staticAcceptation = getIntent().getIntExtra(ArgKeys.STATIC_ACCEPTATION, 0);
        _confirmOnly = getIntent().getBooleanExtra(ArgKeys.CONFIRM_ONLY, false);
        setTitle(readAcceptationText(DbManager.getInstance().getDatabase(), _staticAcceptation, _preferredAlphabet));

        _concept = conceptFromAcceptation(DbManager.getInstance().getDatabase(), _staticAcceptation);
        _listView = findViewById(R.id.listView);

        if (_concept != 0) {
            updateAdapter();
            if (!_confirmOnly) {
                _listView.setOnItemClickListener(this);
                _listView.setOnItemLongClickListener(this);
            }

            switch (_state.getIntrinsicState()) {
                case IntrinsicStates.DELETE_ACCEPTATION:
                    showDeleteConfirmationDialog();
                    break;

                case IntrinsicStates.DELETE_SUPERTYPE:
                    showDeleteSupertypeConfirmationDialog();
                    break;

                case IntrinsicStates.DELETING_FROM_BUNCH:
                    showDeleteFromBunchConfirmationDialog();
                    break;

                case IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                    showDeleteAcceptationFromBunchConfirmationDialog();
                    break;

                case IntrinsicStates.LINKING_CONCEPT:
                    showLinkModeSelectorDialog();
                    break;
            }
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
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // For now, as the only valuable action per item is 'delete', no
        // contextual menu is displayed and confirmation dialog is prompted directly.
        //
        // This routine may be valuable while there is only one enabled item.
        // For multiple, the action mode for the list view should be enabled
        // instead to apply the same action to multiple items at the same time.
        final AcceptationDetailsAdapter.Item item = _listAdapter.getItem(position);
        if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.BUNCH_WHERE_INCLUDED) {
            AcceptationNavigableItem it = (AcceptationNavigableItem) item;
            if (!it.isDynamic()) {
                final int bunch = conceptFromAcceptation(DbManager.getInstance().getDatabase(), it.getId());
                _state.setDeleteBunchTarget(new DisplayableItem(bunch, it.getText().toString()));
                showDeleteFromBunchConfirmationDialog();
            }
            return true;
        }
        else if (item.getItemType() == AcceptationDetailsAdapter.ItemTypes.ACCEPTATION_INCLUDED) {
            AcceptationNavigableItem it = (AcceptationNavigableItem) item;
            if (!it.isDynamic()) {
                _state.setDeleteAcceptationFromBunch(new DisplayableItem(it.getId(), it.getText().toString()));
                showDeleteAcceptationFromBunchConfirmationDialog();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = new MenuInflater(this);

        if (_confirmOnly) {
            inflater.inflate(R.menu.acceptation_details_activity_confirm, menu);
        }
        else {
            inflater.inflate(R.menu.acceptation_details_activity_edit, menu);

            if (_shouldShowBunchChildrenQuizMenuOption) {
                inflater.inflate(R.menu.acceptation_details_activity_bunch_children_quiz, menu);
            }

            inflater.inflate(R.menu.acceptation_details_activity_link_options, menu);

            if (_definition == null) {
                inflater.inflate(R.menu.acceptation_details_activity_include_supertype, menu);
            }
            else {
                inflater.inflate(R.menu.acceptation_details_activity_delete_supertype, menu);
            }

            inflater.inflate(R.menu.acceptation_details_activity_delete_acceptation, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemBunchChildrenQuiz:
                QuizSelectorActivity.open(this, _concept);
                return true;

            case R.id.menuItemEdit:
                WordEditorActivity.open(this, REQUEST_CODE_EDIT, _staticAcceptation);
                return true;

            case R.id.menuItemLinkConcept:
                AcceptationPickerActivity.open(this, REQUEST_CODE_LINKED_ACCEPTATION, _concept);
                return true;

            case R.id.menuItemIncludeAcceptation:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_ACCEPTATION);
                return true;

            case R.id.menuItemIncludeInBunch:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_BUNCH);
                return true;

            case R.id.menuItemDeleteAcceptation:
                _state.setDeletingAcceptation();
                showDeleteConfirmationDialog();
                return true;

            case R.id.menuItemIncludeSupertype:
                AcceptationPickerActivity.open(this, REQUEST_CODE_PICK_SUPERTYPE);
                return true;

            case R.id.menuItemDeleteSupertype:
                _state.setDeletingSupertype();
                showDeleteSupertypeConfirmationDialog();
                return true;

            case R.id.menuItemConfirm:
                final Intent intent = new Intent();
                intent.putExtra(ResultKeys.ACCEPTATION, _staticAcceptation);
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
        }

        return false;
    }

    private void showDeleteFromBunchConfirmationDialog() {
        final String message = getString(R.string.deleteFromBunchConfirmationText, _state.getDeleteTarget().text);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteTarget())
                .create().show();
    }

    private void showDeleteAcceptationFromBunchConfirmationDialog() {
        final String message = getString(R.string.deleteAcceptationFromBunchConfirmationText, _state.getDeleteTarget().text);
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeleteTarget())
                .create().show();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteAcceptationConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeletingAcceptation())
                .create().show();
    }

    private void showDeleteSupertypeConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.deleteSupertypeConfirmationText)
                .setPositiveButton(R.string.menuItemDelete, this)
                .setOnCancelListener(dialog -> _state.clearDeletingSupertype())
                .create().show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (_state.getIntrinsicState()) {
            case IntrinsicStates.DELETE_ACCEPTATION:
                deleteAcceptation();
                break;

            case IntrinsicStates.DELETE_SUPERTYPE:
                _state.clearDeletingSupertype();
                if (!deleteBunchConceptForConcept(DbManager.getInstance().getDatabase(), _concept)) {
                    throw new AssertionError();
                }
                updateAdapter();
                showFeedback(getString(R.string.deleteSupertypeFeedback));
                invalidateOptionsMenu();
                break;

            case IntrinsicStates.LINKING_CONCEPT:
                if (_state.getDialogCheckedOption() == 0) {
                    // Sharing concept
                    showFeedback(shareConcept()? "Concept shared" : "Unable to shared concept");
                }
                else {
                    // Duplicate concepts
                    duplicateAcceptationWithThisConcept();
                    showFeedback("Acceptation linked");
                }
                _state.clearLinkedAcceptation();
                updateAdapter();
                break;

            case IntrinsicStates.DELETING_FROM_BUNCH:
                final DisplayableItem item = _state.getDeleteTarget();
                final int bunch = item.id;
                final String bunchText = item.text;
                _state.clearDeleteTarget();

                if (!removeAcceptationFromBunch(DbManager.getInstance().getDatabase(), bunch, _staticAcceptation)) {
                    throw new AssertionError();
                }

                updateAdapter();
                showFeedback(getString(R.string.deleteFromBunchFeedback, bunchText));
                break;

            case IntrinsicStates.DELETING_ACCEPTATION_FROM_BUNCH:
                final DisplayableItem itemToDelete = _state.getDeleteTarget();
                final int accIdToDelete = itemToDelete.id;
                final String accToDeleteText = itemToDelete.text;
                _state.clearDeleteTarget();

                if (!removeAcceptationFromBunch(DbManager.getInstance().getDatabase(), _concept, accIdToDelete)) {
                    throw new AssertionError();
                }

                updateAdapter();
                showFeedback(getString(R.string.deleteAcceptationFromBunchFeedback, accToDeleteText));
                break;

            default:
                throw new AssertionError("Unable to handle state " + _state.getIntrinsicState());
        }
    }

    private void deleteAcceptation() {
        if (!LangbookDatabase.removeAcceptation(DbManager.getInstance().getDatabase(), _staticAcceptation)) {
            throw new AssertionError();
        }

        showFeedback(getString(R.string.deleteAcceptationFeedback));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_LINKED_ACCEPTATION) {
                final boolean usedConcept = data
                        .getBooleanExtra(AcceptationPickerActivity.ResultKeys.CONCEPT_USED, false);
                if (!usedConcept) {
                    _state.setLinkedAcceptation(data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0));
                    showLinkModeSelectorDialog();
                }
                else {
                    updateAdapter();
                }
            }
            else if (requestCode == REQUEST_CODE_PICK_ACCEPTATION) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                final Database db = DbManager.getInstance().getDatabase();
                final int message = addAcceptationInBunch(db, _concept, pickedAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                updateAdapter();
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_BUNCH) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                final Database db = DbManager.getInstance().getDatabase();
                final int pickedBunch = (pickedAcceptation != 0)? conceptFromAcceptation(db, pickedAcceptation) : 0;
                final int message = addAcceptationInBunch(db, pickedBunch, _staticAcceptation)? R.string.includeInBunchOk : R.string.includeInBunchKo;
                updateAdapter();
                showFeedback(getString(message));
            }
            else if (requestCode == REQUEST_CODE_PICK_SUPERTYPE) {
                final int pickedAcceptation = data.getIntExtra(AcceptationPickerActivity.ResultKeys.ACCEPTATION, 0);
                final Database db = DbManager.getInstance().getDatabase();
                final int pickedConcept = (pickedAcceptation != 0)? conceptFromAcceptation(db, pickedAcceptation) : 0;
                insertBunchConcept(db, pickedConcept, _concept);
                showFeedback(getString(R.string.includeSupertypeOk));
                updateAdapter();
                invalidateOptionsMenu();
            }
            else if (requestCode == REQUEST_CODE_EDIT) {
                updateAdapter();
            }
        }
    }

    private void showLinkModeSelectorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.linkDialogTitle)
                .setPositiveButton(R.string.linkDialogButton, this)
                .setSingleChoiceItems(R.array.linkDialogOptions, 0, this::onLinkDialogChoiceChecked)
                .setOnCancelListener(dialog -> _state.clearLinkedAcceptation())
                .create().show();
    }

    private void onLinkDialogChoiceChecked(DialogInterface dialog, int which) {
        _state.setDialogCheckedOption(which);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    private int getLinkedConcept() {
        final AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), _state.getLinkedAcceptation())
                .select(table.getConceptColumnIndex());
        return DbManager.getInstance().selectSingleRow(query).get(0).toInt();
    }

    private ImmutableIntSet getImmutableConcepts() {
        final AlphabetsTable table = Tables.alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getLanguageColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            builder.add(row.get(0).toInt());
            builder.add(row.get(1).toInt());
        }

        return builder.build();
    }

    private static void updateBunchConceptConcepts(int oldConcept, int newConcept) {
        final BunchConceptsTable table = Tables.bunchConcepts;
        final Database db = DbManager.getInstance().getDatabase();

        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldConcept)
                .put(table.getBunchColumnIndex(), newConcept)
                .build();
        db.update(query);

        query = new DbUpdateQuery.Builder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        db.update(query);
    }

    private static void updateBunchAcceptationConcepts(int oldConcept, int newConcept) {
        final BunchAcceptationsTable table = Tables.bunchAcceptations;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldConcept)
                .put(table.getBunchColumnIndex(), newConcept)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateQuestionRules(int oldRule, int newRule) {
        final QuestionFieldSets table = Tables.questionFieldSets;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateQuizBunches(int oldBunch, int newBunch) {
        final QuizDefinitionsTable table = Tables.quizDefinitions;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateBunchSetBunches(int oldBunch, int newBunch) {
        final BunchSetsTable table = Tables.bunchSets;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getBunchColumnIndex(), oldBunch)
                .put(table.getBunchColumnIndex(), newBunch)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateAgentRules(int oldRule, int newRule) {
        final AgentsTable table = Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getRuleColumnIndex(), oldRule)
                .put(table.getRuleColumnIndex(), newRule)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateAgentTargetBunches(int oldBunch, int newBunch) {
        final AgentsTable table = Tables.agents;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getTargetBunchColumnIndex(), oldBunch)
                .put(table.getTargetBunchColumnIndex(), newBunch)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private static void updateAcceptationConcepts(int oldConcept, int newConcept) {
        final AcceptationsTable table = Tables.acceptations;
        DbUpdateQuery query = new DbUpdateQuery.Builder(table)
                .where(table.getConceptColumnIndex(), oldConcept)
                .put(table.getConceptColumnIndex(), newConcept)
                .build();
        DbManager.getInstance().getDatabase().update(query);
    }

    private boolean shareConcept() {
        final int linkedConcept = getLinkedConcept();

        final ImmutableIntSet immutableConcepts = getImmutableConcepts();
        if (immutableConcepts.contains(linkedConcept)) {
            return false;
        }

        final int oldConcept = _concept;
        if (oldConcept == 0 || linkedConcept == 0) {
            throw new AssertionError();
        }

        updateBunchConceptConcepts(oldConcept, linkedConcept);
        updateBunchAcceptationConcepts(oldConcept, linkedConcept);
        updateQuestionRules(oldConcept, linkedConcept);
        updateQuizBunches(oldConcept, linkedConcept);
        updateBunchSetBunches(oldConcept, linkedConcept);
        updateAgentRules(oldConcept, linkedConcept);
        updateAgentTargetBunches(oldConcept, linkedConcept);
        updateAcceptationConcepts(oldConcept, linkedConcept);
        return true;
    }

    private void duplicateAcceptationWithThisConcept() {
        final int concept = _concept;
        if (concept == 0) {
            throw new AssertionError();
        }

        final int linkedAcceptation = _state.getLinkedAcceptation();
        final AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), linkedAcceptation)
                .select(table.getCorrelationArrayColumnIndex());

        final Database db = DbManager.getInstance().getDatabase();
        final DbResult result = db.select(query);
        final DbResult.Row row = result.next();
        if (result.hasNext()) {
            throw new AssertionError();
        }

        final int correlationArray = row.get(0).toInt();

        final DbInsertQuery insertQuery = new DbInsertQuery.Builder(table)
                .put(table.getConceptColumnIndex(), _concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();

        final int newAccId = db.insert(insertQuery);

        final StringQueriesTable strings = Tables.stringQueries;
        final DbQuery stringsQuery = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), linkedAcceptation)
                .where(strings.getMainAcceptationColumnIndex(), linkedAcceptation)
                .select(strings.getStringAlphabetColumnIndex(), strings.getStringColumnIndex(), strings.getMainStringColumnIndex());

        final ImmutableIntList.Builder alphabetsBuilder = new ImmutableIntList.Builder();
        final ImmutableList.Builder<String> stringsBuilder = new ImmutableList.Builder<>();
        final ImmutableList.Builder<String> mainStringsBuilder = new ImmutableList.Builder<>();

        for (DbResult.Row r : DbManager.getInstance().attach(stringsQuery)) {
            alphabetsBuilder.add(r.get(0).toInt());
            stringsBuilder.add(r.get(1).toText());
            mainStringsBuilder.add(r.get(2).toText());
        }

        final ImmutableIntList alphabets = alphabetsBuilder.build();
        final ImmutableList<String> strs = stringsBuilder.build();
        final ImmutableList<String> mainStrs = mainStringsBuilder.build();
        final int length = alphabets.size();

        for (int i = 0; i < length; i++) {
            final DbInsertQuery iQuery = new DbInsertQuery.Builder(strings)
                    .put(strings.getDynamicAcceptationColumnIndex(), newAccId)
                    .put(strings.getMainAcceptationColumnIndex(), newAccId)
                    .put(strings.getStringAlphabetColumnIndex(), alphabets.valueAt(i))
                    .put(strings.getStringColumnIndex(), strs.valueAt(i))
                    .put(strings.getMainStringColumnIndex(), mainStrs.valueAt(i))
                    .build();

            if (db.insert(iQuery) == null) {
                throw new AssertionError();
            }
        }
    }

    private void showFeedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
