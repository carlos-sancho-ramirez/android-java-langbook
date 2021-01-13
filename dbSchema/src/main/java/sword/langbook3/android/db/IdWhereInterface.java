package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;

public interface IdWhereInterface {
    void where(int columnIndex, DbIdentifiableQueryBuilder builder);
}
