package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.MutableHashSet;
import sword.collections.MutableSet;
import sword.database.DbDeleteQuery;
import sword.database.DbIndex;
import sword.database.DbInsertQuery;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbTable;
import sword.database.DbUpdateQuery;
import sword.database.MutableSchemaDatabase;
import sword.langbook3.android.collections.TraversableUtils;
import sword.langbook3.android.db.LangbookDbSchema.Tables;

final class LangbookDbSchemaTest {

    private static final class Database implements MutableSchemaDatabase {

        private MutableSet<DbTable> _expectedTablesToCreate = MutableHashSet.empty();
        private MutableSet<DbIndex> _expectedIndexesToCreate = MutableHashSet.empty();

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
            throw new AssertionError("Unexpected method call");
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

        final Database db = new Database();
        db.expectTableCreation(expectedTables);
        db.expectIndexCreation(expectedIndexes);

        final LangbookDbSchema instance = LangbookDbSchema.getInstance();
        instance.setup(db);
        db.verify();
    }

    @Test
    void testUpgradeDatabaseVersionFrom5To6() {
        final MutableSet<DbTable> expectedTables = MutableHashSet.empty();
        expectedTables.add(Tables.characterCompositions);
        expectedTables.add(Tables.characterTokens);
        expectedTables.add(Tables.unicodeCharacters);

        final Database db = new Database();
        db.expectTableCreation(expectedTables);

        final LangbookDbSchema instance = LangbookDbSchema.getInstance();
        instance.upgradeDatabaseVersion(db, 5, 6);
        db.verify();
    }
}
