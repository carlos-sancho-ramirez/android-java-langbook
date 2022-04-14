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

    @Override
    public int hashCode() {
        return first.hashCode() * 41 + second.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CharacterCompositionDefinitionRegister)) {
            return false;
        }

        final CharacterCompositionDefinitionRegister that = (CharacterCompositionDefinitionRegister) other;
        return first.equals(that.first) && second.equals(that.second);
    }
}
