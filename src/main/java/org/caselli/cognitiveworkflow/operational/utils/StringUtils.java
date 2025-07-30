package org.caselli.cognitiveworkflow.operational.utils;

/**
 * A utility class for string operations.
 * @author niccolocaselli
 */
public class StringUtils {

    /**
     * Converts a given string to uppercase snake case notation.
     * Handles camelCase, existing snake_case, kebab-case, and spaces.
     * Preserves existing uppercase words/acronyms unless followed by lowercase.
     * Examples:
     * "newUserProvided" -> "NEW_USER_PROVIDED"
     * "DELIVER_DATE" -> "DELIVER_DATE"
     * "Already_Snake_Case" -> "ALREADY_SNAKE_CASE"
     * "kebab-case" -> "KEBAB_CASE"
     * "with spaces" -> "WITH_SPACES"
     * "APIFactory" -> "API_FACTORY"
     * "MyAPI" -> "MY_API"
     * "SomeXMLParser" -> "SOME_XML_PARSER"
     *
     * @param input The string to convert.
     * @return The string converted to uppercase snake case, or null if the input is null.
     */
    public static String toUppercaseSnakeCase(String input) {
        if (input == null) {
            return null;
        }
        if (input.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        char previousChar = '\0';

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            char nextChar = (i < input.length() - 1) ? input.charAt(i + 1) : '\0';

            if (Character.isWhitespace(currentChar) || currentChar == '-' || currentChar == '_') {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != '_')
                    result.append('_');

                previousChar = currentChar;
                continue;
            }

            boolean needsUnderscore = false;
            if (Character.isUpperCase(currentChar)) {
                if (Character.isLowerCase(previousChar)) {
                    needsUnderscore = true;
                } else if (Character.isUpperCase(previousChar) && Character.isLowerCase(nextChar)) {
                    needsUnderscore = true;
                }
            }

            if (needsUnderscore) {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != '_')
                    result.append('_');

            }

            result.append(currentChar);
            previousChar = currentChar;
        }

        String finalResult = result.toString().toUpperCase();

        finalResult = finalResult.replaceAll("__+", "_");

        while (finalResult.startsWith("_"))
            finalResult = finalResult.substring(1);


        while (finalResult.endsWith("_"))
            finalResult = finalResult.substring(0, finalResult.length() - 1);

        return finalResult;
    }
}