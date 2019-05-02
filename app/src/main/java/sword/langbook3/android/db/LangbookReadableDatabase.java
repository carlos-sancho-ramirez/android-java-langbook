package sword.langbook3.android.db;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import sword.collections.Function;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableIntValueHashMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.MutableHashMap;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntList;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.SortUtils;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbTable;
import sword.database.DbValue;
import sword.langbook3.android.DisplayableItem;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.CorrelationDetailsModel;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.IdentifiableResult;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.models.SynonymTranslationResult;
import sword.langbook3.android.models.TableCellReference;
import sword.langbook3.android.models.TableCellValue;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.db.LangbookDbSchema.MAX_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.Tables.alphabets;

public final class LangbookReadableDatabase {

    private LangbookReadableDatabase() {
    }

    static List<DbValue> selectSingleRow(DbExporter.Database db, DbQuery query) {
        try (DbResult result = db.select(query)) {
            if (!result.hasNext()) {
                throw new AssertionError("Nothing found matching the given criteria");
            }

            final List<DbValue> row = result.next();
            if (result.hasNext()) {
                throw new AssertionError("Multiple rows found matching the given criteria");
            }

            return row;
        }
    }

    private static List<DbValue> selectFirstRow(DbExporter.Database db, DbQuery query) {
        try (DbResult result = db.select(query)) {
            if (!result.hasNext()) {
                throw new AssertionError("Nothing found matching the given criteria");
            }

            return result.next();
        }
    }

    private static Integer selectOptionalFirstIntColumn(DbExporter.Database db, DbQuery query) {
        Integer result = null;
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                result = dbResult.next().get(0).toInt();
            }

