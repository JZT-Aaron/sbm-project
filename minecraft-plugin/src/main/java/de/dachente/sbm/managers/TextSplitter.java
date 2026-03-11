package de.dachente.sbm.managers;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TextSplitter {
    public static List<String> split(String text, Locale locale, int maxLineLength) {
        List<String> lines = new ArrayList<>();

        BreakIterator boundary = BreakIterator.getLineInstance(locale);
        boundary.setText(text);

        int start = boundary.first();
        int breakPoint = boundary.next();
        int currentLineStart = start;

        while (breakPoint != BreakIterator.DONE) {
            if(breakPoint - currentLineStart > maxLineLength) {
                int lastSafeEnd = boundary.preceding(currentLineStart + maxLineLength + 1);

                if(lastSafeEnd <= currentLineStart) {
                    lastSafeEnd = breakPoint;
                }

                lines.add(text.substring(currentLineStart, lastSafeEnd).trim());
                currentLineStart = lastSafeEnd;
                boundary.following(currentLineStart);
            }
            breakPoint = boundary.next();
        }

        if(currentLineStart < text.length()) {
            lines.add(text.substring(currentLineStart).trim());
        }
        
        return lines;
    }
}
