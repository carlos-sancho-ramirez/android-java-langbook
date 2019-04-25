package sword.langbook3.android.db;

public final class MorphologyResult {
    public final int agent;
    public final int dynamicAcceptation;
    public final int rule;
    public final String ruleText;
    public final String text;

    MorphologyResult(int agent, int dynamicAcceptation, int rule, String ruleText, String text) {
        this.agent = agent;
        this.dynamicAcceptation = dynamicAcceptation;
        this.rule = rule;
        this.ruleText = ruleText;
        this.text = text;
    }
}
