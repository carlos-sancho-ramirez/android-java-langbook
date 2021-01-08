package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbInsertQuery;
import sword.database.DbValue;

public interface LanguageIdInterface {
    boolean sameValue(DbValue value);
    void where(int columnIndex, DbIdentifiableQueryBuilder builder);
    void put(int columnIndex, DbInsertQuery.Builder builder);
}
