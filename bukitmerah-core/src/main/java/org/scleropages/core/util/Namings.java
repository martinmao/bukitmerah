package org.scleropages.core.util; /**
 * Copyright 2001-2005 The Apache Software Foundation.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 */
public abstract class Namings {

    /**
     * Naming convention used in languages like Database or C, where words are in lower-case
     * letters, separated by underscores('_').
     *
     * @param input
     * @return
     */
    public static String snakeCaseName(String input) {
        if (input == null) return input; // garbage in, garbage out
        int length = input.length();
        StringBuilder result = new StringBuilder(length * 2);
        int resultLength = 0;
        boolean wasPrevTranslated = false;
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (i > 0 || c != '_') // skip first starting underscore
            {
                if (Character.isUpperCase(c)) {
                    if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
                        result.append('_');
                        resultLength++;
                    }
                    c = Character.toLowerCase(c);
                    wasPrevTranslated = true;
                } else {
                    wasPrevTranslated = false;
                }
                result.append(c);
                resultLength++;
            }
        }
        return resultLength > 0 ? result.toString() : input;
    }


    /**
     * Naming convention used in languages like Xml or Lisp, where words are in lower-case
     * letters, separated by hyphens('-').
     *
     * @param input
     * @return
     */
    public static String kebabCaseName(String input) {
        if (input == null) return input; // garbage in, garbage out
        int length = input.length();
        if (length == 0) {
            return input;
        }

        StringBuilder result = new StringBuilder(length + (length >> 1));

        int upperCount = 0;

        for (int i = 0; i < length; ++i) {
            char ch = input.charAt(i);
            char lc = Character.toLowerCase(ch);

            if (lc == ch) { // lower-case letter means we can get new word
                // but need to check for multi-letter upper-case (acronym), where assumption
                // is that the last upper-case char is start of a new word
                if (upperCount > 1) {
                    // so insert hyphen before the last character now
                    result.insert(result.length() - 1, '-');
                }
                upperCount = 0;
            } else {
                // Otherwise starts new word, unless beginning of string
                if ((upperCount == 0) && (i > 0)) {
                    result.append('-');
                }
                ++upperCount;
            }
            result.append(lc);
        }
        return result.toString();
    }
}
