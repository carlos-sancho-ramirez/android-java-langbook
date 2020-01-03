package sword.langbook3.android.db;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableIntSetCreator;
import sword.database.Database;
import sword.database.MemoryDatabase;
import sword.langbook3.android.models.DefinitionDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DefinitionsManagerTest {

    private DefinitionsManager createManager(Database db) {
        return new LangbookDatabaseManager(db);
    }

    @Test
    void testAddDefinitionForSingleComplement() {
        final MemoryDatabase db = new MemoryDatabase();
        final DefinitionsManager manager = createManager(db);

        final int animalConcept = manager.getMaxConcept() + 1;
        final int catConcept = animalConcept + 1;
        final int quadrupedConcept = catConcept + 1;

        manager.addDefinition(animalConcept, catConcept, new ImmutableIntSetCreator().add(quadrupedConcept).build());

        final DefinitionDetails definition = manager.getDefinition(catConcept);
        assertEquals(animalConcept, definition.baseConcept);
        assertEquals(1, definition.complements.size());
        assertEquals(quadrupedConcept, definition.complements.valueAt(0));
    }

    @Test
    void testAddDefinitionForMultipleComplements() {
        final MemoryDatabase db = new MemoryDatabase();
        final DefinitionsManager manager = createManager(db);

        final int animalConcept = manager.getMaxConcept() + 1;
        final int catConcept = animalConcept + 1;
        final int quadrupedConcept = catConcept + 1;
        final int felineConcept = quadrupedConcept + 1;

        manager.addDefinition(animalConcept, catConcept, new ImmutableIntSetCreator().add(quadrupedConcept).add(felineConcept).build());

        final DefinitionDetails definition = manager.getDefinition(catConcept);
        assertEquals(animalConcept, definition.baseConcept);
        assertEquals(2, definition.complements.size());
        if (quadrupedConcept == definition.complements.valueAt(0)) {
            assertEquals(felineConcept, definition.complements.valueAt(1));
        }
        else {
            assertEquals(felineConcept, definition.complements.valueAt(0));
            assertEquals(quadrupedConcept, definition.complements.valueAt(1));
        }
    }
}
