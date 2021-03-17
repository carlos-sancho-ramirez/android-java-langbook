package sword.langbook3.android.db;

public final class CorrelationComposer {

    public static ImmutableCorrelation<AlphabetId> getEmptyCorrelation(LanguageId newLanguageId, int alphabetCount) {
        if (alphabetCount <= 0) {
            throw new IllegalArgumentException();
        }

        final int concept = newLanguageId.key;
        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        for (int rawAlphabet = concept + 1; rawAlphabet <= concept + alphabetCount; rawAlphabet++) {
            builder.put(new AlphabetId(rawAlphabet), null);
        }

        return builder.build();
    }
}
