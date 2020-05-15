package sword.langbook3.android.models;

public final class DerivedAcceptationResult {
    public final int agent;
    public final String text;

    public DerivedAcceptationResult(int agent, String text) {
        if (agent == 0 || text == null) {
            throw new IllegalArgumentException();
        }

        this.agent = agent;
        this.text = text;
    }
}
