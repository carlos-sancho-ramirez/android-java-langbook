package sword.langbook3.android.db.check;

import sword.langbook3.android.db.ImmutableCorrelationArray;

final class DoubleAlphabetCorrelationArrayComposer<AlphabetId> {
    final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer;

    DoubleAlphabetCorrelationArrayComposer(DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer) {
        this.correlationComposer = correlationComposer;
    }

    ImmutableCorrelationArray<AlphabetId> compose(String a1, String a2) {
        return new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlationComposer.compose(a1, a2))
                .build();
    }

    ImmutableCorrelationArray<AlphabetId> compose(String a1, String a2, String b1, String b2) {
        return new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlationComposer.compose(a1, a2))
                .append(correlationComposer.compose(b1, b2))
                .build();
    }

    ImmutableCorrelationArray<AlphabetId> compose(String a1, String a2, String b1, String b2, String c1, String c2) {
        return new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlationComposer.compose(a1, a2))
                .append(correlationComposer.compose(b1, b2))
                .append(correlationComposer.compose(c1, c2))
                .build();
    }
}
