package sword.langbook3.android.sdb;

import sword.langbook3.android.db.LangbookDatabase;

public interface StreamedDatabaseConstants {

    /** Reserved for empty correlations */
    int nullCorrelationId = LangbookDatabase.nullCorrelationId;

    /** Reserved for empty correlations */
    int nullCorrelationArrayId = LangbookDatabase.nullCorrelationArrayId;

    /** Reserved for agents for null references */
    int nullBunchId = 0;

    /** Reserved for agents for null references */
    int nullRuleId = 0;

    /** First concept within the database that is considered to be a valid concept */
    int minValidConcept = 1;
}
