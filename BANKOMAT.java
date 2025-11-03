import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

class Konto implements Serializable {
    private static final long serialVersionUID = 2L;

    String numerKarty;
    String pin;
    double saldo;
    java.util.List<String> historia; 

    public Konto(String numerKarty, String pin, double saldo) {
        this.numerKarty = numerKarty;
        this.pin = pin;
        this.saldo = saldo;
        this.historia = new java.util.ArrayList<>();
    }

    private void ensureHistoria() {
        if (historia == null) historia = new java.util.ArrayList<>();
    }

    public boolean sprawdzPin(String podanyPin) {
        return this.pin.equals(podanyPin);
    }

    public void wplata(double kwota) {
        ensureHistoria();
        saldo += kwota;
        historia.add("💰 Wpłata: +" + kwota + " zł | Saldo: " + saldo);
    }

    public boolean wyplata(double kwota, String nominals) {
        ensureHistoria();
        if (kwota <= saldo) {
            saldo -= kwota;
            historia.add("💸 Wypłata: -" + kwota + " zł | " + nominals + " | Saldo: " + saldo);
            return true;
        }
        return false;
    }

    public void przelew(double kwota, String odbiorca) {
        ensureHistoria();
        saldo -= kwota;
        historia.add("📤 Przelew: -" + kwota + " zł → " + odbiorca + " | Saldo: " + saldo);
    }

    public void otrzymajPrzelew(double kwota, String nadawca) {
        ensureHistoria();
        saldo += kwota;
        historia.add("📥 Przelew: +" + kwota + " zł od " + nadawca + " | Saldo: " + saldo);
    }

    public double getSaldo() {
        return saldo;
    }

    public java.util.List<String> getHistoria() {
        ensureHistoria();
        return historia;
    }

    public String getNumerKarty() {
        return numerKarty;
    }
}

public class BANKOMAT extends JFrame {
    private static final long serialVersionUID = 1L;

    private Map<String, Konto> konta;
    private Konto aktualneKonto;

    private JFormattedTextField poleKarta;
    private JPasswordField polePin;
    private JLabel statusLabel;
    private JLabel saldoLabel;
    private JTextArea historiaArea;

    private JPanel panelLogowania;
    private JPanel panelMenu;

    private static final String PLIK_STANU = "atm_state.ser";
    private static final int[] NOMINALY = {500, 200, 100, 50, 20, 10};

    public BANKOMAT() {
        setTitle("💳 Symulator Bankomatu");
        setSize(520, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        wczytajStan();
        utworzPanelLogowania();
    }

    private void utworzPanelLogowania() {
        panelLogowania = new JPanel();
        panelLogowania.setLayout(new BoxLayout(panelLogowania, BoxLayout.Y_AXIS));
        panelLogowania.setBorder(new EmptyBorder(20, 40, 20, 40));
        panelLogowania.setBackground(new Color(30, 30, 40));

        JLabel tytul = new JLabel("💰 Witaj w Bankomacie");
        tytul.setForeground(Color.WHITE);
        tytul.setFont(new Font("Segoe UI", Font.BOLD, 22));
        tytul.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel kartaLabel = new JLabel("Numer karty:");
        kartaLabel.setForeground(Color.LIGHT_GRAY);
        JLabel pinLabel = new JLabel("PIN:");
        pinLabel.setForeground(Color.LIGHT_GRAY);

        try {
            MaskFormatter maska = new MaskFormatter("####-####-####-####");
            maska.setPlaceholderCharacter('_');
            poleKarta = new JFormattedTextField(maska);
        } catch (Exception e) {
            poleKarta = new JFormattedTextField();
        }

        polePin = new JPasswordField();

        JButton przyciskZaloguj = new JButton("Zaloguj");
        przyciskZaloguj.setBackground(new Color(60, 130, 250));
        przyciskZaloguj.setForeground(Color.WHITE);
        przyciskZaloguj.setFont(new Font("Segoe UI", Font.BOLD, 14));

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.ORANGE);

        przyciskZaloguj.addActionListener(e -> zaloguj());

        panelLogowania.add(tytul);
        panelLogowania.add(Box.createVerticalStrut(15));
        panelLogowania.add(kartaLabel);
        panelLogowania.add(poleKarta);
        panelLogowania.add(Box.createVerticalStrut(10));
        panelLogowania.add(pinLabel);
        panelLogowania.add(polePin);
        panelLogowania.add(Box.createVerticalStrut(15));
        panelLogowania.add(przyciskZaloguj);
        panelLogowania.add(Box.createVerticalStrut(10));
        panelLogowania.add(statusLabel);

        setContentPane(panelLogowania);
        revalidate();
    }

    private void zaloguj() {
        String karta = poleKarta.getText().trim();
        String pin = new String(polePin.getPassword()).trim();

        if (konta.containsKey(karta) && konta.get(karta).sprawdzPin(pin)) {
            aktualneKonto = konta.get(karta);
            zapiszStan();
            pokazMenu();
        } else {
            statusLabel.setText("❌ Błędny numer karty lub PIN!");
        }
    }

