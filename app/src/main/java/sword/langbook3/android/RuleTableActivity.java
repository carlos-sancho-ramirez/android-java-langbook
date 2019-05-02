package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableMap;
import sword.collections.Map;
import sword.collections.MutableIntKeyMap;
import sword.database.Database;
import sword.langbook3.android.models.TableCellReference;
import sword.langbook3.android.models.TableCellValue;

import static sword.langbook3.android.db.LangbookReadableDatabase.getAcceptationTexts;
import static sword.langbook3.android.db.LangbookReadableDatabase.readConceptText;
import static sword.langbook3.android.db.LangbookReadableDatabase.readTableContent;

public class RuleTableActivity extends Activity {

    private interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    public static void open(Context context, int dynamicAcceptation) {
        Intent intent = new Intent(context, RuleTableActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, dynamicAcceptation);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rule_table_activity);

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final int dynAcc = getIntent().getIntExtra(ArgKeys.ACCEPTATION, 0);
        final Database db = DbManager.getInstance().getDatabase();
        final ImmutableMap<TableCellReference, TableCellValue> tableContent = readTableContent(db, dynAcc, preferredAlphabet);

        final MutableIntKeyMap<String> acceptationSet = MutableIntKeyMap.empty();
        final MutableIntKeyMap<String> ruleSet = MutableIntKeyMap.empty();

        for (Map.Entry<TableCellReference, TableCellValue> entry : tableContent.entries()) {
            final TableCellReference ref = entry.key();
            if (acceptationSet.get(ref.bunchSet, null) == null) {
                final ImmutableIntKeyMap<String> texts = getAcceptationTexts(db, entry.value().staticAcceptation);
                String text = texts.get(preferredAlphabet, null);
                if (text == null) {
                    text = texts.valueAt(0);
                }

                acceptationSet.put(ref.bunchSet, text);
            }

            if (ruleSet.get(ref.rule, null) == null) {
                ruleSet.put(ref.rule, readConceptText(DbManager.getInstance().getDatabase(), ref.rule, preferredAlphabet));
            }
        }

        final int acceptationCount = acceptationSet.size();
        int[] acceptations = new int[acceptationCount];
        for (int i = 0; i < acceptationCount; i++) {
            acceptations[i] = acceptationSet.keyAt(i);
        }

        final int ruleCount = ruleSet.size();
        int[] rules = new int[ruleCount];
        for (int i = 0; i < ruleCount; i++) {
            rules[i] = ruleSet.keyAt(i);
        }

        final int columnCount = ruleCount + 1;
        String[] texts = new String[(acceptationCount + 1) * columnCount];
        for (int i = 0; i < ruleCount; i++) {
            texts[i + 1] = ruleSet.get(rules[i]);
        }

        for (int i = 0; i < acceptationCount; i++) {
            texts[(i + 1) * columnCount] = acceptationSet.get(acceptations[i]);
            for (int j = 0; j < ruleCount; j++) {
                final int index = (i + 1) * columnCount + j + 1;
                final TableCellReference ref = new TableCellReference(acceptations[i], rules[j]);
                final TableCellValue value = tableContent.get(ref, null);
                texts[index] = (value != null)? value.text : null;
            }
        }

        final RuleTableView view = findViewById(R.id.ruleTable);
        view.setValues(columnCount, texts);
    }
}
