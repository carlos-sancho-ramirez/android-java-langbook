package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntSetCreator;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.DefinitionDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sword.collections.IntTraversableTestUtils.assertContainsOnly;

interface DefinitionsManagerTest {

    DefinitionsManager createManager(MemoryDatabase db);

    @Test
    default void testAddDefinitionForSingleComplement() {
        final MemoryDatabase db = new MemoryDatabase();
        final DefinitionsManager manager = createManager(db);

        final int animalConcept = manager.getNextAvailableConceptId();
        final int catConcept = animalConcept + 1;
        final int quadrupedConcept = catConcept + 1;

        manager.addDefinition(animalConcept, catConcept, new ImmutableIntSetCreator().add(quadrupedConcept).build());

        final DefinitionDetails definition = manager.getDefinition(catConcept);
        assertEquals(animalConcept, definition.baseConcept);
        assertContainsOnly(quadrupedConcept, definition.complements);
    }

    @Test
    default void testAddDefinitionForMultipleComplements() {
        final MemoryDatabase db = new MemoryDatabase();
        final DefinitionsManager manager = createManager(db);

        final int animalConcept = manager.getNextAvailableConceptId();
        final int catConcept = animalConcept + 1;
        final int quadrupedConcept = catConcept + 1;
        final int felineConcept = quadrupedConcept + 1;

        manager.addDefinition(animalConcept, catConcept, new ImmutableIntSetCreator().add(quadrupedConcept).add(felineConcept).build());

        final DefinitionDetails definition = manager.getDefinition(catConcept);
        assertEquals(animalConcept, definition.baseConcept);
        assertContainsOnly(quadrupedConcept, felineConcept, definition.complements);
    }
}
