package sword.langbook3.android.db;

interface Predicate2<A, B> {
    boolean apply(A a, B b);
}
