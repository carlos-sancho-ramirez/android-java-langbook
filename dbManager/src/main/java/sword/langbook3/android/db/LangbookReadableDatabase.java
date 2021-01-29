package sword.langbook3.android.db;

import sword.collections.Function;
import sword.collections.ImmutableHashMap;
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
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableIntValueMap;
import sword.collections.MutableList;
import sword.collections.MutableMap;
import sword.collections.MutableSet;
import sword.collections.Set;
import sword.collections.SortUtils;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbTable;
import sword.database.DbValue;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.AcceptationDetailsModel.InvolvedAgentResultFlags;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.CorrelationDetailsModel;
import sword.langbook3.android.models.DefinitionDetails;
import sword.langbook3.android.models.DerivedAcceptationResult;
import sword.langbook3.android.models.DerivedAcceptationsReaderResult;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.IdentifiableResult;
import sword.langbook3.android.models.MorphologyReaderResult;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.models.SynonymTranslationResult;
import sword.langbook3.android.models.TableCellReference;
import sword.langbook3.android.models.TableCellValue;

import static sword.langbook3.android.collections.EqualUtils.equal;
import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ID;
import static sword.langbook3.android.db.LangbookDbSchema.MAX_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NO_BUNCH;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NULL_CORRELATION_ARRAY_ID;
import static sword.langbook3.android.db.LangbookDbSchema.Tables.alphabets;

public final class LangbookReadableDatabase {

    private LangbookReadableDatabase() {
    }

