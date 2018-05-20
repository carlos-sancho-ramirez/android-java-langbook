package sword.langbook3.android.db;

public interface DbImporter {

    void init(Database db) throws UnableToImportException;

    interface Database extends DbExporter.Database, DbInserter {
    }

    class UnableToImportException extends Exception {
    }
}
