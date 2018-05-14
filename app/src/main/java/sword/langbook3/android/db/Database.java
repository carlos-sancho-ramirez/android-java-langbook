package sword.langbook3.android.db;

public interface Database extends DbImporter.Database {
    boolean delete(DbDeleteQuery query);
    boolean update(DbUpdateQuery query);
}
