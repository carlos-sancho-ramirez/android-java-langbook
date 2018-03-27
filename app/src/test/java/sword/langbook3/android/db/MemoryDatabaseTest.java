package sword.langbook3.android.db;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class MemoryDatabaseTest {

    private final DbColumn textColumn = new DbTextColumn("myText");
    private final DbTable textTable = new DbTable("TextTable", textColumn);

    private final DbColumn uniqueTextColumn = new DbUniqueTextColumn("nonRepeatedText");
    private final DbTable uniqueTextTable = new DbTable("UniqueTextTable", uniqueTextColumn);

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
}
