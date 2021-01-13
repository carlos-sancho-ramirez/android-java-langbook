package sword.langbook3.android.db;

import sword.database.DbInsertQuery;
import sword.database.DbValue;

public interface AlphabetIdInterface extends IdWhereInterface {
    boolean sameValue(DbValue value);
    void put(int columnIndex, DbInsertQuery.Builder builder);
}
