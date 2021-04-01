package sword.langbook3.android.db;

public interface BunchSetIntSetter<BunchSetId> extends IntSetter<BunchSetId> {
    /**
     * Returns the bunch set identifier for declared empty sets.
     *
     * The returned reference must return true on calling
     * {@link BunchSetIdInterface#isDeclaredEmpty()} and must be a truly empty
     * bunch set if querying it into the database.
     *
     * @return the bunch set identifier for declared empty sets.
     */
    BunchSetId getDeclaredEmpty();
}
