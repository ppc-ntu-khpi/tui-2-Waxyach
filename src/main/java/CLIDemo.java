import com.mybank.domain.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

    private final String[] commandsList = new String[] {"help", "customers", "customer", "account", "deposit", "withdraw", "save", "report", "exit"};

    private Customer activeCustomer;
    private Account activeAccount;
    private int activeAccountIdx = -1;

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
                case "account" -> handleAccountCommand(tokens);
                case "deposit" -> handleDepositCommand(tokens);
                case "withdraw" -> handleWithdrawCommand(tokens);
                case "save" -> handleSaveCommand();
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
                System.out.println(ANSI_RED + "ERROR! Customer not found!" + ANSI_RESET);
                return;
            }

            activeCustomer = Bank.getCustomer(customerNum);
            activeAccount = null;
            activeAccountIdx = -1;

            System.out.println(ANSI_GREEN + "Active customer set to: " + activeCustomer.getFirstName() + " " + activeCustomer.getLastName() + " (ID=" + customerNum + ")" + ANSI_RESET);

            System.out.println("\nAvailable accounts for this customer:");
            System.out.println("Index\tType\t\tBalance");
            System.out.println("---------------------------------------");
            for (int i = 0; i < activeCustomer.getNumberOfAccounts(); i++) {
                Account account = activeCustomer.getAccount(i);
                String type = account instanceof CheckingAccount ? "Checking" : "Savings";
                System.out.printf(Locale.US, "%d\t%s\t\t$%.2f\n", i, type, account.getBalance());
            }

        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "ERROR! Invalid customer index format!" + ANSI_RESET);
        }
    }

    private void handleAccountCommand(String[] tokens) {
        if (activeCustomer == null) {
            System.out.println(ANSI_RED + "ERROR! Please select a customer first using: customer 'index'" + ANSI_RESET);
            return;
        }

        try {
            int accountNum = 0;
            if (tokens.length > 1) {
                accountNum = Integer.parseInt(tokens[1]);
            }

            if (accountNum < 0 || accountNum >= activeCustomer.getNumberOfAccounts()) {
                System.out.println(ANSI_RED + "ERROR! Account not found for this customer!" + ANSI_RESET);
                return;
            }

            activeAccount = activeCustomer.getAccount(accountNum);
            activeAccountIdx = accountNum;

            String type = activeAccount instanceof CheckingAccount ? "Checking" : "Savings";
            System.out.println(ANSI_GREEN + "Active account set to Index " + accountNum + " (" + type + ")" + ANSI_RESET);
            System.out.printf(Locale.US, "Current Balance: $%.2f\n", activeAccount.getBalance());

        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "ERROR! Invalid account index format!" + ANSI_RESET);
        }
    }

    private void handleDepositCommand(String[] tokens) {
        if (activeAccount == null) {
            System.out.println(ANSI_RED + "ERROR! Please select an account first using: account 'index'" + ANSI_RESET);
            return;
        }

        if (tokens.length < 2) {
            System.out.println(ANSI_RED + "ERROR! Usage: deposit 'amount'" + ANSI_RESET);
            return;
        }

        try {
            double amount = Double.parseDouble(tokens[1]);
            if (amount <= 0) {
                System.out.println(ANSI_RED + "ERROR! Amount must be positive!" + ANSI_RESET);
                return;
            }

            activeAccount.deposit(amount);
            System.out.printf(Locale.US, ANSI_GREEN + "Successfully deposited $%.2f. New balance: $%.2f\n" + ANSI_RESET, amount, activeAccount.getBalance());
        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "ERROR! Invalid amount format!" + ANSI_RESET);
        }
    }

    private void handleWithdrawCommand(String[] tokens) {
        if (activeAccount == null) {
            System.out.println(ANSI_RED + "ERROR! Please select an account first using: account 'index'" + ANSI_RESET);
            return;
        }

        if (tokens.length < 2) {
            System.out.println(ANSI_RED + "ERROR! Usage: withdraw 'amount'" + ANSI_RESET);
            return;
        }

        try {
            double amount = Double.parseDouble(tokens[1]);
            if (amount <= 0) {
                System.out.println(ANSI_RED + "ERROR! Amount must be positive!" + ANSI_RESET);
                return;
            }

            if (activeAccount.withdraw(amount)) {
                System.out.printf(Locale.US, ANSI_GREEN + "Successfully withdrew $%.2f. New balance: $%.2f\n" + ANSI_RESET, amount, activeAccount.getBalance());
            }
        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "ERROR! Invalid amount format!" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "TRANSACTION ERROR: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void handleSaveCommand() {
        System.out.println("Saving bank data to data/test.dat...");

        try (PrintWriter writer = new PrintWriter(new FileWriter("data/test.dat"))) {
            int numOfCustomers = Bank.getNumberOfCustomers();
            writer.println(numOfCustomers);
            writer.println();

            for (int i = 0; i < numOfCustomers; i++) {
                Customer customer = Bank.getCustomer(i);
                writer.printf(Locale.US, "%s\t%s\t%d\n",
                        customer.getFirstName(), customer.getLastName(), customer.getNumberOfAccounts());

                for (int j = 0; j < customer.getNumberOfAccounts(); j++) {
                    Account account = customer.getAccount(j);
                    if (account instanceof CheckingAccount checking) {
                        double overdraft = 0.0;
                        try {
                            var field = CheckingAccount.class.getDeclaredField("overdraftAmount");
                            field.setAccessible(true);
                            overdraft = field.getDouble(checking);
                        } catch (Exception ignored) {}

                        writer.printf(Locale.US, "C\t%.2f\t%.2f\n", checking.getBalance(), overdraft);
                    } else if (account instanceof SavingsAccount savings) {
                        double interestRate = 0.0;
                        try {
                            var field = SavingsAccount.class.getDeclaredField("interestRate");
                            field.setAccessible(true);
                            interestRate = field.getDouble(savings);
                        } catch (Exception ignored) {}

                        writer.printf(Locale.US, "S\t%.2f\t%.4f\n", savings.getBalance(), interestRate);
                    }
                }
                writer.println();
            }
            System.out.println(ANSI_GREEN + "Data successfully saved!" + ANSI_RESET);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "ERROR! Failed to save data: " + e.getMessage() + ANSI_RESET);
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
                  customer 'index'    - Select active customer and display their accounts
                  account 'index'     - Select active account for operations and check balance
                  deposit 'amount'    - Deposit money into the active account
                  withdraw 'amount'   - Withdraw money from the active account
                  save                - Save current bank layout to data/test.dat
                  report              - Show full analytical report
                  exit                - Close the application
                """);
    }

    private String readLine(LineReader reader) {
        try {
            String prompt = ANSI_YELLOW + "\nbank";
            if (activeCustomer != null) {
                prompt += " (" + activeCustomer.getLastName() + (activeAccountIdx != -1 ? ":" + activeAccountIdx : "") + ")";
            }
            prompt += "> " + ANSI_RESET;

            String line = reader.readLine(prompt);
            return (line != null) ? line.trim() : null;
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }
}