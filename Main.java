package com.company;
import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        ATM atm = new ATM(
                new DBService("D:/file.txt"),
                new CardsService(10000),
                System.in,
                System.out
        );
        try {
            atm.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

final class ATM {
    private DBService dbService;
    private CardsService cardService;
    private InputStream in;
    private PrintStream out;
    ATM(DBService dbService, CardsService cardService, InputStream in, PrintStream out) {
        this.dbService = dbService;
        this.cardService = cardService;
        this.in = in;
        this.out = out;
    }
    void start() throws IOException {
        this.cardService.loadCardsFromData(dbService.load());
        Scanner scanner = new Scanner(in);
        while (true){
            try {
                out.print("Enter card number: ");
                String cardNumber = scanner.nextLine();
                out.print("Enter card pass: ");
                Integer cardPass = Integer.parseInt(scanner.nextLine());
                CardsService.Card card = this.cardService.openOrCreateCard(cardNumber, cardPass);
                workWithCard(card, scanner);
            } catch (Exception e) {
                out.println(e.getMessage());
            }
            out.print("Exit? (\"yes\" or enter for continue) ");
            String answer = scanner.nextLine();
            if (answer.toLowerCase().equals("yes")) break;
        }
        dbService.save(this.cardService.saveCardsToData());
    }
    private void workWithCard(CardsService.Card card, Scanner scanner) throws Exception {
        out.println("-------------------------------");
        out.println("You card: " + card.getNumber());
        out.print("Enter operation (sum, add, remove, exit): ");
        switch (scanner.nextLine()) {
            case "sum":
                out.println("Current sum: " + card.getSum() + "$");
                break;
            case "add":
                out.print("Enter sum for add: ");
                try {
                    Integer sum = Integer.parseInt(scanner.nextLine());
                    cardService.addToCard(card, sum);
                } catch (NumberFormatException e) {
                    out.println("Not a number!");
                }
                break;
            case "remove":
                out.print("Enter sum for remove: ");
                try {
                    Integer sum = Integer.parseInt(scanner.nextLine());
                    cardService.removeFromCard(card, sum);
                } catch (NumberFormatException e) {
                    out.println("Not a number!");
                }
                break;
        }
        out.println("-------------------------------");
    }
}

final class DBService {
    private String dbPath;
    DBService(String dbPath) {
        this.dbPath = dbPath;
    }
    ArrayList<String> load() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(dbPath));
        ArrayList<String> data = new ArrayList<>();
        String strLine;
        while ((strLine = br.readLine()) != null) data.add(strLine);
        br.close();
        return data;
    }
    void save(ArrayList<String> data) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(dbPath, false));
        for (String str : data) bw.write(str + "\r\n");
        bw.close();
    }
}

final class CardsService {
    final static class Card {
        static String numberSeparator = "-";
        static String dataSeparator = " ";
        private String number;
        private Integer pass;
        private Integer sum;
        private Card(String number, Integer pass, Integer sum) {
            this.number = number;
            this.pass = pass;
            this.sum = sum;
        }
        private Card(String number, Integer pass) {
            this.number = number;
            this.pass = pass;
            this.sum = 0;
        }
        static Card parse(String str) {
            String[] parts = str.split(Card.dataSeparator);
            if (parts.length != 3) return null;
            try {
                String number = parts[0];
                Integer pass = Integer.parseInt(parts[1]);
                Integer sum = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
            return new Card(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        private Boolean isValidNumber() {
            String[] parts = number.split(Card.numberSeparator);
            if (parts.length != 4) return false;
            for (int i = 0; i < 4; i++) {
                if (parts[i].length() != 4) return false;
                for (int j = 0; j < 4; j++) {
                    char c = parts[i].charAt(j);
                    if (Character.isDigit(c) || Character.isLetter(c)) continue;
                    return false;
                }
            }
            return true;
        }
        private Boolean isValidPass() {
            return pass >= 1000 && pass <= 9999;
        }
        private Boolean isValid() {
            return isValidNumber() && isValidPass();
        }
        private Boolean equalsNumber(Card card) {
            return this.getNumber().equals(card.getNumber());
        }
        private Boolean equalsPass(Card card) {
            return this.pass.equals(card.pass);
        }
        String getNumber() {
            return number.toUpperCase();
        }
        Integer getSum() {
            return sum;
        }
        private String fullString() {
            return number + Card.dataSeparator + pass + Card.dataSeparator + sum;
        }
    }
    private ArrayList<Card> cards = new ArrayList<>();
    private Integer sum;
    CardsService(Integer sum) {
        this.sum = sum;
    }
    void loadCardsFromData(ArrayList<String> data) {
        for (String str : data) {
            Card card = Card.parse(str);
            if (card == null || !card.isValid()) continue;
            cards.add(card);
        }
    }
    ArrayList<String> saveCardsToData() {
        ArrayList<String> data = new ArrayList<>();
        for (Card i : this.cards) data.add(i.fullString());
        return data;
    }
    Card openOrCreateCard(String number, Integer pass) throws Exception {
        Card newCard = new Card(number, pass);
        if (!newCard.isValidNumber()) throw new Exception("Not valid number format!");
        if (!newCard.isValidPass()) throw new Exception("Not valid pass format!");
        for (Card i : this.cards) {
            if (i.equalsNumber(newCard)) {
                if (i.equalsPass(newCard)) return i;
                throw new Exception("Incorrect pass, try again!");
            }
        }
        this.cards.add(newCard);
        return newCard;
    }
    void addToCard(Card card, Integer sum) throws Exception {
        if (sum > 1000) throw new Exception("You can't put more then 1000!");
        card.sum += sum;
        this.sum += sum;
    }
    void removeFromCard(Card card, Integer sum) throws Exception {
        if (card.sum < sum) throw new Exception("There is no such amount on card!");
        if (this.sum < sum) throw new Exception("There is no such amount in an ATM!");
        card.sum -= sum;
        this.sum -= sum;
    }
}
