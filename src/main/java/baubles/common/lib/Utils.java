package baubles.common.lib;

public class Utils {
    private static final StringBuilder stripBuffer = new StringBuilder();
    private static final char[] formattingCodes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r' };

    public static String stripFormattingCodes(String string) {
        stripBuffer.setLength(0);

        boolean hasSectionChar = false;

        outer:
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if (hasSectionChar) {
                hasSectionChar = false;
                for (char formatCode : formattingCodes) {
                    if (c == formatCode) {
                        continue outer;
                    }
                }

                stripBuffer.append('§');
            }

            if (c == '§') {
                hasSectionChar = true;
                continue;
            }

            stripBuffer.append(c);
        }

        return stripBuffer.toString();
    }
}
