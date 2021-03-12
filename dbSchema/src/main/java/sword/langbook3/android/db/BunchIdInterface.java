package sword.langbook3.android.db;

import sword.database.DbValue;

public interface BunchIdInterface extends IdWhereInterface, IdPutInterface {
    boolean sameValue(DbValue value);
    int getConceptId();

    /**
     * Key used in quizzes definitions to specify that there is no bunch linked to this quiz.
     * When this id is found, the quiz should pick any acceptation in the database that could match field restrictions.
     * This is the typed identifier for the {@link sword.langbook3.android.db.LangbookDbSchema#NO_BUNCH}.
     */
    boolean isNoBunchForQuiz();
}
