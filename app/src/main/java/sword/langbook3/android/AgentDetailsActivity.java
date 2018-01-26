package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class AgentDetailsActivity extends Activity {

    private static final class BundleKeys {
        static final String AGENT = "a";
    }

    public static void open(Context context, int agent) {
        Intent intent = new Intent(context, AgentDetailsActivity.class);
        intent.putExtra(BundleKeys.AGENT, agent);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agent_details_activity);

        if (!getIntent().hasExtra(BundleKeys.AGENT)) {
            throw new IllegalArgumentException("agent identifier not provided");
        }

        final TextView tv = findViewById(R.id.textView);
        tv.setText("Agent #" + getIntent().getIntExtra(BundleKeys.AGENT, 0));
    }
}
