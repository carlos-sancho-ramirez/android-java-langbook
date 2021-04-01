package sword.langbook3.android.db;

public interface BunchSetIdInterface extends IdInterface {

    /**
     * Whether the current set is known to be empty by definition.
     *
     * {@link sword.langbook3.android.db.LangbookDbSchema#EMPTY_BUNCH_SET_ID} is a reserved key in the database schema to declare empty bunch sets.
     * If this is true, there will not be necessity of querying the database, as we known for sure that this sets will be empty.
     */
    boolean isDeclaredEmpty();
}
