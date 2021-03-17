package sword.langbook3.android.db;

public interface ConceptualizableSetter<ConceptId extends ConceptIdInterface, T extends ConceptualizableIdInterface<ConceptId>> extends IntSetter<T> {
    T getKeyFromConceptId(ConceptId concept);
}
