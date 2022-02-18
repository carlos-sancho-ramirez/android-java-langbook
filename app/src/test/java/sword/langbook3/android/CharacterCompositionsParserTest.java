package sword.langbook3.android;

import org.junit.jupiter.api.Test;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static sword.collections.SetTestUtils.assertEqualSet;
import static sword.collections.SizableTestUtils.assertSize;

public final class CharacterCompositionsParserTest {

    static <T> ImmutableSet<T> setOf(T a, T b, T c, T d) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).add(c).add(d).build();
    }

    static <T> ImmutableSet<T> setOf(T a, T b, T c, T d, T e, T f) {
        return new ImmutableHashSet.Builder<T>().add(a).add(b).add(c).add(d).add(e).add(f).build();
    }

    private void parse(CharacterCompositionsParser parser, String text) throws CharacterCompositionsParseException {
        final int length = text.length();
        for (int i = 0; i < length; i++) {
            parser.next(text.charAt(i));
        }
    }

    private void parseAndFinish(CharacterCompositionsParser parser, String text) throws CharacterCompositionsParseException {
        parse(parser, text);
        parser.finalReached();
    }

    private void checkParser(String linebreak1, String linebreak2) throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parseAndFinish(parser, "明日月1" + linebreak1 + "切匕刀1" + linebreak2);
        final CharacterCompositionsParserResult result = parser.getCharacterCompositions();
        assertEqualSet(setOf('明', '日', '月', '切', '匕', '刀'), result.characters.toSet());
        assertSize(2, result.compositions);
    }

    @Test
    public void testParser() throws CharacterCompositionsParseException {
        checkParser("\n", "");
    }

    @Test
    public void testParserWithCarriageReturn() throws CharacterCompositionsParseException {
        checkParser("\r\n", "");
    }

    @Test
    public void testParserWithBlankLineAtTheEnd() throws CharacterCompositionsParseException {
        checkParser("\n", "\n");
    }

    @Test
    public void testParserWithCarriageReturnAndBlankLineAtTheEnd() throws CharacterCompositionsParseException {
        checkParser("\r\n", "\r\n");
    }

    private void checkParserWithTokens(String linebreak1, String linebreak2) throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parseAndFinish(parser, "{goRight}五口2" + linebreak1 + "語言{goRight}1" + linebreak2);
        final CharacterCompositionsParserResult result = parser.getCharacterCompositions();
        assertEqualSet(setOf('五', '口', '語', '言'), result.characters.toSet());
        assertSize(2, result.compositions);
    }

    @Test
    public void testParserWithTokens() throws CharacterCompositionsParseException {
        checkParserWithTokens("\n", "");
    }

    @Test
    public void testParserWithCarriageReturnAndTokens() throws CharacterCompositionsParseException {
        checkParserWithTokens("\r\n", "");
    }

    @Test
    public void testParserWithTokensAndBlankLineAtTheEnd() throws CharacterCompositionsParseException {
        checkParserWithTokens("\n", "\n");
    }

    @Test
    public void testParserWithCarriageReturnAndBlankLineAtTheEndAndTokens() throws CharacterCompositionsParseException {
        checkParserWithTokens("\r\n", "\r\n");
    }

    private void assertPosition(int line, int column, CharacterCompositionsParseException ex) {
        final String expectedStart = "at (" + line + ", " + column + "). ";
        assertTrue(ex.getMessage().startsWith(expectedStart));
    }

    private void missingExceptionFailure() {
        fail("Exception not thrown");
    }

    @Test
    public void testParserErrorForDoubleOpenBrace() throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parser.next('{');

        try {
            parser.next('{');
            missingExceptionFailure();
        }
        catch (CharacterCompositionsParseException e) {
            assertPosition(1, 2, e);
        }
    }

    @Test
    public void testParserErrorForOpenBraceInATokenName() throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parser.next('{');
        parser.next('a');

        try {
            parser.next('{');
            missingExceptionFailure();
        }
        catch (CharacterCompositionsParseException e) {
            assertPosition(1, 3, e);
        }
    }

    @Test
    public void testParserErrorForLineBreakInATokenName() throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parser.next('{');

        try {
            parser.next('\n');
            missingExceptionFailure();
        }
        catch (CharacterCompositionsParseException e) {
            assertPosition(1, 2, e);
        }
    }

    @Test
    public void testParserErrorForCarriageReturnInATokenName() throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parser.next('{');

        try {
            parser.next('\r');
            missingExceptionFailure();
        }
        catch (CharacterCompositionsParseException e) {
            assertPosition(1, 2, e);
        }
    }

    @Test
    public void testParserErrorForEndOfFileInATokenName() throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parser.next('{');

        try {
            parser.finalReached();
            missingExceptionFailure();
        }
        catch (CharacterCompositionsParseException e) {
            // Nothing to be done
        }
    }

    private void checkParserErrorForInvalidCompositionType(char ch) throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parse(parser, "明日月");

        try {
            parser.next(ch);
            missingExceptionFailure();
        }
        catch (CharacterCompositionsParseException e) {
            assertPosition(1, 4, e);
        }
    }

    @Test
    public void testParserErrorForCharAsCompositionType() throws CharacterCompositionsParseException {
        checkParserErrorForInvalidCompositionType('a');
    }

    @Test
    public void testParserErrorForZeroAsCompositionType() throws CharacterCompositionsParseException {
        checkParserErrorForInvalidCompositionType('0');
    }

    @Test
    public void testParserErrorForOpenBraceAsCompositionType() throws CharacterCompositionsParseException {
        checkParserErrorForInvalidCompositionType('{');
    }

    @Test
    public void testParserErrorWhenCompositionAlreadyDefined() throws CharacterCompositionsParseException {
        final CharacterCompositionsParser parser = new CharacterCompositionsParser();
        parse(parser, "明日月1\n");

        try {
            parser.next('明');
            missingExceptionFailure();
        }
        catch (CharacterCompositionsParseException e) {
            assertPosition(2, 1, e);
        }
    }
}
