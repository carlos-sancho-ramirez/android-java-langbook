package sword.langbook3.android.db;

import org.junit.Test;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.IntSet;
import sword.collections.Procedure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class MemoryDatabaseTest {

    private final DbColumn textColumn = new DbTextColumn("myText");
    private final DbTable textTable = new DbTable("TextTable", textColumn);

    private final DbColumn uniqueTextColumn = new DbUniqueTextColumn("nonRepeatedText");
    private final DbTable uniqueTextTable = new DbTable("UniqueTextTable", uniqueTextColumn);

    private final DbColumn setIdColumn = new DbIntColumn("setId");
    private final DbColumn itemIdColumn = new DbIntColumn("itemId");
    private final DbTable setTable = new DbTable("SetTable", setIdColumn, itemIdColumn);

    private final DbColumn conceptColumn = new DbIntColumn("concept");
    private final DbColumn languageColumn = new DbIntColumn("language");
    private final DbColumn writtenColumn = new DbTextColumn("written");
    private final DbTable wordTable = new DbTable("WordTable", conceptColumn, languageColumn, writtenColumn);

    private final class State {
        final MemoryDatabase db = new MemoryDatabase();

        private Integer insertText(String value) {
            final int columnIndex = textTable.columns().indexOf(textColumn);
            final DbInsertQuery insertQuery = new DbInsertQuery.Builder(textTable)
                    .put(columnIndex, value)
                    .build();
            return db.insert(insertQuery);
        }

        private Integer insertUniqueText(String value) {
            final int columnIndex = uniqueTextTable.columns().indexOf(uniqueTextColumn);
            final DbInsertQuery insertQuery = new DbInsertQuery.Builder(uniqueTextTable)
                    .put(columnIndex, value)
                    .build();
            return db.insert(insertQuery);
        }

        private int obtainMaxSetId() {
            final DbQuery query = new DbQuery.Builder(setTable)
                    .select(DbQuery.max(setTable.columns().indexOf(setIdColumn)));
            final DbResult result = db.select(query);
            try {
                assertTrue(result.hasNext());
                final int max = result.next().get(0).toInt();
                assertFalse(result.hasNext());
                return max;
            }
            finally {
                result.close();
            }
        }

        private int insertSet(IntSet set) {
            final int setId = obtainMaxSetId() + 1;
            final int setIdColumnIndex = setTable.columns().indexOf(setIdColumn);
            final int itemIdColumnIndex = setTable.columns().indexOf(itemIdColumn);

            for (int itemId : set) {
                final DbInsertQuery query = new DbInsertQuery.Builder(setTable)
                        .put(setIdColumnIndex, setId)
                        .put(itemIdColumnIndex, itemId)
                        .build();
                db.insert(query);
            }

            return setId;
        }

        private int insertWord(int concept, int language, String written) {
            final DbInsertQuery query = new DbInsertQuery.Builder(wordTable)
                    .put(wordTable.columns().indexOf(conceptColumn), concept)
                    .put(wordTable.columns().indexOf(languageColumn), language)
                    .put(wordTable.columns().indexOf(writtenColumn), written)
                    .build();
            return db.insert(query);
        }

        private void assertText(int id, String expectedValue) {
            final int columnIndex = textTable.columns().indexOf(textColumn);
            final DbQuery selectQuery = new DbQuery.Builder(textTable)
                    .where(textTable.getIdColumnIndex(), id)
                    .select(columnIndex);
            final DbResult result = db.select(selectQuery);
            try {
                assertTrue(result.hasNext());
                assertEquals(expectedValue, result.next().get(0).toText());
                assertFalse(result.hasNext());
            }
            finally {
                result.close();
            }
        }

        private void assertUniqueText(int id, String expectedValue) {
            final int columnIndex = uniqueTextTable.columns().indexOf(uniqueTextColumn);
            final DbQuery selectQuery = new DbQuery.Builder(uniqueTextTable)
                    .where(uniqueTextTable.getIdColumnIndex(), id)
                    .select(columnIndex);
            final DbResult result = db.select(selectQuery);
            try {
                assertTrue(result.hasNext());
                assertEquals(expectedValue, result.next().get(0).toText());
                assertFalse(result.hasNext());
            }
            finally {
                result.close();
            }
        }

        private void assertSet(int setId, ImmutableIntSet set) {
            final DbQuery query = new DbQuery.Builder(setTable)
                    .where(setTable.columns().indexOf(setIdColumn), setId)
                    .select(setTable.columns().indexOf(itemIdColumn));
            final DbResult result = db.select(query);
            final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
            try {
                while (result.hasNext()) {
                    builder.add(result.next().get(0).toInt());
                }
            }
            finally {
                result.close();
            }

            assertEquals(set, builder.build());
        }
    }

    @Test
    public void testInsertASingleElementInTextTableAndSelectIt() {
        final State state = new State();
        final String value = "hello";
        final int id = state.insertText(value);
        state.assertText(id, value);
    }

    @Test
    public void testInsertTwoElementsInTextTableAndSelectThem() {
        final State state = new State();
        final String value1 = "hello";
        final String value2 = "bye";
        final int id1 = state.insertText(value1);
        final int id2 = state.insertText(value2);
        state.assertText(id1, value1);
        state.assertText(id2, value2);
    }

    @Test
    public void testInsertASingleElementInUniqueTextTableAndSelectIt() {
        final State state = new State();
        final String value = "hello";
        final int id = state.insertUniqueText(value);
        state.assertUniqueText(id, value);
    }

    @Test
    public void testInsertTwoElementsInUniqueTextTableAndSelectThem() {
        final State state = new State();
        final String value1 = "hello";
        final String value2 = "bye";
        final int id1 = state.insertUniqueText(value1);
        final int id2 = state.insertUniqueText(value2);
        state.assertUniqueText(id1, value1);
        state.assertUniqueText(id2, value2);
    }

    @Test
    public void testInsertSameElementTwiceInUniqueTextTableAndSelectIt() {
        final State state = new State();
        final String value = "hello";
        final int id = state.insertUniqueText(value);
        assertNull(state.insertUniqueText(value));
        state.assertUniqueText(id, value);
    }

    @Test
    public void testInsertAndRetrieveSets() {
        final State state = new State();

        final ImmutableIntSet primes = new ImmutableIntSetBuilder()
                .add(2).add(3).add(5).add(7).build();
        final int primesSetId = state.insertSet(primes);

        final ImmutableIntSet fibonacci = new ImmutableIntSetBuilder()
                .add(1).add(2).add(3).add(5).add(8).build();
        final int fibonacciSetId = state.insertSet(fibonacci);
        assertNotEquals(primesSetId, fibonacciSetId);

        final ImmutableIntSet even = new ImmutableIntSetBuilder()
                .add(2).add(4).add(6).add(8).add(10).build();
        final int evenSetId = state.insertSet(even);
        assertNotEquals(primesSetId, evenSetId);
        assertNotEquals(fibonacciSetId, evenSetId);

        state.assertSet(primesSetId, primes);
        state.assertSet(fibonacciSetId, fibonacci);
        state.assertSet(evenSetId, even);
    }

    @Test
    public void testInsertTextsAndSetsRetrieveAnIdMatchingJoinOfThem() {
        final State state = new State();

        final String name1 = "John";
        final String name2 = "Sarah";
        final String name3 = "Marie";
        final String name4 = "Robert";
        final String name5 = "Jill";

        final int johnId = state.insertText(name1);
        final int sarahId = state.insertText(name2);
        final int marieId = state.insertText(name3);
        final int robertId = state.insertText(name4);
        final int jillId = state.insertText(name5);

        final ImmutableIntSet males = new ImmutableIntSetBuilder()
                .add(johnId).add(robertId).build();
        final ImmutableIntSet females = new ImmutableIntSetBuilder()
                .add(sarahId).add(marieId).add(jillId).build();
        final ImmutableIntSet developers = new ImmutableIntSetBuilder()
                .add(sarahId).add(robertId).build();
        state.insertSet(males);
        state.insertSet(females);
        final int developersSetId = state.insertSet(developers);

        final DbQuery query = new DbQuery.Builder(setTable)
                .join(textTable, setTable.columns().indexOf(itemIdColumn), textTable.getIdColumnIndex())
                .where(setTable.columns().indexOf(setIdColumn), developersSetId)
                .select(setTable.columns().size() + textTable.columns().indexOf(textColumn));
        final DbResult result = state.db.select(query);
        final ImmutableList.Builder<String> builder = new ImmutableList.Builder<>(result.getRemainingRows());
        try {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toText());
            }
        }
        finally {
            result.close();
        }

        final ImmutableList<String> devNames = new ImmutableList.Builder<String>()
                .add(name2).add(name4).build();
        assertEquals(devNames, builder.build());
    }

    @Test
    public void testInsertTextsAndSetsRetrieveANonIdMatchingJoinOfThem() {
        final State state = new State();

        final String name1 = "John";
        final String name2 = "Sarah";
        final String name3 = "Marie";
        final String name4 = "Robert";
        final String name5 = "Jill";

        final int johnId = state.insertText(name1);
        final int sarahId = state.insertText(name2);
        final int marieId = state.insertText(name3);
        final int robertId = state.insertText(name4);
        final int jillId = state.insertText(name5);

        final ImmutableIntSet males = new ImmutableIntSetBuilder()
                .add(johnId).add(robertId).build();
        final ImmutableIntSet females = new ImmutableIntSetBuilder()
                .add(sarahId).add(marieId).add(jillId).build();
        final ImmutableIntSet developers = new ImmutableIntSetBuilder()
                .add(sarahId).add(robertId).build();
        state.insertSet(males);
        final int femalesSetId = state.insertSet(females);
        final int developersSetId = state.insertSet(developers);

        final DbQuery query = new DbQuery.Builder(textTable)
                .join(setTable, textTable.getIdColumnIndex(), setTable.columns().indexOf(itemIdColumn))
                .where(textTable.columns().indexOf(textColumn), name2)
                .select(textTable.columns().size() + setTable.columns().indexOf(setIdColumn));
        final DbResult result = state.db.select(query);
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }
        finally {
            result.close();
        }

        final ImmutableIntSet sarahGroups = new ImmutableIntSetBuilder()
                .add(femalesSetId).add(developersSetId).build();
        assertEquals(sarahGroups, builder.build());
    }

    @Test
    public void testInsertTextsAndSetsRetrieveANonMatchingJoinOfThem() {
        final State state = new State();

        final String name1 = "John";
        final String name2 = "Sarah";
        final String name3 = "Marie";
        final String name4 = "Robert";
        final String name5 = "Jill";
        final String name6 = "James";

        final int johnId = state.insertText(name1);
        final int sarahId = state.insertText(name2);
        final int marieId = state.insertText(name3);
        final int robertId = state.insertText(name4);
        final int jillId = state.insertText(name5);
        state.insertText(name6);

        final ImmutableIntSet males = new ImmutableIntSetBuilder()
                .add(johnId).add(robertId).build();
        final ImmutableIntSet females = new ImmutableIntSetBuilder()
                .add(sarahId).add(marieId).add(jillId).build();
        final ImmutableIntSet developers = new ImmutableIntSetBuilder()
                .add(sarahId).add(robertId).build();
        state.insertSet(males);
        state.insertSet(females);
        state.insertSet(developers);

        final DbQuery query = new DbQuery.Builder(textTable)
                .join(setTable, textTable.getIdColumnIndex(), setTable.columns().indexOf(itemIdColumn))
                .where(textTable.columns().indexOf(textColumn), name6)
                .select(textTable.columns().size() + setTable.columns().indexOf(setIdColumn));
        final DbResult result = state.db.select(query);
        try {
            assertFalse(result.hasNext());
        }
        finally {
            result.close();
        }
    }

    private final class WordTableCase {
        static final String wordEnBig = "big";
        static final String wordEnSmall = "small";
        static final String wordEsBig = "grande";
        static final String wordEsSmall = "pequeño";
        static final String wordEnHuge = "huge";
        static final String wordEsHuge = "enorme";

        static final int conceptBig = 1;
        static final int conceptSmall = 2;

        static final int languageEn = 1;
        static final int languageEs = 2;

        final State state;

        WordTableCase(State state) {
            this.state = state;
        }

        void initializeWords() {
            state.insertWord(conceptBig, languageEn, wordEnBig);
            state.insertWord(conceptBig, languageEs, wordEsBig);
            state.insertWord(conceptSmall, languageEn, wordEnSmall);
            state.insertWord(conceptSmall, languageEs, wordEsSmall);
            state.insertWord(conceptBig, languageEn, wordEnHuge);
            state.insertWord(conceptBig, languageEs, wordEsHuge);
        }

        private void performAssertion(ImmutableSet<String> expectedWords, Procedure<DbQuery.Builder> params) {
            final int conceptColumnIndex = wordTable.columns().indexOf(conceptColumn);
            final int writtenColumnIndex = wordTable.columns().indexOf(writtenColumn);

            final DbQuery.Builder queryBuilder = new DbQuery.Builder(wordTable)
                    .join(wordTable, conceptColumnIndex, conceptColumnIndex);
            params.apply(queryBuilder);
            final DbQuery query = queryBuilder.where(writtenColumnIndex, "big")
                    .select(wordTable.columns().size() + writtenColumnIndex);
            final DbResult result = state.db.select(query);
            final ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
            try {
                while (result.hasNext()) {
                    builder.add(result.next().get(0).toText());
                }
            }
            finally {
                result.close();
            }

            assertEquals(expectedWords, builder.build());
        }

        void assertTranslations() {
            final ImmutableSet<String> expectedWords = new ImmutableSet.Builder<String>().add(wordEsBig).add(wordEsHuge).build();
            performAssertion(expectedWords, builder -> {
                final int languageColumnIndex = wordTable.columns().indexOf(languageColumn);
                builder.whereColumnValueDiffer(languageColumnIndex, wordTable.columns().size() + languageColumnIndex);
            });
        }

        void assertSynonyms() {
            final ImmutableSet<String> expectedWords = new ImmutableSet.Builder<String>().add(wordEnHuge).build();
            performAssertion(expectedWords, builder -> {
                final int languageColumnIndex = wordTable.columns().indexOf(languageColumn);
                final int writtenColumnIndex = wordTable.columns().indexOf(writtenColumn);
                builder.whereColumnValueMatch(languageColumnIndex, wordTable.columns().size() + languageColumnIndex)
                        .whereColumnValueDiffer(writtenColumnIndex, wordTable.columns().size() + writtenColumnIndex);
            });
        }
    }

    @Test
    public void testLookForTranslationsInWordTable() {
        final State state = new State();
        final WordTableCase inst = new WordTableCase(state);
        inst.initializeWords();
        inst.assertTranslations();
    }

    @Test
    public void testLookForSynonymsInWordTable() {
        final State state = new State();
        final WordTableCase inst = new WordTableCase(state);
        inst.initializeWords();
        inst.assertSynonyms();
    }
}
