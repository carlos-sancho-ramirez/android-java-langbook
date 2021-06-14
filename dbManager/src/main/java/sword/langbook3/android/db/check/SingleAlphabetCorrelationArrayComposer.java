package sword.langbook3.android.db.check;

import sword.langbook3.android.db.ImmutableCorrelationArray;

final class SingleAlphabetCorrelationArrayComposer<AlphabetId> {
    final SingleAlphabetCorrelationComposer<AlphabetId> correlationComposer;

    SingleAlphabetCorrelationArrayComposer(SingleAlphabetCorrelationComposer<AlphabetId> correlationComposer) {
        this.correlationComposer = correlationComposer;
    }

    ImmutableCorrelationArray<AlphabetId> compose(String a) {
        return new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlationComposer.compose(a))
                .build();
    }

    ImmutableCorrelationArray<AlphabetId> compose(String a, String b) {
        return new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlationComposer.compose(a))
                .append(correlationComposer.compose(b))
                .build();
    }

    ImmutableCorrelationArray<AlphabetId> compose(String a, String b, String c) {
        return new ImmutableCorrelationArray.Builder<AlphabetId>()
                .append(correlationComposer.compose(a))
                .append(correlationComposer.compose(b))
                .append(correlationComposer.compose(c))
                .build();
    }
}