            if (dbResult.hasNext()) {
                throw new AssertionError("Only 0 or 1 row was expected");
            }
        }

        return result;
    }

    private static boolean selectExistingRow(DbExporter.Database db, DbQuery query) {
        boolean result = false;
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                dbResult.next();
                result = true;
            }

            if (dbResult.hasNext()) {
                throw new AssertionError();
            }
        }

        return result;
    }

    private static boolean selectExistAtLeastOneRow(DbExporter.Database db, DbQuery query) {
        try (DbResult dbResult = db.select(query)) {
            return dbResult.hasNext();
        }
    }

    static ImmutableIntSet findCorrelationsByLanguage(DbExporter.Database db, int language) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final int offset = correlations.columns().size();
        final DbQuery query = new DbQuery.Builder(correlations)
                .join(alphabets, correlations.getAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .where(offset + alphabets.getLanguageColumnIndex(), language)
                .select(correlations.getCorrelationIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntPairMap findCorrelationsAndSymbolArrayForAlphabet(DbExporter.Database db, int sourceAlphabet) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(correlations)
                .where(correlations.getAlphabetColumnIndex(), sourceAlphabet)
                .select(correlations.getCorrelationIdColumnIndex(), correlations.getSymbolArrayColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    static ImmutableIntSet findCorrelationsUsedInAgents(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .select(agents.getStartMatcherColumnIndex(), agents.getEndMatcherColumnIndex(),
                        agents.getStartAdderColumnIndex(), agents.getEndAdderColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                for (int i = 0; i < 4; i++) {
                    builder.add(row.get(i).toInt());
                }
            }
        }

        return builder.build();
    }

    static ImmutableIntSet findAcceptationsByLanguage(DbExporter.Database db, int language) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final int offset = strings.columns().size();
        final DbQuery query = new DbQuery.Builder(strings)
                .join(alphabets, strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .where(offset + alphabets.getLanguageColumnIndex(), language)
                .select(strings.getDynamicAcceptationColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                builder.add(dbResult.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    static ImmutableIntSet findAcceptationsByConcept(DbExporter.Database db, int concept) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(acceptations.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntSet findBunchConceptsLinkedToJustThisLanguage(DbExporter.Database db, int language) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;

        final int accOffset = bunchAcceptations.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final int alphabetsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(acceptations, bunchAcceptations.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, strOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .groupBy(bunchAcceptations.getBunchColumnIndex())
                .select(bunchAcceptations.getBunchColumnIndex(), alphabetsOffset + alphabets.getLanguageColumnIndex());

        MutableIntKeyMap<ImmutableIntSet> map = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                final int lang = row.get(1).toInt();
                final ImmutableIntSet set = map.get(bunch, ImmutableIntArraySet.empty()).add(lang);
                map.put(bunch, set);
            }
        }

        return map.filter(set -> set.contains(language) && set.size() == 1).keySet().toImmutable();
    }

    static ImmutableIntSet findIncludedAcceptationLanguages(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;

        final int strOffset = bunchAcceptations.columns().size();
        final int alpOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(strings, bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, strOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .groupBy(alpOffset + alphabets.getLanguageColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .select(alpOffset + alphabets.getLanguageColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntSet findSuperTypesLinkedToJustThisLanguage(DbExporter.Database db, int language) {
        final LangbookDbSchema.BunchConceptsTable bunchConcepts = LangbookDbSchema.Tables.bunchConcepts;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;

        final int accOffset = bunchConcepts.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final int alphabetsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchConcepts)
                .join(acceptations, bunchConcepts.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, strOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .groupBy(bunchConcepts.getBunchColumnIndex())
                .select(bunchConcepts.getBunchColumnIndex(), alphabetsOffset + alphabets.getLanguageColumnIndex());

        MutableIntKeyMap<ImmutableIntSet> map = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                final int lang = row.get(1).toInt();
                final ImmutableIntSet set = map.get(bunch, ImmutableIntArraySet.empty()).add(lang);
                map.put(bunch, set);
            }
        }

        return map.filter(set -> set.contains(language) && set.size() == 1).keySet().toImmutable();
    }

    static Integer findSymbolArray(DbExporter.Database db, String str) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStrColumnIndex(), str)
                .select(table.getIdColumnIndex());

        try (DbResult result = db.select(query)) {
            final Integer value = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError();
            }

            return value;
        }
    }

    public static Integer findCorrelation(DbExporter.Database db, IntKeyMap<String> correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }
        final ImmutableIntKeyMap<String> corr = correlation.toImmutable();

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final int offset = table.columns().size();
        final int offset2 = offset + symbolArrays.columns().size();
        final int offset3 = offset2 + table.columns().size();

        final DbQuery query = new DbQuery.Builder(table)
                .join(symbolArrays, table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(table.getAlphabetColumnIndex(), corr.keyAt(0))
                .where(offset + symbolArrays.getStrColumnIndex(), corr.valueAt(0))
                .join(table, table.getCorrelationIdColumnIndex(), table.getCorrelationIdColumnIndex())
                .join(symbolArrays, offset2 + table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .select(
                        table.getCorrelationIdColumnIndex(),
                        offset2 + table.getAlphabetColumnIndex(),
                        offset3 + symbolArrays.getStrColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int correlationId = row.get(0).toInt();
                ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
                builder.put(row.get(1).toInt(), row.get(2).toText());

                while (result.hasNext()) {
                    row = result.next();
                    int newCorrelationId = row.get(0).toInt();
                    if (newCorrelationId != correlationId) {
                        if (builder.build().equals(corr)) {
                            return correlationId;
                        }

                        correlationId = newCorrelationId;
                        builder = new ImmutableIntKeyMap.Builder<>();
                    }

                    builder.put(row.get(1).toInt(), row.get(2).toText());
                }

                if (builder.build().equals(corr)) {
                    return correlationId;
                }
            }
        }

        return null;
    }

    public static Integer findCorrelation(DbExporter.Database db, IntPairMap correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }
        final ImmutableIntPairMap corr = correlation.toImmutable();

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final int offset = table.columns().size();
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getCorrelationIdColumnIndex(), table.getCorrelationIdColumnIndex())
                .where(table.getAlphabetColumnIndex(), corr.keyAt(0))
                .where(table.getSymbolArrayColumnIndex(), corr.valueAt(0))
                .select(
                        table.getCorrelationIdColumnIndex(),
                        offset + table.getAlphabetColumnIndex(),
                        offset + table.getSymbolArrayColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int correlationId = row.get(0).toInt();
                ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
                builder.put(row.get(1).toInt(), row.get(2).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    int newCorrelationId = row.get(0).toInt();
                    if (newCorrelationId != correlationId) {
                        if (builder.build().equals(corr)) {
                            return correlationId;
                        }

                        correlationId = newCorrelationId;
                        builder = new ImmutableIntPairMap.Builder();
                    }

                    builder.put(row.get(1).toInt(), row.get(2).toInt());
                }

                if (builder.build().equals(corr)) {
                    return correlationId;
                }
            }
        }

        return null;
    }

    static Integer findCorrelationArray(DbImporter.Database db, IntList array) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getArrayIdColumnIndex(), table.getArrayIdColumnIndex())
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), array.get(0))
                .select(table.getArrayIdColumnIndex(), table.columns().size() + table.getCorrelationColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int arrayId = row.get(0).toInt();
                ImmutableIntList.Builder builder = new ImmutableIntList.Builder();
                builder.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    int newArrayId = row.get(0).toInt();
                    if (arrayId != newArrayId) {
                        if (builder.build().equals(array)) {
                            return arrayId;
                        }

                        arrayId = newArrayId;
                        builder = new ImmutableIntList.Builder();
                    }
                    builder.add(row.get(1).toInt());
                }

                if (builder.build().equals(array)) {
                    return arrayId;
                }
            }
        }

        return null;
    }

    static Integer findCorrelationArray(DbExporter.Database db, int... correlations) {
        if (correlations.length == 0) {
            return StreamedDatabaseConstants.nullCorrelationArrayId;
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), correlations[0])
                .select(table.getArrayIdColumnIndex());

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final int arrayId = result.next().get(0).toInt();
                final int[] array = getCorrelationArray(db, arrayId);
                if (Arrays.equals(correlations, array)) {
                    return arrayId;
                }
            }
        }

        return null;
    }

    public static Integer findLanguageByCode(DbExporter.Database db, String code) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getCodeColumnIndex(), code)
                .select(table.getIdColumnIndex());

        return selectOptionalFirstIntColumn(db, query);
    }

    public static ImmutableIntSet findAlphabetsByLanguage(DbExporter.Database db, int language) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getLanguageColumnIndex(), language)
                .select(table.getIdColumnIndex());


        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    public static ImmutableIntPairMap getConversionsMap(DbExporter.Database db) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.put(row.get(1).toInt(), row.get(0).toInt());
            }
        }

        return builder.build();
    }

    public static Integer findRuledAcceptationByRuleAndMainAcceptation(DbExporter.Database db, int rule, int mainAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(ruledAccs)
                .join(agents, ruledAccs.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAccs.getAcceptationColumnIndex(), mainAcceptation)
                .where(ruledAccs.columns().size() + agents.getRuleColumnIndex(), rule)
                .select(ruledAccs.getIdColumnIndex());
        final DbResult dbResult = db.select(query);
        final Integer result = dbResult.hasNext()? dbResult.next().get(0).toInt() : null;
        if (dbResult.hasNext()) {
            throw new AssertionError();
        }

        return result;
    }

    public static ImmutableList<SearchResult> findAcceptationFromText(DbExporter.Database db, String queryText, int restrictionStringType) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringColumnIndex(), new DbQuery.Restriction(new DbStringValue(queryText),
                        restrictionStringType))
                .select(
                        table.getStringColumnIndex(),
                        table.getMainStringColumnIndex(),
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        final MutableIntKeyMap<SearchResult> map = MutableIntKeyMap.empty();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String str = row.get(0).toText();
                final String mainStr = row.get(1).toText();
                final int acc = row.get(2).toInt();
                final int dynAcc = row.get(3).toInt();

                map.put(dynAcc, new SearchResult(str, mainStr, SearchResult.Types.ACCEPTATION, acc, dynAcc));
            }
        }

        return map.toList().toImmutable().sort((a, b) -> !a.isDynamic() && b.isDynamic() || a.isDynamic() == b.isDynamic() && SortUtils.compareCharSequenceByUnicode(a.getStr(), b.getStr()));
    }

    public static ImmutableList<SearchResult> findAcceptationAndRulesFromText(DbExporter.Database db, String queryText, int restrictionStringType) {
        final ImmutableList<SearchResult> rawResult = findAcceptationFromText(db, queryText, restrictionStringType);
        final SyncCacheIntKeyNonNullValueMap<String> mainTexts = new SyncCacheIntKeyNonNullValueMap<>(id -> readAcceptationMainText(db, id));

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final int offset = ruledAcceptations.columns().size();

        return rawResult.map(rawEntry -> {
            if (rawEntry.isDynamic()) {
                ImmutableIntList rules = ImmutableIntList.empty();
                int dynAcc = rawEntry.getAuxiliarId();
                while (dynAcc != rawEntry.getId()) {
                    final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                            .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                            .where(ruledAcceptations.getIdColumnIndex(), dynAcc)
                            .select(ruledAcceptations.getAcceptationColumnIndex(), offset + agents.getRuleColumnIndex());

                    final List<DbValue> row = selectSingleRow(db, query);
                    dynAcc = row.get(0).toInt();
                    rules = rules.append(row.get(1).toInt());
                }

                return rawEntry
                        .withMainAccMainStr(mainTexts.get(rawEntry.getId()))
                        .withRules(rules);
            }
            else {
                return rawEntry;
            }
        });
    }

    static boolean isAcceptationInBunch(DbExporter.Database db, int bunch, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .select(table.getIdColumnIndex());
        try (DbResult result = db.select(query)) {
            return result.hasNext();
        }
    }

    public static Conversion getConversion(DbExporter.Database db, ImmutableIntPair pair) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;
        final LangbookDbSchema.SymbolArraysTable symbols = LangbookDbSchema.Tables.symbolArrays;

        final int off1Symbols = conversions.columns().size();
        final int off2Symbols = off1Symbols + symbols.columns().size();

        final DbQuery query = new DbQuery.Builder(conversions)
                .join(symbols, conversions.getSourceColumnIndex(), symbols.getIdColumnIndex())
                .join(symbols, conversions.getTargetColumnIndex(), symbols.getIdColumnIndex())
                .where(conversions.getSourceAlphabetColumnIndex(), pair.left)
                .where(conversions.getTargetAlphabetColumnIndex(), pair.right)
                .select(
                        off1Symbols + symbols.getStrColumnIndex(),
                        off2Symbols + symbols.getStrColumnIndex());

        final MutableMap<String, String> resultMap = MutableHashMap.empty();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String sourceText = row.get(0).toText();
                final String targetText = row.get(1).toText();
                resultMap.put(sourceText, targetText);
            }
        }

        return new Conversion(pair.left, pair.right, resultMap);
    }

    static boolean checkConversionConflicts(DbExporter.Database db, ConversionProposal conversion) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringAlphabetColumnIndex(), conversion.getSourceAlphabet())
                .select(table.getStringColumnIndex());

        return !db.select(query)
                .map(row -> row.get(0).toText())
                .anyMatch(str -> conversion.convert(str) == null);
    }

    public static ImmutableSet<String> findConversionConflictWords(DbExporter.Database db, ConversionProposal conversion) {
        // TODO: Logic in the word should be somehow centralised with #checkConversionConflicts method
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringAlphabetColumnIndex(), conversion.getSourceAlphabet())
                .select(table.getStringColumnIndex());

        return db.select(query)
                .map(row -> row.get(0).toText())
                .filter(str -> conversion.convert(str) == null)
                .toSet()
                .toImmutable();
    }

    public static Integer findBunchSet(DbExporter.Database db, IntSet bunches) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        if (bunches.isEmpty()) {
            return table.nullReference();
        }

        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunches.valueAt(0))
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getBunchColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                final MutableIntSet set = MutableIntArraySet.empty();
                int setId = row.get(0).toInt();
                set.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    if (row.get(0).toInt() != setId) {
                        if (set.equals(bunches)) {
                            return setId;
                        }

                        setId = row.get(0).toInt();
                        set.clear();
                        set.add(row.get(1).toInt());
                    }
                }

                if (set.equals(bunches)) {
                    return setId;
                }
            }
        }

        return null;
    }

    static Integer findAgentSet(DbExporter.Database db, IntSet agentSet) {
        if (agentSet.isEmpty()) {
            return 0;
        }

        final ImmutableIntSet set = agentSet.toImmutable();
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getAgentColumnIndex(), set.valueAt(0))
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getAgentColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int setId = row.get(0).toInt();
                ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
                builder.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    int newSetId = row.get(0).toInt();
                    if (newSetId != setId) {
                        if (set.equals(builder.build())) {
                            return setId;
                        }

                        setId = newSetId;
                        builder = new ImmutableIntSetCreator();
                    }
                    builder.add(row.get(1).toInt());
                }

                if (set.equals(builder.build())) {
                    return setId;
                }
            }
        }

        return null;
    }

    static Integer findRuledConcept(DbExporter.Database db, int rule, int concept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getRuleColumnIndex(), rule)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex());

        try (DbResult result = db.select(query)) {
            final Integer id = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated ruled concepts");
            }
            return id;
        }
    }

    static Integer findQuizDefinition(DbExporter.Database db, int bunch, int setId) {
        final LangbookDbSchema.QuizDefinitionsTable table = LangbookDbSchema.Tables.quizDefinitions;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getQuestionFieldsColumnIndex(), setId)
                .select(table.getIdColumnIndex());

        try (DbResult result = db.select(query)) {
            final Integer value = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError("Only one quiz definition expected");
            }
            return value;
        }
    }

    static ImmutableIntPairMap findAffectedAgentsByItsSourceWithTarget(DbExporter.Database db, int bunch) {
        if (bunch == 0) {
            return ImmutableIntPairMap.empty();
        }

        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final int offset = bunchSets.columns().size();
        final DbQuery query = new DbQuery.Builder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getSourceBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(offset + agents.getIdColumnIndex(), offset + agents.getTargetBunchColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    static ImmutableIntPairMap findAffectedAgentsByItsDiffWithTarget(DbExporter.Database db, int bunch) {
        if (bunch == 0) {
            return ImmutableIntPairMap.empty();
        }

        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final int offset = bunchSets.columns().size();
        final DbQuery query = new DbQuery.Builder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getDiffBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(offset + agents.getIdColumnIndex(), offset + agents.getTargetBunchColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    static ImmutableIntSet findAgentsWithoutSourceBunches(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getSourceBunchSetColumnIndex(), 0)
                .select(agents.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    static ImmutableIntPairMap findAgentsWithoutSourceBunchesWithTarget(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getSourceBunchSetColumnIndex(), 0)
                .select(agents.getIdColumnIndex(), agents.getTargetBunchColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    static ImmutableIntSet findAffectedAgentsByAcceptationCorrelationModification(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;

        final int bunchSetOffset = agents.columns().size();
        final int bunchAccOffset = bunchSetOffset + bunchSets.columns().size();
        final DbQuery query = new DbQuery.Builder(agents)
                .join(bunchSets, agents.getSourceBunchSetColumnIndex(), bunchSets.getSetIdColumnIndex())
                .join(bunchAcceptations, bunchSetOffset + bunchSets.getBunchColumnIndex(), bunchAcceptations.getBunchColumnIndex())
                .where(bunchAccOffset + bunchAcceptations.getAgentSetColumnIndex(), 0)
                .where(bunchAccOffset + bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(agents.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    static ImmutableIntSet findQuizzesByBunch(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = LangbookDbSchema.Tables.quizDefinitions;
        final DbQuery query = new DbQuery.Builder(quizzes)
                .where(quizzes.getBunchColumnIndex(), bunch)
                .select(quizzes.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    static Integer findQuestionFieldSet(DbExporter.Database db, Iterable<QuestionFieldDetails> collection) {
        final Set<QuestionFieldDetails> set = new HashSet<>();
        if (collection == null) {
            return null;
        }

        for (QuestionFieldDetails field : collection) {
            set.add(field);
        }

        if (set.size() == 0) {
            return null;
        }

        final QuestionFieldDetails firstField = set.iterator().next();
        final LangbookDbSchema.QuestionFieldSets fieldSets = LangbookDbSchema.Tables.questionFieldSets;
        final int columnCount = fieldSets.columns().size();
        final DbQuery query = new DbQuery.Builder(fieldSets)
                .join(fieldSets, fieldSets.getSetIdColumnIndex(), fieldSets.getSetIdColumnIndex())
                .where(fieldSets.getAlphabetColumnIndex(), firstField.alphabet)
                .where(fieldSets.getRuleColumnIndex(), firstField.rule)
                .where(fieldSets.getFlagsColumnIndex(), firstField.flags)
                .select(
                        fieldSets.getSetIdColumnIndex(),
                        columnCount + fieldSets.getAlphabetColumnIndex(),
                        columnCount + fieldSets.getRuleColumnIndex(),
                        columnCount + fieldSets.getFlagsColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int setId = row.get(0).toInt();
                final Set<QuestionFieldDetails> foundSet = new HashSet<>();
                foundSet.add(new QuestionFieldDetails(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt()));

                while (result.hasNext()) {
                    row = result.next();
                    if (setId != row.get(0).toInt()) {
                        if (foundSet.equals(set)) {
                            return setId;
                        }
                        else {
                            foundSet.clear();
                            setId = row.get(0).toInt();
                        }
                    }

                    foundSet.add(new QuestionFieldDetails(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt()));
                }

                if (foundSet.equals(set)) {
                    return setId;
                }
            }
        }

        return null;
    }

    public static ImmutableIntPairMap findConversions(DbExporter.Database db, IntSet alphabets) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final MutableIntSet foundAlphabets = MutableIntArraySet.empty();
        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int source = row.get(0).toInt();
                final int target = row.get(1).toInt();

                if (foundAlphabets.contains(target)) {
                    throw new AssertionError();
                }
                foundAlphabets.add(target);

                if (alphabets.contains(target)) {
                    if (!alphabets.contains(source)) {
                        throw new AssertionError();
                    }

                    builder.put(target, source);
                }
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet findSentenceIdsMatchingMeaning(DbExporter.Database db, int symbolArrayId) {
        final LangbookDbSchema.SentenceMeaningTable table = LangbookDbSchema.Tables.sentenceMeaning;
        final int offset = table.columns().size();
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getMeaning(), table.getMeaning())
                .where(table.getIdColumnIndex(), symbolArrayId)
                .whereColumnValueDiffer(table.getIdColumnIndex(), offset + table.getIdColumnIndex())
                .select(offset + table.getIdColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static int getColumnMax(DbExporter.Database db, DbTable table, int columnIndex) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(DbQuery.max(columnIndex));

        try (DbResult result = db.select(query)) {
            return result.hasNext()? result.next().get(0).toInt() : 0;
        }
    }

    static int getMaxCorrelationId(DbExporter.Database db) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        return getColumnMax(db, table, table.getCorrelationIdColumnIndex());
    }

    static int getMaxCorrelationArrayId(DbExporter.Database db) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        return getColumnMax(db, table, table.getArrayIdColumnIndex());
    }

    public static int getMaxConceptInAcceptations(DbExporter.Database db) {
        LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        return getColumnMax(db, table, table.getConceptColumnIndex());
    }

    private static int getMaxConceptInRuledConcepts(DbExporter.Database db) {
        LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        return getColumnMax(db, table, table.getIdColumnIndex());
    }

    private static int getMaxLanguage(DbExporter.Database db) {
        LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        return getColumnMax(db, table, table.getIdColumnIndex());
    }

    private static int getMaxAlphabet(DbExporter.Database db) {
        LangbookDbSchema.AlphabetsTable table = alphabets;
        return getColumnMax(db, table, table.getIdColumnIndex());
    }

    public static int getMaxConcept(DbExporter.Database db) {
        int max = getMaxConceptInAcceptations(db);
        int temp = getMaxConceptInRuledConcepts(db);
        if (temp > max) {
            max = temp;
        }

        temp = getMaxLanguage(db);
        if (temp > max) {
            max = temp;
        }

        temp = getMaxAlphabet(db);
        if (temp > max) {
            max = temp;
        }

        return max;
    }

    public static int getMaxBunchSetId(DbExporter.Database db) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    public static int getMaxAgentSetId(DbExporter.Database db) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    static int getMaxQuestionFieldSetId(DbExporter.Database db) {
        LangbookDbSchema.QuestionFieldSets table = LangbookDbSchema.Tables.questionFieldSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    static int getMaxSentenceMeaning(DbExporter.Database db) {
        LangbookDbSchema.SentenceMeaningTable table = LangbookDbSchema.Tables.sentenceMeaning;
        return getColumnMax(db, table, table.getMeaning());
    }

    public static String getSymbolArray(DbExporter.Database db, int id) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .select(table.getStrColumnIndex());

        try (DbResult result = db.select(query)) {
            final String str = result.hasNext()? result.next().get(0).toText() : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated identifiers");
            }
            return str;
        }
    }

    static ImmutableIntPairMap getCorrelation(DbExporter.Database db, int id) {
        LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getCorrelationIdColumnIndex(), id)
                .select(table.getAlphabetColumnIndex(), table.getSymbolArrayColumnIndex());

        ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                corrBuilder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return corrBuilder.build();
    }

    public static ImmutableIntKeyMap<String> getCorrelationWithText(DbExporter.Database db, int correlationId) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final DbQuery query = new DbQuery.Builder(correlations)
                .join(symbolArrays, correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(correlations.getCorrelationIdColumnIndex(), correlationId)
                .select(correlations.getAlphabetColumnIndex(), correlations.columns().size() + symbolArrays.getStrColumnIndex());
        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toText());
            }
        }
        return builder.build();
    }

    private static int[] getCorrelationArray(DbExporter.Database db, int id) {
        if (id == StreamedDatabaseConstants.nullCorrelationArrayId) {
            return new int[0];
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayIdColumnIndex(), id)
                .select(table.getArrayPositionColumnIndex(), table.getCorrelationColumnIndex());
        final DbResult dbResult = db.select(query);
        final int[] result = new int[dbResult.getRemainingRows()];
        try {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int pos = row.get(0).toInt();
                final int corr = row.get(1).toInt();
                if (result[pos] != 0) {
                    throw new AssertionError("Malformed correlation array with id " + id);
                }

                result[pos] = corr;
            }
        }
        finally {
            dbResult.close();
        }

        return result;
    }

    static ImmutableIntPairMap getAcceptationsAndAgentSetsInBunch(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getAcceptationColumnIndex(), table.getAgentSetColumnIndex());
        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    private static ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntKeyMap<String>>> getAcceptationCorrelations(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = LangbookDbSchema.Tables.correlationArrays;
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbols = LangbookDbSchema.Tables.symbolArrays;

        final int corrArraysOffset = acceptations.columns().size();
        final int corrOffset = corrArraysOffset + correlationArrays.columns().size();
        final int symbolsOffset = corrOffset + correlations.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(correlationArrays, acceptations.getCorrelationArrayColumnIndex(), correlationArrays.getArrayIdColumnIndex())
                .join(correlations, corrArraysOffset + correlationArrays.getCorrelationColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbols, corrOffset + correlations.getSymbolArrayColumnIndex(), symbols.getIdColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .orderBy(
                        corrArraysOffset + correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex())
                .select(
                        corrArraysOffset + correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getCorrelationIdColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        symbolsOffset + symbols.getStrColumnIndex()
                );

        final MutableIntList correlationIds = MutableIntList.empty();
        final MutableIntKeyMap<ImmutableIntKeyMap<String>> correlationMap = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
                int pos = row.get(0).toInt();
                int correlationId = row.get(1).toInt();
                if (pos != correlationIds.size()) {
                    throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                }

                builder.put(row.get(2).toInt(), row.get(3).toText());

                while (dbResult.hasNext()) {
                    row = dbResult.next();
                    int newPos = row.get(0).toInt();
                    if (newPos != pos) {
                        correlationMap.put(correlationId, builder.build());
                        correlationIds.append(correlationId);
                        correlationId = row.get(1).toInt();
                        builder = new ImmutableIntKeyMap.Builder<>();
                        pos = newPos;
                    }

                    if (newPos != correlationIds.size()) {
                        throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                    }
                    builder.put(row.get(2).toInt(), row.get(3).toText());
                }
                correlationMap.put(correlationId, builder.build());
                correlationIds.append(correlationId);
            }
        }

        return new ImmutablePair<>(correlationIds.toImmutable(), correlationMap.toImmutable());
    }

    private static ImmutableIntSet getBunchSet(DbExporter.Database db, int setId) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .select(table.getBunchColumnIndex());

        return intSetQuery(db, query);
    }

    public static ImmutableList<SearchResult> getSearchHistory(DbExporter.Database db) {
        final LangbookDbSchema.SearchHistoryTable history = LangbookDbSchema.Tables.searchHistory;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final int offset = history.columns().size();
        final DbQuery query = new DbQuery.Builder(history)
                .join(strings, history.getAcceptation(), strings.getDynamicAcceptationColumnIndex())
                .orderBy(new DbQuery.Ordered(history.getIdColumnIndex(), true))
                .select(history.getAcceptation(),
                        offset + strings.getMainAcceptationColumnIndex(),
                        offset + strings.getStringColumnIndex(),
                        offset + strings.getMainStringColumnIndex());

        final MutableIntSet acceptations = MutableIntArraySet.empty();
        final ImmutableList.Builder<SearchResult> builder = new ImmutableList.Builder<>();

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int acceptation = row.get(0).toInt();
                if (!acceptations.contains(acceptation)) {
                    acceptations.add(acceptation);
                    builder.add(new SearchResult(row.get(2).toText(), row.get(3).toText(), SearchResult.Types.ACCEPTATION, row.get(1).toInt(), acceptation));
                }
            }
        }

        return builder.build();
    }

    static Integer getSentenceMeaning(DbExporter.Database db, int symbolArrayId) {
        final LangbookDbSchema.SentenceMeaningTable table = LangbookDbSchema.Tables.sentenceMeaning;

        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), symbolArrayId)
                .select(table.getMeaning());

        Integer result = null;
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                result = dbResult.next().get(0).toInt();
            }

            if (dbResult.hasNext()) {
                throw new AssertionError();
            }
        }

        return result;
    }

    private static ImmutableIntKeyMap<String> readAcceptationsIncludingCorrelation(DbExporter.Database db, int correlation, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = LangbookDbSchema.Tables.correlationArrays;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = correlationArrays.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(correlationArrays)
                .join(acceptations, correlationArrays.getArrayIdColumnIndex(), acceptations.getCorrelationArrayColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(correlationArrays.getCorrelationColumnIndex(), correlation)
                .select(accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int accId = row.get(0).toInt();
                if (row.get(1).toInt() == preferredAlphabet || result.get(accId, null) == null) {
                    result.put(accId, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private static ImmutableIntKeyMap<ImmutableIntKeyMap<String>> readCorrelationsWithSameSymbolArray(DbExporter.Database db, int correlation, int alphabet) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final int corrOffset2 = correlations.columns().size();
        final int corrOffset3 = corrOffset2 + corrOffset2;
        final int strOffset = corrOffset3 + corrOffset2;

        final DbQuery query = new DbQuery.Builder(correlations)
                .join(correlations, correlations.getSymbolArrayColumnIndex(), correlations.getSymbolArrayColumnIndex())
                .join(correlations, corrOffset2 + correlations.getCorrelationIdColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrOffset3 + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(correlations.getCorrelationIdColumnIndex(), correlation)
                .where(correlations.getAlphabetColumnIndex(), alphabet)
                .whereColumnValueMatch(correlations.getAlphabetColumnIndex(), corrOffset2 + correlations.getAlphabetColumnIndex())
                .whereColumnValueDiffer(correlations.getCorrelationIdColumnIndex(), corrOffset2 + correlations.getCorrelationIdColumnIndex())
                .select(corrOffset2 + correlations.getCorrelationIdColumnIndex(),
                        corrOffset3 + correlations.getAlphabetColumnIndex(),
                        strOffset + symbolArrays.getStrColumnIndex());

        final MutableIntKeyMap<ImmutableIntKeyMap<String>> result = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int corrId = row.get(0).toInt();
                final int textAlphabet = row.get(1).toInt();
                final String text = row.get(2).toText();
                final ImmutableIntKeyMap<String> currentCorr = result.get(corrId, ImmutableIntKeyMap.empty());
                result.put(corrId, currentCorr.put(textAlphabet, text));
            }
        }

        return result.toImmutable();
    }

    public static int conceptFromAcceptation(DbExporter.Database db, int accId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), accId)
                .select(table.getConceptColumnIndex());
        return selectSingleRow(db, query).get(0).toInt();
    }

    static int correlationArrayFromAcceptation(DbExporter.Database db, int accId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), accId)
                .select(table.getCorrelationArrayColumnIndex());
        return selectSingleRow(db, query).get(0).toInt();
    }

    static ImmutableIntSet getAlphabetAndLanguageConcepts(DbExporter.Database db) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getLanguageColumnIndex());

        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.add(row.get(0).toInt());
                builder.add(row.get(1).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet getAgentIds(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static MutableIntKeyMap<String> readCorrelationArrayTexts(DbExporter.Database db, int correlationArrayId) {
        MutableIntKeyMap<String> texts = MutableIntKeyMap.empty();
        for (int correlationId : getCorrelationArray(db, correlationArrayId)) {
            for (IntKeyMap.Entry<String> entry : getCorrelationWithText(db, correlationId).entries()) {
                final String currentValue = texts.get(entry.key(), "");
                texts.put(entry.key(), currentValue + entry.value());
            }
        }

        return texts;
    }

    static IntKeyMap<String> readCorrelationArrayTextAndItsAppliedConversions(DbExporter.Database db, int correlationArrayId) {
        final MutableIntKeyMap<String> texts = readCorrelationArrayTexts(db, correlationArrayId);
        if (texts.isEmpty()) {
            return null;
        }

        final ImmutableIntPairMap conversionMap = getConversionsMap(db);
        final int conversionCount = conversionMap.size();
        for (IntKeyMap.Entry<String> entry : texts.entries().toImmutable()) {
            for (int conversionIndex = 0; conversionIndex < conversionCount; conversionIndex++) {
                if (conversionMap.valueAt(conversionIndex) == entry.key()) {
                    final ImmutableIntPair pair = new ImmutableIntPair(conversionMap.valueAt(conversionIndex), conversionMap.keyAt(conversionIndex));
                    final String convertedText = getConversion(db, pair).convert(entry.value());
                    if (convertedText == null) {
                        return null;
                    }

                    texts.put(pair.right, convertedText);
                }
            }
        }

        return texts;
    }

    private static ImmutableIntKeyMap<SynonymTranslationResult> readAcceptationSynonymsAndTranslations(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.LanguagesTable languages = LangbookDbSchema.Tables.languages;

        final int accOffset = acceptations.columns().size();
        final int stringsOffset = accOffset * 2;
        final int alphabetsOffset = stringsOffset + strings.columns().size();
        final int languagesOffset = alphabetsOffset + alphabets.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, stringsOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .join(languages, alphabetsOffset + alphabets.getLanguageColumnIndex(), languages.getIdColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .whereColumnValueMatch(alphabetsOffset + alphabets.getIdColumnIndex(), languagesOffset + languages.getMainAlphabetColumnIndex())
                .select(accOffset + acceptations.getIdColumnIndex(),
                        languagesOffset + languages.getIdColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<SynonymTranslationResult> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int accId = row.get(0).toInt();
                if (accId != acceptation) {
                    builder.put(accId, new SynonymTranslationResult(row.get(1).toInt(), row.get(2).toText()));
                }
            }
        }

        return builder.build();
    }

    private static IdentifiableResult readSupertypeFromAcceptation(DbExporter.Database db, int acceptation, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchConceptsTable bunchConcepts = LangbookDbSchema.Tables.bunchConcepts;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int bunchOffset = acceptations.columns().size();
        final int accOffset = bunchOffset + bunchConcepts.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(bunchConcepts, acceptations.getConceptColumnIndex(), bunchConcepts.getConceptColumnIndex())
                .join(acceptations, bunchOffset + bunchConcepts.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        IdentifiableResult result = null;
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                int acc = row.get(0).toInt();
                int firstAlphabet = row.get(1).toInt();
                String text = row.get(2).toText();
                while (firstAlphabet != preferredAlphabet && dbResult.hasNext()) {
                    row = dbResult.next();
                    if (row.get(1).toInt() == preferredAlphabet) {
                        acc = row.get(0).toInt();
                        text = row.get(2).toText();
                        break;
                    }
                }

                result = new IdentifiableResult(acc, text);
            }
        }

        return result;
    }

    private static ImmutableIntKeyMap<String> readSubtypesFromAcceptation(DbExporter.Database db, int acceptation, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchConceptsTable bunchConcepts = LangbookDbSchema.Tables.bunchConcepts;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int bunchOffset = acceptations.columns().size();
        final int accOffset = bunchOffset + bunchConcepts.columns().size();
        final int strOffset = accOffset + bunchOffset;

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(bunchConcepts, acceptations.getConceptColumnIndex(), bunchConcepts.getBunchColumnIndex())
                .join(acceptations, bunchOffset + bunchConcepts.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(bunchOffset + bunchConcepts.getConceptColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<String> conceptText = MutableIntKeyMap.empty();
        final MutableIntPairMap conceptAccs = MutableIntPairMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int concept = row.get(0).toInt();
                final int alphabet = row.get(2).toInt();
                if (alphabet == preferredAlphabet || !conceptAccs.keySet().contains(concept)) {
                    conceptAccs.put(concept, row.get(1).toInt());
                    conceptText.put(concept, row.get(3).toText());
                }
            }
        }

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        final int length = conceptAccs.size();
        for (int i = 0; i < length; i++) {
            builder.put(conceptAccs.valueAt(i), conceptText.valueAt(i));
        }

        return builder.build();
    }

    private static ImmutableList<MorphologyResult> readMorphologiesFromAcceptation(DbExporter.Database db, int acceptation, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;

        final int strOffset1 = ruledAcceptations.columns().size();
        final int agentsOffset = strOffset1 + strings.columns().size();
        final int accOffset = agentsOffset + agents.columns().size();
        final int strOffset2 = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(acceptations, agentsOffset + agents.getRuleColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex(),
                        accOffset + acceptations.getConceptColumnIndex(),
                        strOffset1 + strings.getStringAlphabetColumnIndex(),
                        strOffset1 + strings.getStringColumnIndex(),
                        strOffset2 + strings.getStringAlphabetColumnIndex(),
                        strOffset2 + strings.getStringColumnIndex(),
                        agentsOffset + agents.getIdColumnIndex());

        final MutableIntKeyMap<String> texts = MutableIntKeyMap.empty();
        final MutableIntKeyMap<String> ruleTexts = MutableIntKeyMap.empty();
        final MutableIntPairMap ruleAgents = MutableIntPairMap.empty();
        final MutableIntPairMap ruledAccs = MutableIntPairMap.empty();

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int acc = row.get(0).toInt();
                final int rule = row.get(1).toInt();
                final int alphabet = row.get(2).toInt();
                final String text = row.get(3).toText();
                final int ruleAlphabet = row.get(4).toInt();
                final String ruleText = row.get(5).toText();
                final int agent = row.get(6).toInt();

                if (!ruleTexts.keySet().contains(rule)) {
                    ruleTexts.put(rule, ruleText);
                    texts.put(rule, text);
                    ruleAgents.put(rule, agent);
                    ruledAccs.put(rule, acc);
                }
                else {
                    if (ruleAlphabet == preferredAlphabet) {
                        ruleTexts.put(rule, ruleText);
                    }

                    if (alphabet == preferredAlphabet) {
                        texts.put(rule, text);
                    }
                }
            }
        }

        final ImmutableList.Builder<MorphologyResult> builder = new ImmutableList.Builder<>();
        final int length = ruleAgents.size();
        for (int i = 0; i < length; i++) {
            builder.add(new MorphologyResult(ruleAgents.valueAt(i), ruledAccs.valueAt(i), ruleAgents.keyAt(i), ruleTexts.valueAt(i), texts.valueAt(i)));
        }

        return builder.build();
    }

    private static ImmutableIntSet intSetQuery(DbExporter.Database db, DbQuery query) {
        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                builder.add(dbResult.next().get(0).toInt());
            }
        }
        return builder.build();
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsTarget(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(agents, acceptations.getConceptColumnIndex(), agents.getTargetBunchColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(acceptations.columns().size() + agents.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsSource(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;

        final int bunchSetsOffset = acceptations.columns().size();
        final int agentsOffset = bunchSetsOffset + bunchSets.getSetIdColumnIndex();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(bunchSets, acceptations.getConceptColumnIndex(), bunchSets.getBunchColumnIndex())
                .join(agents, bunchSetsOffset + bunchSets.getSetIdColumnIndex(), agents.getSourceBunchSetColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(agentsOffset + agents.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsRule(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(agents, acceptations.getConceptColumnIndex(), agents.getRuleColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(acceptations.columns().size() + agents.getIdColumnIndex());
        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsProcessed(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.AgentSetsTable agentSets = LangbookDbSchema.Tables.agentSets;

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(agentSets, bunchAcceptations.getAgentSetColumnIndex(), agentSets.getSetIdColumnIndex())
                .where(bunchAcceptations.getAcceptationColumnIndex(), staticAcceptation)
                .select(bunchAcceptations.columns().size() + agentSets.getAgentColumnIndex());

        return intSetQuery(db, query).remove(agents.nullReference());
    }

    public interface InvolvedAgentResultFlags {
        int target = 1;
        int source = 2;
        int diff = 4;
        int rule = 8;
        int processed = 16;
    }

    /**
     * Return information about all agents involved with the given acceptation.
     * @param db Readable database to be used
     * @param staticAcceptation Identifier for the acceptation to analyze.
     * @return a map whose keys are agent identifier and values are flags. Flags should match the values at {@link InvolvedAgentResultFlags}
     */
    private static ImmutableIntPairMap readAcceptationInvolvedAgents(DbExporter.Database db, int staticAcceptation) {
        final MutableIntPairMap flags = MutableIntPairMap.empty();

        for (int agentId : readAgentsWhereAcceptationIsTarget(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResultFlags.target);
        }

        for (int agentId : readAgentsWhereAcceptationIsSource(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResultFlags.source);
        }

        // TODO: Diff not implemented as right now it is impossible

        for (int agentId : readAgentsWhereAcceptationIsRule(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResultFlags.rule);
        }

        for (int agentId : readAgentsWhereAcceptationIsProcessed(db, staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | InvolvedAgentResultFlags.processed);
        }

        return flags.toImmutable();
    }

    static boolean isSymbolArrayPresent(DbExporter.Database db, int symbolArray) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), symbolArray)
                .select(table.getIdColumnIndex());

        return selectExistingRow(db, query);
    }

    static boolean isLanguagePresent(DbExporter.Database db, int language) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), language)
                .select(table.getIdColumnIndex());

        return selectExistingRow(db, query);
    }

    public static boolean isAnyLanguagePresent(DbExporter.Database db) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(db, query);
    }

    public static boolean isAlphabetPresent(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .select(table.getIdColumnIndex());

        return selectExistingRow(db, query);
    }

    static boolean areAllAlphabetsFromSameLanguage(DbExporter.Database db, IntSet alphabets) {
        final Integer language = getLanguageFromAlphabet(db, alphabets.valueAt(0));
        if (language == null) {
            return false;
        }

        final int size = alphabets.size();
        for (int i = 1; i < size; i++) {
            final Integer lang = getLanguageFromAlphabet(db, alphabets.valueAt(i));
            if (lang == null || language.intValue() != lang.intValue()) {
                return false;
            }
        }

        return true;
    }

    static ImmutableIntSet alphabetsWithinLanguage(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final int offset = table.columns().size();
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getLanguageColumnIndex(), table.getLanguageColumnIndex())
                .where(table.getIdColumnIndex(), alphabet)
                .select(offset + table.getIdColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                builder.add(dbResult.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static Integer getLanguageFromAlphabet(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .select(table.getLanguageColumnIndex());

        return selectOptionalFirstIntColumn(db, query);
    }

    private static IdentifiableResult readLanguageFromAlphabet(DbExporter.Database db, int alphabet, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(alphabets)
                .join(acceptations, alphabets.getLanguageColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(alphabets.getIdColumnIndex(), alphabet)
                .select(
                        accOffset + acceptations.getConceptColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex()
                );

        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                int lang = row.get(0).toInt();
                int firstAlphabet = row.get(1).toInt();
                String text = row.get(2).toText();
                while (firstAlphabet != preferredAlphabet && dbResult.hasNext()) {
                    row = dbResult.next();
                    if (row.get(1).toInt() == preferredAlphabet) {
                        lang = row.get(0).toInt();
                        firstAlphabet = preferredAlphabet;
                        text = row.get(2).toText();
                    }
                }

                return new IdentifiableResult(lang, text);
            }
        }

        throw new IllegalArgumentException("alphabet " + alphabet + " not found");
    }

    public static ImmutablePair<ImmutableIntKeyMap<String>, Integer> readAcceptationTextsAndLanguage(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabetsTable = alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .join(alphabetsTable, table.getStringAlphabetColumnIndex(), alphabetsTable.getIdColumnIndex())
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(
                        table.getStringAlphabetColumnIndex(),
                        table.getStringColumnIndex(),
                        table.columns().size() + alphabetsTable.getLanguageColumnIndex());
        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        boolean languageSet = false;
        int language = 0;
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int alphabet = row.get(0).toInt();
                final String text = row.get(1).toText();

                if (!languageSet) {
                    language = row.get(2).toInt();
                    languageSet = true;
                }
                else if (row.get(2).toInt() != language) {
                    throw new AssertionError();
                }

                builder.put(alphabet, text);
            }
        }

        return new ImmutablePair<>(builder.build(), language);
    }

    static ImmutablePair<ImmutableIntKeyMap<String>, Integer> readAcceptationTextsAndMain(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(
                        table.getStringAlphabetColumnIndex(),
                        table.getStringColumnIndex(),
                        table.getMainAcceptationColumnIndex());
        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        boolean mainAccSet = false;
        int mainAcc = 0;
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int alphabet = row.get(0).toInt();
                final String text = row.get(1).toText();

                if (!mainAccSet) {
                    mainAcc = row.get(2).toInt();
                    mainAccSet = true;
                }
                else if (row.get(2).toInt() != mainAcc) {
                    throw new AssertionError();
                }

                builder.put(alphabet, text);
            }
        }

        return new ImmutablePair<>(builder.build(), mainAcc);
    }

    private static String readAcceptationMainText(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getMainStringColumnIndex());

        return selectFirstRow(db, query).get(0).toText();
    }

    public static String readConceptText(DbExporter.Database db, int concept, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations; // J0
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int j1Offset = acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(strings, acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(j1Offset + strings.getStringAlphabetColumnIndex(), j1Offset + strings.getStringColumnIndex());

        String text;
        try (DbResult result = db.select(query)) {
            List<DbValue> row = result.next();
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

        return text;
    }

    public static ImmutableMap<TableCellReference, TableCellValue> readTableContent(DbExporter.Database db, int dynamicAcceptation, int preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int ruledAccOffset1 = ruledAcceptations.columns().size();
        final int agentOffset1 = ruledAccOffset1 + ruledAccOffset1;
        final int agentOffset2 = agentOffset1 + agents.columns().size();
        final int ruledAccOffset2 = agentOffset2 + agents.columns().size();
        final int strOffset = ruledAccOffset2 + ruledAcceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                .join(ruledAcceptations, ruledAcceptations.getAcceptationColumnIndex(), ruledAcceptations.getAcceptationColumnIndex())
                .join(agents, ruledAccOffset1 + ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(agents, agentOffset1 + agents.getRuleColumnIndex(), agents.getRuleColumnIndex())
                .join(ruledAcceptations, agentOffset2 + agents.getIdColumnIndex(), ruledAcceptations.getAgentColumnIndex())
                .join(strings, ruledAccOffset2 + ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(ruledAcceptations.getIdColumnIndex(), dynamicAcceptation)
                .select(agentOffset2 + agents.getSourceBunchSetColumnIndex(),
                        agentOffset1 + agents.getRuleColumnIndex(),
                        ruledAccOffset2 + ruledAcceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        strOffset + strings.getMainAcceptationColumnIndex());

        final MutableMap<TableCellReference, TableCellValue> resultMap = MutableHashMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final TableCellReference ref = new TableCellReference(row.get(0).toInt(), row.get(1).toInt());

                final int dynamicAcc = row.get(2).toInt();
                final int alphabet = row.get(3).toInt();
                final String text = row.get(4).toText();
                final int staticAcc = row.get(5).toInt();

                TableCellValue cellValue = resultMap.get(ref, null);
                if (cellValue == null || alphabet == preferredAlphabet) {
                    resultMap.put(ref, new TableCellValue(staticAcc, dynamicAcc, text));
                }
            }
        }

        return resultMap.toImmutable();
    }

    public static DisplayableItem readConceptAcceptationAndText(DbExporter.Database db, int concept, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations; // J0
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int j1Offset = acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(strings, acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(j1Offset + strings.getStringAlphabetColumnIndex(),
                        acceptations.getIdColumnIndex(),
                        j1Offset + strings.getStringColumnIndex());

        int acceptation;
        String text;
        try (DbResult result = db.select(query)) {
            List<DbValue> row = result.next();
            int firstAlphabet = row.get(0).toInt();
            acceptation = row.get(1).toInt();
            text = row.get(2).toText();
            while (firstAlphabet != preferredAlphabet && result.hasNext()) {
                row = result.next();
                if (row.get(0).toInt() == preferredAlphabet) {
                    firstAlphabet = preferredAlphabet;
                    acceptation = row.get(1).toInt();
                    text = row.get(2).toText();
                }
            }
        }

        return new DisplayableItem(acceptation, text);
    }

    public static ImmutableList<DisplayableItem> readBunchSetAcceptationsAndTexts(DbExporter.Database db, int bunchSet, int preferredAlphabet) {
        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = bunchSets.columns().size();
        final int stringsOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchSets)
                .join(acceptations, bunchSets.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchSets.getSetIdColumnIndex(), bunchSet)
                .select(
                        bunchSets.getBunchColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        stringsOffset + strings.getStringAlphabetColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex()
                );

        ImmutableList.Builder<DisplayableItem> builder = new ImmutableList.Builder<>();
        try (DbResult cursor = db.select(query)) {
            if (cursor.hasNext()) {
                List<DbValue> row = cursor.next();
                int bunch = row.get(0).toInt();
                int acc = row.get(1).toInt();
                int alphabet = row.get(2).toInt();
                String text = row.get(3).toText();

                while (cursor.hasNext()) {
                    row = cursor.next();
                    if (bunch == row.get(0).toInt()) {
                        if (alphabet != preferredAlphabet && row.get(2).toInt() == preferredAlphabet) {
                            acc = row.get(1).toInt();
                            text = row.get(3).toText();
                            alphabet = preferredAlphabet;
                        }
                    }
                    else {
                        builder.add(new DisplayableItem(acc, text));

                        bunch = row.get(0).toInt();
                        acc = row.get(1).toInt();
                        alphabet = row.get(2).toInt();
                        text = row.get(3).toText();
                    }
                }

                builder.add(new DisplayableItem(acc, text));
            }
        }

        return builder.build();
    }

    private static ImmutableList<DynamizableResult> readBunchesWhereAcceptationIsIncluded(DbExporter.Database db, int acceptation, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = bunchAcceptations.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(acceptations, bunchAcceptations.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(bunchAcceptations.getBunchColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        bunchAcceptations.getAgentSetColumnIndex());

        final MutableIntKeyMap<DynamizableResult> resultMap = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            final int nullAgentSet = LangbookDbSchema.Tables.agentSets.nullReference();
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                final int alphabet = row.get(2).toInt();

                if (alphabet == preferredAlphabet || !resultMap.keySet().contains(bunch)) {
                    final int acc = row.get(1).toInt();
                    final String text = row.get(3).toText();
                    final int agentSet = row.get(4).toInt();
                    final boolean dynamic = agentSet != nullAgentSet;

                    resultMap.put(bunch, new DynamizableResult(acc, dynamic, text));
                }
            }
        }

        final ImmutableList.Builder<DynamizableResult> builder = new ImmutableList.Builder<>();
        for (DynamizableResult r : resultMap) {
            builder.add(r);
        }

        return builder.build();
    }

    private static ImmutableList<DynamizableResult> readAcceptationBunchChildren(DbExporter.Database db, int acceptation, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int bunchAccOffset = acceptations.columns().size();
        final int strOffset = bunchAccOffset + bunchAcceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(bunchAcceptations, acceptations.getConceptColumnIndex(), bunchAcceptations.getBunchColumnIndex())
                .join(strings, bunchAccOffset + bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(bunchAccOffset + bunchAcceptations.getAcceptationColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        bunchAccOffset + bunchAcceptations.getAgentSetColumnIndex());

        final MutableIntKeyMap<DynamizableResult> resultMap = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            final int nullAgentSet = LangbookDbSchema.Tables.agentSets.nullReference();
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int acc = row.get(0).toInt();
                final int alphabet = row.get(1).toInt();

                if (alphabet == preferredAlphabet || resultMap.get(acc, null) == null) {
                    final String text = row.get(2).toText();
                    final int agentSet = row.get(3).toInt();
                    final boolean dynamic = agentSet != nullAgentSet;
                    resultMap.put(acc, new DynamizableResult(acc, dynamic, text));
                }
            }
        }

        return resultMap.toList().toImmutable();
    }

    public static ImmutableIntKeyMap<String> readAllAlphabets(DbExporter.Database db, int preferredAlphabet) {
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .orderBy(alphabets.getIdColumnIndex()) // I do not understand why, but it seems to be required to ensure order
                .select(alphabets.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int alphabet = row.get(0).toInt();
                String text = row.get(2).toText();

                while (result.hasNext()) {
                    row = result.next();
                    if (alphabet == row.get(0).toInt()) {
                        if (row.get(1).toInt() == preferredAlphabet) {
                            text = row.get(2).toText();
                        }
                    }
                    else {
                        builder.put(alphabet, text);

                        alphabet = row.get(0).toInt();
                        text = row.get(2).toText();
                    }
                }

                builder.put(alphabet, text);
            }
        }

        return builder.build();
    }

    public static ImmutableIntKeyMap<String> readAlphabetsForLanguage(DbExporter.Database db, int language, int preferredAlphabet) {
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable stringQueries = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(stringQueries, accOffset + acceptations.getIdColumnIndex(), stringQueries.getDynamicAcceptationColumnIndex())
                .where(alphabets.getLanguageColumnIndex(), language)
                .select(
                   alphabets.getIdColumnIndex(),
                        strOffset + stringQueries.getStringAlphabetColumnIndex(),
                        strOffset + stringQueries.getStringColumnIndex());
        final MutableIntSet foundAlphabets = MutableIntArraySet.empty();
        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        try (DbResult r = db.select(query)) {
            while (r.hasNext()) {
                final List<DbValue> row = r.next();
                final int id = row.get(0).toInt();
                final int strAlphabet = row.get(1).toInt();

                if (strAlphabet == preferredAlphabet || !foundAlphabets.contains(id)) {
                    foundAlphabets.add(id);
                    result.put(id, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    static int readMainAlphabetFromAlphabet(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.AlphabetsTable alpTable = alphabets;
        final LangbookDbSchema.LanguagesTable langTable = LangbookDbSchema.Tables.languages;
        final DbQuery mainAlphableQuery = new DbQuery.Builder(alpTable)
                .join(langTable, alpTable.getLanguageColumnIndex(), langTable.getIdColumnIndex())
                .where(alpTable.getIdColumnIndex(), alphabet)
                .select(alpTable.columns().size() + langTable.getMainAlphabetColumnIndex());
        return selectSingleRow(db, mainAlphableQuery).get(0).toInt();
    }

    public static ImmutableIntKeyMap<String> readAllLanguages(DbExporter.Database db, int preferredAlphabet) {
        final LangbookDbSchema.LanguagesTable languages = LangbookDbSchema.Tables.languages;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable stringQueries = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = languages.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(languages)
                .join(acceptations, languages.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(stringQueries, accOffset + acceptations.getIdColumnIndex(), stringQueries.getDynamicAcceptationColumnIndex())
                .select(
                        languages.getIdColumnIndex(),
                        strOffset + stringQueries.getStringAlphabetColumnIndex(),
                        strOffset + stringQueries.getStringColumnIndex()
                );

        MutableIntSet foundLanguages = MutableIntArraySet.empty();
        MutableIntKeyMap<String> result = MutableIntKeyMap.empty();

        try (DbResult r = db.select(query)) {
            while (r.hasNext()) {
                List<DbValue> row = r.next();
                final int lang = row.get(0).toInt();
                final int alphabet = row.get(1).toInt();

                if (alphabet == preferredAlphabet || !foundLanguages.contains(lang)) {
                    foundLanguages.add(lang);
                    result.put(lang, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private static ImmutableIntSet readAllAcceptations(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueMatch(strings.getMainAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(strings.getDynamicAcceptationColumnIndex());

        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAllAcceptationsInBunch(DbExporter.Database db, int alphabet, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(strings, bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(bunchAcceptations.columns().size() + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(bunchAcceptations.getAcceptationColumnIndex());

        return intSetQuery(db, query);
    }

    public static ImmutableIntKeyMap<String> readAllRules(DbExporter.Database db, int preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.RuledConceptsTable ruledConcepts = LangbookDbSchema.Tables.ruledConcepts;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = ruledConcepts.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(ruledConcepts)
                .join(acceptations, ruledConcepts.getRuleColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .groupBy(ruledConcepts.getRuleColumnIndex())
                .select(ruledConcepts.getRuleColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                final int rule = row.get(0).toInt();

                if (result.get(rule, null) == null || row.get(1).toInt() == preferredAlphabet) {
                    result.put(rule, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private static String readSameConceptTexts(DbExporter.Database db, int acceptation, int alphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = acceptations.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueDiffer(acceptations.getIdColumnIndex(), accOffset + acceptations.getIdColumnIndex())
                .select(strOffset + strings.getStringColumnIndex());

        return db.select(query)
                .map(row -> row.get(0).toText())
                .reduce((a, b) -> a + ", " + b);
    }

    private static String readApplyRuleText(DbExporter.Database db, int acceptation, QuestionFieldDetails field) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int strOffset = ruledAcceptations.columns().size();
        final int agentsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .where(agentsOffset + agents.getRuleColumnIndex(), field.rule)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), field.alphabet)
                .select(strOffset + strings.getStringColumnIndex());
        return selectSingleRow(db, query).get(0).toText();
    }

    public static String readQuestionFieldText(DbExporter.Database db, int acceptation, QuestionFieldDetails field) {
        switch (field.getType()) {
            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC:
                return getAcceptationText(db, acceptation, field.alphabet);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return readSameConceptTexts(db, acceptation, field.alphabet);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_APPLY_RULE:
                return readApplyRuleText(db, acceptation, field);
        }

        throw new UnsupportedOperationException("Unsupported question field type");
    }

    public static Progress readQuizProgress(DbExporter.Database db, int quizId) {
        final LangbookDbSchema.KnowledgeTable knowledge = LangbookDbSchema.Tables.knowledge;

        final DbQuery query = new DbQuery.Builder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getScoreColumnIndex());

        final int[] progress = new int[MAX_ALLOWED_SCORE - MIN_ALLOWED_SCORE + 1];
        int numberOfQuestions = 0;

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                numberOfQuestions++;
                final int score = dbResult.next().get(0).toInt();
                if (score != NO_SCORE) {
                    progress[score - MIN_ALLOWED_SCORE]++;
                }
            }
        }

        return new Progress(ImmutableIntList.from(progress), numberOfQuestions);
    }

    public static ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails>> readQuizSelectorEntriesForBunch(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = LangbookDbSchema.Tables.quizDefinitions;
        final LangbookDbSchema.QuestionFieldSets fieldSets = LangbookDbSchema.Tables.questionFieldSets;

        final int offset = quizzes.columns().size();
        final DbQuery query = new DbQuery.Builder(quizzes)
                .join(fieldSets, quizzes.getQuestionFieldsColumnIndex(), fieldSets.getSetIdColumnIndex())
                .where(quizzes.getBunchColumnIndex(), bunch)
                .select(
                        quizzes.getIdColumnIndex(),
                        offset + fieldSets.getAlphabetColumnIndex(),
                        offset + fieldSets.getRuleColumnIndex(),
                        offset + fieldSets.getFlagsColumnIndex());

        final MutableIntKeyMap<ImmutableSet<QuestionFieldDetails>> resultMap = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int quizId = row.get(0).toInt();
                final QuestionFieldDetails field = new QuestionFieldDetails(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt());
                final ImmutableSet<QuestionFieldDetails> set = resultMap.get(quizId, ImmutableHashSet.empty());
                resultMap.put(quizId, set.add(field));
            }
        }

        return resultMap.toImmutable();
    }

    private static boolean checkMatching(ImmutableIntKeyMap<String> startMatcher, ImmutableIntKeyMap<String> endMatcher, ImmutableIntKeyMap<String> texts) {
        for (IntKeyMap.Entry<String> entry : startMatcher.entries()) {
            final String text = texts.get(entry.key(), null);
            if (text == null || !text.startsWith(entry.value())) {
                return false;
            }
        }

        for (IntKeyMap.Entry<String> entry : endMatcher.entries()) {
            final String text = texts.get(entry.key(), null);
            if (text == null || !text.endsWith(entry.value())) {
                return false;
            }
        }

        return !startMatcher.isEmpty() || !endMatcher.isEmpty();
    }

    private static ImmutableIntSet readBunchesFromSetOfBunchSets(DbExporter.Database db, ImmutableIntSet bunchSets) {
        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        for (int bunchSet : bunchSets) {
            for (int bunch : getBunchSet(db, bunchSet)) {
                builder.add(bunch);
            }
        }

        return builder.build();
    }

    /**
     * Check all bunches including agents that may match the given texts.
     *
     * For simplicity, this will only pick bunches declared as source bunches
     * in agents that are applying a rule and has no diff bunches nor target bunch.
     *
     * Required conditions are:
     * <li>The agent's matchers must match the given string</li>
     * <li>Start or end matcher must not be empty</li>
     * <li>There must be an adder different from the matcher</li>
     *
     * @param db Database to be used
     * @param texts Map containing the word to be matched. Keys in the map are alphabets and values are the text on those alphabets.
     * @param preferredAlphabet User's defined alphabet.
     * @return A map whose keys are bunches (concepts) and value are the suitable way to represent that bunch, according to the given preferred alphabet.
     */
    public static ImmutableIntKeyMap<String> readAllMatchingBunches(DbExporter.Database db, ImmutableIntKeyMap<String> texts, int preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getTargetBunchColumnIndex(), NO_BUNCH)
                .where(agents.getDiffBunchSetColumnIndex(), 0)
                .select(agents.getSourceBunchSetColumnIndex(),
                        agents.getStartMatcherColumnIndex(),
                        agents.getEndMatcherColumnIndex());

        final SyncCacheIntKeyNonNullValueMap<ImmutableIntKeyMap<String>> cachedCorrelations =
                new SyncCacheIntKeyNonNullValueMap<>(id -> getCorrelationWithText(db, id));
        final ImmutableIntSet.Builder validBunchSetsBuilder = new ImmutableIntSetCreator();

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                int bunchSet = row.get(0).toInt();
                int startMatcherId = row.get(1).toInt();
                int endMatcherId = row.get(2).toInt();

                final ImmutableIntKeyMap<String> startMatcher = cachedCorrelations.get(startMatcherId);
                final ImmutableIntKeyMap<String> endMatcher = cachedCorrelations.get(endMatcherId);
                if (checkMatching(startMatcher, endMatcher, texts)) {
                    validBunchSetsBuilder.add(bunchSet);
                }
            }
        }

        final ImmutableIntSet bunches = readBunchesFromSetOfBunchSets(db, validBunchSetsBuilder.build());
        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        for (int bunch : bunches) {
            builder.put(bunch, readConceptText(db, bunch, preferredAlphabet));
        }

        return builder.build();
    }

    private static ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptations(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int strOffset = acceptations.columns().size() * 2;
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, acceptations.columns().size() + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueDiffer(acceptations.getIdColumnIndex(), acceptations.columns().size() + acceptations.getIdColumnIndex())
                .select(acceptations.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptationsInBunch(DbExporter.Database db, int alphabet, int bunch) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset1 = bunchAcceptations.columns().size();
        final int accOffset2 = accOffset1 + acceptations.columns().size();
        final int strOffset = accOffset2 + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(acceptations, bunchAcceptations.getAcceptationColumnIndex(), acceptations.getIdColumnIndex())
                .join(acceptations, accOffset1 + acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset2 + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueDiffer(accOffset1 + acceptations.getIdColumnIndex(), accOffset2 + acceptations.getIdColumnIndex())
                .select(accOffset1 + acceptations.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAllRulableAcceptations(DbExporter.Database db, int alphabet, int rule) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final int agentOffset = ruledAcceptations.columns().size();
        final int strOffset = agentOffset + agents.columns().size();
        final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .where(agentOffset + agents.getRuleColumnIndex(), rule)
                .select(ruledAcceptations.getAcceptationColumnIndex());

        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAllRulableAcceptationsInBunch(DbExporter.Database db, int alphabet, int rule, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final int ruledAccOffset = bunchAcceptations.columns().size();
        final int agentOffset = ruledAccOffset + ruledAcceptations.columns().size();
        final int strOffset = agentOffset + agents.columns().size();
        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(ruledAcceptations, bunchAcceptations.getAcceptationColumnIndex(), ruledAcceptations. getAcceptationColumnIndex())
                .join(agents, ruledAccOffset + ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(strings, ruledAccOffset + ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .where(agentOffset + agents.getRuleColumnIndex(), rule)
                .select(bunchAcceptations.getAcceptationColumnIndex());

        return intSetQuery(db, query);
    }

    private static ImmutableIntSet readAllPossibleAcceptationForField(DbExporter.Database db, int bunch, QuestionFieldDetails field) {
        switch (field.getType()) {
            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC:
                return (bunch == NO_BUNCH)? readAllAcceptations(db, field.alphabet) : readAllAcceptationsInBunch(db, field.alphabet, bunch);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return (bunch == NO_BUNCH)? readAllPossibleSynonymOrTranslationAcceptations(db, field.alphabet) :
                        readAllPossibleSynonymOrTranslationAcceptationsInBunch(db, field.alphabet, bunch);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_APPLY_RULE:
                return (bunch == NO_BUNCH)? readAllRulableAcceptations(db, field.alphabet, field.rule) :
                        readAllRulableAcceptationsInBunch(db, field.alphabet, field.rule, bunch);

            default:
                throw new AssertionError();
        }
    }

    static ImmutableIntSet readAllPossibleAcceptations(DbExporter.Database db, int bunch, ImmutableSet<QuestionFieldDetails> fields) {
        final Function<QuestionFieldDetails, ImmutableIntSet> mapFunc = field -> readAllPossibleAcceptationForField(db, bunch, field);
        return fields.map(mapFunc).reduce((a,b) -> a.filter(b::contains));
    }

    static ImmutableIntSet getAllRuledAcceptationsForAgent(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getIdColumnIndex());

        return intSetQuery(db, query);
    }

    static ImmutableIntPairMap getAgentProcessedMap(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getAcceptationColumnIndex(), table.getIdColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    static ImmutableIntKeyMap<ImmutableIntSet> getAllAgentSetsContaining(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getAgentColumnIndex());

        final ImmutableIntKeyMap.Builder<ImmutableIntSet> mapBuilder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int setId = row.get(0).toInt();
                ImmutableIntSet.Builder setBuilder = new ImmutableIntSetCreator();
                setBuilder.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    int newSetId = row.get(0).toInt();
                    if (newSetId != setId) {
                        mapBuilder.put(setId, setBuilder.build());
                        setId = newSetId;
                        setBuilder = new ImmutableIntSetCreator();
                    }
                    setBuilder.add(row.get(1).toInt());
                }

                mapBuilder.put(setId, setBuilder.build());
            }
        }

        return mapBuilder.build();
    }

    public static final class AgentRegister {
        public final int targetBunch;
        public final int sourceBunchSetId;
        public final int diffBunchSetId;
        public final int startMatcherId;
        public final int startAdderId;
        public final int endMatcherId;
        public final int endAdderId;
        public final int rule;

        public AgentRegister(int targetBunch, int sourceBunchSetId, int diffBunchSetId,
                int startMatcherId, int startAdderId, int endMatcherId, int endAdderId, int rule) {

            if (startMatcherId == startAdderId && endMatcherId == endAdderId) {
                if (targetBunch == 0 || rule != 0) {
                    throw new IllegalArgumentException();
                }
            }
            else if (rule == 0) {
                throw new IllegalArgumentException();
            }

            this.targetBunch = targetBunch;
            this.sourceBunchSetId = sourceBunchSetId;
            this.diffBunchSetId = diffBunchSetId;
            this.startMatcherId = startMatcherId;
            this.startAdderId = startAdderId;
            this.endMatcherId = endMatcherId;
            this.endAdderId = endAdderId;
            this.rule = rule;
        }
    }

    public static AgentRegister getAgentRegister(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), agentId)
                .select(table.getTargetBunchColumnIndex(),
                        table.getSourceBunchSetColumnIndex(),
                        table.getDiffBunchSetColumnIndex(),
                        table.getStartMatcherColumnIndex(),
                        table.getStartAdderColumnIndex(),
                        table.getEndMatcherColumnIndex(),
                        table.getEndAdderColumnIndex(),
                        table.getRuleColumnIndex());
        final List<DbValue> agentRow = selectSingleRow(db, query);

        final int sourceBunchSetId = agentRow.get(1).toInt();
        final int diffBunchSetId = agentRow.get(2).toInt();
        final int startMatcherId = agentRow.get(3).toInt();
        final int startAdderId = agentRow.get(4).toInt();
        final int endMatcherId = agentRow.get(5).toInt();
        final int endAdderId = agentRow.get(6).toInt();
        return new AgentRegister(agentRow.get(0).toInt(), sourceBunchSetId, diffBunchSetId,
                startMatcherId, startAdderId, endMatcherId, endAdderId, agentRow.get(7).toInt());
    }

    static AgentDetails getAgentDetails(DbExporter.Database db, int agentId) {
        final AgentRegister register = getAgentRegister(db, agentId);
        final ImmutableIntSet sourceBunches = getBunchSet(db, register.sourceBunchSetId);
        final ImmutableIntSet diffBunches = (register.sourceBunchSetId != register.diffBunchSetId)?
                getBunchSet(db, register.diffBunchSetId) : sourceBunches;

        final SyncCacheIntKeyNonNullValueMap<ImmutableIntKeyMap<String>> correlationCache =
                new SyncCacheIntKeyNonNullValueMap<>(id -> getCorrelationWithText(db, id));
        final ImmutableIntKeyMap<String> startMatcher = correlationCache.get(register.startMatcherId);
        final ImmutableIntKeyMap<String> startAdder = correlationCache.get(register.startAdderId);
        final ImmutableIntKeyMap<String> endMatcher = correlationCache.get(register.endMatcherId);
        final ImmutableIntKeyMap<String> endAdder = correlationCache.get(register.endAdderId);

        return new AgentDetails(register.targetBunch, sourceBunches, diffBunches,
                startMatcher, startAdder, endMatcher, endAdder, register.rule);
    }

    public static QuizDetails getQuizDetails(DbExporter.Database db, int quizId) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = LangbookDbSchema.Tables.quizDefinitions;
        final LangbookDbSchema.QuestionFieldSets questions = LangbookDbSchema.Tables.questionFieldSets;
        final int offset = quizzes.columns().size();
        final DbQuery query = new DbQuery.Builder(quizzes)
                .join(questions, quizzes.getQuestionFieldsColumnIndex(), questions.getSetIdColumnIndex())
                .where(quizzes.getIdColumnIndex(), quizId)
                .select(quizzes.getBunchColumnIndex(),
                        offset + questions.getAlphabetColumnIndex(),
                        offset + questions.getRuleColumnIndex(),
                        offset + questions.getFlagsColumnIndex());

        final ImmutableList.Builder<QuestionFieldDetails> builder = new ImmutableList.Builder<>();
        int bunch = 0;

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                bunch = row.get(0).toInt();
                builder.add(new QuestionFieldDetails(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt()));
            }
        }

        final ImmutableList<QuestionFieldDetails> fields = builder.build();
        return fields.isEmpty()? null : new QuizDetails(bunch, fields);
    }

    public static ImmutableIntPairMap getCurrentKnowledge(DbExporter.Database db, int quizId) {
        final LangbookDbSchema.KnowledgeTable table = LangbookDbSchema.Tables.knowledge;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .select(table.getAcceptationColumnIndex(), table.getScoreColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    private static ImmutableIntKeyMap<String> getSampleSentences(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final DbQuery query = new DbQuery.Builder(spans)
                .where(spans.getAcceptation(), staticAcceptation)
                .select(spans.getSymbolArray());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final int symbolArrayId = dbResult.next().get(0).toInt();
                builder.put(symbolArrayId, getSymbolArray(db, symbolArrayId));
            }
        }

        return builder.build();
    }

    public static AcceptationDetailsModel getAcceptationsDetails(
            DbExporter.Database db, int staticAcceptation, int preferredAlphabet) {
        final int concept = conceptFromAcceptation(db, staticAcceptation);
        if (concept == 0) {
            return null;
        }

        ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntKeyMap<String>>> correlationResultPair = getAcceptationCorrelations(db, staticAcceptation);
        final int givenAlphabet = correlationResultPair.right.get(correlationResultPair.left.get(0)).keyAt(0);
        final IdentifiableResult languageResult = readLanguageFromAlphabet(db, givenAlphabet, preferredAlphabet);
        final MutableIntKeyMap<String> languageStrs = MutableIntKeyMap.empty();
        languageStrs.put(languageResult.id, languageResult.text);

        final IdentifiableResult definition = readSupertypeFromAcceptation(db, staticAcceptation, preferredAlphabet);
        final ImmutableIntKeyMap<String> subtypes = readSubtypesFromAcceptation(db, staticAcceptation, preferredAlphabet);
        final ImmutableIntKeyMap<SynonymTranslationResult> synonymTranslationResults =
                readAcceptationSynonymsAndTranslations(db, staticAcceptation);
        for (IntKeyMap.Entry<SynonymTranslationResult> entry : synonymTranslationResults.entries()) {
            final int language = entry.value().language;
            if (languageStrs.get(language, null) == null) {
                languageStrs.put(language, readConceptText(db, language, preferredAlphabet));
            }
        }

        final ImmutableList<DynamizableResult> bunchesWhereAcceptationIsIncluded = readBunchesWhereAcceptationIsIncluded(db, staticAcceptation, preferredAlphabet);
        final ImmutableList<MorphologyResult> morphologyResults = readMorphologiesFromAcceptation(db, staticAcceptation, preferredAlphabet);
        final ImmutableList<DynamizableResult> bunchChildren = readAcceptationBunchChildren(db, staticAcceptation, preferredAlphabet);
        final ImmutableIntPairMap involvedAgents = readAcceptationInvolvedAgents(db, staticAcceptation);

        final ImmutableIntKeyMap.Builder<String> supertypesBuilder = new ImmutableIntKeyMap.Builder<>();
        if (definition != null) {
            supertypesBuilder.put(definition.id, definition.text);
        }

        final ImmutableIntKeyMap<String> sampleSentences = getSampleSentences(db, staticAcceptation);
        return new AcceptationDetailsModel(concept, languageResult, correlationResultPair.left,
                correlationResultPair.right, supertypesBuilder.build(), subtypes,
                synonymTranslationResults, bunchChildren, bunchesWhereAcceptationIsIncluded,
                morphologyResults, involvedAgents, languageStrs.toImmutable(), sampleSentences);
    }

    public static CorrelationDetailsModel getCorrelationDetails(DbExporter.Database db, int correlationId, int preferredAlphabet) {
        final ImmutableIntKeyMap<String> correlation = getCorrelationWithText(db, correlationId);
        if (correlation.isEmpty()) {
            return null;
        }

        final ImmutableIntKeyMap<String> alphabets = readAllAlphabets(db, preferredAlphabet);
        final ImmutableIntKeyMap<String> acceptations = readAcceptationsIncludingCorrelation(db, correlationId, preferredAlphabet);

        final int entryCount = correlation.size();
        final MutableIntKeyMap<ImmutableIntSet> relatedCorrelationsByAlphabet = MutableIntKeyMap.empty();
        final MutableIntKeyMap<ImmutableIntKeyMap<String>> relatedCorrelations = MutableIntKeyMap.empty();

        for (int i = 0; i < entryCount; i++) {
            final int matchingAlphabet = correlation.keyAt(i);
            final ImmutableIntKeyMap<ImmutableIntKeyMap<String>> correlations = readCorrelationsWithSameSymbolArray(db, correlationId, matchingAlphabet);

            final int amount = correlations.size();
            final ImmutableIntSet.Builder setBuilder = new ImmutableIntSetCreator();
            for (int j = 0; j < amount; j++) {
                final int corrId = correlations.keyAt(j);
                if (relatedCorrelations.get(corrId, null) == null) {
                    relatedCorrelations.put(corrId, correlations.valueAt(j));
                }

                setBuilder.add(corrId);
            }
            relatedCorrelationsByAlphabet.put(matchingAlphabet, setBuilder.build());
        }

        return new CorrelationDetailsModel(alphabets, correlation, acceptations,
                relatedCorrelationsByAlphabet.toImmutable(), relatedCorrelations.toImmutable());
    }

    public static ImmutableSet<SentenceSpan> getSentenceSpans(DbExporter.Database db, int symbolArray) {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSymbolArray(), symbolArray)
                .select(table.getStart(), table.getLength(), table.getAcceptation());
        final ImmutableHashSet.Builder<SentenceSpan> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int start = row.get(0).toInt();
                final int length = row.get(1).toInt();
                final int acc = row.get(2).toInt();
                final ImmutableIntRange range = new ImmutableIntRange(start, start + length - 1);
                builder.add(new SentenceSpan(range, acc));
            }
        }

        return builder.build();
    }

    public static ImmutableIntValueMap<SentenceSpan> getSentenceSpansWithIds(DbExporter.Database db, int symbolArray) {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSymbolArray(), symbolArray)
                .select(table.getIdColumnIndex(), table.getStart(), table.getLength(), table.getAcceptation());
        final ImmutableIntValueHashMap.Builder<SentenceSpan> builder = new ImmutableIntValueHashMap.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int id = row.get(0).toInt();
                final int start = row.get(1).toInt();
                final int length = row.get(2).toInt();
                final int acc = row.get(3).toInt();
                final ImmutableIntRange range = new ImmutableIntRange(start, start + length - 1);
                builder.put(new SentenceSpan(range, acc), id);
            }
        }

        return builder.build();
    }

    public static Integer getStaticAcceptationFromDynamic(DbExporter.Database db, int dynamicAcceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .select(table.getMainAcceptationColumnIndex());

        boolean found = false;
        int value = 0;
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final int newValue = dbResult.next().get(0).toInt();
                if (!found) {
                    found = true;
                    value = newValue;
                }
                else if (value != newValue) {
                    throw new AssertionError();
                }
            }
        }

        return found? value : null;
    }

    public static ImmutableIntKeyMap<String> getAcceptationTexts(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getStringAlphabetColumnIndex(), table.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.put(row.get(0).toInt(), row.get(1).toText());
            }
        }

        return builder.build();
    }

    private static String getAcceptationText(DbExporter.Database db, int acceptation, int alphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getStringColumnIndex());
        return selectSingleRow(db, query).get(0).toText();
    }

    /**
     * Checks if the given symbolArray is not used neither as a correlation nor as a conversion,
     * and then it is merely a sentence.
     */
    public static boolean isSymbolArrayMerelyASentence(DbExporter.Database db, int symbolArrayId) {
        final LangbookDbSchema.CorrelationsTable corrTable = LangbookDbSchema.Tables.correlations;

        DbQuery query = new DbQuery.Builder(corrTable)
                .where(corrTable.getSymbolArrayColumnIndex(), symbolArrayId)
                .select(corrTable.getIdColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                return false;
            }
        }

        final LangbookDbSchema.ConversionsTable convTable = LangbookDbSchema.Tables.conversions;
        query = new DbQuery.Builder(convTable)
                .where(convTable.getSourceColumnIndex(), symbolArrayId)
                .select(convTable.getIdColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                return false;
            }
        }

        query = new DbQuery.Builder(convTable)
                .where(convTable.getTargetColumnIndex(), symbolArrayId)
                .select(convTable.getIdColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                return false;
            }
        }

        return true;
    }

    public static boolean checkAlphabetCanBeRemoved(DbExporter.Database db, int alphabet) {
        // There must be at least another alphabet in the same language to avoid leaving the language without alphabets
        if (alphabetsWithinLanguage(db, alphabet).size() < 2) {
            return false;
        }

        // For now, let's assume that a source alphabet cannot be removed while the conversion already exists
        if (getConversionsMap(db).contains(alphabet)) {
            return false;
        }

        // First, quizzes using this alphabet should be removed
        return !isAlphabetUsedInQuestions(db, alphabet);
    }

    static boolean isAlphabetUsedInQuestions(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.QuestionFieldSets table = LangbookDbSchema.Tables.questionFieldSets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAlphabetColumnIndex(), alphabet)
                .select(table.getIdColumnIndex());

        try (DbResult dbResult = db.select(query)) {
            return dbResult.hasNext();
        }
    }
}
