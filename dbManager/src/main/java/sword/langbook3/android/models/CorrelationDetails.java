package sword.langbook3.android.models;

import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.ImmutableCorrelation;

public interface CorrelationDetails<AlphabetId, CorrelationId, AcceptationId> {

    /**
     * Map matching each alphabet with its alphabet name, according to the given preferred alphabet.
     */
    ImmutableMap<AlphabetId, String> getAlphabets();

    /**
     * Map matching each alphabet with its corresponding text representation for this correlation
     */
    ImmutableCorrelation<AlphabetId> getCorrelation();

    /**
     * Contains all acceptations that contains this correlation.
     * This map matches the acceptation identifier with its text representation
     * according to the given preferred alphabet.
     */
    ImmutableMap<AcceptationId, String> getAcceptations();

    /**
     * Contains the relationship for the all correlations that contains at least one of the text representation for an alphabet, according to its alphabet.
     * The key of this map is the alphabet that matches between this correlation and the ones in the value set.
     * The value of this map is a set of correlation identifiers.
     * {@link #getRelatedCorrelations()} should contains keys for all values on the value sets found here.
     *
     * This map must contain the same keys that the map at {@link #getCorrelation()} field.
     * In case, no related correlation is found for a concrete alphabet, an empty set will be found on its value.
     */
    ImmutableMap<AlphabetId, ImmutableSet<CorrelationId>> getRelatedCorrelationsByAlphabet();

    /**
     * Contains all correlations that contains at least one text representation in common for the same alphabet.
     * The key of this map is its correlation identifier, while the value is the correlation itself (alphabet -&gt; text representation).
     */
    ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> getRelatedCorrelations();
}
