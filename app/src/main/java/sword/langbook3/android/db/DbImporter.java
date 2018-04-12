package sword.langbook3.android.db;

public interface DbImporter {

    void init(Database db) throws UnableToImportException;

    interface Database extends DbExporter.Database {
        Integer insert(DbInsertQuery query);
    }

    class UnableToImportException extends Exception {
    }
}
