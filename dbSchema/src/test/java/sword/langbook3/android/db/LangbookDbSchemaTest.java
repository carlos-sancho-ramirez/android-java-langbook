package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.Function;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntValueHashMap;
import sword.collections.ImmutableList;
import sword.collections.IntResultFunction;
import sword.collections.IntTransformer;
import sword.collections.IntValueMap;
import sword.collections.List;
import sword.collections.MutableHashSet;
import sword.collections.MutableSet;
import sword.collections.Predicate;
import sword.collections.Set;
import sword.collections.Transformer;
import sword.database.DbDeleteQuery;
import sword.database.DbIndex;
import sword.database.DbInsertQuery;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbTable;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.database.MutableSchemaDatabase;
import sword.langbook3.android.collections.TraversableUtils;
import sword.langbook3.android.db.LangbookDbSchema.Tables;

final class LangbookDbSchemaTest {

    private static final class Database implements MutableSchemaDatabase {

        private MutableSet<DbTable> _expectedTablesToCreate = MutableHashSet.empty();
        private MutableSet<DbIndex> _expectedIndexesToCreate = MutableHashSet.empty();
        private final boolean _expectUnicodeCharactersTableFill;

        Database(boolean expectUnicodeCharactersTableFill) {
            _expectUnicodeCharactersTableFill = expectUnicodeCharactersTableFill;
        }

        void expectTableCreation(MutableSet<DbTable> expectationsSet) {
            _expectedTablesToCreate = expectationsSet;
        }

        void expectIndexCreation(MutableSet<DbIndex> expectationsSet) {
            _expectedIndexesToCreate = expectationsSet;
        }

        @Override
        public void createTable(DbTable table) {
            if (!_expectedTablesToCreate.remove(table)) {
                throw new AssertionError("Unexpected method call for table " + table.name());
            }
        }

        @Override
        public void createIndex(DbIndex index) {
            if (!_expectedIndexesToCreate.remove(index)) {
                throw new AssertionError("Unexpected method call for index on table " + index.table.name() + " and column " + index.table.columns().get(index.column).name());
            }
        }

        @Override
        public boolean update(DbUpdateQuery query) {
            throw new AssertionError("Unexpected method call");
        }

        @Override
        public DbResult select(DbQuery query) {
            if (!_expectUnicodeCharactersTableFill || query.getTableCount() != 1 || query.getView(0) != Tables.symbolArrays) {
                throw new AssertionError("Unexpected method call");
            }

            return new DbResult() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public List<DbValue> next() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<List<DbValue>> toList() {
                    return ImmutableList.empty();
                }

                @Override
                public Set<List<DbValue>> toSet() {
                    return ImmutableHashSet.empty();
                }

                @Override
                public IntTransformer indexes() {
                    return ImmutableIntList.empty().iterator();
                }

                @Override
                public Transformer<List<DbValue>> filter(Predicate<? super List<DbValue>> predicate) {
                    return this;
                }

                @Override
                public Transformer<List<DbValue>> filterNot(Predicate<? super List<DbValue>> predicate) {
                    return this;
                }

                @Override
                public IntTransformer mapToInt(IntResultFunction<? super List<DbValue>> mapFunc) {
                    return ImmutableIntList.empty().iterator();
                }

                @Override
                public <U> Transformer<U> map(Function<? super List<DbValue>, ? extends U> mapFunc) {
                    return ImmutableList.<U>empty().iterator();
                }

                @Override
                public IntValueMap<List<DbValue>> count() {
                    return ImmutableIntValueHashMap.empty();
                }

                @Override
                public void close() {
                    // Nothing to be done
                }

                @Override
                public int getRemainingRows() {
                    return 0;
                }
            };
        }

        @Override
        public Integer insert(DbInsertQuery query) {
            throw new AssertionError("Unexpected method call");
        }

        @Override
        public boolean delete(DbDeleteQuery query) {
            throw new AssertionError("Unexpected method call");
        }

        void verify() {
            if (!_expectedTablesToCreate.isEmpty()) {
                throw new AssertionError("Expectation mismatch: createTable not called for table " + TraversableUtils.first(_expectedTablesToCreate) + ((_expectedTablesToCreate.size() > 1)? " among others" : ""));
            }

            if (!_expectedIndexesToCreate.isEmpty()) {
                final DbIndex index = TraversableUtils.first(_expectedIndexesToCreate);
                final DbTable table = index.table;
                throw new AssertionError("Expectation mismatch: createIndex not called for index on table " + table.name() + " and column " + table.columns().get(index.column).name() + ((_expectedTablesToCreate.size() > 1)? " among others" : ""));
            }
        }
    }

    @Test
    void testDatabaseCreation() {
        final MutableSet<DbTable> expectedTables = MutableHashSet.empty();
        expectedTables.add(Tables.acceptations);
        expectedTables.add(Tables.agents);
        expectedTables.add(Tables.alphabets);
        expectedTables.add(Tables.bunchAcceptations);
        expectedTables.add(Tables.bunchSets);
        expectedTables.add(Tables.characterCompositions);
        expectedTables.add(Tables.characterCompositionDefinitions);
        expectedTables.add(Tables.characterTokens);
        expectedTables.add(Tables.complementedConcepts);
        expectedTables.add(Tables.conceptCompositions);
        expectedTables.add(Tables.conversions);
        expectedTables.add(Tables.correlations);
        expectedTables.add(Tables.correlationArrays);
        expectedTables.add(Tables.knowledge);
        expectedTables.add(Tables.languages);
        expectedTables.add(Tables.questionFieldSets);
        expectedTables.add(Tables.quizDefinitions);
        expectedTables.add(Tables.ruleSentenceMatches);
        expectedTables.add(Tables.ruledAcceptations);
        expectedTables.add(Tables.ruledConcepts);
        expectedTables.add(Tables.searchHistory);
        expectedTables.add(Tables.sentences);
        expectedTables.add(Tables.spans);
        expectedTables.add(Tables.stringQueries);
        expectedTables.add(Tables.symbolArrays);
        expectedTables.add(Tables.unicodeCharacters);

        final MutableSet<DbIndex> expectedIndexes = MutableHashSet.empty();
        expectedIndexes.add(new DbIndex(Tables.stringQueries, Tables.stringQueries.getDynamicAcceptationColumnIndex()));
        expectedIndexes.add(new DbIndex(Tables.correlations, Tables.correlations.getCorrelationIdColumnIndex()));
        expectedIndexes.add(new DbIndex(Tables.correlationArrays, Tables.correlationArrays.getArrayIdColumnIndex()));
        expectedIndexes.add(new DbIndex(Tables.acceptations, Tables.acceptations.getConceptColumnIndex()));

        final Database db = new Database(false);
        db.expectTableCreation(expectedTables);
        db.expectIndexCreation(expectedIndexes);

        final LangbookDbSchema instance = LangbookDbSchema.getInstance();
        instance.setup(db);
        db.verify();
    }

    // TODO: This test must be improved

    @Test
    void testUpgradeDatabaseVersionFrom5To6() {
        final MutableSet<DbTable> expectedTables = MutableHashSet.empty();
        expectedTables.add(Tables.characterCompositions);
        expectedTables.add(Tables.characterCompositionDefinitions);
        expectedTables.add(Tables.characterTokens);
        expectedTables.add(Tables.unicodeCharacters);

        final Database db = new Database(true);
        db.expectTableCreation(expectedTables);

        final LangbookDbSchema instance = LangbookDbSchema.getInstance();
        instance.upgradeDatabaseVersion(db, 5, 6);
        db.verify();
    }
}
