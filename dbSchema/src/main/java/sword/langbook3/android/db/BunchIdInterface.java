package sword.langbook3.android.db;

public interface BunchIdInterface<ConceptId> extends ConceptualizableIdInterface<ConceptId> {

    /**
     * Key used in quizzes definitions to specify that there is no bunch linked to this quiz.
     * When this id is found, the quiz should pick any acceptation in the database that could match field restrictions.
     * This is the typed identifier for the {@link sword.langbook3.android.db.LangbookDbSchema#NO_BUNCH}.
     */
    boolean isNoBunchForQuiz();
}
