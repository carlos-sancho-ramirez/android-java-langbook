package sword.langbook3.android.db;

public final class CorrelationComposer {

    public static ImmutableCorrelation<AlphabetId> getEmptyCorrelation(int newLanguageId, int alphabetCount) {
        if (alphabetCount <= 0) {
            throw new IllegalArgumentException();
        }

        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        for (int rawAlphabet = newLanguageId + 1; rawAlphabet <= newLanguageId + alphabetCount; rawAlphabet++) {
            builder.put(new AlphabetId(rawAlphabet), null);
        }

        return builder.build();
    }
}
