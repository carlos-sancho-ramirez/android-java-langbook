package sword.langbook3.android;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

class CharacterCompositionsFileReader {

    private final Context _context;
    private final Uri _uri;

    CharacterCompositionsFileReader(Context context, Uri uri) {
        if (context == null || uri == null) {
            throw new IllegalArgumentException();
        }

        _context = context;
        _uri = uri;
    }

    CharacterCompositionsParserResult read() throws IOException, CharacterCompositionsParseException {
        final char[] buffer = new char[1024];
        int charsInBuffer = 0;
        int bufferIndex = 0;
        try (InputStreamReader reader = new InputStreamReader(_context.getContentResolver().openInputStream(_uri), Charset.forName("UTF-8"))) {
            final CharacterCompositionsParser parser = new CharacterCompositionsParser();
            while (charsInBuffer >= 0) {
                if (bufferIndex >= charsInBuffer) {
                    charsInBuffer = reader.read(buffer);
                    bufferIndex = 0;
                }

                if (bufferIndex < charsInBuffer) {
                    final char ch = buffer[bufferIndex++];
                    parser.next(ch);
                }
            }

            parser.finalReached();
            return parser.getCharacterCompositions();
        }
    }
}
