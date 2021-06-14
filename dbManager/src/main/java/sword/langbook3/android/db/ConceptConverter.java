package sword.langbook3.android.db;

public interface ConceptConverter<ConceptId, T extends ConceptIdSupplier<ConceptId>> {
    T getKeyFromConceptId(ConceptId concept);
}