    private void pokazMenu() {
        panelMenu = new JPanel(new BorderLayout(10, 10));
        panelMenu.setBorder(new EmptyBorder(15, 15, 15, 15));
        panelMenu.setBackground(new Color(245, 247, 250));

        saldoLabel = new JLabel("💵 Saldo: " + String.format("%.2f", aktualneKonto.getSaldo()) + " zł", SwingConstants.CENTER);
        saldoLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        saldoLabel.setForeground(new Color(0, 128, 0));

        historiaArea = new JTextArea();
        historiaArea.setEditable(false);
        historiaArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        odswiezHistorie();

        JScrollPane scroll = new JScrollPane(historiaArea);
        scroll.setBorder(BorderFactory.createTitledBorder("📜 Historia transakcji"));

        JPanel przyciskiPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        przyciskiPanel.setBackground(new Color(245, 247, 250));

        JButton wplataBtn = new JButton("Wpłata 💰");
        JButton wyplataBtn = new JButton("Wypłata 💸");
        JButton przelewBtn = new JButton("Przelew 💱");
        JButton wylogujBtn = new JButton("Wyloguj 🚪");

        for (JButton b : new JButton[]{wplataBtn, wyplataBtn, przelewBtn, wylogujBtn}) {
            b.setFocusPainted(false);
            b.setFont(new Font("Segoe UI", Font.BOLD, 14));
            b.setBackground(new Color(60, 130, 250));
            b.setForeground(Color.WHITE);
        }

        wplataBtn.addActionListener(e -> wykonajWplate());
        wyplataBtn.addActionListener(e -> wykonajWyplate());
        przelewBtn.addActionListener(e -> wykonajPrzelew());
        wylogujBtn.addActionListener(e -> wyloguj());

        przyciskiPanel.add(wplataBtn);
        przyciskiPanel.add(wyplataBtn);
        przyciskiPanel.add(przelewBtn);
        przyciskiPanel.add(wylogujBtn);

        panelMenu.add(saldoLabel, BorderLayout.NORTH);
        panelMenu.add(przyciskiPanel, BorderLayout.CENTER);
        panelMenu.add(scroll, BorderLayout.SOUTH);

        setContentPane(panelMenu);
        revalidate();
    }

    private void odswiezSaldo() {
        saldoLabel.setText("💵 Saldo: " + String.format("%.2f", aktualneKonto.getSaldo()) + " zł");
    }

    private void odswiezHistorie() {
        historiaArea.setText("");
        for (String wpis : aktualneKonto.getHistoria()) {
            historiaArea.append(wpis + "\n");
        }
    }

    private String generujNominaly(double kwota) {
        StringBuilder sb = new StringBuilder();
        int pozostale = (int) kwota;
        for (int nom : NOMINALY) {
            int ile = pozostale / nom;
            if (ile > 0) {
                sb.append(ile).append("×").append(nom).append(" zł, ");
                pozostale %= nom;
            }
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "brak banknotów";
    }

    private void wykonajWplate() {
        String kwotaStr = JOptionPane.showInputDialog(this, "Podaj kwotę wpłaty:");
        if (kwotaStr == null) return;
        try {
            double kwota = Double.parseDouble(kwotaStr);
            aktualneKonto.wplata(kwota);
            zapiszStan();
            odswiezSaldo();
            odswiezHistorie();
            JOptionPane.showMessageDialog(this, "✅ Wpłacono " + kwota + " zł");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Nieprawidłowa kwota!");
        }
    }

    private void wykonajWyplate() {
        String kwotaStr = JOptionPane.showInputDialog(this, "Podaj kwotę do wypłaty:");
        if (kwotaStr == null) return;
        try {
            double kwota = Double.parseDouble(kwotaStr);
            String nominals = generujNominaly(kwota);
            if (aktualneKonto.wyplata(kwota, nominals)) {
                zapiszStan();
                odswiezSaldo();
                odswiezHistorie();
                JOptionPane.showMessageDialog(this, "💸 Wypłacono " + kwota + " zł\nNominały: " + nominals);
            } else {
                JOptionPane.showMessageDialog(this, "❌ Brak środków!");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Nieprawidłowa kwota!");
        }
    }

    private void wykonajPrzelew() {
        String numerDocelowy = JOptionPane.showInputDialog(this, "Podaj numer karty odbiorcy (XXXX-XXXX-XXXX-XXXX):");
        if (numerDocelowy == null) return;

        if (!konta.containsKey(numerDocelowy)) {
            JOptionPane.showMessageDialog(this, "❌ Nie znaleziono konta docelowego!");
            return;
        }

        String kwotaStr = JOptionPane.showInputDialog(this, "Podaj kwotę przelewu:");
        if (kwotaStr == null) return;

        try {
            double kwota = Double.parseDouble(kwotaStr);
            if (aktualneKonto.getSaldo() >= kwota) {
                aktualneKonto.przelew(kwota, numerDocelowy);
                konta.get(numerDocelowy).otrzymajPrzelew(kwota, aktualneKonto.getNumerKarty());
                zapiszStan();
                odswiezSaldo();
                odswiezHistorie();
                JOptionPane.showMessageDialog(this, "✅ Przelano " + kwota + " zł do " + numerDocelowy);
            } else {
                JOptionPane.showMessageDialog(this, "❌ Brak środków!");
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
            for (Konto k : konta.values()) {
                if (k.historia == null) k.historia = new ArrayList<>();
            }
        } catch (Exception e) {
            konta = new HashMap<>();
            konta.put("1111-2222-3333-4444", new Konto("1111-2222-3333-4444", "1234", 1500));
            konta.put("5555-6666-7777-8888", new Konto("5555-6666-7777-8888", "5678", 3000));
            konta.put("9999-0000-1111-2222", new Konto("9999-0000-1111-2222", "4321", 500));
            zapiszStan();
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

