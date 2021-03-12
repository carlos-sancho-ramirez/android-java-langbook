package sword.langbook3.android.db;

import sword.database.DbValue;

public interface RuleIdInterface extends IdWhereInterface, IdPutInterface {
    boolean sameValue(DbValue value);
    int getConceptId();
}
