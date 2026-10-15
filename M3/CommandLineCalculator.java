package M3;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/
/*
aia47 10/14/2026
Grab the user input using scanner, separate input into two arrays one for operator and one for values
Check if number is valid and if operator is valid
Check if number is decimal and if so pick the largest of two decimal values
Use switch case for operator 
Display result with mathcing number type of input
Run 5 initial tests
*/
public class CommandLineCalculator extends BaseClass {
    private static String ucid = "aia47"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");

            String numText = args[0];
            String operator = args[1];
            String numText2 = args[2];
            double num1 = Double.parseDouble(numText);
            double num2 = Double.parseDouble(numText2);
            int decimals1 = 0;
            int decimals2 = 0;

            if (numText.contains(".")) {
                decimals1 = numText.length() - numText.indexOf(".") - 1;
            }
            if (numText2.contains(".")) {
                decimals2 = numText2.length() - numText2.indexOf(".") - 1;
            }
            int places = decimals1;
            if (decimals2 > decimals1) {
                places = decimals2;
            }
            double result = 0;
            switch (operator) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                default:
                    System.out.println("Error: Unsupported Operator");
                    break;
            }
            if (places == 0) {
                System.out.println("Result" + (int) result);
            } else {
                System.out.println("Result" + String.format("%." + places + "f", result));
            }
        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}
