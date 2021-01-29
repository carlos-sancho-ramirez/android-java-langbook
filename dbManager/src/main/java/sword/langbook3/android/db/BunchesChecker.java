package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;

public interface BunchesChecker<LanguageId, AlphabetId, CorrelationId> extends AcceptationsChecker<LanguageId, AlphabetId, CorrelationId> {
    ImmutableIntSet getAcceptationsInBunch(int bunch);
    ImmutableIntSet findBunchesWhereAcceptationIsIncluded(int acceptation);
}
