package sword.langbook3.android.db;

public interface DbInitializer {

    void init(Database db) throws UnableToInitializeException;

    interface Database {
        Integer insert(DbInsertQuery query);
        DbResult select(DbQuery query);
    }

    class UnableToInitializeException extends Exception {
    }
}
