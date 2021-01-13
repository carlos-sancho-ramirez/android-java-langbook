package sword.langbook3.android.db;

import sword.database.DbInsertQuery;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;

public interface SymbolArrayIdInterface extends IdWhereInterface {
    boolean sameValue(DbValue value);
    void put(int columnIndex, DbInsertQuery.Builder builder);
    void put(int columnIndex, DbUpdateQuery.Builder builder);
}
