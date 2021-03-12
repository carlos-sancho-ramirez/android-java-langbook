package sword.langbook3.android.db;

import sword.collections.ImmutableSet;

public interface BunchesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId, BunchId> extends AcceptationsChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> {
    ImmutableSet<AcceptationId> getAcceptationsInBunch(BunchId bunch);
    ImmutableSet<BunchId> findBunchesWhereAcceptationIsIncluded(AcceptationId acceptation);
}
