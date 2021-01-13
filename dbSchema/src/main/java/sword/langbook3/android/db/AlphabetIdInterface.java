package sword.langbook3.android.db;

import sword.database.DbValue;

public interface AlphabetIdInterface extends IdWhereInterface, IdPutInterface {
    boolean sameValue(DbValue value);
}
