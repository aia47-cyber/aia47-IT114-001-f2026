package M3;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/
/*
aia47 10/14/2026
read whole line input from user (scan.nextLine() along with trim method)
Split into two pieces and lowercase first half 
check command using if statements - correction used switch cases to better streamline (if else was looking too bulky but had to label loop)
implement /greet and /roll
review implemenetation before moving on to next
implement /echo and /quit
anything else is not recongnized
delete the sample block

*/

import java.util.Scanner;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "aia47"; // <-- change to your UCID

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);

        loop: while (true) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(" ", 2);
            String command = parts[0].toLowerCase();
            String arg = "";
            if (parts.length > 1) {
                arg = parts[1].trim();
            }
            switch (command) {
                case "/greet":
                    if (arg.isEmpty()) {
                        System.out.println("Error");
                    } else {
                        System.out.println("Hello, " + arg + "!");
                    }
                    break;
                case "/roll":
                    String[] dice = arg.toLowerCase().split("d");
                    if (dice.length != 2) {
                        System.out.println("Error");
                    } else {
                        try {
                            int num = Integer.parseInt(dice[0].trim());
                            int sides = Integer.parseInt(dice[1].trim());
                            if (num < 1 || sides < 1) {
                                System.out.println("Error: <num> and <sides> must be at least 1");
                            } else {
                                int total = 0;
                                for (int i = 0; i < num; i++) {
                                    total += (int) (Math.random() * sides) + 1;
                                }
                                System.out.println("Rolled " + num + "d" + sides + " and got " + total + "!");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Error: usage is /roll <num>d<sides> (example: /roll 2d6)");
                        }
                    }
                    break;
                case "/echo":
                    if (arg.isEmpty()) {
                        System.out.println("Error: usage is /echo <message>");
                    } else {
                        System.out.println(arg);
                    }
                    break;
                case "/quit":
                    System.out.println("Goodbye.");
                    break loop;
                default:
                    System.out.println("Error command not recognized");
                    break;

            }
            scanner.close();
            // delete this condition/block, it's just here so the sample runs without edits
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}
