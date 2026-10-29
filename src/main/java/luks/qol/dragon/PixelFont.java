package luks.qol.dragon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Крошечный шрифт 3×5 для надписи из частиц: цифры и буквы слова «БЛОК/БЛОКА/БЛОКОВ» (или BLOCK/BLOCKS).
 * Надпись — две строки: крупное число и под ним мелкое слово.
 */
public final class PixelFont {
    private PixelFont() {}

    /** Одна точка надписи: u — вправо, v — вверх (в блоках, от центра надписи). */
    public record Dot(double u, double v, boolean number) {}

    private static final double NUMBER_PITCH = 0.26D;
    private static final double WORD_PITCH = 0.15D;
    private static final double LINE_GAP = 0.30D;

    private static final Map<Character, String[]> GLYPHS = Map.ofEntries(
            Map.entry('0', new String[]{"111", "101", "101", "101", "111"}),
            Map.entry('1', new String[]{"010", "110", "010", "010", "111"}),
            Map.entry('2', new String[]{"111", "001", "111", "100", "111"}),
            Map.entry('3', new String[]{"111", "001", "111", "001", "111"}),
            Map.entry('4', new String[]{"101", "101", "111", "001", "001"}),
            Map.entry('5', new String[]{"111", "100", "111", "001", "111"}),
            Map.entry('6', new String[]{"111", "100", "111", "101", "111"}),
            Map.entry('7', new String[]{"111", "001", "001", "001", "001"}),
            Map.entry('8', new String[]{"111", "101", "111", "101", "111"}),
            Map.entry('9', new String[]{"111", "101", "111", "001", "111"}),
            // кириллица
            Map.entry('Б', new String[]{"111", "100", "110", "101", "110"}),
            Map.entry('Л', new String[]{"011", "101", "101", "101", "101"}),
            Map.entry('О', new String[]{"111", "101", "101", "101", "111"}),
            Map.entry('К', new String[]{"101", "101", "110", "101", "101"}),
            Map.entry('В', new String[]{"110", "101", "110", "101", "110"}),
            Map.entry('А', new String[]{"010", "101", "111", "101", "101"}),
            // латиница
            Map.entry('B', new String[]{"110", "101", "110", "101", "110"}),
            Map.entry('L', new String[]{"100", "100", "100", "100", "111"}),
            Map.entry('O', new String[]{"111", "101", "101", "101", "111"}),
            Map.entry('C', new String[]{"111", "100", "100", "100", "111"}),
            Map.entry('K', new String[]{"101", "101", "110", "101", "101"}),
            Map.entry('S', new String[]{"111", "100", "111", "001", "111"})
    );

    /** Слово в нужной форме и языке: 1 блок, 2 блока, 5 блоков / 1 block, 2 blocks. */
    public static String word(int count, boolean english) {
        if (english) {
            return count == 1 ? "BLOCK" : "BLOCKS";
        }
        int mod100 = count % 100;
        int mod10 = count % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return "БЛОКОВ";
        }
        if (mod10 == 1) {
            return "БЛОК";
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return "БЛОКА";
        }
        return "БЛОКОВ";
    }

    /** Раскладывает «число + слово» в точки, центрированные по обеим осям вокруг (0, 0). */
    public static List<Dot> layout(int distance, boolean english, double scale) {
        List<Dot> dots = new ArrayList<>();
        double numberPitch = NUMBER_PITCH * scale;
        double wordPitch = WORD_PITCH * scale;
        double gap = LINE_GAP * scale;
        double numberHeight = 5 * numberPitch;
        double wordHeight = 5 * wordPitch;
        double total = numberHeight + gap + wordHeight;
        double numberTop = total / 2.0D;
        double wordTop = numberTop - numberHeight - gap;
        addLine(dots, Integer.toString(distance), numberPitch, numberTop, true);
        addLine(dots, word(distance, english), wordPitch, wordTop, false);
        return dots;
    }

    private static void addLine(List<Dot> out, String text, double pitch, double top, boolean number) {
        int columns = text.length() * 4 - 1;
        for (int index = 0; index < text.length(); index++) {
            String[] glyph = GLYPHS.get(text.charAt(index));
            if (glyph == null) {
                continue;
            }
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (glyph[row].charAt(col) != '1') {
                        continue;
                    }
                    double column = index * 4 + col;
                    double u = (column - (columns - 1) / 2.0D) * pitch;
                    double v = top - (row + 0.5D) * pitch;
                    out.add(new Dot(u, v, number));
                }
            }
        }
    }
}
