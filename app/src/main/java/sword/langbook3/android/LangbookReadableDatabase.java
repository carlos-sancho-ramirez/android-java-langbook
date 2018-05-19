package sword.langbook3.android;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
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

    public static Integer findBunchSet(DbExporter.Database db, IntSet bunches) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        if (bunches.isEmpty()) {
            return table.nullReference();
        }

        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunches.iterator().next())
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

    public static int getMaxBunchSetId(DbExporter.Database db) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
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
}
