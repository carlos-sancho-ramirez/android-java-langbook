package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import sword.collections.MutableList;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.BunchIdManager;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.RuleIdManager;
import sword.langbook3.android.db.check.ClearScenario;
import sword.langbook3.android.db.check.Logger;

public final class PerformanceTestActivity extends Activity implements View.OnClickListener {

    public static void open(Context context) {
        final Intent intent = new Intent(context, PerformanceTestActivity.class);
        context.startActivity(intent);
    }

    public static void open(Activity context, int requestCode) {
        final Intent intent = new Intent(context, PerformanceTestActivity.class);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.performance_test_activity);
        findViewById(R.id.startButton).setOnClickListener(this);
    }

    private static final class LoggerEntry {
        final long time;
        final String text;

        LoggerEntry(long time, String text) {
            this.time = time;
            this.text = text;
        }
    }

    private static final class MyLogger implements Logger {

        final MutableList<LoggerEntry> entryList = MutableList.empty();

        @Override
        public void scenarioReached(String scenarioName) {
            entryList.append(new LoggerEntry(System.currentTimeMillis(), scenarioName));
        }
    }

    @Override
    public void onClick(View v) {
        final TextView textView = findViewById(R.id.resultText);

        clearDatabase();
        textView.append(runTest4());

        clearDatabase();
        textView.append(runTest20());
    }

    private void clearDatabase() {
        DbManager.getInstance().getWritableDatabase().close();
        new File(DbManager.getInstance().getDatabasePath()).delete();
    }

    private String runTest4() {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final MyLogger logger = new MyLogger();
        ClearScenario.setUp(manager, logger, AlphabetIdManager::conceptAsAlphabetId, BunchIdManager::conceptAsBunchId, RuleIdManager::conceptAsRuleId)
                .setLanguages()
                .addConversion()
                .add4IAdjectives()
                .addPastAgent()
                .addNegativeAgent()
                .add20IchidanVerbs()
                .addWishAgent();

        final long initialTime = logger.entryList.valueAt(0).time;
        return "\n\nTest finished! (4 adjectives)" + logger.entryList.toImmutable().skip(1).map(entry -> "\n" + (entry.time - initialTime) + ". " + entry.text).reduce((a, b) -> a + b);
    }

    private String runTest20() {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final MyLogger logger = new MyLogger();
        ClearScenario.setUp(manager, logger, AlphabetIdManager::conceptAsAlphabetId, BunchIdManager::conceptAsBunchId, RuleIdManager::conceptAsRuleId)
                .setLanguages()
                .addConversion()
                .add20IAdjectives()
                .addPastAgent()
                .addNegativeAgent()
                .add20IchidanVerbs()
                .addWishAgent();

        final long initialTime = logger.entryList.valueAt(0).time;
        return "\n\nTest finished! (20 adjectives)" + logger.entryList.toImmutable().skip(1).map(entry -> "\n" + (entry.time - initialTime) + ". " + entry.text).reduce((a, b) -> a + b);
    }
}
