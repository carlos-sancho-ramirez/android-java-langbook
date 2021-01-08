package sword.langbook3.android.db;

public abstract class ConceptId {
    final int key;

    ConceptId(int key) {
        if (key == 0) {
            throw new IllegalArgumentException();
        }

        this.key = key;
    }
}
