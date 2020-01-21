package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;

public interface BunchesChecker extends AcceptationsChecker {
    ImmutableIntSet getAcceptationsInBunch(int bunch);
    ImmutableIntSet findBunchesWhereAcceptationIsIncluded(int acceptation);
}
