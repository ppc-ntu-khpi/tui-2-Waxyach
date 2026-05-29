import com.mybank.domain.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.jline.reader.*;
import org.jline.reader.impl.completer.*;
import org.jline.utils.*;
import org.fusesource.jansi.*;

public class CLIDemo {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    private final String[] commandsList = new String[] {"help", "customers", "customer", "report", "exit"};

    public static void main(String[] args) {
        try {
            DataSource dataSource = new DataSource("data/test.dat");
            dataSource.loadData();
        } catch (IOException e) {
            System.err.println(ANSI_RED + "An error occurred while loading test.dat: " + e.getMessage() + ANSI_RESET);
            return;
        }

        CLIDemo demo = new CLIDemo();
        demo.run();
    }

    public void run() {
        AnsiConsole.systemInstall();

        printWelcomeMessage();

        LineReaderBuilder readerBuilder = LineReaderBuilder.builder();
        List<Completer> completers = new LinkedList<>();
        completers.add(new StringsCompleter(commandsList));
        readerBuilder.completer(new ArgumentCompleter(completers));
        LineReader reader = readerBuilder.build();

        String line;
        while ((line = readLine(reader)) != null) {
            String[] tokens = line.split("\\s+");
            if (tokens.length == 0 || tokens[0].isEmpty()) {
                continue;
            }

            String command = tokens[0];
            switch (command) {
                case "help" -> printHelp();
                case "customers" -> handleCustomersCommand();
                case "customer" -> handleCustomerDetailsCommand(tokens);
                case "report" -> handleReportCommand();
                case "exit" -> {
                    System.out.println("Exiting application...");
                    AnsiConsole.systemUninstall();
                    return;
                }
                default -> System.out.println(ANSI_RED + "Invalid command. For assistance press TAB or type \"help\"." + ANSI_RESET);
            }
        }
        AnsiConsole.systemUninstall();
    }

    private void handleCustomersCommand() {
        AttributedStringBuilder title = new AttributedStringBuilder()
                .append("\nThis is all of your ")
                .append("customers", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                .append(":");
        System.out.println(title.toAnsi());

        if (Bank.getNumberOfCustomers() == 0) {
            System.out.println(ANSI_RED + "Your bank has no customers!" + ANSI_RESET);
            return;
        }

        System.out.println("\nLast name\tFirst Name\tBalance");
        System.out.println("---------------------------------------");

        for (int i = 0; i < Bank.getNumberOfCustomers(); i++) {
            Customer customer = Bank.getCustomer(i);
            double balance = customer.getNumberOfAccounts() > 0 ? customer.getAccount(0).getBalance() : 0.0;

            System.out.printf(Locale.US, "%s\t\t%s\t\t$%.2f\n",
                    customer.getLastName(), customer.getFirstName(), balance);
        }
    }

    private void handleCustomerDetailsCommand(String[] tokens) {
        try {
            int customerNum = 0;
            if (tokens.length > 1) {
                customerNum = Integer.parseInt(tokens[1]);
            }

            if (customerNum < 0 || customerNum >= Bank.getNumberOfCustomers()) {
                throw new IndexOutOfBoundsException();
            }

            Customer customer = Bank.getCustomer(customerNum);
            String accountType = "None";
            double balance = 0.0;

            if (customer.getNumberOfAccounts() > 0) {
                Account account = customer.getAccount(0);
                accountType = account instanceof CheckingAccount ? "Checking" : "Savings";
                balance = account.getBalance();
            }

            AttributedStringBuilder detailsHeader = new AttributedStringBuilder()
                    .append("\nThis is detailed information about customer #")
                    .append(Integer.toString(customerNum), AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                    .append("!");
            System.out.println(detailsHeader.toAnsi());

            System.out.println("\nLast name\tFirst Name\tAccount Type\tBalance");
            System.out.println("-------------------------------------------------------");
            System.out.printf(Locale.US, "%s\t\t%s\t\t%s\t\t$%.2f\n",
                    customer.getLastName(), customer.getFirstName(), accountType, balance);

        } catch (Exception e) {
            System.out.println(ANSI_RED + "ERROR! Wrong customer number!" + ANSI_RESET);
        }
    }

    private void handleReportCommand() {
        System.out.println("\n=======================================================");
        System.out.println(ANSI_GREEN + "CUSTOMERS REPORT" + ANSI_RESET);
        System.out.println("=======================================================");

        if (Bank.getNumberOfCustomers() == 0) {
            System.out.println(ANSI_RED + "Your bank has no customers to report!" + ANSI_RESET);
            System.out.println("=======================================================");
            return;
        }

        for (int i = 0; i < Bank.getNumberOfCustomers(); i++) {
            Customer customer = Bank.getCustomer(i);
            System.out.println("\nCustomer: " + customer.getLastName() + ", " + customer.getFirstName());

            for (int j = 0; j < customer.getNumberOfAccounts(); j++) {
                Account account = customer.getAccount(j);
                String accountType = "Unknown Account";

                if (account instanceof CheckingAccount) {
                    accountType = "Checking Account";
                } else if (account instanceof SavingsAccount) {
                    accountType = "Savings Account";
                }

                System.out.printf(Locale.US, "    %s: current balance is $%.2f\n", accountType, account.getBalance());
            }
        }
        System.out.println("=======================================================");
    }

    private void printWelcomeMessage() {
        System.out.println("\nWelcome to " + ANSI_GREEN + " MyBank Console Client App" + ANSI_RESET + "!");
        System.out.println("For assistance press TAB or type \"help\" then hit ENTER.");
    }

    private void printHelp() {
        System.out.print("""
                
                Available commands:
                  help                - Show this help message
                  customers           - Show short list of all customers
                  customer 'index'    - Show detailed information about specific customer
                  exit                - Close the application
                """);
    }

    private String readLine(LineReader reader) {
        try {
            String line = reader.readLine(ANSI_YELLOW + "\nbank> " + ANSI_RESET);
            return (line != null) ? line.trim() : null;
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }
}