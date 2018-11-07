package sword.langbook3.android;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.Predicate;

public final class CorrelationDetailsModel {
    /**
     * Map matching each alphabet with its alphabet name, according to the given preferred alphabet.
     */
    public final ImmutableIntKeyMap<String> alphabets;

    /**
     * Map matching each alphabet with its corresponding text representation for this correlation
     */
    public final ImmutableIntKeyMap<String> correlation;

    /**
     * Contains all acceptations that contains this correlation.
     * This map matches the acceptation identifier with its text representation
     * according to the given preferred alphabet.
     */
    public final ImmutableIntKeyMap<String> acceptations;

    /**
     * Contains the relationship for the all correlations that contains at least one of the text representation for an alphabet, according to its alphabet.
     * The key of this map is the alphabet that matches between this correlation and the ones in the value set.
     * The value of this map is a set of correlation identifiers.
     * {@link #relatedCorrelations} should contains keys for all values on the value sets found here.
     *
     * This map must contain the same keys that the map at {@link #correlation} field.
     * In case, no related correlation is found for a concrete alphabet, an empty set will be found on its value.
     */
    public final ImmutableIntKeyMap<ImmutableIntSet> relatedCorrelationsByAlphabet;

    /**
     * Contains all correlations that contains at least one text representation in common for the same alphabet.
     * The key of this map is its correlation identifier, while the value is the correlation itself (alphabet -> text representation).
     */
    public final ImmutableIntKeyMap<ImmutableIntKeyMap<String>> relatedCorrelations;

    public CorrelationDetailsModel(
            ImmutableIntKeyMap<String> alphabets,
            ImmutableIntKeyMap<String> correlation,
            ImmutableIntKeyMap<String> acceptations,
            ImmutableIntKeyMap<ImmutableIntSet> relatedCorrelationsByAlphabet,
            ImmutableIntKeyMap<ImmutableIntKeyMap<String>> relatedCorrelations) {
        if (alphabets == null || correlation == null || acceptations == null ||
                relatedCorrelationsByAlphabet == null || relatedCorrelations == null) {
            throw new IllegalArgumentException();
        }

        final Predicate<String> isNull = EqualUtils::isNull;
        if (correlation.isEmpty() || correlation.anyMatch(isNull)) {
            throw new IllegalArgumentException();
        }

        final ImmutableIntSet alphabetsKeySet = alphabets.keySet();
        final ImmutableIntSet correlationKeySet = correlation.keySet();
        final ImmutableIntSet relatedCorrelationsKeySet = relatedCorrelations.keySet();
        if (correlationKeySet.anyMatch(alphabet -> !alphabetsKeySet.contains(alphabet)) ||
                alphabets.anyMatch(isNull)) {
            throw new IllegalArgumentException();
        }

        if (acceptations.anyMatch(isNull)) {
            throw new IllegalArgumentException();
        }

        if (!correlationKeySet.equals(relatedCorrelationsByAlphabet.keySet()) ||
                relatedCorrelationsByAlphabet.anyMatch(EqualUtils::isNull)) {
            throw new IllegalArgumentException();
        }

        for (ImmutableIntSet set : relatedCorrelationsByAlphabet) {
            if (set.anyMatch(v -> !relatedCorrelationsKeySet.contains(v))) {
                throw new IllegalArgumentException();
            }
        }

        this.alphabets = alphabets;
        this.correlation = correlation;
        this.acceptations = acceptations;
        this.relatedCorrelationsByAlphabet = relatedCorrelationsByAlphabet;
        this.relatedCorrelations = relatedCorrelations;
    }
}
