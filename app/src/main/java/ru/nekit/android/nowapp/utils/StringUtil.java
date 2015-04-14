package ru.nekit.android.nowapp.utils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by chuvac on 10.04.15.
 */
public class StringUtil {

    private static final int MAX_WORD_LENGTH = 12;
    private static final int MIN_WORD_LENGTH = 7;

    public static ArrayList<String> wrapText(String text) {
        ArrayList<String> textArray = new ArrayList<String>(Arrays.asList(text.split(" ")));
        ArrayList<String> textArrayWithWordLengthLimit = new ArrayList<String>();
        ArrayList<String> textArrayResult = new ArrayList<String>();
        int maxWordLength = 0;
        for (int i = 0; i < textArray.size(); i++) {
            String word = textArray.get(i);
            if (word.length() > MAX_WORD_LENGTH) {
                String wordPart = word.substring(0, MAX_WORD_LENGTH);
                if (word.length() - wordPart.length() >= 2) {
                    if (maxWordLength < wordPart.length()) {
                        maxWordLength = wordPart.length();
                    }
                    textArrayWithWordLengthLimit.add(wordPart + "-");
                    wordPart = word.substring(MAX_WORD_LENGTH, word.length());
                    if (maxWordLength < wordPart.length()) {
                        maxWordLength = wordPart.length();
                    }
                    textArrayWithWordLengthLimit.add(wordPart);
                } else {
                    textArrayWithWordLengthLimit.add(word);
                    if (maxWordLength < word.length()) {
                        maxWordLength = word.length();
                    }
                }
            } else {
                textArrayWithWordLengthLimit.add(word);
                if (maxWordLength < word.length()) {
                    maxWordLength = word.length();
                }
            }
        }
        maxWordLength = maxWordLength < MIN_WORD_LENGTH ? MIN_WORD_LENGTH : maxWordLength;
        String wordString;
        String wordStringTemp = "";
        for (int i = 0; i < textArrayWithWordLengthLimit.size(); i++) {
            String word = textArrayWithWordLengthLimit.get(i);
            if (word.length() >= maxWordLength) {
                if (!"".equals(wordStringTemp)) {
                    textArrayResult.add(getStringWithoutLastChar(wordStringTemp));
                    wordStringTemp = "";
                }
                textArrayResult.add(word);
            } else {
                wordString = wordStringTemp;
                wordStringTemp += word + " ";
                boolean isOverLength = wordStringTemp.replaceAll(" ", "").length() > maxWordLength;
                boolean isLast = i == textArrayWithWordLengthLimit.size() - 1;
                if (isOverLength && isLast) {
                    textArrayResult.add(getStringWithoutLastChar(wordString));
                    textArrayResult.add(word);
                } else if (isOverLength) {
                    textArrayResult.add(getStringWithoutLastChar(wordString));
                    wordString = "";
                    wordStringTemp = word + " ";
                } else if (isLast) {
                    textArrayResult.add(getStringWithoutLastChar(wordStringTemp));
                }
            }
        }
        return textArrayResult;
    }

    private static String getStringWithoutLastChar(String string) {
        return string.substring(0, string.length() - 1);
    }

}
