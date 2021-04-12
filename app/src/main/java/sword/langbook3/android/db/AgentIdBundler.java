package sword.langbook3.android.db;

import android.content.Intent;

public final class AgentIdBundler {

    public static AgentId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new AgentId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, AgentId agentId) {
        if (agentId != null) {
            intent.putExtra(key, agentId.key);
        }
    }

    private AgentIdBundler() {
    }
}
