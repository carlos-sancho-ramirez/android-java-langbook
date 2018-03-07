package sword.langbook3.android.db;

public interface DbSchema {

    int getTableCount();
    DbTable getTable(int index);
}
