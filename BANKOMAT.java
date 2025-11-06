import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.time.*;

// ---- Klasa Konto ----
class Konto implements Serializable {
    private static final long serialVersionUID = 3L;

    String numerKarty;
    String pin;
    double saldo;
    java.util.List<String> historia;
    String blikKod;
    LocalDateTime blikWaznosc;

    public Konto(String numerKarty, String pin, double saldo) {
        this.numerKarty = numerKarty;
        this.pin = pin;
        this.saldo = saldo;
        this.historia = new ArrayList<>();
    }

    private void ensureHistoria() {
        if (historia == null) historia = new ArrayList<>();
    }

    public boolean sprawdzPin(String podanyPin) {
        return this.pin.equals(podanyPin);
    }

    public void wplata(double kwota) {
        ensureHistoria();
        saldo += kwota;
        historia.add("💰 Wpłata +" + kwota + " zł | Saldo: " + saldo);
    }

    public boolean wyplata(double kwota, String nominals) {
        ensureHistoria();
        if (kwota <= saldo) {
            saldo -= kwota;
            historia.add("💸 Wypłata -" + kwota + " zł | " + nominals + " | Saldo: " + saldo);
            return true;
        }
        return false;
    }

    public void przelew(double kwota, String odbiorca) {
        ensureHistoria();
        saldo -= kwota;
        historia.add("📤 Przelew -" + kwota + " zł → " + odbiorca + " | Saldo: " + saldo);
    }

    public void otrzymajPrzelew(double kwota, String nadawca) {
        ensureHistoria();
        saldo += kwota;
        historia.add("📥 Przelew +" + kwota + " zł od " + nadawca + " | Saldo: " + saldo);
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

    public String generujBlik() {
        Random r = new Random();
        blikKod = String.format("%06d", r.nextInt(1000000));
        blikWaznosc = LocalDateTime.now().plusMinutes(2);
        historia.add("🔢 Wygenerowano kod BLIK: " + blikKod + " (ważny 2 min)");
        return blikKod;
    }

    public boolean czyBlikWazny(String kod) {
        return blikKod != null && blikKod.equals(kod)
                && blikWaznosc != null
                && LocalDateTime.now().isBefore(blikWaznosc);
    }

    public void uniewaznijBlik() {
        blikKod = null;
        blikWaznosc = null;
    }
}

// ---- Klasa Bankomat ----
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
        setSize(720, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        wczytajStan();
        utworzPanelLogowania();
    }

    // ----------------- PANEL LOGOWANIA -----------------
    private void utworzPanelLogowania() {
        panelLogowania = new JPanel();
        panelLogowania.setLayout(new BoxLayout(panelLogowania, BoxLayout.Y_AXIS));
        panelLogowania.setBorder(new EmptyBorder(40, 80, 40, 80));
        panelLogowania.setBackground(new Color(25, 28, 38));

        JLabel tytul = new JLabel("🏦 Witaj w Bankomacie");
        tytul.setForeground(Color.WHITE);
        tytul.setFont(new Font("Segoe UI", Font.BOLD, 28));
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

        poleKarta.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        polePin = new JPasswordField();
        polePin.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JButton przyciskZaloguj = new JButton("Zaloguj się");
        stylPrzycisku(przyciskZaloguj, new Color(70, 140, 250));

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.ORANGE);

        przyciskZaloguj.addActionListener(e -> zaloguj());

        panelLogowania.add(tytul);
        panelLogowania.add(Box.createVerticalStrut(20));
        panelLogowania.add(kartaLabel);
        panelLogowania.add(poleKarta);
        panelLogowania.add(Box.createVerticalStrut(10));
        panelLogowania.add(pinLabel);
        panelLogowania.add(polePin);
        panelLogowania.add(Box.createVerticalStrut(25));
        panelLogowania.add(przyciskZaloguj);
        panelLogowania.add(Box.createVerticalStrut(10));
        panelLogowania.add(statusLabel);

