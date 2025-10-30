import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

class Konto implements Serializable {
    private static final long serialVersionUID = 1L;
    String numerKarty;
    String pin;
    double saldo;

    public Konto(String numerKarty, String pin, double saldo) {
        this.numerKarty = numerKarty;
        this.pin = pin;
        this.saldo = saldo;
    }

    public boolean sprawdzPin(String podanyPin) {
        return this.pin.equals(podanyPin);
    }

    public void wplata(double kwota) {
        saldo += kwota;
    }

    public boolean wyplata(double kwota) {
        if (kwota <= saldo) {
            saldo -= kwota;
            return true;
        }
        return false;
    }

    public double getSaldo() {
        return saldo;
    }

    public String getNumerKarty() {
        return numerKarty;
    }
}

public class BANKOMAT extends JFrame {
    private static final long serialVersionUID = 1L;

    private Map<String, Konto> konta;
    private Konto aktualneKonto;
    private JTextField poleKarta;
    private JPasswordField polePin;
    private JLabel statusLabel;
    private JPanel panelLogowania;
    private JPanel panelMenu;

    private static final String PLIK_STANU = "atm_state.ser";

    public BANKOMAT() {
        setTitle("Symulator Bankomatu");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        wczytajStan();
        utworzPanelLogowania();
    }

    private void utworzPanelLogowania() {
        panelLogowania = new JPanel(new GridLayout(4, 1));
        panelLogowania.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        poleKarta = new JTextField();
        polePin = new JPasswordField();
        JButton przyciskZaloguj = new JButton("Zaloguj");
        statusLabel = new JLabel(" ", SwingConstants.CENTER);

        panelLogowania.add(new JLabel("Numer karty:"));
        panelLogowania.add(poleKarta);
        panelLogowania.add(new JLabel("PIN:"));
        panelLogowania.add(polePin);
        panelLogowania.add(przyciskZaloguj);
        panelLogowania.add(statusLabel);

        przyciskZaloguj.addActionListener(e -> zaloguj());

        setContentPane(panelLogowania);
        revalidate();
    }

    private void zaloguj() {
        String karta = poleKarta.getText().trim();
        String pin = new String(polePin.getPassword()).trim();

        if (konta.containsKey(karta) && konta.get(karta).sprawdzPin(pin)) {
            aktualneKonto = konta.get(karta);
            pokazMenu();
        } else {
            statusLabel.setText("Błędny numer karty lub PIN!");
        }
    }

    private void pokazMenu() {
        panelMenu = new JPanel(new GridLayout(6, 1, 10, 10));
        panelMenu.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton saldoBtn = new JButton("Sprawdź saldo");
        JButton wplataBtn = new JButton("Wpłata");
        JButton wyplataBtn = new JButton("Wypłata");
        JButton przelewBtn = new JButton("Przelew");
        JButton wylogujBtn = new JButton("Wyloguj");

        saldoBtn.addActionListener(e -> pokazSaldo());
        wplataBtn.addActionListener(e -> wykonajWplate());
        wyplataBtn.addActionListener(e -> wykonajWyplate());
        przelewBtn.addActionListener(e -> wykonajPrzelew());
        wylogujBtn.addActionListener(e -> wyloguj());

        panelMenu.add(saldoBtn);
        panelMenu.add(wplataBtn);
        panelMenu.add(wyplataBtn);
        panelMenu.add(przelewBtn);
        panelMenu.add(wylogujBtn);

        setContentPane(panelMenu);
        revalidate();
    }

    private void pokazSaldo() {
        JOptionPane.showMessageDialog(this, "Saldo: " + aktualneKonto.getSaldo() + " zł");
    }

    private void wykonajWplate() {
        String kwotaStr = JOptionPane.showInputDialog(this, "Podaj kwotę wpłaty:");
        if (kwotaStr == null) return;
        try {
            double kwota = Double.parseDouble(kwotaStr);
            aktualneKonto.wplata(kwota);
            zapiszStan();
            JOptionPane.showMessageDialog(this, "Wpłacono " + kwota + " zł");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Nieprawidłowa kwota!");
        }
    }

    private void wykonajWyplate() {
        String kwotaStr = JOptionPane.showInputDialog(this, "Podaj kwotę do wypłaty:");
        if (kwotaStr == null) return;
        try {
            double kwota = Double.parseDouble(kwotaStr);
            if (aktualneKonto.wyplata(kwota)) {
                zapiszStan();
                JOptionPane.showMessageDialog(this, "Wypłacono " + kwota + " zł");
            } else {
                JOptionPane.showMessageDialog(this, "Brak środków!");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Nieprawidłowa kwota!");
        }
    }

    private void wykonajPrzelew() {
        String numerDocelowy = JOptionPane.showInputDialog(this, "Podaj numer karty odbiorcy:");
        if (numerDocelowy == null) return;

        if (!konta.containsKey(numerDocelowy)) {
            JOptionPane.showMessageDialog(this, "Nie znaleziono konta docelowego!");
            return;
        }

        String kwotaStr = JOptionPane.showInputDialog(this, "Podaj kwotę przelewu:");
        if (kwotaStr == null) return;

        try {
            double kwota = Double.parseDouble(kwotaStr);
            if (aktualneKonto.wyplata(kwota)) {
                konta.get(numerDocelowy).wplata(kwota);
                zapiszStan();
                JOptionPane.showMessageDialog(this, "Przelano " + kwota + " zł do " + numerDocelowy);
            } else {
                JOptionPane.showMessageDialog(this, "Brak środków!");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Nieprawidłowa kwota!");
        }
    }

    private void wyloguj() {
        aktualneKonto = null;
        utworzPanelLogowania();
    }

    @SuppressWarnings("unchecked")
    private void wczytajStan() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PLIK_STANU))) {
            konta = (Map<String, Konto>) ois.readObject();
        } catch (Exception e) {
            konta = new HashMap<>();
            konta.put("1111-2222-3333-4444", new Konto("1111-2222-3333-4444", "1234", 1500));
            konta.put("5555-6666-7777-8888", new Konto("5555-6666-7777-8888", "5678", 3000));
            konta.put("9999-0000-1111-2222", new Konto("9999-0000-1111-2222", "4321", 500));
        }
    }

    private void zapiszStan() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PLIK_STANU))) {
            oos.writeObject(konta);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Błąd zapisu stanu kont!");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BANKOMAT().setVisible(true));
    }
}
