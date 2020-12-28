package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;

public interface BunchesChecker<AlphabetId> extends AcceptationsChecker<AlphabetId> {
    ImmutableIntSet getAcceptationsInBunch(int bunch);
    ImmutableIntSet findBunchesWhereAcceptationIsIncluded(int acceptation);
}
