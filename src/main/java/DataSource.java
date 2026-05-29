import com.mybank.domain.Bank;
import com.mybank.domain.CheckingAccount;
import com.mybank.domain.Customer;
import com.mybank.domain.SavingsAccount;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

/**
 * Фікс {@link com.mybank.data.DataSource} через нечітке зчитування структури файлу
 */
public class DataSource {

    private final File dataFile;

    public DataSource(String dataFilePath) {
        this.dataFile = new File(dataFilePath);
    }

    public void loadData() throws IOException {
        Scanner input = new Scanner(dataFile);
        input.useLocale(Locale.US);

        if (!input.hasNextInt()) return;
        int numOfCustomers = input.nextInt();

        for (int idx = 0; idx < numOfCustomers; ++idx) {
            String firstName = input.next();
            String lastName = input.next();
            Bank.addCustomer(firstName, lastName);
            Customer customer = Bank.getCustomer(idx);

            int numOfAccounts = input.nextInt();

            for (int i = 0; i < numOfAccounts; i++) {
                String type = input.next();
                char accountType = type.charAt(0);

                double initBalance = input.nextDouble();
                double extra = input.nextDouble();

                if (accountType == 'C') {
                    customer.addAccount(new CheckingAccount(initBalance, extra));
                } else if (accountType == 'S') {
                    customer.addAccount(new SavingsAccount(initBalance, extra));
                }
            }
        }
    }
}
