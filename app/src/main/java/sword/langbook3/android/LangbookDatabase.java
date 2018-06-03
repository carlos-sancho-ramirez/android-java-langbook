package sword.langbook3.android;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbStringValue;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.LangbookDbInserter.insertAcceptation;
import static sword.langbook3.android.LangbookDbInserter.insertRuledAcceptation;
import static sword.langbook3.android.LangbookDbInserter.insertStringQuery;
import static sword.langbook3.android.LangbookDbInserter.insertSymbolArray;
import static sword.langbook3.android.LangbookDeleter.deleteAgentSet;
import static sword.langbook3.android.LangbookDeleter.deleteBunchAcceptationsForAgentSet;
import static sword.langbook3.android.LangbookDeleter.deleteRuledAcceptation;
import static sword.langbook3.android.LangbookDeleter.deleteStringQueriesForDynamicAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.conceptFromAcceptation;
import static sword.langbook3.android.LangbookReadableDatabase.findAgentSet;
import static sword.langbook3.android.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.LangbookReadableDatabase.findConversions;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.findSymbolArray;
import static sword.langbook3.android.LangbookReadableDatabase.getAllAgentSetsContaining;
import static sword.langbook3.android.LangbookReadableDatabase.getAllRuledAcceptationsForAgent;
import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelationArray;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelationWithText;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxAgentSetId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationArrayId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationId;
import static sword.langbook3.android.LangbookReadableDatabase.selectSingleRow;

public final class LangbookDatabase {

    private final Database db;
    private final LangbookReadableDatabase selector;
    private final LangbookDbInserter inserter;

    LangbookDatabase(Database db) {
        this.db = db;
        selector = new LangbookReadableDatabase(db);
        inserter = new LangbookDbInserter(db);
    }

    public static int obtainSymbolArray(DbImporter.Database db, String str) {
        Integer id = insertSymbolArray(db, str);
        if (id != null) {
            return id;
        }

        id = findSymbolArray(db, str);
        if (id == null) {
            throw new AssertionError("Unable to insert, and not present");
        }

        return id;
    }

    public static int insertCorrelation(DbImporter.Database db, IntPairMap correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }

