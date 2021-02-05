package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableSet;

public interface BunchesChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> extends AcceptationsChecker<LanguageId, AlphabetId, CorrelationId, AcceptationId> {
    ImmutableSet<AcceptationId> getAcceptationsInBunch(int bunch);
    ImmutableIntSet findBunchesWhereAcceptationIsIncluded(AcceptationId acceptation);
}
