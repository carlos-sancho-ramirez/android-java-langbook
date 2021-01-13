package sword.langbook3.android.db;

import sword.database.DbValue;

public interface LanguageIdInterface extends IdWhereInterface, IdPutInterface {
    boolean sameValue(DbValue value);
}
