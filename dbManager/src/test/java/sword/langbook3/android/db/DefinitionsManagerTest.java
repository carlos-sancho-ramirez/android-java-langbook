package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.DefinitionDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sword.collections.TraversableTestUtils.assertContainsOnly;

interface DefinitionsManagerTest<ConceptId extends ConceptIdInterface> {

    DefinitionsManager<ConceptId> createManager(MemoryDatabase db);
    ConceptSetter<ConceptId> getConceptIdManager();

    @Test
    default void testAddDefinitionForSingleComplement() {
        final MemoryDatabase db = new MemoryDatabase();
        final DefinitionsManager<ConceptId> manager = createManager(db);

        final ConceptId animalConcept = manager.getNextAvailableConceptId();
        final ConceptId catConcept = getConceptIdManager().recheckAvailability(animalConcept, animalConcept);
        final ConceptId quadrupedConcept = getConceptIdManager().recheckAvailability(catConcept, catConcept);

        manager.addDefinition(animalConcept, catConcept, new ImmutableHashSet.Builder<ConceptId>().add(quadrupedConcept).build());

        final DefinitionDetails<ConceptId> definition = manager.getDefinition(catConcept);
        assertEquals(animalConcept, definition.baseConcept);
        assertContainsOnly(quadrupedConcept, definition.complements);
    }

    @Test
    default void testAddDefinitionForMultipleComplements() {
        final MemoryDatabase db = new MemoryDatabase();
        final DefinitionsManager<ConceptId> manager = createManager(db);

        final ConceptId animalConcept = manager.getNextAvailableConceptId();
        final ConceptId catConcept = getConceptIdManager().recheckAvailability(animalConcept, animalConcept);
        final ConceptId quadrupedConcept = getConceptIdManager().recheckAvailability(catConcept, catConcept);
        final ConceptId felineConcept = getConceptIdManager().recheckAvailability(quadrupedConcept, quadrupedConcept);

        manager.addDefinition(animalConcept, catConcept, new ImmutableHashSet.Builder<ConceptId>().add(quadrupedConcept).add(felineConcept).build());

        final DefinitionDetails<ConceptId> definition = manager.getDefinition(catConcept);
        assertEquals(animalConcept, definition.baseConcept);
        assertContainsOnly(quadrupedConcept, felineConcept, definition.complements);
    }
}
