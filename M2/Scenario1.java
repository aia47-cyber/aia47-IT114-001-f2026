package M2;
// copilot: disable

// @ts-nocheck

public class Scenario1 extends BaseClass {
    private static int[] array1 = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    private static int[] array2 = { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };
    private static int[] array3 = { 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9 };
    private static int[] array4 = { 9, 9, 8, 8, 7, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0, 0 };

    private static void printOdds(int[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printScenario1ArrayInfo(arr, arrayNumber);
        // This should be solved without Copilot auto-completion, to toggle it, click
        // the Copilot chat bubble at the top of the editor.
        // Configure inline suggestions to "Disabled Inline Suggestions" (or similar)
        // when writing code for this problem.

        // Challenge 1: From each passed in array, print odd values only in a single
        // line separated by commas and a space after each comma (should not have
        // leading or trailing commas)
        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        /*
         * aia47 10/5/2026
         * Make a loop for array to check elements
         * Make a boolean variable to check if value has printed
         * Check if value is odd and if value already printed, print ", " first
         * Print the odd value using print
         * Set boolean to true after printing first odd value
         */
        // Start Solution Edits
        boolean first = true;
        for (int value : arr) {
            if (value % 2 != 0) {
                if (!first) {
                    System.out.print(", ");
                }
                System.out.print(value);
                first = false;
            }
        }
        // End Solution Edits
        System.out.println("");
        System.out.println("______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "aia47"; // <-- change to your UCID
        // no edits below this line
        printHeader(ucid, 1);
        printOdds(array1, 1);
        printOdds(array2, 2);
        printOdds(array3, 3);
        printOdds(array4, 4);
        printFooter(ucid, 1);

    }
}
