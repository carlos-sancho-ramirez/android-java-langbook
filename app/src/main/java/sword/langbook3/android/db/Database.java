package sword.langbook3.android.db;

public interface Database extends DbImporter.Database {
    boolean delete(DbTable table, int id);
    boolean update(DbUpdateQuery query);
}
