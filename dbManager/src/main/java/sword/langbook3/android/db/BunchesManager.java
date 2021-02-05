package sword.langbook3.android.db;

public interface BunchesManager<LanguageId, AlphabetId, CorrelationId, AcceptationId> extends AcceptationsManager<LanguageId, AlphabetId, CorrelationId, AcceptationId>, BunchesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> {

    /**
     * Include an acceptation within a bunch in a secure way.
     *
     * This method will check that the given combination is not already registered in the database table,
     * if so, it will do nothing and will return false.
     *
     * @param bunch Bunch identifier.
     * @param acceptation Acceptation identifier.
     * @return Whether the acceptation has been properly included.
     *         False if the acceptation is already included in the bunch.
     */
    boolean addAcceptationInBunch(int bunch, AcceptationId acceptation);

    boolean removeAcceptationFromBunch(int bunch, AcceptationId acceptation);
}
