package sword.langbook3.android;

import sword.collections.ImmutableIntPairMap;
import sword.collections.IntKeyMap;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.LangbookDbInserter.insertSymbolArray;
import static sword.langbook3.android.LangbookReadableDatabase.findAgentSet;
import static sword.langbook3.android.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.findSymbolArray;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxAgentSetId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationArrayId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationId;

public final class LangbookDatabase {

    private final DbImporter.Database db;
    private final LangbookReadableDatabase selector;
    private final LangbookDbInserter inserter;

    LangbookDatabase(DbImporter.Database db) {
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
}
