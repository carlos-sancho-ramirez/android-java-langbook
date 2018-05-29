package sword.langbook3.android.db;

public interface Database extends DbImporter.Database, Deleter {
    boolean update(DbUpdateQuery query);
}
