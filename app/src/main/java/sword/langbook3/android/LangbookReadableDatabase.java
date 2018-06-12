package sword.langbook3.android;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.langbook3.android.db.DbExporter;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

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

    public static Integer findCorrelationArray(DbImporter.Database db, ImmutableIntList array) {
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

        final ImmutableSet.Builder<ImmutableIntPair> builder = new ImmutableSet.Builder<>();
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
        final DbResult result = db.select(query);
        try {
            final Integer id = result.hasNext()? result.next().get(0).toInt() : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated ruled concepts");
            }
            return id;
        }
        finally {
            result.close();
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

    public static ImmutableIntSet findAffectedAgentsByAnyAcceptationChange(DbExporter.Database db) {
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

    public static ImmutableIntPairMap findAffectedAgentsByAnyAcceptationChangeWithTarget(DbExporter.Database db) {
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

    public static int getMaxBunchSetId(DbExporter.Database db) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    public static int getMaxAgentSetId(DbExporter.Database db) {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
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

    public static int conceptFromAcceptation(DbExporter.Database db, int accId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getIdColumnIndex(), accId)
                .select(table.getConceptColumnIndex());
        return selectSingleRow(db, query).get(0).toInt();
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
        final int strOffset = acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(alphabets.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int alphabet = row.get(0).toInt();
                int textAlphabet = row.get(1).toInt();
                String text = row.get(2).toText();

                while (result.hasNext()) {
                    row = result.next();
                    if (alphabet == row.get(0).toInt()) {
                        if (textAlphabet != preferredAlphabet && row.get(1).toInt() == preferredAlphabet) {
                            textAlphabet = preferredAlphabet;
                            text = row.get(2).toText();
                        }
                    }
                    else {
                        builder.put(alphabet, text);

                        alphabet = row.get(0).toInt();
                        textAlphabet = row.get(1).toInt();
                        text = row.get(2).toText();
                    }
                }

                builder.put(alphabet, text);
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
                .join(acceptations, ruledConcepts.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .orderBy(ruledConcepts.getRuleColumnIndex())
                .select(ruledConcepts.getIdColumnIndex(),
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

        public boolean matchWordStarting() {
            return (flags & 1) != 0;
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
}