        final int newCorrelationId = getMaxCorrelationId(db) + 1;
        LangbookDbInserter.insertCorrelation(db, newCorrelationId, correlation);
        return newCorrelationId;
    }

    public static int obtainCorrelation(DbImporter.Database db, IntPairMap correlation) {
        final Integer id = findCorrelation(db, correlation);
        return (id != null)? id : insertCorrelation(db, correlation);
    }

    public static int insertCorrelation(DbImporter.Database db, IntKeyMap<String> correlation) {
        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        for (IntKeyMap.Entry<String> entry : correlation.entries()) {
            builder.put(entry.key(), obtainSymbolArray(db, entry.value()));
        }

        return insertCorrelation(db, builder.build());
    }

    public static int insertCorrelationArray(DbImporter.Database db, int... correlation) {
        final int maxArrayId = getMaxCorrelationArrayId(db);
        final int newArrayId = maxArrayId + ((maxArrayId + 1 != StreamedDatabaseConstants.nullCorrelationArrayId)? 1 : 2);
        LangbookDbInserter.insertCorrelationArray(db, newArrayId, correlation);
        return newArrayId;
    }

    public static int insertCorrelationArray(DbImporter.Database db, IntList correlations) {
        final int[] values = new int[correlations.size()];
        int index = 0;
        for (int value : correlations) {
            values[index++] = value;
        }
        return insertCorrelationArray(db, values);
    }

    public static int insertBunchSet(DbImporter.Database db, IntSet bunchSet) {
        if (bunchSet.isEmpty()) {
            return 0;
        }

        final int setId = LangbookReadableDatabase.getMaxBunchSetId(db) + 1;
        LangbookDbInserter.insertBunchSet(db, setId, bunchSet);
        return setId;
    }

    public static int insertBunchSet(DbImporter.Database db, int... bunches) {
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (int bunch : bunches) {
            builder.add(bunch);
        }
        return insertBunchSet(db, builder.build());
    }

    public static int obtainBunchSet(DbImporter.Database db, IntSet bunchSet) {
        final Integer id = findBunchSet(db, bunchSet);
        return (id != null)? id : insertBunchSet(db, bunchSet);
    }

    public static int insertAgentSet(DbImporter.Database db, IntSet agentSet) {
        if (agentSet.isEmpty()) {
            return 0;
        }

        final int setId = getMaxAgentSetId(db) + 1;
        LangbookDbInserter.insertAgentSet(db, setId, agentSet);
        return setId;
    }

    public static int obtainAgentSet(DbImporter.Database db, IntSet set) {
        final Integer setId = findAgentSet(db, set);
        return (setId != null)? setId : insertAgentSet(db, set);
    }

    public static int insertRuledConcept(DbImporter.Database db, int rule, int concept) {
        final int ruledConcept = LangbookReadableDatabase.getMaxConceptInAcceptations(db) + 1;
        LangbookDbInserter.insertRuledConcept(db, ruledConcept, rule, concept);
        return ruledConcept;
    }

    public static int obtainRuledConcept(DbImporter.Database db, int rule, int concept) {
        final Integer id = LangbookReadableDatabase.findRuledConcept(db, rule, concept);
        if (id != null) {
            return id;
        }

        return insertRuledConcept(db, rule, concept);
    }

    /**
     * Apply the given conversion to the given text to generate a converted one.
     * @param pairs sorted set of pairs to be traversed in order to convert the <pre>text</pre> string.
     * @param text Text to be converted
     * @return The converted text, or null if text cannot be converted.
     */
    public static String convertText(ImmutableList<ImmutablePair<String, String>> pairs, String text) {
        String result = "";
        while (text.length() > 0) {
            boolean found = false;
            for (ImmutablePair<String, String> pair : pairs) {
                if (text.startsWith(pair.left)) {
                    result += pair.right;
                    text = text.substring(pair.left.length());
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return result;
    }

    private static void runAgent(Database db, int agentId, int targetBunch, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches, ImmutableIntKeyMap<String> matcher, ImmutableIntKeyMap<String> adder, int rule, boolean matchWordStarting) {
        final ImmutableIntSetBuilder diffAccBuilder = new ImmutableIntSetBuilder();
        for (int bunch : diffBunches) {
            final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
            final DbQuery query = new DbQuery.Builder(table)
                    .where(table.getBunchColumnIndex(), bunch)
                    .select(table.getAcceptationColumnIndex());
            try (DbResult result = db.select(query)) {
                while (result.hasNext()) {
                    diffAccBuilder.add(result.next().get(0).toInt());
                }
            }
        }
        final ImmutableIntSet diffAcceptations = diffAccBuilder.build();

        ImmutableIntSet matchingAcceptations = null;
        if (!sourceBunches.isEmpty()) {
            final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            for (int bunch : sourceBunches) {
                final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
                final DbQuery query = new DbQuery.Builder(table)
                        .where(table.getBunchColumnIndex(), bunch)
                        .select(table.getAcceptationColumnIndex());
                try (DbResult result = db.select(query)) {
                    while (result.hasNext()) {
                        final int acc = result.next().get(0).toInt();
                        if (!diffAcceptations.contains(acc)) {
                            builder.add(acc);
                        }
                    }
                }
            }

            matchingAcceptations = builder.build();
        }

        final LangbookDbSchema.StringQueriesTable strTable = LangbookDbSchema.Tables.stringQueries;
        for (IntKeyMap.Entry<String> entry : matcher.entries()) {
            final DbQuery matchQuery = new DbQuery.Builder(strTable)
                    .where(strTable.getStringAlphabetColumnIndex(), entry.key())
                    .where(strTable.getStringColumnIndex(), new DbQuery.Restriction(new DbStringValue(entry.value()),
                            matchWordStarting? DbQuery.RestrictionStringTypes.STARTS_WITH : DbQuery.RestrictionStringTypes.ENDS_WITH))
                    .select(strTable.getDynamicAcceptationColumnIndex());
            final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            try (DbResult result = db.select(matchQuery)) {
                while (result.hasNext()) {
                    final int acc = result.next().get(0).toInt();
                    if (matchingAcceptations == null && !diffAcceptations.contains(acc) ||
                            matchingAcceptations != null && matchingAcceptations.contains(acc)) {
                        builder.add(acc);
                    }
                }
            }
            matchingAcceptations = builder.build();
        }

        if (matchingAcceptations == null) {
            throw new AssertionError("Unable to select all acceptations from the database");
        }

        final ImmutableIntSet processedAcceptations;
        if (matcher.equals(adder)) {
            processedAcceptations = matchingAcceptations;
        }
        else {
            final MutableIntPairMap mainAlphabets = MutableIntPairMap.empty();
            final ImmutableIntSetBuilder processedAccBuilder = new ImmutableIntSetBuilder();
            for (int acc : matchingAcceptations) {
                final DbQuery query = new DbQuery.Builder(strTable)
                        .where(strTable.getDynamicAcceptationColumnIndex(), acc)
                        .select(
                                strTable.getStringAlphabetColumnIndex(),
                                strTable.getStringColumnIndex(),
                                strTable.getMainAcceptationColumnIndex(),
                                strTable.getMainStringColumnIndex());
                final MutableIntKeyMap<String> correlation = MutableIntKeyMap.empty();
                int mainAcc = 0;
                String mainString = null;
                boolean firstFound = false;
                try (DbResult result = db.select(query)) {
                    while (result.hasNext()) {
                        final DbResult.Row row = result.next();
                        final int alphabet = row.get(0).toInt();
                        final String text = row.get(1).toText();
                        correlation.put(alphabet, text);

                        if (firstFound) {
                            if (mainAcc != row.get(2).toInt() || !mainString.equals(row.get(3).toText())) {
                                throw new AssertionError();
                            }
                        }
                        else {
                            mainAcc = row.get(2).toInt();
                            mainString = row.get(3).toText();
                            firstFound = true;
                        }
                    }
                }

                for (IntKeyMap.Entry<String> entry : matcher.entries()) {
                    String text = correlation.get(entry.key());
                    final int length = entry.value().length();
                    if (matchWordStarting) {
                        text = text.substring(length);
                    }
                    else {
                        text = text.substring(0, text.length() - length);
                    }
                    correlation.put(entry.key(), text);
                }

                for (IntKeyMap.Entry<String> entry : adder.entries()) {
                    String text = correlation.get(entry.key());
                    if (matchWordStarting) {
                        text = entry.value() + text;
                    }
                    else {
                        text = text + entry.value();
                    }
                    correlation.put(entry.key(), text);
                }

                boolean validConversion = true;
                for (ImmutableIntPair pair : findConversions(db)) {
                    final IntSet keySet = correlation.keySet();
                    if (keySet.contains(pair.left)) {
                        final String result = convertText(getConversion(db, pair), correlation.get(pair.left));
                        if (result == null) {
                            validConversion = false;
                            break;
                        }
                        correlation.put(pair.right, result);
                    }
                }

                if (validConversion) {
                    final ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
                    for (ImmutableIntKeyMap.Entry<String> entry : correlation.entries()) {
                        corrBuilder.put(entry.key(), obtainSymbolArray(db, entry.value()));
                    }

                    final int correlationId = LangbookDatabase.obtainCorrelation(db, corrBuilder.build());
                    final int correlationArrayId = insertCorrelationArray(db, correlationId);

                    final int baseConcept = conceptFromAcceptation(db, acc);
                    final int ruledConcept = obtainRuledConcept(db, rule, baseConcept);
                    final int newAcc = insertAcceptation(db, ruledConcept, correlationArrayId);
                    insertRuledAcceptation(db, newAcc, agentId, acc);

                    for (IntKeyMap.Entry<String> entry : correlation.entries()) {
                        int mainTextAlphabet = mainAlphabets.get(entry.key(), 0);
                        if (mainTextAlphabet == 0) {
                            final LangbookDbSchema.AlphabetsTable alpTable = LangbookDbSchema.Tables.alphabets;
                            final LangbookDbSchema.LanguagesTable langTable = LangbookDbSchema.Tables.languages;
                            final DbQuery mainAlphableQuery = new DbQuery.Builder(alpTable)
                                    .join(langTable, alpTable.getLanguageColumnIndex(), langTable.getIdColumnIndex())
                                    .where(alpTable.getIdColumnIndex(), entry.key())
                                    .select(alpTable.columns().size() + langTable.getMainAlphabetColumnIndex());
                            mainTextAlphabet = selectSingleRow(db, mainAlphableQuery).get(0).toInt();
                            mainAlphabets.put(entry.key(), mainTextAlphabet);
                        }

                        final String mainText = correlation.get(mainTextAlphabet, entry.value());
                        final DbInsertQuery strInsertQuery = new DbInsertQuery.Builder(strTable)
                                .put(strTable.getDynamicAcceptationColumnIndex(), newAcc)
                                .put(strTable.getMainAcceptationColumnIndex(), mainAcc)
                                .put(strTable.getMainStringColumnIndex(), mainText)
                                .put(strTable.getStringAlphabetColumnIndex(), entry.key())
                                .put(strTable.getStringColumnIndex(), entry.value())
                                .build();

                        if (db.insert(strInsertQuery) == null) {
                            throw new AssertionError();
                        }
                    }
                    processedAccBuilder.add(newAcc);
                }
            }
            processedAcceptations = processedAccBuilder.build();
        }

        if (targetBunch != 0) {
            final int agentSetId = obtainAgentSet(db, new ImmutableIntSetBuilder().add(agentId).build());
            final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
            for (int acc : processedAcceptations) {
                final DbInsertQuery query = new DbInsertQuery.Builder(table)
                        .put(table.getBunchColumnIndex(), targetBunch)
                        .put(table.getAgentSetColumnIndex(), agentSetId)
                        .put(table.getAcceptationColumnIndex(), acc)
                        .build();
                if (db.insert(query) == null) {
                    throw new AssertionError();
                }
            }
        }
    }

    public static Integer addAcceptation(Database db, int concept, int correlationArrayId) {
        MutableIntKeyMap<String> texts = MutableIntKeyMap.empty();
        for (int correlationId : getCorrelationArray(db, correlationArrayId)) {
            for (IntKeyMap.Entry<String> entry : getCorrelationWithText(db, correlationId).entries()) {
                final String currentValue = texts.get(entry.key(), "");
                texts.put(entry.key(), currentValue + entry.value());
            }
        }

        if (texts.isEmpty()) {
            return null;
        }

        final ImmutableSet<ImmutableIntPair> conversions = findConversions(db);
        for (IntKeyMap.Entry<String> entry : texts.entries().toImmutable()) {
            for (ImmutableIntPair pair : conversions) {
                if (pair.left == entry.key()) {
                    final ImmutableList<ImmutablePair<String, String>> conversion = getConversion(db, pair);
                    final String convertedText = convertText(conversion, entry.value());
                    if (convertedText == null) {
                        return null;
                    }

                    texts.put(pair.right, convertedText);
                }
            }
        }

        final String mainStr = texts.valueAt(0);
        final int acceptation = insertAcceptation(db, concept, correlationArrayId);
        for (IntKeyMap.Entry<String> entry : texts.entries()) {
            final int alphabet = entry.key();
            final String str = entry.value();
            insertStringQuery(db, str, mainStr, acceptation, acceptation, alphabet);
        }

        return acceptation;
    }

    public static Integer addAgent(Database db, int targetBunch, ImmutableIntSet sourceBunches,
            ImmutableIntSet diffBunches, ImmutableIntKeyMap<String> matcher,
            ImmutableIntKeyMap<String> adder, int rule, int flags) {
        final int sourceBunchSetId = obtainBunchSet(db, sourceBunches);
        final int diffBunchSetId = obtainBunchSet(db, diffBunches);
        final int matcherId = obtainCorrelation(db, matcher.map((String str) -> obtainSymbolArray(db, str)));
        final int adderId = obtainCorrelation(db, adder.map((String str) -> obtainSymbolArray(db, str)));
        final Integer agentId = LangbookDbInserter.insertAgent(db,
                targetBunch,  sourceBunchSetId, diffBunchSetId, matcherId, adderId, rule, flags);
        if (agentId != null) {
            runAgent(db, agentId, targetBunch, sourceBunches, diffBunches, matcher, adder, rule,
                    (flags & 1) != 0);
        }

        return agentId;
    }

    public static void deleteAgent(Database db, int agentId) {
        // This implementation has lot of holes.
        // 1. It is assuming that there is no chained agents
        // 2. It is assuming that agents sets only contains a single agent.
        // TODO: Improve this logic once it is centralised and better defined

        final ImmutableIntKeyMap<ImmutableIntSet> agentSets = getAllAgentSetsContaining(db, agentId);
        final ImmutableIntPairMap.Builder agentSetMapBuilder = new ImmutableIntPairMap.Builder();
        final ImmutableIntSetBuilder removableAgentSetsBuilder = new ImmutableIntSetBuilder();
        for (IntKeyMap.Entry<ImmutableIntSet> entry : agentSets.entries()) {
            final int setId = obtainAgentSet(db, entry.value().remove(agentId));
            if (setId == 0) {
                removableAgentSetsBuilder.add(entry.key());
            }
            else {
                agentSetMapBuilder.put(entry.key(), setId);
            }
        }

        if (!agentSetMapBuilder.build().isEmpty()) {
            throw new UnsupportedOperationException("Unimplemented: Multiple agents");
        }

        for (int setId : removableAgentSetsBuilder.build()) {
            if (!deleteBunchAcceptationsForAgentSet(db, setId)) {
                throw new AssertionError();
            }

            if (!deleteAgentSet(db, setId)) {
                throw new AssertionError();
            }
        }

        final ImmutableIntSet ruledAcceptations = getAllRuledAcceptationsForAgent(db, agentId);
        for (int ruleAcceptation : ruledAcceptations) {
            if (!deleteStringQueriesForDynamicAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }

            if (!deleteRuledAcceptation(db, ruleAcceptation)) {
                throw new AssertionError();
            }
        }

        if (!LangbookDeleter.deleteAgent(db, agentId)) {
            throw new AssertionError();
        }
    }
}
