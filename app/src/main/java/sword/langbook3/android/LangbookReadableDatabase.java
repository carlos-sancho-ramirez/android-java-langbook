package sword.langbook3.android;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import sword.collections.Function;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
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
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.DbExporter;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.LangbookDatabaseUtils.convertText;
import static sword.langbook3.android.LangbookDbSchema.NO_BUNCH;

public final class LangbookReadableDatabase {

    private final DbExporter.Database db;

    public LangbookReadableDatabase(DbExporter.Database db) {
        this.db = db;
    }

    public static DbResult.Row selectSingleRow(DbExporter.Database db, DbQuery query) {
        try (DbResult result = db.select(query)) {
            if (!result.hasNext()) {
                throw new AssertionError("Nothing found matching the given criteria");
            }

            final DbResult.Row row = result.next();
            if (result.hasNext()) {
                throw new AssertionError("Multiple rows found matching the given criteria");
            }

            return row;
        }
    }

    public Integer findSymbolArray(String str) {
        return findSymbolArray(db, str);
    }

    public Integer findCorrelation(IntPairMap correlation) {
        return findCorrelation(db, correlation);
    }

    public static Integer findSymbolArray(DbExporter.Database db, String str) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStrColumnIndex(), str)
                .select(table.getIdColumnIndex());
        final DbResult result = db.select(query);
        try {
            final Integer value = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError();
            }

            return value;
        }
        finally {
            result.close();
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
        final DbResult result = db.select(query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
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
        } finally {
            result.close();
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
        final DbResult result = db.select(query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
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
        finally {
            result.close();
        }

        return null;
    }

    public static Integer findCorrelationArray(DbImporter.Database db, IntList array) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getArrayIdColumnIndex(), table.getArrayIdColumnIndex())
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), array.get(0))
                .select(table.getArrayIdColumnIndex(), table.columns().size() + table.getCorrelationColumnIndex());
        final DbResult result = db.select(query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
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
        finally {
            result.close();
        }

        return null;
    }

    public static Integer findCorrelationArray(DbExporter.Database db, int... correlations) {
        if (correlations.length == 0) {
            return StreamedDatabaseConstants.nullCorrelationArrayId;
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), correlations[0])
                .select(table.getArrayIdColumnIndex());

        final DbResult result = db.select(query);
        try {
            while (result.hasNext()) {
                final int arrayId = result.next().get(0).toInt();
                final int[] array = getCorrelationArray(db, arrayId);
                if (Arrays.equals(correlations, array)) {
                    return arrayId;
                }
            }
        }
        finally {
            result.close();
        }

        return null;
    }

    public static ImmutableSet<ImmutableIntPair> findConversions(DbExporter.Database db) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final ImmutableSet.Builder<ImmutableIntPair> builder = new ImmutableHashSet.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int source = row.get(0).toInt();
                final int target = row.get(1).toInt();
                builder.add(new ImmutableIntPair(source, target));
            }
        }

        return builder.build();
    }

    public static boolean isAcceptationInBunch(DbExporter.Database db, int bunch, int acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .select(table.getIdColumnIndex());
        try (DbResult result = db.select(query)) {
            return result.hasNext();
        }
    }

    public static ImmutableList<ImmutablePair<String, String>> getConversion(DbExporter.Database db, ImmutableIntPair pair) {
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

        final ImmutableList.Builder<ImmutablePair<String, String>> builder = new ImmutableList.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final String sourceText = row.get(0).toText();
                final String targetText = row.get(1).toText();
                builder.add(new ImmutablePair<>(sourceText, targetText));
            }
        }

        return builder.build();
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
        final DbResult result = db.select(query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                final HashSet<Integer> set = new HashSet<>();
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
        finally {
            result.close();
        }

        return null;
    }

    public static Integer findAgentSet(DbExporter.Database db, IntSet agentSet) {
        if (agentSet.isEmpty()) {
            return 0;
        }

        final ImmutableIntSet set = agentSet.toImmutable();
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getAgentColumnIndex(), set.valueAt(0))
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getAgentColumnIndex());

        final DbResult result = db.select(query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int setId = row.get(0).toInt();
                ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
                builder.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    int newSetId = row.get(0).toInt();
                    if (newSetId != setId) {
                        if (set.equals(builder.build())) {
                            return setId;
                        }

                        setId = newSetId;
                        builder = new ImmutableIntSetBuilder();
                    }
                    builder.add(row.get(1).toInt());
                }

                if (set.equals(builder.build())) {
                    return setId;
                }
            }
        }
        finally {
            result.close();
        }

        return null;
    }

    public static Integer findRuledConcept(DbExporter.Database db, int rule, int concept) {
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

    public static Integer findQuizDefinition(DbExporter.Database db, int bunch, int setId) {
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

    public static ImmutableIntSet findAffectedAgentsByItsSource(DbExporter.Database db, int bunch) {
        if (bunch == 0) {
            return new ImmutableIntSetBuilder().build();
        }

        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getSourceBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(bunchSets.columns().size() + agents.getIdColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntPairMap findAffectedAgentsByItsSourceWithTarget(DbExporter.Database db, int bunch) {
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
                DbResult.Row row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet findAffectedAgentsByItsDiff(DbExporter.Database db, int bunch) {
        if (bunch == 0) {
            return new ImmutableIntSetBuilder().build();
        }

        final LangbookDbSchema.BunchSetsTable bunchSets = LangbookDbSchema.Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getDiffBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(bunchSets.columns().size() + agents.getIdColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntPairMap findAffectedAgentsByItsDiffWithTarget(DbExporter.Database db, int bunch) {
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
                final DbResult.Row row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet findAgentsWithoutSourceBunches(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getSourceBunchSetColumnIndex(), 0)
                .select(agents.getIdColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntPairMap findAgentsWithoutSourceBunchesWithTarget(DbExporter.Database db) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getSourceBunchSetColumnIndex(), 0)
                .select(agents.getIdColumnIndex(), agents.getTargetBunchColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                DbResult.Row row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet findAffectedAgentsByAcceptationCorrelationModification(DbExporter.Database db, int acceptation) {
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

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet findQuizzesByBunch(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = LangbookDbSchema.Tables.quizDefinitions;
        final DbQuery query = new DbQuery.Builder(quizzes)
                .where(quizzes.getBunchColumnIndex(), bunch)
                .select(quizzes.getIdColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static Integer findQuestionFieldSet(DbExporter.Database db, Iterable<QuestionFieldDetails> collection) {
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
                DbResult.Row row = result.next();
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

    public static Integer findSearchHistoryEntry(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.SearchHistoryTable table = LangbookDbSchema.Tables.searchHistory;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAcceptation(), acceptation)
                .select(table.getIdColumnIndex());

        try (DbResult result = db.select(query)) {
            final Integer id = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError();
            }
            return id;
        }
    }
    public static int getColumnMax(DbExporter.Database db, DbTable table, int columnIndex) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(DbQuery.max(columnIndex));
        final DbResult result = db.select(query);
        try {
            return result.hasNext()? result.next().get(0).toInt() : 0;
        }
        finally {
            result.close();
        }
    }

    public static int getMaxCorrelationId(DbExporter.Database db) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        return getColumnMax(db, table, table.getCorrelationIdColumnIndex());
    }

    public static int getMaxCorrelationArrayId(DbExporter.Database db) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        return getColumnMax(db, table, table.getArrayIdColumnIndex());
    }

    public static int getMaxConceptInAcceptations(DbExporter.Database db) {
        LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        return getColumnMax(db, table, table.getConceptColumnIndex());
    }

    public static int getMaxConceptInRuledConcepts(DbExporter.Database db) {
        LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        return getColumnMax(db, table, table.getIdColumnIndex());
    }

    public static int getMaxConcept(DbExporter.Database db) {
        int max = getMaxConceptInAcceptations(db);
        int temp = getMaxConceptInRuledConcepts(db);
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

    public static int getMaxQuestionFieldSetId(DbExporter.Database db) {
        LangbookDbSchema.QuestionFieldSets table = LangbookDbSchema.Tables.questionFieldSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    public static String getSymbolArray(DbExporter.Database db, int id) {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), id)
                .select(table.getStrColumnIndex());
        final DbResult result = db.select(query);
        try {
            final String str = result.hasNext()? result.next().get(0).toText() : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated identifiers");
            }
            return str;
        }
        finally {
            result.close();
        }
    }

    public static ImmutableIntPairMap getCorrelation(DbExporter.Database db, int id) {
        LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getCorrelationIdColumnIndex(), id)
                .select(table.getAlphabetColumnIndex(), table.getSymbolArrayColumnIndex());

        ImmutableIntPairMap.Builder corrBuilder = new ImmutableIntPairMap.Builder();
        final DbResult result = db.select(query);
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                corrBuilder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }
        finally {
            result.close();
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
                final DbResult.Row row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toText());
            }
        }
        return builder.build();
    }

    public static int[] getCorrelationArray(DbExporter.Database db, int id) {
        if (id == StreamedDatabaseConstants.nullCorrelationArrayId) {
            return new int[0];
        }

        LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getArrayIdColumnIndex(), id)
                .select(table.getArrayPositionColumnIndex(), table.getCorrelationColumnIndex());
        final DbResult dbResult = db.select(query);
        final int[] result = new int[dbResult.getRemainingRows()];
        final BitSet set = new BitSet();
        try {
            while (dbResult.hasNext()) {
                final DbResult.Row row = dbResult.next();
                final int pos = row.get(0).toInt();
                final int corr = row.get(1).toInt();
                if (set.get(pos)) {
                    throw new AssertionError("Malformed correlation array with id " + id);
                }

                set.set(pos);
                result[pos] = corr;
            }
        }
        finally {
            dbResult.close();
        }

        return result;
    }

    public static ImmutableIntPairMap getAcceptationsAndAgentSetsInBunch(DbExporter.Database db, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getAcceptationColumnIndex(), table.getAgentSetColumnIndex());
        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet getBunchSet(DbExporter.Database db, int setId) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .select(table.getBunchColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
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

        final MutableIntSet acceptations = MutableIntSet.empty();
        final ImmutableList.Builder<SearchResult> builder = new ImmutableList.Builder<>();

        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int acceptation = row.get(0).toInt();
                if (!acceptations.contains(acceptation)) {
                    acceptations.add(acceptation);
                    builder.add(new SearchResult(row.get(2).toText(), row.get(3).toText(), SearchResult.Types.ACCEPTATION, row.get(1).toInt(), acceptation));
                }
            }
        }

        return builder.build();
    }

    public static int conceptFromAcceptation(DbExporter.Database db, int accId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), accId)
                .select(table.getConceptColumnIndex());
        return selectSingleRow(db, query).get(0).toInt();
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

        return texts;
    }

    public static String readAcceptationText(DbExporter.Database db, int acceptation, int preferredAlphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getStringAlphabetColumnIndex(), strings.getStringColumnIndex());

        String text;
        try (DbResult result = db.select(query)) {
            DbResult.Row row = result.next();
            int alphabet = row.get(0).toInt();
            text = row.get(1).toText();
            while (alphabet != preferredAlphabet && result.hasNext()) {
                row = result.next();
                if (row.get(0).toInt() == preferredAlphabet) {
                    alphabet = preferredAlphabet;
                    text = row.get(1).toText();
                }
            }
        }

        return text;
    }

    public static ImmutablePair<ImmutableIntKeyMap<String>, Integer> readAcceptationTextsAndLanguage(DbExporter.Database db, int acceptation) {
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabetsTable = LangbookDbSchema.Tables.alphabets;
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
                final DbResult.Row row = result.next();
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

    public static ImmutablePair<ImmutableIntKeyMap<String>, Integer> readAcceptationTextsAndMain(DbExporter.Database db, int acceptation) {
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
                final DbResult.Row row = result.next();
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

        return text;
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
            DbResult.Row row = result.next();
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
                DbResult.Row row = cursor.next();
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
                DbResult.Row row = result.next();
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
        final MutableIntSet foundAlphabets = MutableIntSet.empty();
        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        try (DbResult r = db.select(query)) {
            while (r.hasNext()) {
                final DbResult.Row row = r.next();
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

    public static int readMainAlphabetFromAlphabet(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.AlphabetsTable alpTable = LangbookDbSchema.Tables.alphabets;
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

        MutableIntSet foundLanguages = MutableIntSet.empty();
        MutableIntKeyMap<String> result = MutableIntKeyMap.empty();

        try (DbResult r = db.select(query)) {
            while (r.hasNext()) {
                DbResult.Row row = r.next();
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

    public static ImmutableIntSet readAllAcceptations(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(strings)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueMatch(strings.getMainAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(strings.getDynamicAcceptationColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet readAllAcceptationsInBunch(DbExporter.Database db, int alphabet, int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = LangbookDbSchema.Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(strings, bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(bunchAcceptations.columns().size() + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(bunchAcceptations.getAcceptationColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }
        return builder.build();
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
                .orderBy(ruledConcepts.getRuleColumnIndex())
                .select(ruledConcepts.getRuleColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int rule = row.get(0).toInt();
                int textAlphabet = row.get(1).toInt();
                String text = row.get(2).toText();

                while (result.hasNext()) {
                    row = result.next();
                    if (rule == row.get(0).toInt()) {
                        if (textAlphabet != preferredAlphabet && row.get(1).toInt() == preferredAlphabet) {
                            textAlphabet = preferredAlphabet;
                            text = row.get(2).toText();
                        }
                    }
                    else {
                        builder.put(rule, text);

                        rule = row.get(0).toInt();
                        textAlphabet = row.get(1).toInt();
                        text = row.get(2).toText();
                    }
                }

                builder.put(rule, text);
            }
        }

        return builder.build();
    }

    private static boolean checkMatching(ImmutableIntKeyMap<String> matcher, int agentFlags, ImmutableIntKeyMap<String> texts) {
        final boolean matchWordStarting = AgentDetails.matchWordStarting(agentFlags);
        for (IntKeyMap.Entry<String> entry : matcher.entries()) {
            final String text = texts.get(entry.key(), null);
            if (text == null || matchWordStarting && !text.startsWith(entry.value()) ||
                    !matchWordStarting && !text.endsWith(entry.value())) {
                return false;
            }
        }

        return true;
    }

    public static ImmutableIntSet readBunchesFromSetOfBunchSets(DbExporter.Database db, ImmutableIntSet bunchSets) {
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (int bunchSet : bunchSets) {
            for (int bunch : getBunchSet(db, bunchSet)) {
                builder.add(bunch);
            }
        }

        return builder.build();
    }

    /**
     * Check all bunches including agents that may match the given texts.
     * For simplicity, this will only pick bunches declared as source bunches in agents that are applying a rule and has no diff bunches nor target bunch.
     * The agent's matcher must match the given string and there must be an adder different from the matcher.
     *
     * @param db Database to be used
     * @param texts Map containing the word to be matched. Keys in the map are alphabets and values are the text on those alphabets.
     * @param preferredAlphabet User's defined alphabet.
     * @return A map whose keys are bunches (concepts) and value are the suitable way to represent that bunch, according to the given preferred alphabet.
     */
    public static ImmutableIntKeyMap<String> readAllMatchingBunches(DbExporter.Database db, ImmutableIntKeyMap<String> texts, int preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;
        final LangbookDbSchema.CorrelationsTable correlations = LangbookDbSchema.Tables.correlations;

        final int corrOffset = agents.columns().size();
        final int symbolOffset = corrOffset + correlations.columns().size();

        final DbQuery query = new DbQuery.Builder(agents)
                .join(correlations, agents.getMatcherColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrOffset + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .whereColumnValueDiffer(agents.getMatcherColumnIndex(), agents.getAdderColumnIndex())
                .where(agents.getTargetBunchColumnIndex(), NO_BUNCH)
                .where(agents.getDiffBunchSetColumnIndex(), 0)
                .orderBy(agents.getIdColumnIndex())
                .select(
                        agents.getIdColumnIndex(),
                        agents.getSourceBunchSetColumnIndex(),
                        agents.getFlagsColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        symbolOffset + symbolArrays.getStrColumnIndex());

        final ImmutableIntSetBuilder validBunchSetsBuilder = new ImmutableIntSetBuilder();

        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int agentId = row.get(0).toInt();
                int bunchSet = row.get(1).toInt();
                int agentFlags = row.get(2).toInt();
                ImmutableIntKeyMap.Builder<String> matcherBuilder = new ImmutableIntKeyMap.Builder<>();
                matcherBuilder.put(row.get(3).toInt(), row.get(4).toText());

                while (result.hasNext()) {
                    row = result.next();
                    if (agentId == row.get(0).toInt()) {
                        matcherBuilder.put(row.get(3).toInt(), row.get(4).toText());
                    }
                    else {
                        if (checkMatching(matcherBuilder.build(), agentFlags, texts)) {
                            validBunchSetsBuilder.add(bunchSet);
                        }

                        agentId = row.get(0).toInt();
                        bunchSet = row.get(1).toInt();
                        agentFlags = row.get(2).toInt();

                        matcherBuilder = new ImmutableIntKeyMap.Builder<>();
                        matcherBuilder.put(row.get(3).toInt(), row.get(4).toText());
                    }
                }

                if (checkMatching(matcherBuilder.build(), agentFlags, texts)) {
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

    public static ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptations(DbExporter.Database db, int alphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = LangbookDbSchema.Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = LangbookDbSchema.Tables.stringQueries;

        final int strOffset = acceptations.columns().size() * 2;
        final DbQuery query = new DbQuery.Builder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, acceptations.columns().size() + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueDiffer(acceptations.getIdColumnIndex(), acceptations.columns().size() + acceptations.getIdColumnIndex())
                .select(acceptations.getIdColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet readAllPossibleSynonymOrTranslationAcceptationsInBunch(DbExporter.Database db, int alphabet, int bunch) {
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

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet readAllRulableAcceptations(DbExporter.Database db, int alphabet, int rule) {
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

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntSet readAllRulableAcceptationsInBunch(DbExporter.Database db, int alphabet, int rule, int bunch) {
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

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
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

    public static ImmutableIntSet readAllPossibleAcceptations(DbExporter.Database db, int bunch, ImmutableSet<QuestionFieldDetails> fields) {
        final Function<QuestionFieldDetails, ImmutableIntSet> mapFunc = field -> readAllPossibleAcceptationForField(db, bunch, field);
        return fields.map(mapFunc).reduce((a,b) -> a.filter(b::contains));
    }

    public static ImmutableIntSet getAllRuledAcceptationsForAgent(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getIdColumnIndex());

        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntPairMap getAgentProcessedMap(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getAcceptationColumnIndex(), table.getIdColumnIndex());

        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }

    public static ImmutableIntKeyMap<ImmutableIntSet> getAllAgentSetsContaining(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getAgentColumnIndex());

        final ImmutableIntKeyMap.Builder<ImmutableIntSet> mapBuilder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int setId = row.get(0).toInt();
                ImmutableIntSetBuilder setBuilder = new ImmutableIntSetBuilder();
                setBuilder.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    int newSetId = row.get(0).toInt();
                    if (newSetId != setId) {
                        mapBuilder.put(setId, setBuilder.build());
                        setId = newSetId;
                        setBuilder = new ImmutableIntSetBuilder();
                    }
                    setBuilder.add(row.get(1).toInt());
                }

                mapBuilder.put(setId, setBuilder.build());
            }
        }

        return mapBuilder.build();
    }

    public static final class AgentDetails {
        public final int targetBunch;
        public final ImmutableIntSet sourceBunches;
        public final ImmutableIntSet diffBunches;
        public final ImmutableIntKeyMap<String> matcher;
        public final ImmutableIntKeyMap<String> adder;
        public final int rule;
        public final int flags;

        public static boolean matchWordStarting(int flags) {
            return (flags & 1) != 0;
        }

        public boolean matchWordStarting() {
            return matchWordStarting(flags);
        }

        public AgentDetails(int targetBunch, ImmutableIntSet sourceBunches,
                ImmutableIntSet diffBunches, ImmutableIntKeyMap<String> matcher,
                ImmutableIntKeyMap<String> adder, int rule, int flags) {
            if (matcher == null) {
                matcher = ImmutableIntKeyMap.empty();
            }

            if (adder == null) {
                adder = ImmutableIntKeyMap.empty();
            }

            if (matcher.equals(adder)) {
                if (targetBunch == 0) {
                    throw new IllegalArgumentException();
                }
                rule = 0;
            }
            else if (rule == 0) {
                throw new IllegalArgumentException();
            }

            if (sourceBunches == null) {
                sourceBunches = new ImmutableIntSetBuilder().build();
            }

            if (diffBunches == null) {
                diffBunches = new ImmutableIntSetBuilder().build();
            }

            if (!sourceBunches.filter(diffBunches::contains).isEmpty()) {
                throw new IllegalArgumentException();
            }

            if (sourceBunches.contains(0)) {
                throw new IllegalArgumentException();
            }

            if (diffBunches.contains(0)) {
                throw new IllegalArgumentException();
            }

            this.targetBunch = targetBunch;
            this.sourceBunches = sourceBunches;
            this.diffBunches = diffBunches;
            this.matcher = matcher;
            this.adder = adder;
            this.rule = rule;
            this.flags = flags;
        }
    }

    public static AgentDetails getAgentDetails(DbExporter.Database db, int agentId) {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), agentId)
                .select(table.getTargetBunchColumnIndex(),
                        table.getSourceBunchSetColumnIndex(),
                        table.getDiffBunchSetColumnIndex(),
                        table.getMatcherColumnIndex(),
                        table.getAdderColumnIndex(),
                        table.getRuleColumnIndex(),
                        table.getFlagsColumnIndex());
        final DbResult.Row agentRow = selectSingleRow(db, query);
        final ImmutableIntSet sourceBunches = getBunchSet(db, agentRow.get(1).toInt());
        final ImmutableIntSet diffBunches = getBunchSet(db, agentRow.get(2).toInt());
        final ImmutableIntKeyMap<String> matcher = getCorrelationWithText(db, agentRow.get(3).toInt());
        final ImmutableIntKeyMap<String> adder = getCorrelationWithText(db, agentRow.get(4).toInt());
        return new AgentDetails(agentRow.get(0).toInt(), sourceBunches, diffBunches,
                matcher, adder, agentRow.get(5).toInt(), agentRow.get(6).toInt());
    }

    public static final class QuestionFieldDetails {
        public final int alphabet;
        public final int rule;
        public final int flags;

        public QuestionFieldDetails(int alphabet, int rule, int flags) {
            this.alphabet = alphabet;
            this.rule = rule;
            this.flags = flags;
        }

        public int getType() {
            return flags & LangbookDbSchema.QuestionFieldFlags.TYPE_MASK;
        }

        public boolean isAnswer() {
            return (flags & LangbookDbSchema.QuestionFieldFlags.IS_ANSWER) != 0;
        }

        @Override
        public int hashCode() {
            return (flags * 37 + rule) * 37 + alphabet;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof QuestionFieldDetails)) {
                return false;
            }

            final QuestionFieldDetails that = (QuestionFieldDetails) other;
            return alphabet == that.alphabet && rule == that.rule && flags == that.flags;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + alphabet + ',' + rule + ',' + flags + ')';
        }
    }

    public static final class QuizDetails {
        public final int bunch;
        public final ImmutableList<QuestionFieldDetails> fields;

        public QuizDetails(int bunch, ImmutableList<QuestionFieldDetails> fields) {
            if (fields == null || fields.size() < 2 || !fields.anyMatch(field -> !field.isAnswer()) || !fields.anyMatch(QuestionFieldDetails::isAnswer)) {
                throw new IllegalArgumentException();
            }

            this.bunch = bunch;
            this.fields = fields;
        }

        @Override
        public int hashCode() {
            return fields.hashCode() * 41 + bunch;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof QuizDetails)) {
                return false;
            }

            final QuizDetails that = (QuizDetails) other;
            return bunch == that.bunch && fields.equals(that.fields);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + bunch + ',' + fields.toString() + ')';
        }
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
                final DbResult.Row row = result.next();
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
                final DbResult.Row row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toInt());
            }
        }

        return builder.build();
    }
}