    private static List<DbValue> selectSingleRow(DbExporter.Database db, DbQuery query) {
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

    private static List<DbValue> selectOptionalSingleRow(DbExporter.Database db, DbQuery query) {
        try (DbResult result = db.select(query)) {
            return result.hasNext()? result.next() : null;
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

    private static String selectOptionalFirstTextColumn(DbExporter.Database db, DbQuery query) {
        String result = null;
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                result = dbResult.next().get(0).toText();
            }

            if (dbResult.hasNext()) {
                throw new AssertionError("Only 0 or 1 row was expected");
            }
        }

        return result;
    }

    private static DbValue selectOptionalFirstDbValue(DbExporter.Database db, DbQuery query) {
        DbValue result = null;
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                result = dbResult.next().get(0);
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

    static <LanguageId extends LanguageIdInterface> ImmutableIntSet findCorrelationsByLanguage(DbExporter.Database db, LanguageId language) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final int offset = correlations.columns().size();
        final DbQuery query = new DbQueryBuilder(correlations)
                .join(alphabets, correlations.getAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .where(offset + alphabets.getLanguageColumnIndex(), language)
                .select(correlations.getCorrelationIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface, SymbolArrayId, CorrelationId> ImmutableMap<CorrelationId, SymbolArrayId> findCorrelationsAndSymbolArrayForAlphabet(DbExporter.Database db, IntSetter<SymbolArrayId> symbolArrayIdSetter, IntSetter<CorrelationId> correlationIdSetter, AlphabetId sourceAlphabet) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQueryBuilder(correlations)
                .where(correlations.getAlphabetColumnIndex(), sourceAlphabet)
                .select(correlations.getCorrelationIdColumnIndex(), correlations.getSymbolArrayColumnIndex());

        final ImmutableMap.Builder<CorrelationId, SymbolArrayId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final CorrelationId correlationId = correlationIdSetter.getKeyFromDbValue(row.get(0));
                final SymbolArrayId symbolArrayId = symbolArrayIdSetter.getKeyFromDbValue(row.get(1));
                builder.put(correlationId, symbolArrayId);
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

    static <LanguageId extends LanguageIdInterface> ImmutableIntSet findAcceptationsByLanguage(DbExporter.Database db, LanguageId language) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final int offset = strings.columns().size();
        final DbQuery query = new DbQueryBuilder(strings)
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

    static <LanguageId> ImmutableIntSet findBunchConceptsLinkedToJustThisLanguage(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, LanguageId language) {
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

        MutableIntKeyMap<ImmutableSet<LanguageId>> map = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                final LanguageId lang = languageIdSetter.getKeyFromDbValue(row.get(1));
                final ImmutableSet<LanguageId> set = map.get(bunch, ImmutableHashSet.empty()).add(lang);
                map.put(bunch, set);
            }
        }

        return map.filter(set -> set.contains(language) && set.size() == 1).keySet().toImmutable();
    }

    static <LanguageId> ImmutableSet<LanguageId> findIncludedAcceptationLanguages(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, int bunch) {
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

        return db.select(query).map(row -> languageIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    static <LanguageId> ImmutableIntSet findSuperTypesLinkedToJustThisLanguage(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, LanguageId language) {
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = LangbookDbSchema.Tables.complementedConcepts;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;

        final int accOffset = complementedConcepts.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final int alphabetsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(complementedConcepts)
                .join(acceptations, complementedConcepts.getBaseColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, strOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .groupBy(complementedConcepts.getBaseColumnIndex())
                .select(complementedConcepts.getBaseColumnIndex(), alphabetsOffset + alphabets.getLanguageColumnIndex());

        MutableIntKeyMap<ImmutableSet<LanguageId>> map = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                final LanguageId lang = languageIdSetter.getKeyFromDbValue(row.get(1));
                final ImmutableSet<LanguageId> set = map.get(bunch, ImmutableHashSet.empty()).add(lang);
                map.put(bunch, set);
            }
        }

        return map.filter(set -> set.contains(language) && set.size() == 1).keySet().toImmutable();
    }

    static <SymbolArrayId> SymbolArrayId findSymbolArray(DbExporter.Database db, IntSetter<SymbolArrayId> symbolArrayIdSetter, String str) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStrColumnIndex(), str)
                .select(table.getIdColumnIndex());

        try (DbResult result = db.select(query)) {
            final SymbolArrayId value = result.hasNext()? symbolArrayIdSetter.getKeyFromDbValue(result.next().get(0)) : null;
            if (result.hasNext()) {
                throw new AssertionError();
            }

            return value;
        }
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId> CorrelationId findCorrelation(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, IntSetter<CorrelationId> correlationIdSetter, Correlation<AlphabetId> correlation) {
        if (correlation.isEmpty()) {
            return null;
        }
        final ImmutableCorrelation<AlphabetId> immutableCorrelation = correlation.toImmutable();

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final int offset = table.columns().size();
        final int offset2 = offset + symbolArrays.columns().size();
        final int offset3 = offset2 + table.columns().size();

        final DbQuery query = new DbQueryBuilder(table)
                .join(symbolArrays, table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .join(table, table.getCorrelationIdColumnIndex(), table.getCorrelationIdColumnIndex())
                .join(symbolArrays, offset2 + table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(offset + symbolArrays.getStrColumnIndex(), immutableCorrelation.valueAt(0))
                .where(table.getAlphabetColumnIndex(), immutableCorrelation.keyAt(0))
                .select(
                        table.getCorrelationIdColumnIndex(),
                        offset2 + table.getAlphabetColumnIndex(),
                        offset3 + symbolArrays.getStrColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                CorrelationId correlationId = correlationIdSetter.getKeyFromDbValue(row.get(0));
                ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
                builder.put(alphabetIntSetter.getKeyFromDbValue(row.get(1)), row.get(2).toText());

                while (result.hasNext()) {
                    row = result.next();
                    CorrelationId newCorrelationId = correlationIdSetter.getKeyFromDbValue(row.get(0));
                    if (newCorrelationId != correlationId) {
                        if (builder.build().equals(immutableCorrelation)) {
                            return correlationId;
                        }

                        correlationId = newCorrelationId;
                        builder = new ImmutableCorrelation.Builder<>();
                    }

                    builder.put(alphabetIntSetter.getKeyFromDbValue(row.get(1)), row.get(2).toText());
                }

                if (builder.build().equals(immutableCorrelation)) {
                    return correlationId;
                }
            }
        }

        return null;
    }

    static <AlphabetId extends AlphabetIdInterface, SymbolArrayId extends SymbolArrayIdInterface, CorrelationId> CorrelationId findCorrelation(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, IntSetter<SymbolArrayId> symbolArrayIdSetter, IntSetter<CorrelationId> correlationIdSetter, Map<AlphabetId, SymbolArrayId> correlation) {
        if (correlation.size() == 0) {
            return correlationIdSetter.getKeyFromInt(EMPTY_CORRELATION_ID);
        }
        final ImmutableMap<AlphabetId, SymbolArrayId> corr = correlation.toImmutable();

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getCorrelationIdColumnIndex(), table.getCorrelationIdColumnIndex())
                .where(table.getSymbolArrayColumnIndex(), corr.valueAt(0))
                .where(table.getAlphabetColumnIndex(), corr.keyAt(0))
                .select(
                        table.getCorrelationIdColumnIndex(),
                        offset + table.getAlphabetColumnIndex(),
                        offset + table.getSymbolArrayColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                CorrelationId correlationId = correlationIdSetter.getKeyFromDbValue(row.get(0));
                final SymbolArrayId symbolArrayId = symbolArrayIdSetter.getKeyFromDbValue(row.get(2));
                ImmutableMap.Builder<AlphabetId, SymbolArrayId> builder = new ImmutableHashMap.Builder<>();
                builder.put(alphabetIntSetter.getKeyFromDbValue(row.get(1)), symbolArrayId);

                while (result.hasNext()) {
                    row = result.next();
                    final CorrelationId newCorrelationId = correlationIdSetter.getKeyFromDbValue(row.get(0));
                    if (!equal(newCorrelationId, correlationId)) {
                        if (builder.build().equals(corr)) {
                            return correlationId;
                        }

                        correlationId = newCorrelationId;
                        builder = new ImmutableHashMap.Builder<>();
                    }

                    final AlphabetId alphabet = alphabetIntSetter.getKeyFromDbValue(row.get(1));
                    final SymbolArrayId symbolArray = symbolArrayIdSetter.getKeyFromDbValue(row.get(2));
                    builder.put(alphabet, symbolArray);
                }

                if (builder.build().equalMap(corr)) {
                    return correlationId;
                }
            }
        }

        return null;
    }

    static <CorrelationId extends CorrelationIdInterface> Integer findCorrelationArray(DbImporter.Database db, IntSetter<CorrelationId> correlationIdSetter, List<CorrelationId> array) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getArrayIdColumnIndex(), table.getArrayIdColumnIndex())
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), array.get(0))
                .orderBy(table.getArrayIdColumnIndex(), offset + table.getArrayPositionColumnIndex())
                .select(table.getArrayIdColumnIndex(), offset + table.getCorrelationColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int arrayId = row.get(0).toInt();
                ImmutableList.Builder<CorrelationId> builder = new ImmutableList.Builder<>();
                builder.add(correlationIdSetter.getKeyFromDbValue(row.get(1)));

                while (result.hasNext()) {
                    row = result.next();
                    int newArrayId = row.get(0).toInt();
                    if (arrayId != newArrayId) {
                        if (builder.build().equalTraversable(array)) {
                            return arrayId;
                        }

                        arrayId = newArrayId;
                        builder = new ImmutableList.Builder<>();
                    }
                    builder.add(correlationIdSetter.getKeyFromDbValue(row.get(1)));
                }

                if (builder.build().equalTraversable(array)) {
                    return arrayId;
                }
            }
        }

        return null;
    }

    static <CorrelationId extends CorrelationIdInterface> Integer findCorrelationArray(DbExporter.Database db, IntSetter<CorrelationId> correlationIdSetter, List<CorrelationId> correlations) {
        if (correlations.isEmpty()) {
            return NULL_CORRELATION_ARRAY_ID;
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), correlations.valueAt(0))
                .select(table.getArrayIdColumnIndex());

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final int arrayId = result.next().get(0).toInt();
                final MutableList<CorrelationId> array = getCorrelationArray(db, correlationIdSetter, arrayId);
                if (array.equalTraversable(correlations)) {
                    return arrayId;
                }
            }
        }

        return null;
    }

    static <LanguageId> LanguageId findLanguageByCode(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, String code) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getCodeColumnIndex(), code)
                .select(table.getIdColumnIndex());

        final DbValue value = selectOptionalFirstDbValue(db, query);
        return (value != null)? languageIdSetter.getKeyFromDbValue(value) : null;
    }

    static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> AlphabetId findMainAlphabetForLanguage(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, LanguageId language) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), language)
                .select(table.getMainAlphabetColumnIndex());

        final DbValue value = selectOptionalFirstDbValue(db, query);
        return (value != null)? alphabetIdSetter.getKeyFromDbValue(value) : null;
    }

    static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> ImmutableSet<AlphabetId> findAlphabetsByLanguage(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, LanguageId language) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getLanguageColumnIndex(), language)
                .select(table.getIdColumnIndex());
        return db.select(query).map(row -> alphabetIntSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableMap<AlphabetId, AlphabetId> getConversionsMap(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final ImmutableMap.Builder<AlphabetId, AlphabetId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.put(alphabetIdSetter.getKeyFromDbValue(row.get(1)), alphabetIdSetter.getKeyFromDbValue(row.get(0)));
            }
        }

        return builder.build();
    }

    static Integer findRuledAcceptationByAgentAndBaseAcceptation(DbExporter.Database db, int agentId, int baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(ruledAccs)
                .where(ruledAccs.getAcceptationColumnIndex(), baseAcceptation)
                .where(ruledAccs.getAgentColumnIndex(), agentId)
                .select(ruledAccs.getIdColumnIndex());
        final DbResult dbResult = db.select(query);
        final Integer result = dbResult.hasNext()? dbResult.next().get(0).toInt() : null;
        if (dbResult.hasNext()) {
            throw new AssertionError();
        }

        return result;
    }

    static ImmutableIntSet findRuledAcceptationByBaseAcceptation(DbExporter.Database db, int baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(ruledAccs)
                .where(ruledAccs.getAcceptationColumnIndex(), baseAcceptation)
                .select(ruledAccs.getIdColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static Integer findRuledAcceptationByRuleAndBaseAcceptation(DbExporter.Database db, int rule, int baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(ruledAccs)
                .join(agents, ruledAccs.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAccs.getAcceptationColumnIndex(), baseAcceptation)
                .where(ruledAccs.columns().size() + agents.getRuleColumnIndex(), rule)
                .select(ruledAccs.getIdColumnIndex());
        final DbResult dbResult = db.select(query);
        final Integer result = dbResult.hasNext()? dbResult.next().get(0).toInt() : null;
        if (dbResult.hasNext()) {
            throw new AssertionError();
        }

        return result;
    }

    static ImmutableList<SearchResult> findAcceptationFromText(DbExporter.Database db, String queryText, int restrictionStringType, ImmutableIntRange range) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringColumnIndex(), new DbQuery.Restriction(new DbStringValue(queryText),
                        restrictionStringType))
                .range(range)
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

                map.put(dynAcc, new SearchResult(str, mainStr, SearchResult.Types.ACCEPTATION, dynAcc, dynAcc != acc));
            }
        }

        return map.toList().toImmutable().sort((a, b) -> !a.isDynamic() && b.isDynamic() || a.isDynamic() == b.isDynamic() && SortUtils.compareCharSequenceByUnicode(a.getStr(), b.getStr()));
    }

    private static final class AcceptationFromTextResult {
        final String str;
        final String mainStr;
        final int acceptation;
        final int baseAcceptation;

        AcceptationFromTextResult(String str, String mainStr, int acceptation, int baseAcceptation) {
            this.str = str;
            this.mainStr = mainStr;
            this.acceptation = acceptation;
            this.baseAcceptation = baseAcceptation;
        }

        boolean isDynamic() {
            return acceptation != baseAcceptation;
        }
    }

    private static ImmutableList<AcceptationFromTextResult> findAcceptationFromText2(DbExporter.Database db, String queryText, int restrictionStringType, ImmutableIntRange range) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringColumnIndex(), new DbQuery.Restriction(new DbStringValue(queryText),
                        restrictionStringType))
                .range(range)
                .select(
                        table.getStringColumnIndex(),
                        table.getMainStringColumnIndex(),
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        final MutableIntKeyMap<AcceptationFromTextResult> map = MutableIntKeyMap.empty();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String str = row.get(0).toText();
                final String mainStr = row.get(1).toText();
                final int acc = row.get(2).toInt();
                final int dynAcc = row.get(3).toInt();

                map.put(dynAcc, new AcceptationFromTextResult(str, mainStr, dynAcc, acc));
            }
        }

        return map.toList().toImmutable().sort((a, b) -> !a.isDynamic() && b.isDynamic() || a.isDynamic() == b.isDynamic() && SortUtils.compareCharSequenceByUnicode(a.str, b.str));
    }

    static ImmutableList<SearchResult> findAcceptationAndRulesFromText(DbExporter.Database db, String queryText, int restrictionStringType, ImmutableIntRange range) {
        final ImmutableList<AcceptationFromTextResult> rawResult = findAcceptationFromText2(db, queryText, restrictionStringType, range);
        final SyncCacheIntKeyNonNullValueMap<String> mainTexts = new SyncCacheIntKeyNonNullValueMap<>(id -> readAcceptationMainText(db, id));

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final int offset = ruledAcceptations.columns().size();

        return rawResult.map(rawEntry -> {
            if (rawEntry.isDynamic()) {
                ImmutableIntList rules = ImmutableIntList.empty();
                int acc = rawEntry.acceptation;
                while (acc != rawEntry.baseAcceptation) {
                    final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                            .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                            .where(ruledAcceptations.getIdColumnIndex(), acc)
                            .select(ruledAcceptations.getAcceptationColumnIndex(), offset + agents.getRuleColumnIndex());

                    final List<DbValue> row = selectSingleRow(db, query);
                    acc = row.get(0).toInt();
                    rules = rules.append(row.get(1).toInt());
                }

                return new SearchResult(rawEntry.str, rawEntry.mainStr, SearchResult.Types.ACCEPTATION, rawEntry.acceptation,
                        true, mainTexts.get(acc), rules);
            }
            else {
                return new SearchResult(rawEntry.str, rawEntry.mainStr, SearchResult.Types.ACCEPTATION, rawEntry.acceptation,
                        false);
            }
        });
    }

    static boolean isAcceptationStaticallyInBunch(DbExporter.Database db, int bunch, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .where(table.getAgentColumnIndex(), 0)
                .select(table.getIdColumnIndex());
        try (DbResult result = db.select(query)) {
            return result.hasNext();
        }
    }

    static <AlphabetId extends AlphabetIdInterface> Conversion<AlphabetId> getConversion(DbExporter.Database db, ImmutablePair<AlphabetId, AlphabetId> pair) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;
        final LangbookDbSchema.SymbolArraysTable symbols = LangbookDbSchema.Tables.symbolArrays;

        final int off1Symbols = conversions.columns().size();
        final int off2Symbols = off1Symbols + symbols.columns().size();

        final DbQuery query = new DbQueryBuilder(conversions)
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

        return new Conversion<>(pair.left, pair.right, resultMap);
    }

    static <AlphabetId extends AlphabetIdInterface> boolean checkConversionConflicts(DbExporter.Database db, ConversionProposal<AlphabetId> conversion) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getStringAlphabetColumnIndex(), conversion.getSourceAlphabet())
                .select(table.getStringColumnIndex());

        return !db.select(query)
                .map(row -> row.get(0).toText())
                .anyMatch(str -> conversion.convert(str) == null);
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableSet<String> findConversionConflictWords(DbExporter.Database db, ConversionProposal<AlphabetId> conversion) {
        // TODO: Logic in the word should be somehow centralised with #checkConversionConflicts method
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getStringAlphabetColumnIndex(), conversion.getSourceAlphabet())
                .select(table.getStringColumnIndex());

        return db.select(query)
                .map(row -> row.get(0).toText())
                .filter(str -> conversion.convert(str) == null)
                .toSet()
                .toImmutable();
    }

    static Integer findBunchSet(DbExporter.Database db, IntSet bunches) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        if (bunches.isEmpty()) {
            return table.nullReference();
        }

        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunches.valueAt(0))
                .orderBy(table.getSetIdColumnIndex())
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
                    }

                    set.add(row.get(1).toInt());
                }

                if (set.equals(bunches)) {
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

    static MutableIntPairMap findRuledConceptsByConceptInvertedMap(DbExporter.Database db, int concept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex(), table.getRuleColumnIndex());

        final MutableIntPairMap ruledConcepts = MutableIntPairMap.empty();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                ruledConcepts.put(row.get(1).toInt(), row.get(0).toInt());
            }
        }

        return ruledConcepts;
    }

    static ImmutableIntPairMap findRuledConceptsByRule(DbExporter.Database db, int rule) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getRuleColumnIndex(), rule)
                .select(table.getIdColumnIndex(), table.getConceptColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> list = result.next();
                builder.put(list.get(0).toInt(), list.get(1).toInt());
            }
        }

        return builder.build();
    }

    static ImmutableIntPairMap findRuledConceptsByRuleInvertedMap(DbExporter.Database db, int rule) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getRuleColumnIndex(), rule)
                .select(table.getIdColumnIndex(), table.getConceptColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> list = result.next();
                builder.put(list.get(1).toInt(), list.get(0).toInt());
            }
        }

        return builder.build();
    }

    static Integer getRuleByRuledConcept(DbExporter.Database db, int ruledConcept) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), ruledConcept)
                .select(table.getRuleColumnIndex());

        return selectOptionalFirstIntColumn(db, query);
    }

    static ImmutableIntSet findAgentsByRule(DbExporter.Database db, int rule) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getRuleColumnIndex(), rule)
                .select(table.getIdColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
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

    static ImmutableIntKeyMap<ImmutableIntSet> findAffectedAgentsByItsSourceWithTarget(DbExporter.Database db, int bunch) {
        if (bunch == 0) {
            return ImmutableIntKeyMap.empty();
        }

        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final int offset = bunchSets.columns().size();
        final DbQuery query = new DbQuery.Builder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getSourceBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(offset + agents.getIdColumnIndex(), offset + agents.getTargetBunchSetColumnIndex());

        final ImmutableIntKeyMap.Builder<ImmutableIntSet> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), getBunchSet(db, row.get(1).toInt()));
            }
        }

        return builder.build();
    }

    static ImmutableIntKeyMap<ImmutableIntSet> findAffectedAgentsByItsDiffWithTarget(DbExporter.Database db, int bunch) {
        if (bunch == 0) {
            return ImmutableIntKeyMap.empty();
        }

        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final int offset = bunchSets.columns().size();
        final DbQuery query = new DbQuery.Builder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getDiffBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(offset + agents.getIdColumnIndex(), offset + agents.getTargetBunchSetColumnIndex());

        final ImmutableIntKeyMap.Builder<ImmutableIntSet> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), getBunchSet(db, row.get(1).toInt()));
            }
        }

        return builder.build();
    }

    static ImmutableIntSet findAgentsWithoutSourceBunches(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getSourceBunchSetColumnIndex(), 0)
                .select(agents.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntKeyMap<ImmutableIntSet> findAgentsWithoutSourceBunchesWithTarget(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getSourceBunchSetColumnIndex(), 0)
                .select(agents.getIdColumnIndex(), agents.getTargetBunchSetColumnIndex());

        final ImmutableIntKeyMap.Builder<ImmutableIntSet> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), getBunchSet(db, row.get(1).toInt()));
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
                .where(bunchAccOffset + bunchAcceptations.getAgentColumnIndex(), 0)
                .where(bunchAccOffset + bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(agents.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntSet findQuizzesByBunch(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = LangbookDbSchema.Tables.quizDefinitions;
        final DbQuery query = new DbQuery.Builder(quizzes)
                .where(quizzes.getBunchColumnIndex(), bunch)
                .select(quizzes.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface> Integer findQuestionFieldSet(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, Iterable<QuestionFieldDetails<AlphabetId>> collection) {
        final MutableHashSet<QuestionFieldDetails<AlphabetId>> set = MutableHashSet.empty();
        if (collection == null) {
            return null;
        }

        for (QuestionFieldDetails<AlphabetId> field : collection) {
            set.add(field);
        }

        if (set.isEmpty()) {
            return null;
        }

        final QuestionFieldDetails<AlphabetId> firstField = set.iterator().next();
        final LangbookDbSchema.QuestionFieldSets fieldSets = LangbookDbSchema.Tables.questionFieldSets;
        final int columnCount = fieldSets.columns().size();
        final DbQuery query = new DbQueryBuilder(fieldSets)
                .join(fieldSets, fieldSets.getSetIdColumnIndex(), fieldSets.getSetIdColumnIndex())
                .where(fieldSets.getRuleColumnIndex(), firstField.rule)
                .where(fieldSets.getFlagsColumnIndex(), firstField.flags)
                .where(fieldSets.getAlphabetColumnIndex(), firstField.alphabet)
                .select(
                        fieldSets.getSetIdColumnIndex(),
                        columnCount + fieldSets.getAlphabetColumnIndex(),
                        columnCount + fieldSets.getRuleColumnIndex(),
                        columnCount + fieldSets.getFlagsColumnIndex());

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int setId = row.get(0).toInt();
                final MutableSet<QuestionFieldDetails<AlphabetId>> foundSet = MutableHashSet.empty();
                foundSet.add(new QuestionFieldDetails<>(alphabetIntSetter.getKeyFromDbValue(row.get(1)), row.get(2).toInt(), row.get(3).toInt()));

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

                    foundSet.add(new QuestionFieldDetails<>(alphabetIntSetter.getKeyFromDbValue(row.get(1)), row.get(2).toInt(), row.get(3).toInt()));
                }

                if (foundSet.equals(set)) {
                    return setId;
                }
            }
        }

        return null;
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableMap<AlphabetId, AlphabetId> findConversions(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, Set<AlphabetId> alphabets) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final MutableSet<AlphabetId> foundAlphabets = MutableHashSet.empty();
        final ImmutableMap.Builder<AlphabetId, AlphabetId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AlphabetId source = alphabetIntSetter.getKeyFromDbValue(row.get(0));
                final AlphabetId target = alphabetIntSetter.getKeyFromDbValue(row.get(1));

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

    private static ImmutableIntSet findSentenceIdsMatchingMeaning(DbExporter.Database db, int sentenceId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final int offset = table.columns().size();
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getConceptColumnIndex(), table.getConceptColumnIndex())
                .where(table.getIdColumnIndex(), sentenceId)
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

    private static int getMaxConceptInAcceptations(DbExporter.Database db) {
        LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        return getColumnMax(db, table, table.getConceptColumnIndex());
    }

    private static int getMaxConceptInSentences(DbExporter.Database db) {
        LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        return getColumnMax(db, table, table.getConceptColumnIndex());
    }

    private static int getMaxConceptInRuledConcepts(DbExporter.Database db) {
        LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        return getColumnMax(db, table, table.getIdColumnIndex());
    }

    private static int getMaxConceptInComplementedConcepts(DbExporter.Database db) {
        LangbookDbSchema.ComplementedConceptsTable table = LangbookDbSchema.Tables.complementedConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getBaseColumnIndex(), table.getComplementColumnIndex());

        int max = 0;
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int id = row.get(0).toInt();
                final int base = row.get(1).toInt();
                final int complement = row.get(2).toInt();
                final int localMax = (id > base && id > complement)? id : (base > complement)? base : complement;
                if (localMax > max) {
                    max = localMax;
                }
            }
        }

        return max;
    }

    private static int getMaxConceptInConceptCompositions(DbExporter.Database db) {
        LangbookDbSchema.ConceptCompositionsTable table = LangbookDbSchema.Tables.conceptCompositions;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getComposedColumnIndex(), table.getItemColumnIndex());

        int max = 0;
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int compositionId = row.get(0).toInt();
                final int item = row.get(1).toInt();
                final int localMax = (item > compositionId)? item : compositionId;
                if (localMax > max) {
                    max = localMax;
                }
            }
        }

        return max;
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

        temp = getMaxConceptInComplementedConcepts(db);
        if (temp > max) {
            max = temp;
        }

        temp = getMaxConceptInConceptCompositions(db);
        if (temp > max) {
            max = temp;
        }

        temp = getMaxConceptInSentences(db);
        if (temp > max) {
            max = temp;
        }

        return max;
    }

    static int getMaxBunchSetId(DbExporter.Database db) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    static int getMaxQuestionFieldSetId(DbExporter.Database db) {
        LangbookDbSchema.QuestionFieldSets table = LangbookDbSchema.Tables.questionFieldSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> ImmutableCorrelation<AlphabetId> getCorrelationWithText(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, CorrelationId correlationId) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final DbQuery query = new DbQueryBuilder(correlations)
                .join(symbolArrays, correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(correlations.getCorrelationIdColumnIndex(), correlationId)
                .select(correlations.getAlphabetColumnIndex(), correlations.columns().size() + symbolArrays.getStrColumnIndex());
        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(alphabetIdSetter.getKeyFromDbValue(row.get(0)), row.get(1).toText());
            }
        }
        return new ImmutableCorrelation<>(builder.build());
    }

    static <SymbolArrayId> ImmutableSet<SymbolArrayId> getCorrelationSymbolArrayIds(DbExporter.Database db, IntSetter<SymbolArrayId> symbolArrayIdSetter, CorrelationIdInterface correlationId) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;

        final DbQuery query = new DbQueryBuilder(correlations)
                .where(correlations.getCorrelationIdColumnIndex(), correlationId)
                .select(correlations.getSymbolArrayColumnIndex());
        return db.select(query).map(row -> symbolArrayIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    static <CorrelationId> MutableList<CorrelationId> getCorrelationArray(DbExporter.Database db, IntSetter<CorrelationId> correlationIdSetter, int id) {
        if (id == NULL_CORRELATION_ARRAY_ID) {
            return MutableList.empty();
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayIdColumnIndex(), id)
                .select(table.getArrayPositionColumnIndex(), table.getCorrelationColumnIndex());
        final DbResult dbResult = db.select(query);
        final int arrayLength = dbResult.getRemainingRows();
        final MutableList<CorrelationId> result = MutableList.empty((currentSize, newSize) -> arrayLength);
        for (int i = 0; i < arrayLength; i++) {
            result.append(null);
        }

        try {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int pos = row.get(0).toInt();
                final CorrelationId corr = correlationIdSetter.getKeyFromDbValue(row.get(1));
                if (result.get(pos) != null) {
                    throw new AssertionError("Malformed correlation array with id " + id);
                }

                result.put(pos, corr);
            }
        }
        finally {
            dbResult.close();
        }

        return result;
    }

    static ImmutableIntSet getAcceptationsInBunchByBunchAndAgent(DbExporter.Database db, int bunch, int agent) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAgentColumnIndex(), agent)
                .select(table.getAcceptationColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static boolean isBunchAcceptationPresentByAgent(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(db, query);
    }

    static ImmutableIntSet getAcceptationsInBunch(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getAcceptationColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static MutableIntKeyMap<MutableIntSet> readBunchSetsWhereBunchIsIncluded(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getBunchColumnIndex());
        final MutableIntKeyMap<MutableIntSet> map = MutableIntKeyMap.empty();
        final SyncCacheIntKeyNonNullValueMap<MutableIntSet> cache = new SyncCacheIntKeyNonNullValueMap<>(map, id -> MutableIntArraySet.empty());
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                cache.get(row.get(0).toInt()).add(row.get(1).toInt());
            }
        }

        return map;
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId> ImmutablePair<ImmutableList<CorrelationId>, ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>>> getAcceptationCorrelations(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, IntSetter<CorrelationId> correlationIdSetter, int acceptation) {
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

        final MutableList<CorrelationId> correlationIds = MutableList.empty();
        final MutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlationMap = MutableHashMap.empty();
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
                int pos = row.get(0).toInt();
                CorrelationId correlationId = correlationIdSetter.getKeyFromDbValue(row.get(1));
                if (pos != correlationIds.size()) {
                    throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                }

                builder.put(alphabetIdSetter.getKeyFromDbValue(row.get(2)), row.get(3).toText());

                while (dbResult.hasNext()) {
                    row = dbResult.next();
                    int newPos = row.get(0).toInt();
                    if (newPos != pos) {
                        correlationMap.put(correlationId, builder.build());
                        correlationIds.append(correlationId);
                        correlationId = correlationIdSetter.getKeyFromDbValue(row.get(1));
                        builder = new ImmutableCorrelation.Builder<>();
                        pos = newPos;
                    }

                    if (newPos != correlationIds.size()) {
                        throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                    }
                    builder.put(alphabetIdSetter.getKeyFromDbValue(row.get(2)), row.get(3).toText());
                }
                correlationMap.put(correlationId, builder.build());
                correlationIds.append(correlationId);
            }
        }

        return new ImmutablePair<>(correlationIds.toImmutable(), correlationMap.toImmutable());
    }

    static ImmutableIntSet getBunchSet(DbExporter.Database db, int setId) {
        if (setId == 0) {
            return ImmutableIntArraySet.empty();
        }

        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .select(table.getBunchColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableList<SearchResult> getSearchHistory(DbExporter.Database db) {
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
                    builder.add(new SearchResult(row.get(2).toText(), row.get(3).toText(), SearchResult.Types.ACCEPTATION, acceptation, row.get(1).toInt() != acceptation));
                }
            }
        }

        return builder.build();
    }

    private static boolean isSymbolArrayUsedInAnyCorrelation(DbExporter.Database db, SymbolArrayIdInterface symbolArrayId) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSymbolArrayColumnIndex(), symbolArrayId)
                .select(table.getIdColumnIndex());
        return selectExistAtLeastOneRow(db, query);
    }

    private static boolean isSymbolArrayUsedInAnyConversion(DbExporter.Database db, SymbolArrayIdInterface symbolArrayId) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getSourceColumnIndex(), table.getTargetColumnIndex());
        return db.select(query).anyMatch(row -> symbolArrayId.sameValue(row.get(0)) || symbolArrayId.sameValue(row.get(1)));
    }

    private static boolean isSymbolArrayUsedInAnySentence(DbExporter.Database db, SymbolArrayIdInterface symbolArrayId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSymbolArrayColumnIndex(), symbolArrayId)
                .select(table.getIdColumnIndex());
        return selectExistAtLeastOneRow(db, query);
    }

    static boolean isSymbolArrayInUse(DbExporter.Database db, SymbolArrayIdInterface symbolArrayId) {
        return isSymbolArrayUsedInAnyCorrelation(db, symbolArrayId) ||
                isSymbolArrayUsedInAnyConversion(db, symbolArrayId) ||
                isSymbolArrayUsedInAnySentence(db, symbolArrayId);
    }

    private static boolean isCorrelationUsedInAnyCorrelationArray(DbExporter.Database db, CorrelationIdInterface correlationId) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getCorrelationColumnIndex(), correlationId)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(db, query);
    }

    private static boolean isCorrelationUsedInAnyAgent(DbExporter.Database db, CorrelationIdInterface correlationId) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getStartMatcherColumnIndex(), table.getEndMatcherColumnIndex(), table.getStartAdderColumnIndex(), table.getEndAdderColumnIndex());

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                if (dbResult.next().anyMatch(correlationId::sameValue)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean isCorrelationInUse(DbExporter.Database db, CorrelationIdInterface correlationId) {
        return isCorrelationUsedInAnyCorrelationArray(db, correlationId) || isCorrelationUsedInAnyAgent(db, correlationId);
    }

    private static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> ImmutableIntKeyMap<String> readAcceptationsIncludingCorrelation(DbExporter.Database db, CorrelationId correlation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = LangbookDbSchema.Tables.correlationArrays;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = correlationArrays.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(correlationArrays)
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
                if (preferredAlphabet.sameValue(row.get(1)) || result.get(accId, null) == null) {
                    result.put(accId, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> readCorrelationsWithSameSymbolArray(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, IntSetter<CorrelationId> correlationIdSetter, CorrelationId correlation, AlphabetId alphabet) {
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final int corrOffset2 = correlations.columns().size();
        final int corrOffset3 = corrOffset2 + corrOffset2;
        final int strOffset = corrOffset3 + corrOffset2;

        final DbQuery query = new DbQueryBuilder(correlations)
                .join(correlations, correlations.getSymbolArrayColumnIndex(), correlations.getSymbolArrayColumnIndex())
                .join(correlations, corrOffset2 + correlations.getCorrelationIdColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrOffset3 + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(correlations.getCorrelationIdColumnIndex(), correlation)
                .whereColumnValueMatch(correlations.getAlphabetColumnIndex(), corrOffset2 + correlations.getAlphabetColumnIndex())
                .whereColumnValueDiffer(correlations.getCorrelationIdColumnIndex(), corrOffset2 + correlations.getCorrelationIdColumnIndex())
                .where(correlations.getAlphabetColumnIndex(), alphabet)
                .select(
                        corrOffset2 + correlations.getCorrelationIdColumnIndex(),
                        corrOffset3 + correlations.getAlphabetColumnIndex(),
                        strOffset + symbolArrays.getStrColumnIndex());

        final MutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> result = MutableHashMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final CorrelationId corrId = correlationIdSetter.getKeyFromDbValue(row.get(0));
                final AlphabetId textAlphabet = alphabetIntSetter.getKeyFromDbValue(row.get(1));
                final String text = row.get(2).toText();
                final ImmutableCorrelation<AlphabetId> currentCorr = result.get(corrId, ImmutableCorrelation.empty());
                result.put(corrId, currentCorr.put(textAlphabet, text));
            }
        }

        return result.toImmutable();
    }

    static int conceptFromAcceptation(DbExporter.Database db, int accId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), accId)
                .select(table.getConceptColumnIndex());
        try (DbResult result = db.select(query)) {
            final int concept = result.hasNext()? result.next().get(0).toInt() : 0;

            if (result.hasNext()) {
                throw new AssertionError("Multiple rows found matching the given criteria");
            }

            return concept;
        }
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

    static ImmutableIntSet getAgentIds(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> MutableCorrelation<AlphabetId> readCorrelationArrayTexts(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, IntSetter<CorrelationId> correlationIdSetter, int correlationArrayId) {
        MutableMap<AlphabetId, String> texts = MutableHashMap.empty();
        for (CorrelationId correlationId : getCorrelationArray(db, correlationIdSetter, correlationArrayId)) {
            for (Map.Entry<AlphabetId, String> entry : getCorrelationWithText(db, alphabetIdSetter, correlationId).entries()) {
                final String currentValue = texts.get(entry.key(), "");
                texts.put(entry.key(), currentValue + entry.value());
            }
        }

        return new MutableCorrelation<>(texts);
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> Correlation<AlphabetId> readCorrelationArrayTextAndItsAppliedConversions(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, IntSetter<CorrelationId> correlationIdSetter, int correlationArrayId) {
        final MutableCorrelation<AlphabetId> texts = readCorrelationArrayTexts(db, alphabetIdSetter, correlationIdSetter, correlationArrayId);
        if (texts.isEmpty()) {
            return null;
        }

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap(db, alphabetIdSetter);
        final int conversionCount = conversionMap.size();
        for (Map.Entry<AlphabetId, String> entry : texts.entries().toImmutable()) {
            for (int conversionIndex = 0; conversionIndex < conversionCount; conversionIndex++) {
                if (equal(conversionMap.valueAt(conversionIndex), entry.key())) {
                    final ImmutablePair<AlphabetId, AlphabetId> pair = new ImmutablePair<>(conversionMap.valueAt(conversionIndex), conversionMap.keyAt(conversionIndex));
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
                .select(stringsOffset + strings.getMainAcceptationColumnIndex(),
                        stringsOffset + strings.getDynamicAcceptationColumnIndex(),
                        languagesOffset + languages.getIdColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<SynonymTranslationResult> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int accId = row.get(0).toInt();
                if (accId != acceptation) {
                    builder.put(accId, new SynonymTranslationResult(row.get(2).toInt(), row.get(3).toText(), row.get(1).toInt() != accId));
                }
            }
        }

        return builder.build();
    }

    private static <AlphabetId extends AlphabetIdInterface> MutableIntKeyMap<SynonymTranslationResult> readAcceptationSynonymsAndTranslations(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = acceptations.columns().size();
        final int stringsOffset = accOffset * 2;
        final int alphabetsOffset = stringsOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, stringsOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(stringsOffset + strings.getMainAcceptationColumnIndex(),
                        stringsOffset + strings.getDynamicAcceptationColumnIndex(),
                        alphabetsOffset + alphabets.getLanguageColumnIndex(),
                        stringsOffset + strings.getStringAlphabetColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<SynonymTranslationResult> builder = MutableIntKeyMap.empty();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int accId = row.get(1).toInt();
                if (accId != acceptation && (builder.get(accId, null) == null || preferredAlphabet.sameValue(row.get(3)))) {
                    builder.put(accId, new SynonymTranslationResult(row.get(2).toInt(), row.get(4).toText(), row.get(0).toInt() != accId));
                }
            }
        }

        return builder;
    }

    static boolean isCorrelationArrayInUse(DbExporter.Database db, int correlationArrayId) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;

        final DbQuery query = new DbQuery.Builder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId)
                .select(acceptations.getIdColumnIndex());

        return selectExistAtLeastOneRow(db, query);
    }

    static boolean isBunchSetInUse(DbExporter.Database db, int setId) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final DbQuery query = new DbQuery.Builder(agents).select(
                agents.getTargetBunchSetColumnIndex(),
                agents.getSourceBunchSetColumnIndex(),
                agents.getDiffBunchSetColumnIndex());

        return db.select(query).anyMatch(row -> row.get(0).toInt() == setId ||
                row.get(1).toInt() == setId || row.get(2).toInt() == setId);
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntKeyMap<ImmutableSet<AlphabetId>> readAcceptationsSharingTexts(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, int acceptation) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int offset = strings.columns().size();
        final DbQuery query = new DbQuery.Builder(strings)
                .join(strings, strings.getStringColumnIndex(), strings.getStringColumnIndex())
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .whereColumnValueMatch(strings.getStringAlphabetColumnIndex(), offset + strings.getStringAlphabetColumnIndex())
                .select(strings.getStringAlphabetColumnIndex(), offset + strings.getDynamicAcceptationColumnIndex());

        final MutableSet<AlphabetId> foundAlphabets = MutableHashSet.empty();
        final MutableList<AlphabetId> sortedAlphabets = MutableList.empty();
        final MutableIntKeyMap<ImmutableSet<AlphabetId>> alphabetSets = MutableIntKeyMap.empty();
        alphabetSets.put(0, ImmutableHashSet.empty());
        final MutableIntPairMap result = MutableIntPairMap.empty();

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AlphabetId alphabet = alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final int acc = row.get(1).toInt();

                if (acc != acceptation) {
                    if (!foundAlphabets.contains(alphabet)) {
                        final int bitMask = 1 << foundAlphabets.size();
                        foundAlphabets.add(alphabet);
                        sortedAlphabets.append(alphabet);

                        for (int key : alphabetSets.keySet().toImmutable()) {
                            alphabetSets.put(bitMask | key, alphabetSets.get(key).add(alphabet));
                        }
                    }

                    final int alphabetBitPosition = 1 << sortedAlphabets.indexOf(alphabet);
                    final int value = result.get(acc, 0);
                    result.put(acc, value | alphabetBitPosition);
                }
            }
        }

        return result.map(alphabetSets::get).toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutablePair<IdentifiableResult, ImmutableIntKeyMap<String>> readDefinitionFromAcceptation(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = LangbookDbSchema.Tables.complementedConcepts;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int bunchOffset = acceptations.columns().size();
        final int accOffset = bunchOffset + complementedConcepts.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(complementedConcepts, acceptations.getConceptColumnIndex(), complementedConcepts.getIdColumnIndex())
                .join(acceptations, bunchOffset + complementedConcepts.getBaseColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        bunchOffset + complementedConcepts.getComplementColumnIndex());

        IdentifiableResult result = null;
        int compositionId = 0;
        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                int acc = row.get(0).toInt();
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(1));
                String text = row.get(2).toText();
                compositionId = row.get(3).toInt();
                while (!preferredAlphabetFound && dbResult.hasNext()) {
                    row = dbResult.next();
                    if (preferredAlphabet.sameValue(row.get(1))) {
                        acc = row.get(0).toInt();
                        text = row.get(2).toText();
                        break;
                    }
                }

                result = new IdentifiableResult(acc, text);
            }
        }

        ImmutableIntKeyMap<String> componentTexts = ImmutableIntKeyMap.empty();
        if (compositionId != 0) {
            final ImmutableIntKeyMap<String> texts = readDefinitionComponentsText(db, compositionId, preferredAlphabet);
            if (texts.isEmpty()) {
                final DisplayableItem item = readConceptAcceptationAndText(db, compositionId, preferredAlphabet);
                componentTexts = componentTexts.put(item.id, item.text);
            }
            else {
                componentTexts = texts;
            }
        }

        return new ImmutablePair<>(result, componentTexts);
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntKeyMap<String> readDefinitionComponentsText(DbExporter.Database db, int compositionId, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.ConceptCompositionsTable compositions = LangbookDbSchema.Tables.conceptCompositions;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = compositions.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(compositions)
                .join(acceptations, compositions.getItemColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(compositions.getComposedColumnIndex(), compositionId)
                .select(
                        compositions.getItemColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntPairMap conceptAccMap = MutableIntPairMap.empty();
        final MutableIntKeyMap<String> conceptTextMap = MutableIntKeyMap.empty();

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int concept = row.get(0).toInt();
                final int accId = row.get(1).toInt();
                final DbValue alphabet = row.get(2);
                final String text = row.get(3).toText();

                final int currentAccId = conceptAccMap.get(concept, 0);
                if (currentAccId == 0 || preferredAlphabet.sameValue(alphabet)) {
                    conceptAccMap.put(concept, accId);
                    conceptTextMap.put(concept, text);
                }
            }
        }

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        for (IntPairMap.Entry pair : conceptAccMap.entries()) {
            builder.put(pair.value(), conceptTextMap.get(pair.key()));
        }

        return builder.build();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntKeyMap<String> readSubtypesFromAcceptation(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = LangbookDbSchema.Tables.complementedConcepts;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int bunchOffset = acceptations.columns().size();
        final int accOffset = bunchOffset + complementedConcepts.columns().size();
        final int strOffset = accOffset + bunchOffset;

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(complementedConcepts, acceptations.getConceptColumnIndex(), complementedConcepts.getBaseColumnIndex())
                .join(acceptations, bunchOffset + complementedConcepts.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(bunchOffset + complementedConcepts.getIdColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<String> conceptText = MutableIntKeyMap.empty();
        final MutableIntPairMap conceptAccs = MutableIntPairMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int concept = row.get(0).toInt();
                final DbValue alphabet = row.get(2);
                if (preferredAlphabet.sameValue(alphabet) || !conceptAccs.keySet().contains(concept)) {
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

    private static final class AcceptationOrigin {
        final int originalAcceptationId;
        final int appliedAgent;
        final String originalAcceptationText;

        AcceptationOrigin(int originalAcceptationId, int appliedAgent, String originalAcceptationText) {
            this.originalAcceptationId = originalAcceptationId;
            this.appliedAgent = appliedAgent;
            this.originalAcceptationText = originalAcceptationText;
        }
    }

    private static <AlphabetId extends AlphabetIdInterface> AcceptationOrigin readOriginalAcceptation(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final int offset = ruledAcceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                .join(strings, ruledAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(ruledAcceptations.getIdColumnIndex(), acceptation)
                .select(ruledAcceptations.getAcceptationColumnIndex(),
                        ruledAcceptations.getAgentColumnIndex(),
                        offset + strings.getStringAlphabetColumnIndex(),
                        offset + strings.getStringColumnIndex());

        int id = 0;
        int agentId = 0;
        String text = null;
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                if (id == 0 || preferredAlphabet.sameValue(row.get(2))) {
                    id = row.get(0).toInt();
                    agentId = row.get(1).toInt();
                    text = row.get(3).toText();
                }
            }
        }

        return (id == 0)? null : new AcceptationOrigin(id, agentId, text);
    }

    private static final class AppliedRule {
        final int acceptationId;
        final int rule;
        final String text;

        AppliedRule(int acceptationId, int rule, String text) {
            this.acceptationId = acceptationId;
            this.rule = rule;
            this.text = text;
        }
    }

    private static <AlphabetId extends AlphabetIdInterface> AppliedRule readRulePreferredTextByAgent(DbExporter.Database db, int agentId, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final int accOffset = agents.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(agents)
                .join(acceptations, agents.getRuleColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(agents.getIdColumnIndex(), agentId)
                .select(agents.getRuleColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        int acceptationId = 0;
        int ruleId = 0;
        String text = null;
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                if (ruleId == 0 || preferredAlphabet.sameValue(row.get(2))) {
                    ruleId = row.get(0).toInt();
                    acceptationId = row.get(1).toInt();
                    text = row.get(3).toText();
                }
            }
        }

        return (ruleId == 0)? null : new AppliedRule(acceptationId, ruleId, text);
    }

    static <AlphabetId extends AlphabetIdInterface> MorphologyReaderResult readMorphologiesFromAcceptation(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final int ruledAccOffset = strings.columns().size();
        final int agentsOffset = ruledAccOffset + ruledAcceptations.columns().size();

        final DbQuery mainQuery = new DbQuery.Builder(strings)
                .join(ruledAcceptations, strings.getDynamicAcceptationColumnIndex(), ruledAcceptations.getIdColumnIndex())
                .join(agents, ruledAccOffset + ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(strings.getMainAcceptationColumnIndex(), acceptation)
                .select(strings.getDynamicAcceptationColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex(),
                        ruledAccOffset + ruledAcceptations.getAcceptationColumnIndex(),
                        ruledAccOffset + ruledAcceptations.getAgentColumnIndex(),
                        agentsOffset + agents.getRuleColumnIndex());

        final MutableIntKeyMap<String> texts = MutableIntKeyMap.empty();
        final MutableIntPairMap sourceAccs = MutableIntPairMap.empty();
        final MutableIntPairMap accRules = MutableIntPairMap.empty();
        final MutableIntPairMap agentRules = MutableIntPairMap.empty();

        try (DbResult dbResult = db.select(mainQuery)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int dynAcc = row.get(0).toInt();
                final DbValue alphabet = row.get(1);
                final int agent = row.get(4).toInt();
                final int rule = row.get(5).toInt();

                final boolean dynAccNotFound = texts.get(dynAcc, null) == null;
                if (dynAccNotFound || preferredAlphabet.sameValue(alphabet)) {
                    texts.put(dynAcc, row.get(2).toText());
                }

                if (dynAccNotFound) {
                    sourceAccs.put(dynAcc, row.get(3).toInt());
                    accRules.put(dynAcc, rule);
                }

                agentRules.put(agent, rule);
            }
        }

        final ImmutableIntSet rules = accRules.toSet().toImmutable();
        final ImmutableIntKeyMap<String> ruleTexts = rules.assign(rule -> readConceptText(db, rule, preferredAlphabet));

        final ImmutableList<MorphologyResult> morphologies = sourceAccs.keySet().map(dynAcc -> {
            int acc = dynAcc;
            ImmutableIntList ruleChain = ImmutableIntList.empty();
            while (acc != acceptation) {
                ruleChain = ruleChain.append(accRules.get(acc));
                acc = sourceAccs.get(acc);
            }
            return new MorphologyResult(dynAcc, ruleChain, texts.get(dynAcc));
        }).toImmutable();

        return new MorphologyReaderResult(morphologies, ruleTexts, agentRules.toImmutable());
    }

    static <AlphabetId extends AlphabetIdInterface> DerivedAcceptationsReaderResult readDerivedAcceptations(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final int strOffset = ruledAcceptations.columns().size();
        final int agentsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(ruledAcceptations)
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        ruledAcceptations.getAgentColumnIndex(),
                        agentsOffset + agents.getRuleColumnIndex());

        final MutableIntKeyMap<String> texts = MutableIntKeyMap.empty();
        final MutableIntPairMap accAgents = MutableIntPairMap.empty();
        final MutableIntPairMap agentRules = MutableIntPairMap.empty();

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int dynAcc = row.get(0).toInt();

                final boolean dynAccNotFound = texts.get(dynAcc, null) == null;
                if (dynAccNotFound || preferredAlphabet.sameValue(row.get(1))) {
                    texts.put(dynAcc, row.get(2).toText());
                }

                if (dynAccNotFound) {
                    final int agent = row.get(3).toInt();
                    final int rule = row.get(4).toInt();
                    accAgents.put(dynAcc, agent);
                    agentRules.put(agent, rule);
                }
            }
        }

        final ImmutableIntKeyMap<DerivedAcceptationResult> acceptations = accAgents.keySet().assign(acc -> new DerivedAcceptationResult(accAgents.get(acc), texts.get(acc))).toImmutable();
        final ImmutableIntKeyMap<String> ruleTexts = agentRules.toSet().assign(rule -> readConceptText(db, rule, preferredAlphabet)).toImmutable();
        return new DerivedAcceptationsReaderResult(acceptations, ruleTexts, agentRules.toImmutable());
    }

    private static ImmutableIntValueMap<String> readTextAndDynamicAcceptationsMapFromStaticAcceptation(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getMainAcceptationColumnIndex(), staticAcceptation)
                .select(strings.getDynamicAcceptationColumnIndex(),
                        strings.getStringColumnIndex());

        final MutableIntValueMap<String> result = MutableIntValueHashMap.empty();
        final MutableSet<String> discardTexts = MutableHashSet.empty();

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int dynAcc = row.get(0).toInt();
                final String text = row.get(1).toText();

                if (!discardTexts.contains(text)) {
                    final int foundDynAcc = result.get(text, 0);
                    if (foundDynAcc == 0) {
                        result.put(text, dynAcc);
                    }
                    else if (dynAcc != foundDynAcc) {
                        discardTexts.add(text);
                        result.removeAt(result.indexOfKey(text));
                    }
                }
            }
        }

        return result.toImmutable();
    }

    static ImmutableIntValueMap<String> readTextAndDynamicAcceptationsMapFromAcceptation(DbExporter.Database db, int acceptation) {
        return readTextAndDynamicAcceptationsMapFromStaticAcceptation(db, getMainAcceptation(db, acceptation));
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsTarget(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final int bunchSetsOffset = acceptations.columns().size();
        final int agentsOffset = bunchSetsOffset + bunchSets.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(bunchSets, acceptations.getConceptColumnIndex(), bunchSets.getBunchColumnIndex())
                .join(agents, bunchSetsOffset + bunchSets.getSetIdColumnIndex(), agents.getTargetBunchSetColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(agentsOffset + agents.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsSource(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;

        final int bunchSetsOffset = acceptations.columns().size();
        final int agentsOffset = bunchSetsOffset + bunchSets.columns().size();

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(bunchSets, acceptations.getConceptColumnIndex(), bunchSets.getBunchColumnIndex())
                .join(agents, bunchSetsOffset + bunchSets.getSetIdColumnIndex(), agents.getSourceBunchSetColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(agentsOffset + agents.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsRule(DbExporter.Database db, int staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(agents, acceptations.getConceptColumnIndex(), agents.getRuleColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(acceptations.columns().size() + agents.getIdColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static ImmutableIntSet readAgentsWhereAcceptationIsProcessed(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(bunchAcceptations.getAgentColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).filter(agentId -> agentId != 0).toSet().toImmutable();
    }

    static ImmutableIntSet findAllAgentsThatIncludedAcceptationInBunch(DbExporter.Database db, int bunch, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(bunchAcceptations.getAgentColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static Integer findConceptComposition(DbExporter.Database db, ImmutableIntSet concepts) {
        final int conceptCount = concepts.size();
        if (conceptCount == 0) {
            return 0;
        }
        else if (conceptCount == 1) {
            return concepts.valueAt(0);
        }

        final LangbookDbSchema.ConceptCompositionsTable table = LangbookDbSchema.Tables.conceptCompositions;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getComposedColumnIndex(), table.getComposedColumnIndex())
                .where(table.getItemColumnIndex(), concepts.valueAt(0))
                .select(table.getComposedColumnIndex(), table.columns().size() + table.getItemColumnIndex());

        final MutableIntKeyMap<ImmutableIntSet> possibleSets = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int compositionId = row.get(0).toInt();
                final int item = row.get(1).toInt();

                final ImmutableIntSet set = possibleSets.get(compositionId, ImmutableIntArraySet.empty());
                possibleSets.put(compositionId, set.add(item));
            }
        }

        final int mapSize = possibleSets.size();
        for (int i = 0; i < mapSize; i++) {
            if (possibleSets.valueAt(i).equalSet(concepts)) {
                return possibleSets.keyAt(i);
            }
        }

        return null;
    }

    /**
     * Return true if the given concept is required for any agent as source, diff, target or rule
     * @param db Readable database to be used.
     * @param concept Concept to by looked up
     * @return Whether there is at least one agent that uses the concept as source, target, diff or rule.
     */
    static boolean hasAgentsRequiringAcceptation(DbExporter.Database db, int concept) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        DbQuery query = new DbQuery.Builder(agents)
                .select(agents.getRuleColumnIndex(), agents.getTargetBunchSetColumnIndex(), agents.getSourceBunchSetColumnIndex(), agents.getDiffBunchSetColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int rule = row.get(0).toInt();

                if (rule == concept) {
                    return true;
                }

                final int targetBunchSet = row.get(1).toInt();
                final int sourceBunchSet = row.get(2).toInt();
                final int diffBunchSet = row.get(3).toInt();
                builder.add(sourceBunchSet).add(diffBunchSet).add(targetBunchSet);
            }
        }
        final ImmutableIntSet requiredBunchSets = builder.build();

        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        query = new DbQuery.Builder(bunchSets)
                .where(bunchSets.getBunchColumnIndex(), concept)
                .select(bunchSets.getSetIdColumnIndex());
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final int bunch = dbResult.next().get(0).toInt();
                if (requiredBunchSets.contains(bunch)) {
                    return true;
                }
            }
        }

        return false;
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

    static boolean isSymbolArrayPresent(DbExporter.Database db, SymbolArrayIdInterface symbolArray) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), symbolArray)
                .select(table.getIdColumnIndex());

        return selectExistingRow(db, query);
    }

    static boolean isLanguagePresent(DbExporter.Database db, LanguageIdInterface language) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), language)
                .select(table.getIdColumnIndex());

        return selectExistingRow(db, query);
    }

    static boolean isAnyLanguagePresent(DbExporter.Database db) {
        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(db, query);
    }

    static <AlphabetId extends AlphabetIdInterface> boolean isAlphabetPresent(DbExporter.Database db, AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .select(table.getIdColumnIndex());

        return selectExistingRow(db, query);
    }

    static <LanguageId, AlphabetId extends AlphabetIdInterface> boolean areAllAlphabetsFromSameLanguage(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, Set<AlphabetId> alphabets) {
        final LanguageId language = getLanguageFromAlphabet(db, languageIdSetter, alphabets.valueAt(0));
        if (language == null) {
            return false;
        }

        final int size = alphabets.size();
        for (int i = 1; i < size; i++) {
            final LanguageId lang = getLanguageFromAlphabet(db, languageIdSetter, alphabets.valueAt(i));
            if (lang == null || !lang.equals(language)) {
                return false;
            }
        }

        return true;
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableSet<AlphabetId> alphabetsWithinLanguage(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getLanguageColumnIndex(), table.getLanguageColumnIndex())
                .where(table.getIdColumnIndex(), alphabet)
                .select(offset + table.getIdColumnIndex());

        final ImmutableSet.Builder<AlphabetId> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                builder.add(alphabetIntSetter.getKeyFromDbValue(dbResult.next().get(0)));
            }
        }

        return builder.build();
    }

    static <LanguageId, AlphabetId extends AlphabetIdInterface> LanguageId getLanguageFromAlphabet(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .select(table.getLanguageColumnIndex());

        final DbValue value = selectOptionalFirstDbValue(db, query);
        return (value != null)? languageIdSetter.getKeyFromDbValue(value) : null;
    }

    private static <AlphabetId extends AlphabetIdInterface> IdentifiableResult readLanguageFromAlphabet(DbExporter.Database db, AlphabetId alphabet, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(alphabets)
                .join(acceptations, alphabets.getLanguageColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(alphabets.getIdColumnIndex(), alphabet)
                .select(
                        accOffset + acceptations.getConceptColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                int lang = row.get(0).toInt();
                String text = row.get(2).toText();
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(1));
                while (!preferredAlphabetFound && dbResult.hasNext()) {
                    row = dbResult.next();
                    if (preferredAlphabet.sameValue(row.get(1))) {
                        lang = row.get(0).toInt();
                        text = row.get(2).toText();
                        preferredAlphabetFound = true;
                    }
                }

                return new IdentifiableResult(lang, text);
            }
        }

        throw new IllegalArgumentException("alphabet " + alphabet + " not found");
    }

    static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> readAcceptationTextsAndLanguage(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, IntSetter<AlphabetId> alphabetIdSetter, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabetsTable = alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .join(alphabetsTable, table.getStringAlphabetColumnIndex(), alphabetsTable.getIdColumnIndex())
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(
                        table.getStringAlphabetColumnIndex(),
                        table.getStringColumnIndex(),
                        table.columns().size() + alphabetsTable.getLanguageColumnIndex());
        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        boolean languageSet = false;
        LanguageId language = null;
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AlphabetId alphabet = alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final String text = row.get(1).toText();

                if (!languageSet) {
                    language = languageIdSetter.getKeyFromDbValue(row.get(2));
                    languageSet = true;
                }
                else if (!language.sameValue(row.get(2))) {
                    throw new AssertionError();
                }

                builder.put(alphabet, text);
            }
        }

        return new ImmutablePair<>(new ImmutableCorrelation<>(builder.build()), language);
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutablePair<ImmutableCorrelation<AlphabetId>, Integer> readAcceptationTextsAndMain(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(
                        table.getStringAlphabetColumnIndex(),
                        table.getStringColumnIndex(),
                        table.getMainAcceptationColumnIndex());
        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        boolean mainAccSet = false;
        int mainAcc = 0;
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AlphabetId alphabet = alphabetIdSetter.getKeyFromDbValue(row.get(0));
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

    static String readAcceptationMainText(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getMainStringColumnIndex());

        return selectFirstRow(db, query).get(0).toText();
    }

    static <AlphabetId extends AlphabetIdInterface> String readConceptText(DbExporter.Database db, int concept, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int j1Offset = acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(strings, acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(j1Offset + strings.getStringAlphabetColumnIndex(), j1Offset + strings.getStringColumnIndex());

        String text;
        try (DbResult result = db.select(query)) {
            List<DbValue> row = result.next();
            boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(0));
            text = row.get(1).toText();
            while (!preferredAlphabetFound && result.hasNext()) {
                row = result.next();
                if (preferredAlphabet.sameValue(row.get(0))) {
                    preferredAlphabetFound = true;
                    text = row.get(1).toText();
                }
            }
        }

        return text;
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableMap<TableCellReference, TableCellValue> readTableContent(DbExporter.Database db, int dynamicAcceptation, AlphabetId preferredAlphabet) {
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
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(3));
                final String text = row.get(4).toText();
                final int staticAcc = row.get(5).toInt();

                TableCellValue cellValue = resultMap.get(ref, null);
                if (cellValue == null || preferredAlphabetFound) {
                    resultMap.put(ref, new TableCellValue(staticAcc, dynamicAcc, text));
                }
            }
        }

        return resultMap.toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface> DisplayableItem readConceptAcceptationAndText(DbExporter.Database db, int concept, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
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
            boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(0));
            acceptation = row.get(1).toInt();
            text = row.get(2).toText();
            while (!preferredAlphabetFound && result.hasNext()) {
                row = result.next();
                if (preferredAlphabet.sameValue(row.get(0))) {
                    preferredAlphabetFound = true;
                    acceptation = row.get(1).toInt();
                    text = row.get(2).toText();
                }
            }
        }

        return new DisplayableItem(acceptation, text);
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableList<DisplayableItem> readBunchSetAcceptationsAndTexts(DbExporter.Database db, int bunchSet, AlphabetId preferredAlphabet) {
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
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(2));
                String text = row.get(3).toText();

                while (cursor.hasNext()) {
                    row = cursor.next();
                    if (bunch == row.get(0).toInt()) {
                        if (!preferredAlphabetFound && preferredAlphabet.sameValue(row.get(2))) {
                            acc = row.get(1).toInt();
                            text = row.get(3).toText();
                            preferredAlphabetFound = true;
                        }
                    }
                    else {
                        builder.add(new DisplayableItem(acc, text));

                        bunch = row.get(0).toInt();
                        acc = row.get(1).toInt();
                        preferredAlphabetFound = preferredAlphabet.sameValue(row.get(2));
                        text = row.get(3).toText();
                    }
                }

                builder.add(new DisplayableItem(acc, text));
            }
        }

        return builder.build();
    }

    static ImmutableIntSet findBunchesWhereAcceptationIsIncluded(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .select(table.getBunchColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableList<DynamizableResult> readBunchesWhereAcceptationIsIncluded(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
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
                        bunchAcceptations.getAgentColumnIndex());

        final MutableIntSet bunchesWhereIncludedStatically = MutableIntArraySet.empty();
        final MutableIntPairMap acceptationsMap = MutableIntPairMap.empty();
        final MutableIntKeyMap<String> textsMap = MutableIntKeyMap.empty();

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(2));

                if (preferredAlphabetFound || acceptationsMap.get(bunch, 0) == 0) {
                    final int acc = row.get(1).toInt();
                    final String text = row.get(3).toText();
                    acceptationsMap.put(bunch, acc);
                    textsMap.put(bunch, text);
                }

                final int agent = row.get(4).toInt();
                if (agent == 0) {
                    bunchesWhereIncludedStatically.add(bunch);
                }
            }
        }

        return acceptationsMap.keySet().map(bunch -> new DynamizableResult(acceptationsMap.get(bunch), !bunchesWhereIncludedStatically.contains(bunch), textsMap.get(bunch))).toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableList<DynamizableResult> readAcceptationBunchChildren(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int bunchAccOffset = acceptations.columns().size();
        final int strOffset = bunchAccOffset + bunchAcceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(bunchAcceptations, acceptations.getConceptColumnIndex(), bunchAcceptations.getBunchColumnIndex())
                .join(strings, bunchAccOffset + bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(strOffset + strings.getMainAcceptationColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        bunchAccOffset + bunchAcceptations.getAgentColumnIndex());

        final MutableIntSet includedStatically = MutableIntArraySet.empty();
        final MutableIntKeyMap<String> accTexts = MutableIntKeyMap.empty();

        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int acc = row.get(0).toInt();
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(1));

                if (preferredAlphabetFound || accTexts.get(acc, null) == null) {
                    final String text = row.get(2).toText();
                    final int agent = row.get(3).toInt();
                    accTexts.put(acc, text);
                    if (agent == 0) {
                        includedStatically.add(acc);
                    }
                }
            }
        }

        return accTexts.keySet().map(acc -> new DynamizableResult(acc, !includedStatically.contains(acc), accTexts.get(acc))).toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableMap<AlphabetId, String> readAllAlphabets(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, AlphabetId preferredAlphabet) {
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

        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                AlphabetId alphabet = alphabetIdSetter.getKeyFromDbValue(row.get(0));
                String text = row.get(2).toText();

                while (result.hasNext()) {
                    row = result.next();
                    if (alphabet.sameValue(row.get(0))) {
                        if (preferredAlphabet.sameValue(row.get(1))) {
                            text = row.get(2).toText();
                        }
                    }
                    else {
                        builder.put(alphabet, text);

                        alphabet = alphabetIdSetter.getKeyFromDbValue(row.get(0));
                        text = row.get(2).toText();
                    }
                }

                builder.put(alphabet, text);
            }
        }

        return builder.build();
    }

    static <LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> ImmutableMap<AlphabetId, String> readAlphabetsForLanguage(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, LanguageId language, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AlphabetsTable alphabets = LangbookDbSchema.Tables.alphabets;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable stringQueries = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(stringQueries, accOffset + acceptations.getIdColumnIndex(), stringQueries.getDynamicAcceptationColumnIndex())
                .where(alphabets.getLanguageColumnIndex(), language)
                .select(
                        alphabets.getIdColumnIndex(),
                        strOffset + stringQueries.getStringAlphabetColumnIndex(),
                        strOffset + stringQueries.getStringColumnIndex());

        final MutableSet<AlphabetId> foundAlphabets = MutableHashSet.empty();
        final MutableMap<AlphabetId, String> result = MutableHashMap.empty();
        try (DbResult r = db.select(query)) {
            while (r.hasNext()) {
                final List<DbValue> row = r.next();
                final AlphabetId id = alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final boolean isPreferredAlphabet = preferredAlphabet.sameValue(row.get(1));

                if (isPreferredAlphabet || !foundAlphabets.contains(id)) {
                    foundAlphabets.add(id);
                    result.put(id, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface> AlphabetId readMainAlphabetFromAlphabet(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable alpTable = alphabets;
        final LangbookDbSchema.LanguagesTable langTable = LangbookDbSchema.Tables.languages;
        final DbQuery query = new DbQueryBuilder(alpTable)
                .join(langTable, alpTable.getLanguageColumnIndex(), langTable.getIdColumnIndex())
                .where(alpTable.getIdColumnIndex(), alphabet)
                .select(alpTable.columns().size() + langTable.getMainAlphabetColumnIndex());

        return alphabetIntSetter.getKeyFromDbValue(selectSingleRow(db, query).get(0));
    }

    static <LanguageId, AlphabetId extends AlphabetIdInterface> ImmutableMap<LanguageId, String> readAllLanguages(DbExporter.Database db, IntSetter<LanguageId> languageIdSetter, AlphabetId preferredAlphabet) {
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

        MutableSet<LanguageId> foundLanguages = MutableHashSet.empty();
        MutableMap<LanguageId, String> result = MutableHashMap.empty();

        try (DbResult r = db.select(query)) {
            while (r.hasNext()) {
                List<DbValue> row = r.next();
                final LanguageId lang = languageIdSetter.getKeyFromDbValue(row.get(0));
                final boolean isPreferredAlphabet = preferredAlphabet.sameValue(row.get(1));

                if (isPreferredAlphabet || !foundLanguages.contains(lang)) {
                    foundLanguages.add(lang);
                    result.put(lang, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllAcceptations(DbExporter.Database db, AlphabetId alphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(strings)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueMatch(strings.getMainAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(strings.getDynamicAcceptationColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllAcceptationsInBunch(DbExporter.Database db, AlphabetId alphabet, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .join(strings, bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(bunchAcceptations.columns().size() + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(bunchAcceptations.getAcceptationColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface> ImmutableIntKeyMap<String> readAllRules(DbExporter.Database db, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = agents.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(agents)
                .join(acceptations, agents.getRuleColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(agents.getRuleColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                final int rule = row.get(0).toInt();

                if (result.get(rule, null) == null || preferredAlphabet.sameValue(row.get(1))) {
                    result.put(rule, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> String readSameConceptTexts(DbExporter.Database db, int acceptation, AlphabetId alphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset = acceptations.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .whereColumnValueDiffer(acceptations.getIdColumnIndex(), accOffset + acceptations.getIdColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strOffset + strings.getStringColumnIndex());

        return db.select(query)
                .map(row -> row.get(0).toText())
                .reduce((a, b) -> a + ", " + b);
    }

    private static <AlphabetId extends AlphabetIdInterface> String readApplyRuleText(DbExporter.Database db, int acceptation, QuestionFieldDetails<AlphabetId> field) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int strOffset = ruledAcceptations.columns().size();
        final int agentsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQueryBuilder(ruledAcceptations)
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .where(agentsOffset + agents.getRuleColumnIndex(), field.rule)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), field.alphabet)
                .select(strOffset + strings.getStringColumnIndex());
        return selectSingleRow(db, query).get(0).toText();
    }

    static <AlphabetId extends AlphabetIdInterface> String readQuestionFieldText(DbExporter.Database db, int acceptation, QuestionFieldDetails<AlphabetId> field) {
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

    static Progress readQuizProgress(DbExporter.Database db, int quizId) {
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

    static <AlphabetId extends AlphabetIdInterface> ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails<AlphabetId>>> readQuizSelectorEntriesForBunch(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, int bunch) {
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

        final MutableIntKeyMap<ImmutableSet<QuestionFieldDetails<AlphabetId>>> resultMap = MutableIntKeyMap.empty();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int quizId = row.get(0).toInt();
                final QuestionFieldDetails<AlphabetId> field = new QuestionFieldDetails<>(alphabetIdSetter.getKeyFromDbValue(row.get(1)), row.get(2).toInt(), row.get(3).toInt());
                final ImmutableSet<QuestionFieldDetails<AlphabetId>> set = resultMap.get(quizId, ImmutableHashSet.empty());
                resultMap.put(quizId, set.add(field));
            }
        }

        return resultMap.toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> boolean checkMatching(ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> texts) {
        for (Map.Entry<AlphabetId, String> entry : startMatcher.entries()) {
            final String text = texts.get(entry.key(), null);
            if (text == null || !text.startsWith(entry.value())) {
                return false;
            }
        }

        for (Map.Entry<AlphabetId, String> entry : endMatcher.entries()) {
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
     * in agents that are applying a rule and has no diff bunches.
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
    static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> ImmutableIntKeyMap<String> readAllMatchingBunches(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, IntSetter<CorrelationId> correlationIdSetter, ImmutableCorrelation<AlphabetId> texts, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getDiffBunchSetColumnIndex(), 0)
                .select(agents.getSourceBunchSetColumnIndex(),
                        agents.getStartMatcherColumnIndex(),
                        agents.getEndMatcherColumnIndex());

        final SyncCacheMap<CorrelationId, ImmutableCorrelation<AlphabetId>> cachedCorrelations =
                new SyncCacheMap<>(id -> getCorrelationWithText(db, alphabetIdSetter, id));
        final ImmutableIntSet.Builder validBunchSetsBuilder = new ImmutableIntSetCreator();

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                int bunchSet = row.get(0).toInt();
                CorrelationId startMatcherId = correlationIdSetter.getKeyFromDbValue(row.get(1));
                CorrelationId endMatcherId = correlationIdSetter.getKeyFromDbValue(row.get(2));

                final ImmutableCorrelation<AlphabetId> startMatcher = cachedCorrelations.get(startMatcherId);
                final ImmutableCorrelation<AlphabetId> endMatcher = cachedCorrelations.get(endMatcherId);
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

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptations(DbExporter.Database db, AlphabetId alphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int strOffset = acceptations.columns().size() * 2;
        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, acceptations.columns().size() + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .whereColumnValueDiffer(acceptations.getIdColumnIndex(), acceptations.columns().size() + acceptations.getIdColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(acceptations.getIdColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptationsInBunch(DbExporter.Database db, AlphabetId alphabet, int bunch) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int accOffset1 = bunchAcceptations.columns().size();
        final int accOffset2 = accOffset1 + acceptations.columns().size();
        final int strOffset = accOffset2 + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .join(acceptations, bunchAcceptations.getAcceptationColumnIndex(), acceptations.getIdColumnIndex())
                .join(acceptations, accOffset1 + acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset2 + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .whereColumnValueDiffer(accOffset1 + acceptations.getIdColumnIndex(), accOffset2 + acceptations.getIdColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(accOffset1 + acceptations.getIdColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllRulableAcceptations(DbExporter.Database db, AlphabetId alphabet, int rule) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final int agentOffset = ruledAcceptations.columns().size();
        final int strOffset = agentOffset + agents.columns().size();
        final DbQuery query = new DbQueryBuilder(ruledAcceptations)
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(agentOffset + agents.getRuleColumnIndex(), rule)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(ruledAcceptations.getAcceptationColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllRulableAcceptationsInBunch(DbExporter.Database db, AlphabetId alphabet, int rule, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;

        final int ruledAccOffset = bunchAcceptations.columns().size();
        final int agentOffset = ruledAccOffset + ruledAcceptations.columns().size();
        final int strOffset = agentOffset + agents.columns().size();
        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .join(ruledAcceptations, bunchAcceptations.getAcceptationColumnIndex(), ruledAcceptations.getAcceptationColumnIndex())
                .join(agents, ruledAccOffset + ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(strings, ruledAccOffset + ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(agentOffset + agents.getRuleColumnIndex(), rule)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllPossibleAcceptationForField(DbExporter.Database db, int bunch, QuestionFieldDetails<AlphabetId> field) {
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

    static <AlphabetId extends AlphabetIdInterface> ImmutableIntSet readAllPossibleAcceptations(DbExporter.Database db, int bunch, ImmutableSet<QuestionFieldDetails<AlphabetId>> fields) {
        final Function<QuestionFieldDetails<AlphabetId>, ImmutableIntSet> mapFunc = field -> readAllPossibleAcceptationForField(db, bunch, field);
        return fields.map(mapFunc).reduce((a, b) -> a.filter(b::contains));
    }

    static ImmutableIntSet getAllRuledAcceptationsForAgent(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static ImmutableIntPairMap getFilteredAgentProcessedMap(DbExporter.Database db, int agentId, IntSet acceptations) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getAcceptationColumnIndex(), table.getIdColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int acceptation = row.get(0).toInt();
                if (acceptations.contains(acceptation)) {
                    builder.put(acceptation, row.get(1).toInt());
                }
            }
        }

        return builder.build();
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

    static <CorrelationId> AgentRegister<CorrelationId> getAgentRegister(DbExporter.Database db, IntSetter<CorrelationId> correlationIdSetter, int agentId) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), agentId)
                .select(table.getTargetBunchSetColumnIndex(),
                        table.getSourceBunchSetColumnIndex(),
                        table.getDiffBunchSetColumnIndex(),
                        table.getStartMatcherColumnIndex(),
                        table.getStartAdderColumnIndex(),
                        table.getEndMatcherColumnIndex(),
                        table.getEndAdderColumnIndex(),
                        table.getRuleColumnIndex());

        final List<DbValue> agentRow = selectOptionalSingleRow(db, query);
        if (agentRow != null) {
            final int targetBunchSetId = agentRow.get(0).toInt();
            final int sourceBunchSetId = agentRow.get(1).toInt();
            final int diffBunchSetId = agentRow.get(2).toInt();
            final CorrelationId startMatcherId = correlationIdSetter.getKeyFromDbValue(agentRow.get(3));
            final CorrelationId startAdderId = correlationIdSetter.getKeyFromDbValue(agentRow.get(4));
            final CorrelationId endMatcherId = correlationIdSetter.getKeyFromDbValue(agentRow.get(5));
            final CorrelationId endAdderId = correlationIdSetter.getKeyFromDbValue(agentRow.get(6));
            return new AgentRegister<>(targetBunchSetId, sourceBunchSetId, diffBunchSetId,
                    startMatcherId, startAdderId, endMatcherId, endAdderId, agentRow.get(7).toInt());
        }

        return null;
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> AgentDetails<AlphabetId> getAgentDetails(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, IntSetter<CorrelationId> correlationIdSetter, int agentId) {
        final AgentRegister<CorrelationId> register = getAgentRegister(db, correlationIdSetter, agentId);
        final ImmutableIntSet targetBunches = getBunchSet(db, register.targetBunchSetId);
        final ImmutableIntSet sourceBunches = getBunchSet(db, register.sourceBunchSetId);
        final ImmutableIntSet diffBunches = (register.sourceBunchSetId != register.diffBunchSetId)?
                getBunchSet(db, register.diffBunchSetId) : sourceBunches;

        final SyncCacheMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlationCache =
                new SyncCacheMap<>(id -> getCorrelationWithText(db, alphabetIdSetter, id));
        final ImmutableCorrelation<AlphabetId> startMatcher = correlationCache.get(register.startMatcherId);
        final ImmutableCorrelation<AlphabetId> startAdder = correlationCache.get(register.startAdderId);
        final ImmutableCorrelation<AlphabetId> endMatcher = correlationCache.get(register.endMatcherId);
        final ImmutableCorrelation<AlphabetId> endAdder = correlationCache.get(register.endAdderId);

        return new AgentDetails<>(targetBunches, sourceBunches, diffBunches,
                startMatcher, startAdder, endMatcher, endAdder, register.rule);
    }

    static <AlphabetId extends AlphabetIdInterface> QuizDetails<AlphabetId> getQuizDetails(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, int quizId) {
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

        final ImmutableList.Builder<QuestionFieldDetails<AlphabetId>> builder = new ImmutableList.Builder<>();
        int bunch = 0;

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                bunch = row.get(0).toInt();
                builder.add(new QuestionFieldDetails<>(alphabetIdSetter.getKeyFromDbValue(row.get(1)), row.get(2).toInt(), row.get(3).toInt()));
            }
        }

        final ImmutableList<QuestionFieldDetails<AlphabetId>> fields = builder.build();
        return fields.isEmpty()? null : new QuizDetails<>(bunch, fields);
    }

    static ImmutableIntPairMap getCurrentKnowledge(DbExporter.Database db, int quizId) {
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

    private static int getMainAcceptation(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getMainAcceptationColumnIndex());

        try (DbResult result = db.select(query)) {
            return result.hasNext()? result.next().get(0).toInt() : acceptation;
        }
    }

    static ImmutableIntKeyMap<String> getSampleSentences(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final int offset = strings.columns().size();

        final DbQuery query = new DbQuery.Builder(strings)
                .join(spans, strings.getDynamicAcceptationColumnIndex(), spans.getDynamicAcceptationColumnIndex())
                .where(strings.getMainAcceptationColumnIndex(), getMainAcceptation(db, acceptation))
                .select(offset + spans.getSentenceIdColumnIndex());

        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable().assign(sentenceId -> getSentenceText(db, sentenceId));
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> AcceptationDetailsModel<AlphabetId, CorrelationId> getAcceptationsDetails(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, IntSetter<CorrelationId> correlationIdSetter, int acceptation, AlphabetId preferredAlphabet) {
        final int concept = conceptFromAcceptation(db, acceptation);
        if (concept == 0) {
            return null;
        }

        final AcceptationOrigin origin = readOriginalAcceptation(db, acceptation, preferredAlphabet);
        final int originalAcceptationId = (origin != null)? origin.originalAcceptationId : 0;
        final int appliedAgentId = (origin != null)? origin.appliedAgent : 0;
        final String originalAcceptationText = (origin != null)? origin.originalAcceptationText : null;

        final int appliedRuleAcceptationId;
        final int appliedRuleId;
        final String appliedRuleAcceptationText;
        if (appliedAgentId != 0) {
            final AppliedRule appliedRule = readRulePreferredTextByAgent(db, appliedAgentId, preferredAlphabet);
            appliedRuleAcceptationId = appliedRule.acceptationId;
            appliedRuleId = appliedRule.rule;
            appliedRuleAcceptationText = appliedRule.text;
        }
        else {
            appliedRuleAcceptationId = 0;
            appliedRuleId = 0;
            appliedRuleAcceptationText = null;
        }

        ImmutablePair<ImmutableList<CorrelationId>, ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>>> correlationResultPair = getAcceptationCorrelations(db, alphabetIntSetter, correlationIdSetter, acceptation);
        final AlphabetId givenAlphabet = correlationResultPair.right.get(correlationResultPair.left.get(0)).keyAt(0);
        final IdentifiableResult languageResult = readLanguageFromAlphabet(db, givenAlphabet, preferredAlphabet);
        final MutableIntKeyMap<String> languageStrs = MutableIntKeyMap.empty();
        languageStrs.put(languageResult.id, languageResult.text);

        final ImmutableCorrelation<AlphabetId> texts = getAcceptationTexts(db, alphabetIntSetter, acceptation);
        final ImmutableIntKeyMap<ImmutableSet<AlphabetId>> acceptationsSharingTexts = readAcceptationsSharingTexts(db, alphabetIntSetter, acceptation);

        final ImmutableSet<AlphabetId> allAphabets = texts.keySet();
        final ImmutableIntSet accsSharingSome = acceptationsSharingTexts.filterNot(allAphabets::equalSet).keySet();
        final int accSharingSomeSize = accsSharingSome.size();
        final MutableIntKeyMap<String> accSharingSomeText = MutableIntKeyMap.empty((currentSize, desiredSize) -> accSharingSomeSize);
        for (int acc : accsSharingSome) {
            accSharingSomeText.put(acc, getAcceptationDisplayableText(db, acc, preferredAlphabet));
        }

        final ImmutablePair<IdentifiableResult, ImmutableIntKeyMap<String>> definition = readDefinitionFromAcceptation(db, acceptation, preferredAlphabet);
        final ImmutableIntKeyMap<String> subtypes = readSubtypesFromAcceptation(db, acceptation, preferredAlphabet);
        final ImmutableIntKeyMap<SynonymTranslationResult> synonymTranslationResults =
                readAcceptationSynonymsAndTranslations(db, acceptation, preferredAlphabet).toImmutable();
        for (IntKeyMap.Entry<SynonymTranslationResult> entry : synonymTranslationResults.entries()) {
            final int language = entry.value().language;
            if (languageStrs.get(language, null) == null) {
                languageStrs.put(language, readConceptText(db, language, preferredAlphabet));
            }
        }

        final ImmutableList<DynamizableResult> bunchesWhereAcceptationIsIncluded = readBunchesWhereAcceptationIsIncluded(db, acceptation, preferredAlphabet);
        final DerivedAcceptationsReaderResult morphologyResults = readDerivedAcceptations(db, acceptation, preferredAlphabet);
        final ImmutableList<DynamizableResult> bunchChildren = readAcceptationBunchChildren(db, acceptation, preferredAlphabet);
        final ImmutableIntPairMap involvedAgents = readAcceptationInvolvedAgents(db, acceptation);

        final ImmutableIntKeyMap<String> ruleTexts = (appliedRuleAcceptationId == 0)? morphologyResults.ruleTexts :
                morphologyResults.ruleTexts.put(appliedRuleId, appliedRuleAcceptationText);

        final ImmutableIntKeyMap<String> sampleSentences = getSampleSentences(db, acceptation);
        final int baseConceptAcceptationId = (definition.left != null)? definition.left.id : 0;
        final String baseConceptText = (definition.left != null)? definition.left.text : null;
        return new AcceptationDetailsModel<>(concept, languageResult, originalAcceptationId, originalAcceptationText,
                appliedAgentId, appliedRuleId, appliedRuleAcceptationId, correlationResultPair.left,
                correlationResultPair.right, texts, acceptationsSharingTexts, accSharingSomeText.toImmutable(), baseConceptAcceptationId,
                baseConceptText, definition.right, subtypes, synonymTranslationResults, bunchChildren,
                bunchesWhereAcceptationIsIncluded, morphologyResults.acceptations,
                ruleTexts, involvedAgents, morphologyResults.agentRules, languageStrs.toImmutable(), sampleSentences);
    }

    static <AlphabetId extends AlphabetIdInterface, CorrelationId extends CorrelationIdInterface> CorrelationDetailsModel<AlphabetId, CorrelationId> getCorrelationDetails(DbExporter.Database db, IntSetter<AlphabetId> alphabetIntSetter, IntSetter<CorrelationId> correlationIdSetter, CorrelationId correlationId, AlphabetId preferredAlphabet) {
        final ImmutableCorrelation<AlphabetId> correlation = getCorrelationWithText(db, alphabetIntSetter, correlationId);
        if (correlation.isEmpty()) {
            return null;
        }

        final ImmutableMap<AlphabetId, String> alphabets = readAllAlphabets(db, alphabetIntSetter, preferredAlphabet);
        final ImmutableIntKeyMap<String> acceptations = readAcceptationsIncludingCorrelation(db, correlationId, preferredAlphabet);

        final int entryCount = correlation.size();
        final MutableMap<AlphabetId, ImmutableSet<CorrelationId>> relatedCorrelationsByAlphabet = MutableHashMap.empty();
        final MutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> relatedCorrelations = MutableHashMap.empty();

        for (int i = 0; i < entryCount; i++) {
            final AlphabetId matchingAlphabet = correlation.keyAt(i);
            final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlations = readCorrelationsWithSameSymbolArray(db, alphabetIntSetter, correlationIdSetter, correlationId, matchingAlphabet);

            final int amount = correlations.size();
            final ImmutableSet.Builder<CorrelationId> setBuilder = new ImmutableHashSet.Builder<>();
            for (int j = 0; j < amount; j++) {
                final CorrelationId corrId = correlations.keyAt(j);
                if (relatedCorrelations.get(corrId, null) == null) {
                    relatedCorrelations.put(corrId, correlations.valueAt(j));
                }

                setBuilder.add(corrId);
            }
            relatedCorrelationsByAlphabet.put(matchingAlphabet, setBuilder.build());
        }

        return new CorrelationDetailsModel<>(alphabets, correlation, acceptations,
                relatedCorrelationsByAlphabet.toImmutable(), relatedCorrelations.toImmutable());
    }

    static ImmutableSet<SentenceSpan> getSentenceSpans(DbExporter.Database db, int sentenceId) {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSentenceIdColumnIndex(), sentenceId)
                .select(table.getStartColumnIndex(), table.getLengthColumnIndex(), table.getDynamicAcceptationColumnIndex());
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

    static ImmutableIntValueMap<SentenceSpan> getSentenceSpansWithIds(DbExporter.Database db, int sentenceId) {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSentenceIdColumnIndex(), sentenceId)
                .select(table.getIdColumnIndex(), table.getStartColumnIndex(), table.getLengthColumnIndex(), table.getDynamicAcceptationColumnIndex());
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

    static Integer getStaticAcceptationFromDynamic(DbExporter.Database db, int dynamicAcceptation) {
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

    static <AlphabetId extends AlphabetIdInterface> ImmutableCorrelation<AlphabetId> getAcceptationTexts(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getStringAlphabetColumnIndex(), table.getStringColumnIndex());

        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.put(alphabetIdSetter.getKeyFromDbValue(row.get(0)), row.get(1).toText());
            }
        }

        return new ImmutableCorrelation<>(builder.build());
    }

    private static <AlphabetId extends AlphabetIdInterface> String getAcceptationText(DbExporter.Database db, int acceptation, AlphabetId alphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getStringColumnIndex());
        return selectSingleRow(db, query).get(0).toText();
    }

    private static <AlphabetId extends AlphabetIdInterface> String getAcceptationDisplayableText(DbExporter.Database db, int acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getStringAlphabetColumnIndex(), strings.getStringColumnIndex());

        String text = null;
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                if (text == null || preferredAlphabet.sameValue(row.get(0))) {
                    text = row.get(1).toText();
                }
            }
        }

        return text;
    }

    /**
     * Checks if the given symbolArray is not used neither as a correlation nor as a conversion,
     * and then it is merely a sentence.
     */
    static boolean isSymbolArrayMerelyASentence(DbExporter.Database db, SymbolArrayIdInterface symbolArrayId) {
        return !isSymbolArrayUsedInAnyCorrelation(db, symbolArrayId) && !isSymbolArrayUsedInAnyConversion(db, symbolArrayId);
    }

    /**
     * Returns a set for all sentences linked to the given symbolArray.
     */
    static ImmutableIntSet findSentencesBySymbolArrayId(DbExporter.Database db, SymbolArrayIdInterface symbolArrayId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;

        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSymbolArrayColumnIndex(), symbolArrayId)
                .select(table.getIdColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    static <AlphabetId extends AlphabetIdInterface> boolean checkAlphabetCanBeRemoved(DbExporter.Database db, IntSetter<AlphabetId> alphabetIdSetter, AlphabetId alphabet) {
        // There must be at least another alphabet in the same language to avoid leaving the language without alphabets
        if (alphabetsWithinLanguage(db, alphabetIdSetter, alphabet).size() < 2) {
            return false;
        }

        // For now, let's assume that a source alphabet cannot be removed while the conversion already exists
        if (getConversionsMap(db, alphabetIdSetter).contains(alphabet)) {
            return false;
        }

        // First, quizzes using this alphabet should be removed
        return !isAlphabetUsedInQuestions(db, alphabet);
    }

    static <AlphabetId extends AlphabetIdInterface> boolean isAlphabetUsedInQuestions(DbExporter.Database db, AlphabetId alphabet) {
        final LangbookDbSchema.QuestionFieldSets table = LangbookDbSchema.Tables.questionFieldSets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getAlphabetColumnIndex(), alphabet)
                .select(table.getIdColumnIndex());

        try (DbResult dbResult = db.select(query)) {
            return dbResult.hasNext();
        }
    }

    static DefinitionDetails getDefinition(DbExporter.Database db, int concept) {
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = LangbookDbSchema.Tables.complementedConcepts;
        final LangbookDbSchema.ConceptCompositionsTable compositions = LangbookDbSchema.Tables.conceptCompositions;

        DbQuery query = new DbQuery.Builder(complementedConcepts)
                .where(complementedConcepts.getIdColumnIndex(), concept)
                .select(complementedConcepts.getBaseColumnIndex(), complementedConcepts.getComplementColumnIndex());

        final int baseConcept;
        final int compositionId;
        try (DbResult dbResult = db.select(query)) {
            if (!dbResult.hasNext()) {
                return null;
            }

            final List<DbValue> row = dbResult.next();
            baseConcept = row.get(0).toInt();
            compositionId = row.get(1).toInt();
        }

        query = new DbQuery.Builder(compositions)
                .where(compositions.getComposedColumnIndex(), compositionId)
                .select(compositions.getItemColumnIndex());
        ImmutableIntSet complements = db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
        if (complements.isEmpty() && compositionId != 0) {
            complements = new ImmutableIntSetCreator().add(compositionId).build();
        }

        return new DefinitionDetails(baseConcept, complements);
    }

    static Integer getSentenceConcept(DbExporter.Database db, int sentenceId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .select(table.getConceptColumnIndex());
        return selectOptionalFirstIntColumn(db, query);
    }

    static <SymbolArrayId> SymbolArrayId getSentenceSymbolArray(DbExporter.Database db, IntSetter<SymbolArrayId> symbolArrayIdSetter, int sentenceId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .select(table.getSymbolArrayColumnIndex());
        return symbolArrayIdSetter.getKeyFromDbValue(selectOptionalFirstDbValue(db, query));
    }

    static String getSentenceText(DbExporter.Database db, int sentenceId) {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final LangbookDbSchema.SymbolArraysTable texts = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .join(texts, table.getSymbolArrayColumnIndex(), texts.getIdColumnIndex())
                .where(table.getIdColumnIndex(), sentenceId)
                .select(table.columns().size() + texts.getStrColumnIndex());
        return selectOptionalFirstTextColumn(db, query);
    }

    private static final class SentenceConceptAndText {
        final int concept;
        final String text;

        SentenceConceptAndText(int concept, String text) {
            this.concept = concept;
            this.text = text;
        }
    }

    private static SentenceConceptAndText getSentenceConceptAndText(DbExporter.Database db, int sentenceId) {
        final LangbookDbSchema.SentencesTable sentences = LangbookDbSchema.Tables.sentences;
        final LangbookDbSchema.SymbolArraysTable texts = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(sentences)
                .join(texts, sentences.getSymbolArrayColumnIndex(), texts.getIdColumnIndex())
                .where(sentences.getIdColumnIndex(), sentenceId)
                .select(sentences.getConceptColumnIndex(), sentences.columns().size() + texts.getStrColumnIndex());

        try (DbResult dbResult = db.select(query)) {
            if (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                return new SentenceConceptAndText(row.get(0).toInt(), row.get(1).toText());
            }
        }

        return null;
    }

    static SentenceDetailsModel getSentenceDetails(DbExporter.Database db, int sentenceId) {
        final SentenceConceptAndText conceptAndText = getSentenceConceptAndText(db, sentenceId);
        if (conceptAndText == null) {
            return null;
        }

        final ImmutableSet<SentenceSpan> spans = getSentenceSpans(db, sentenceId);
        final ImmutableIntKeyMap<String> sameMeaningSentences = findSentenceIdsMatchingMeaning(db, sentenceId).assign(id -> getSentenceText(db, id));
        return new SentenceDetailsModel(conceptAndText.concept, conceptAndText.text, spans, sameMeaningSentences);
    }

    static ImmutablePair<ImmutableIntList, ImmutableIntKeyMap<ImmutableIntSet>> getAgentExecutionOrder(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getTargetBunchSetColumnIndex(), table.getSourceBunchSetColumnIndex(), table.getDiffBunchSetColumnIndex());

        final MutableIntKeyMap<ImmutableIntSet> agentDependencies = MutableIntKeyMap.empty();
        final MutableIntKeyMap<ImmutableIntSet> agentDependenciesWithZero = MutableIntKeyMap.empty();

        final MutableIntSet agentsWithoutSource = MutableIntArraySet.empty();
        final MutableIntKeyMap<ImmutableIntSet> agentTargets = MutableIntKeyMap.empty();
        final SyncCacheIntKeyNonNullValueMap<ImmutableIntSet> bunchSets = new SyncCacheIntKeyNonNullValueMap<>(setId -> getBunchSet(db, setId));
        try (DbResult dbResult = db.select(query)) {
            final ImmutableIntSet justZeroDependency = ImmutableIntArraySet.empty().add(0);
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int id = row.get(0).toInt();

                agentTargets.put(id, bunchSets.get(row.get(1).toInt()));

                final ImmutableIntSet sourceBunches = bunchSets.get(row.get(2).toInt());
                final ImmutableIntSet diffBunches = bunchSets.get(row.get(3).toInt());
                agentDependencies.put(id, sourceBunches.addAll(diffBunches));

                final ImmutableIntSet sourceBunchesWithZero = sourceBunches.isEmpty()? justZeroDependency : sourceBunches;
                agentDependenciesWithZero.put(id, sourceBunchesWithZero.addAll(diffBunches));

                if (sourceBunches.isEmpty()) {
                    agentsWithoutSource.add(id);
                }
            }
        }

        final int agentCount = agentDependencies.size();
        final int[] agentList = new int[agentCount];
        for (int i = 0; i < agentCount; i++) {
            final int agentId = agentDependencies.keyAt(i);
            final ImmutableIntSet targets = agentTargets.get(agentId);
            boolean inserted = false;
            for (int j = 0; j < i; j++) {
                if (agentDependencies.get(agentList[j]).anyMatch(targets::contains)) {
                    for (int k = i; k > j; k--) {
                        agentList[k] = agentList[k - 1];
                    }
                    agentList[j] = agentId;
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                agentList[i] = agentId;
            }
        }

        final ImmutableIntList sortedIdentifiers = ImmutableIntList.from(agentList);
        return new ImmutablePair<>(sortedIdentifiers, agentDependenciesWithZero.toImmutable());
    }
}
