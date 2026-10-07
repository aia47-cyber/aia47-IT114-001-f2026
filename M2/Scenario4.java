package M2;
// copilot: disable

// @ts-nocheck

public class Scenario4 extends BaseClass {
    private static String[] array1 = { "hello world!", "java programming", "special@#$%^&characters", "numbers 123 456",
            "mIxEd CaSe InPut!" };
    private static String[] array2 = { "hello world", "java programming", "this is a title case test",
            "capitalize every word", "mixEd CASE input" };
    private static String[] array3 = { "  hello   world  ", "java    programming  ",
            "  extra    spaces  between   words   ",
            "      leading and trailing spaces      ", "multiple      spaces" };
    private static String[] array4 = { "hello world", "java programming", "short", "a", "even" };

    private static void transformText(String[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printScenario4ArrayInfo(arr, arrayNumber);
        // This should be solved without Copilot auto-completion, to toggle it, click
        // the Copilot chat bubble at the top of the editor.
        // Configure inline suggestions to "Disabled Inline Suggestions" (or similar)
        // when writing code for this problem.

        // Challenge 1: Remove non-alphanumeric characters except spaces
        // Challenge 2: Convert text to Title Case
        // Challenge 3: Remove leading/trailing spaces and remove duplicate spaces
        // between words
        // Result 1-3: Assign final phrase to `placeholderForModifiedPhrase`
        // Challenge 4 (extra credit): Extract up to middle 3 characters (beginning
        // starts at middle of phrase, exclude the first and last character for shorter
        // phrases)
        // Assign result to 'placeholderForMiddleCharacters'
        // If not enough characters in a word, instead assign "Not enough characters" to
        // `placeholderForMiddleCharacters`

        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        /*
         * aia47 10/5/2026
         * grab current phase by index i
         * remove every character that is not a letter digit or space (replaceAll)
         * trim string of leading and trailing spaces
         * collapse multiple spaces into one
         * convert whole string into lowercase
         * split phrase into words by 1 space
         * uppercase each starting character on each word
         * join words back together
         * assign the result to placheholder for phrase
         * use placeholder and get length
         * if length too short print phrase "Not eanough characters"
         * find the middle index of the phrase and take up to 3 characters starting
         * there. For shorter phrases, exclude the first and last character before
         * picking the middle.
         * assign result to placeholder characters
         * make sure they both get assigned on every loop to prevent carry over
         */
        String placeholderForModifiedPhrase = "";
        String placeholderForMiddleCharacters = "";

        for (int i = 0; i < arr.length; i++) {
            // Start Solution Edits
            String sentence = arr[i];
            sentence = sentence.replaceAll("[^a-zA-Z0-9]", "");
            sentence = sentence.trim().replaceAll(" +", "");
            sentence = sentence.toLowerCase();
            String[] words = sentence.split("");
            String tCased = "";
            for (int j = 0; j < words.length; j++) {
                if (words[j].length() > 0) {
                    tCased += Character.toUpperCase(words[j].charAt(0)) + words[j].substring(1);
                }
                if (j < words.length - 1) {
                    tCased += " ";
                }
            }
            placeholderForModifiedPhrase = tCased;
            int len = placeholderForModifiedPhrase.length();
            if (len < 3) {
                placeholderForMiddleCharacters = "Not enough characters";
            } else if (len <= 5) {
                placeholderForMiddleCharacters = placeholderForModifiedPhrase.substring(1, len - 1);
            } else {
                int start = len / 2;
                int end = Math.min(start + 3, len);
                placeholderForMiddleCharacters = placeholderForModifiedPhrase.substring(start, end);
            }

            // End Solution Edits
            System.out.println(String.format("Index[%d] \"%s\" | Middle: \"%s\"", i, placeholderForModifiedPhrase,
                    placeholderForMiddleCharacters));
        }
        System.out.println("");
        System.out.println("______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "aia47"; // <-- change to your UCID
        // No edits below this line
        printHeader(ucid, 4);

        transformText(array1, 1);
        transformText(array2, 2);
        transformText(array3, 3);
        transformText(array4, 4);
        printFooter(ucid, 4);
    }

}
