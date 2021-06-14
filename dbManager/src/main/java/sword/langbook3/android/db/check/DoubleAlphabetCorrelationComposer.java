package sword.langbook3.android.db.check;

import sword.langbook3.android.db.ImmutableCorrelation;

final class DoubleAlphabetCorrelationComposer<AlphabetId> {
    final AlphabetId first;
    final AlphabetId second;

    DoubleAlphabetCorrelationComposer(AlphabetId first, AlphabetId second) {
        this.first = first;
        this.second = second;
    }

    ImmutableCorrelation<AlphabetId> compose(String text1, String text2) {
        return new ImmutableCorrelation.Builder<AlphabetId>()
                .put(first, text1)
                .put(second, text2)
                .build();
    }
}