        setContentPane(panelLogowania);
        revalidate();
    }

    private void stylPrzycisku(JButton b, Color c) {
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
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

    // ----------------- PANEL MENU -----------------
    private void pokazMenu() {
        panelMenu = new JPanel(new BorderLayout(10, 10));
        panelMenu.setBorder(new EmptyBorder(20, 20, 20, 20));
        panelMenu.setBackground(new Color(240, 245, 250));

        saldoLabel = new JLabel("💵 Saldo: " + String.format("%.2f", aktualneKonto.getSaldo()) + " zł", SwingConstants.CENTER);
        saldoLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        saldoLabel.setForeground(new Color(0, 128, 0));

        historiaArea = new JTextArea();
        historiaArea.setEditable(false);
        historiaArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        odswiezHistorie();

        JScrollPane scroll = new JScrollPane(historiaArea);
        scroll.setBorder(BorderFactory.createTitledBorder("📜 Historia transakcji"));

        JPanel przyciskiPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        przyciskiPanel.setBackground(new Color(240, 245, 250));

        JButton wplataBtn = new JButton("Wpłata 💰");
        JButton wyplataBtn = new JButton("Wypłata 💸");
        JButton przelewBtn = new JButton("Przelew 💱");
        JButton blikBtn = new JButton("BLIK 💡");
        JButton wylogujBtn = new JButton("Wyloguj 🚪");

        stylPrzycisku(wplataBtn, new Color(0, 153, 51));
        stylPrzycisku(wyplataBtn, new Color(255, 102, 51));
        stylPrzycisku(przelewBtn, new Color(70, 140, 250));
        stylPrzycisku(blikBtn, new Color(250, 190, 0));
        stylPrzycisku(wylogujBtn, new Color(160, 160, 160));

        wplataBtn.addActionListener(e -> wykonajWplate());
        wyplataBtn.addActionListener(e -> wykonajWyplate());
        przelewBtn.addActionListener(e -> wykonajPrzelew());
        blikBtn.addActionListener(e -> menuBlik());
        wylogujBtn.addActionListener(e -> wyloguj());

        przyciskiPanel.add(wplataBtn);
        przyciskiPanel.add(wyplataBtn);
        przyciskiPanel.add(przelewBtn);
        przyciskiPanel.add(blikBtn);
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

    // ---------- BLIK ----------
    private void menuBlik() {
        Object[] options = {"Generuj kod", "Wypłata BLIK", "Anuluj"};
        int wybor = JOptionPane.showOptionDialog(this, "Wybierz opcję BLIK:", "BLIK 💡",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (wybor == 0) { // generowanie
            String kod = aktualneKonto.generujBlik();
            zapiszStan();
            odswiezHistorie();
            JOptionPane.showMessageDialog(this, "🔢 Twój kod BLIK: " + kod + "\nWażny 2 minuty!");
        } else if (wybor == 1) { // wypłata BLIK
            String kod = JOptionPane.showInputDialog(this, "Podaj kod BLIK:");
            if (kod == null) return;
            Konto odbiorca = null;
            for (Konto k : konta.values()) {
                if (k.czyBlikWazny(kod)) {
                    odbiorca = k;
                    break;
                }
            }
            if (odbiorca == null) {
                JOptionPane.showMessageDialog(this, "❌ Nie znaleziono ważnego kodu BLIK!");
                return;
            }
            String kwotaStr = JOptionPane.showInputDialog(this, "Podaj kwotę wypłaty:");
            if (kwotaStr == null) return;
            try {
                double kwota = Double.parseDouble(kwotaStr);
                if (odbiorca.getSaldo() >= kwota) {
                    String nom = generujNominaly(kwota);
                    odbiorca.wyplata(kwota, nom);
                    odbiorca.uniewaznijBlik();
                    zapiszStan();
                    odswiezSaldo();
                    odswiezHistorie();
                    JOptionPane.showMessageDialog(this, "✅ Wypłata BLIK " + kwota + " zł\nNominały: " + nom);
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Konto powiązane z kodem nie ma środków!");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Nieprawidłowa kwota!");
            }
        }
    }

    private void wyloguj() {
        aktualneKonto = null;
        utworzPanelLogowania();
    }

    // ----------------- ZAPIS / ODCZYT -----------------
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
            JOptionPane.showMessageDialog(this, "Błąd zapisu stanu bankomatu!");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BANKOMAT().setVisible(true));
    }
}
