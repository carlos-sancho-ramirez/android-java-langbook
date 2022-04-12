package sword.langbook3.android.models;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class CharacterCompositionDefinitionRegister {
    public final CharacterCompositionDefinitionArea first;
    public final CharacterCompositionDefinitionArea second;

    public CharacterCompositionDefinitionRegister(
            CharacterCompositionDefinitionArea first,
            CharacterCompositionDefinitionArea second) {
        ensureNonNull(first, second);
        this.first = first;
        this.second = second;
    }
}
