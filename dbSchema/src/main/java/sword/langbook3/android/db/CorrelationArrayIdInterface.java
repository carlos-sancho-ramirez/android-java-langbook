package sword.langbook3.android.db;

import sword.database.DbValue;

public interface CorrelationArrayIdInterface extends IdWhereInterface, IdPutInterface {
    boolean isEmptyReference();
    boolean sameValue(DbValue value);
}
