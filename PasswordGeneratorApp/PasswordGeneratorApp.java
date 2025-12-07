/*
 * PasswordGeneratorApp - Java SE Desktop Application
 * TOP TIER 2026 GRADE (MAXIMUM FEATURES + LIFESTYLE TYPOGRAPHY)
 * Soft Dark Mode Design, Records, Entropy Calculation, Hide/Show Password.
 *
 * Notes:
 * - Java 17+ recommended (Uses Records)
 * - File MUST be named PasswordGeneratorApp.java
 * - Fonts use generic modern Sans-serif names (e.g., 'Segoe UI' or 'Arial' variation)
 * with bold/light weights for a 'Lifestyle' aesthetic.
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.plaf.basic.BasicSliderUI;

public class PasswordGeneratorApp {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    //==========================
    // MODEL & LOGIC (Inchangé)
    //==========================

    record PasswordGenerationConfig(
            int length,
            boolean includeUppercase,
            boolean includeLowercase,
            boolean includeDigits,
            boolean includeSymbols,
            boolean avoidAmbiguous,
            boolean requireEachSelectedSet,
            boolean pronounceableLike,
            String customCharacters,
            String excludeCharacters) {

        PasswordGenerationConfig() {
            this(16, true, true, true, true, true, true, false, "", "");
        }

        PasswordGenerationConfig cloneConfig() {
            return new PasswordGenerationConfig(
                    this.length,
                    this.includeUppercase,
                    this.includeLowercase,
                    this.includeDigits,
                    this.includeSymbols,
                    this.avoidAmbiguous,
                    this.requireEachSelectedSet,
                    this.pronounceableLike,
                    this.customCharacters,
                    this.excludeCharacters
            );
        }
    }

    record PasswordEntry(String password, LocalDateTime createdAt, PasswordGenerationConfig configSnapshot) {
        PasswordEntry(String password, PasswordGenerationConfig configSnapshot) {
            this(password, LocalDateTime.now(), configSnapshot);
        }

        String toDisplayString() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
            return "[" + createdAt.format(fmt) + "]  " + password;
        }
    }

    static class PasswordGenerator {
        private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
        private static final String DIGITS = "0123456789";
        private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>/?";
        private static final String AMBIGUOUS = "O0Il1|";

        private final SecureRandom random = new SecureRandom();

        String generate(PasswordGenerationConfig cfg) throws IllegalArgumentException {
            List<String> pools = new ArrayList<>();

            String upper = UPPERCASE;
            String lower = LOWERCASE;
            String digits = DIGITS;
            String symbols = SYMBOLS;

            if (cfg.avoidAmbiguous()) {
                upper = removeAmbiguous(upper);
                lower = removeAmbiguous(lower);
                digits = removeAmbiguous(digits);
            }

            if (cfg.includeUppercase()) pools.add(upper);
            if (cfg.includeLowercase()) pools.add(lower);
            if (cfg.includeDigits()) pools.add(digits);
            if (cfg.includeSymbols()) pools.add(symbols);

            String customPool = cfg.customCharacters();
            if (!customPool.trim().isEmpty()) {
                pools.add(customPool);
            }
            
            String excluded = cfg.excludeCharacters();
            if (!excluded.isEmpty()) {
                List<String> cleanedPools = new ArrayList<>();
                for (String pool : pools) {
                    cleanedPools.add(removeExcluded(pool, excluded));
                }
                pools = cleanedPools;
            }

            if (pools.stream().allMatch(String::isEmpty)) {
                throw new IllegalArgumentException("Veuillez sélectionner au moins un type de caractère utilisable.");
            }
            if (cfg.length() <= 0) {
                throw new IllegalArgumentException("La longueur du mot de passe doit être supérieure à 0.");
            }

            StringBuilder allChars = new StringBuilder();
            for (String p : pools) {
                allChars.append(p);
            }

            if (allChars.length() == 0) {
                throw new IllegalArgumentException("Aucun caractère disponible pour générer le mot de passe.");
            }

            char[] password = new char[cfg.length()];
            int index = 0;

            if (cfg.pronounceableLike() && (cfg.includeLowercase() || cfg.includeUppercase())) {
                String vowels = "aeiou";
                String consonants = "bcdfghjklmnpqrstvwxyz";
                String vowelPool = removeExcluded(cfg.includeUppercase() ? vowels + vowels.toUpperCase() : vowels, excluded);
                String consPool = removeExcluded(cfg.includeUppercase() ? consonants + consonants.toUpperCase() : consonants, excluded);

                for (int i = 0; i < cfg.length(); i++) {
                    String pool = (i % 2 == 0) ? consPool : vowelPool;
                    if (pool.isEmpty()) pool = allChars.toString();
                    password[i] = pool.charAt(random.nextInt(pool.length()));
                }

                if (cfg.requireEachSelectedSet()) enforcePools(password, pools);
                return new String(password);
            }

            if (cfg.requireEachSelectedSet()) {
                for (String pool : pools) {
                    if (index >= cfg.length()) break;
                    if (!pool.isEmpty()) {
                        password[index++] = pool.charAt(random.nextInt(pool.length()));
                    }
                }
            }

            while (index < cfg.length()) {
                password[index++] = allChars.charAt(random.nextInt(allChars.length()));
            }

            shuffle(password);
            return new String(password);
        }

        private String removeAmbiguous(String s) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (AMBIGUOUS.indexOf(c) < 0) sb.append(c);
            }
            return sb.toString();
        }
        
        private String removeExcluded(String s, String excluded) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (excluded.indexOf(c) < 0) sb.append(c);
            }
            return sb.toString();
        }

        private void shuffle(char[] array) {
            for (int i = array.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                char tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
            }
        }

        private void enforcePools(char[] password, List<String> pools) {
            String pwd = new String(password);
            for (String pool : pools) {
                if (!pool.isEmpty() && !containsAny(pwd, pool)) {
                    int pos = random.nextInt(password.length);
                    password[pos] = pool.charAt(random.nextInt(pool.length()));
                }
            }
            shuffle(password);
        }

        private boolean containsAny(String s, String pool) {
            for (char c : s.toCharArray()) {
                if (pool.indexOf(c) >= 0) return true;
            }
            return false;
        }
    }

    static class EntropyCalculator {
        static double calculate(String password, PasswordGenerationConfig cfg) {
            if (password == null || password.isEmpty()) return 0.0;

            int length = password.length();
            
            StringBuilder allChars = new StringBuilder();
            String excluded = cfg.excludeCharacters();
            
            if (cfg.includeUppercase()) allChars.append(PasswordGenerator.UPPERCASE);
            if (cfg.includeLowercase()) allChars.append(PasswordGenerator.LOWERCASE);
            if (cfg.includeDigits()) allChars.append(PasswordGenerator.DIGITS);
            if (cfg.includeSymbols()) allChars.append(PasswordGenerator.SYMBOLS);

            if (!cfg.customCharacters().isEmpty()) allChars.append(cfg.customCharacters());

            String pool = allChars.toString();
            
            if (cfg.avoidAmbiguous()) {
                pool = removeAmbiguous(pool);
            }
            if (!excluded.isEmpty()) {
                pool = removeExcluded(pool, excluded);
            }

            long alphabetSize = pool.chars().distinct().count();
            
            if (alphabetSize <= 1) return 0.0;

            return length * (Math.log(alphabetSize) / Math.log(2));
        }
        
        private static String removeAmbiguous(String s) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (PasswordGenerator.AMBIGUOUS.indexOf(c) < 0) sb.append(c);
            }
            return sb.toString();
        }
        
        private static String removeExcluded(String s, String excluded) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (excluded.indexOf(c) < 0) sb.append(c);
            }
            return sb.toString();
        }
    }

    static class PasswordStrengthEvaluator {
        enum StrengthLevel {
            VERY_WEAK("Très faible (< 40 bits)", new Color(0xFF4B4B)),
            WEAK("Faible (40-60 bits)", new Color(0xFF884B)),
            MEDIUM("Moyen (60-80 bits)", new Color(0xFFC14B)),
            STRONG("Fort (80-100 bits)", new Color(0x73DA80)),
            VERY_STRONG("Très fort (> 100 bits)", new Color(0x4CAF50));

            final String label;
            final Color color;

            StrengthLevel(String label, Color color) {
                this.label = label;
                this.color = color;
            }
        }

        StrengthLevel evaluate(double entropy, PasswordGenerationConfig cfg) {
            if (entropy < 40) return StrengthLevel.VERY_WEAK;
            if (entropy < 60) return StrengthLevel.WEAK;
            if (entropy < 80) return StrengthLevel.MEDIUM;
            if (entropy < 100) return StrengthLevel.STRONG;
            return StrengthLevel.VERY_STRONG;
        }
    }

    //==========================
    // UTILS DESIGN (Inchangé)
    //==========================

    static class RoundedButton extends JButton {
        private static final int ARC_SIZE = 12;
        private Color baseColor;
        private Color hoverColor;

        public RoundedButton(String text, Color baseColor, Color hoverColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Color currentColor = baseColor;
            if (getModel().isArmed()) {
                currentColor = baseColor.darker();
            } else if (getModel().isRollover()) {
                currentColor = hoverColor;
            } 

            g2.setColor(currentColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC_SIZE, ARC_SIZE);

            super.paintComponent(g);
            g2.dispose();
        }
    }
    
    static class ModernSliderUI extends BasicSliderUI {
        private final Color thumbColor;
        private final Color trackColor;

        public ModernSliderUI(JSlider b, Color thumbColor, Color trackColor) {
            super(b);
            this.thumbColor = thumbColor;
            this.trackColor = trackColor;
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int trackHeight = 6;
            int trackY = trackRect.y + (trackRect.height - trackHeight) / 2;

            g2.setColor(trackColor);
            g2.fillRoundRect(trackRect.x, trackY, trackRect.width, trackHeight, 6, 6);

            int fillWidth = xPositionForValue(slider.getValue()) - trackRect.x;
            g2.setColor(thumbColor.darker().darker());
            g2.fillRoundRect(trackRect.x, trackY, fillWidth, trackHeight, 6, 6);
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int thumbSize = 16;
            int x = thumbRect.x + (thumbRect.width - thumbSize) / 2;
            int y = thumbRect.y + (thumbRect.height - thumbSize) / 2;

            g2.setColor(thumbColor);
            g2.fillOval(x, y, thumbSize, thumbSize);

            g2.setColor(thumbColor.darker());
            g2.drawOval(x, y, thumbSize, thumbSize);
        }
    }


    interface DocumentChangeListener {
        void onChange();
    }

    static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final DocumentChangeListener listener;

        SimpleDocumentListener(DocumentChangeListener listener) {
            this.listener = Objects.requireNonNull(listener);
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            listener.onChange();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            listener.onChange();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            listener.onChange();
        }
    }
    
    //==========================
    // VIEW + CONTROLLER (MainFrame)
    //==========================
    static class MainFrame extends JFrame {

        private PasswordGenerationConfig cfg = new PasswordGenerationConfig();
        private final PasswordGenerator generator = new PasswordGenerator();
        private final PasswordStrengthEvaluator strengthEvaluator = new PasswordStrengthEvaluator();

        // UI components
        private JTextField passwordField;
        private JButton generateButton;
        private JButton copyButton;
        private JButton hideShowButton;
        private JButton newConfigButton;
        private JSlider lengthSlider;
        private JLabel lengthValueLabel;
        private JCheckBox uppercaseCheck;
        private JCheckBox lowercaseCheck;
        private JCheckBox digitsCheck;
        private JCheckBox symbolsCheck;
        private JCheckBox avoidAmbiguousCheck;
        private JCheckBox requireEachSetCheck;
        private JCheckBox pronounceableCheck;
        private JTextField customCharactersField;
        private JTextField excludeCharactersField;
        private JLabel strengthLabel;
        private JLabel entropyLabel;
        private JProgressBar strengthBar;
        private DefaultListModel<String> historyModel;
        private JList<String> historyList;
        private boolean isPasswordHidden = false;
        private String currentPassword = "";

        // Colors and theme (Soft Dark Mode)
        private final Color bgPrimary = new Color(0x1A1B26);
        private final Color bgSecondary = new Color(0x282A36);
        private final Color accent = new Color(0x82AAFF);
        private final Color accentSoft = new Color(0x7996DB);
        private final Color textPrimary = new Color(0xF8F8F2);
        private final Color textSecondary = new Color(0xAEAEB5);
        private final Color borderColor = new Color(0x44475A);

        // NOUVELLES DEFINITIONS DE POLICES POUR LE STYLE LIFESTYLE
        // Utilisation de 'Arial' ou 'Segoe UI' (plus doux) comme substitut de Sans-serif moderne
        private static final String FONT_NAME_SANS = "Segoe UI"; // Police plus douce et arrondie
        private static final String FONT_NAME_MONO = "JetBrains Mono"; // Reste technique pour la lisibilité
        
        // Tailles et Poids pour le style Lifestyle (plus aéré et marqué)
        private final Font FONT_TITLE = new Font(FONT_NAME_SANS, Font.BOLD, 20); // Plus grande et grasse
        private final Font FONT_SUBTITLE = new Font(FONT_NAME_SANS, Font.PLAIN, 12); // Plus aérée
        private final Font FONT_CARD_HEADER = new Font(FONT_NAME_SANS, Font.BOLD, 16);
        private final Font FONT_LABEL = new Font(FONT_NAME_SANS, Font.PLAIN, 14); // Taille légèrement augmentée
        private final Font FONT_BUTTON = new Font(FONT_NAME_SANS, Font.BOLD, 15);
        
        private final Font FONT_MONO_LARGE = new Font(FONT_NAME_MONO, Font.BOLD, 24); // Encore plus grand
        private final Font FONT_MONO_SMALL = new Font(FONT_NAME_MONO, Font.PLAIN, 13);


        MainFrame() {
            super("Ultra Password Generator - Édition Lifestyle 2026");
            initFrame();
            initComponents();
            layoutComponents();
            attachListeners();
            generateInitialPassword();
        }

        private void initFrame() {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(1080, 700));
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());
            getContentPane().setBackground(bgPrimary);
        }

        private void initComponents() {
            passwordField = new JTextField();
            // POLICE MONOSPACE GRANDE
            passwordField.setFont(FONT_MONO_LARGE);
            passwordField.setForeground(accent);
            passwordField.setBackground(bgSecondary.darker());
            passwordField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor.darker(), 1),
                    new EmptyBorder(12, 16, 12, 16)
            ));
            passwordField.setCaretColor(accent);
            passwordField.setEditable(false);

            generateButton = createPrimaryButton("Générer");
            copyButton = createSecondaryButton("Copier");
            
            hideShowButton = createIconButton("👁️");
            newConfigButton = createIconButton("✨ Nouveau");

            lengthSlider = new JSlider(6, 64, cfg.length());
            lengthSlider.setOpaque(false);
            lengthSlider.setMajorTickSpacing(8);
            lengthSlider.setMinorTickSpacing(1);
            lengthSlider.setPaintTicks(false);
            lengthSlider.setPaintLabels(false);
            lengthSlider.setUI(new ModernSliderUI(lengthSlider, accent, borderColor));

            lengthValueLabel = new JLabel(cfg.length() + " caractères");
            lengthValueLabel.setForeground(textSecondary);
            // POLICE LABEL GRASSE
            lengthValueLabel.setFont(FONT_LABEL.deriveFont(Font.BOLD));

            // POLICE LABEL pour les options
            uppercaseCheck = createCheckbox("Lettres majuscules (A-Z)", cfg.includeUppercase());
            lowercaseCheck = createCheckbox("Lettres minuscules (a-z)", cfg.includeLowercase());
            digitsCheck = createCheckbox("Chiffres (0-9)", cfg.includeDigits());
            symbolsCheck = createCheckbox("Symboles (!@#$...)", cfg.includeSymbols());
            avoidAmbiguousCheck = createCheckbox("Éviter les caractères ambigus (O,0,I,l,1,|)", cfg.avoidAmbiguous());
            requireEachSetCheck = createCheckbox("Inclure au moins un caractère de chaque type sélectionné", cfg.requireEachSelectedSet());
            pronounceableCheck = createCheckbox("Mot de passe 'prononçable' (consonne/voyelle)", cfg.pronounceableLike());

            customCharactersField = createTextField(cfg.customCharacters());
            customCharactersField.setToolTipText("Caractères supplémentaires à inclure.");
            
            excludeCharactersField = createTextField(cfg.excludeCharacters());
            excludeCharactersField.setToolTipText("Caractères à exclure de la génération (même si sélectionnés).");

            strengthLabel = new JLabel("Force du mot de passe : -");
            strengthLabel.setForeground(textSecondary);
            // POLICE LABEL GRASSE pour la force
            strengthLabel.setFont(FONT_LABEL.deriveFont(Font.BOLD));
            
            entropyLabel = new JLabel("Entropie : 0.0 bits");
            entropyLabel.setForeground(textSecondary);
            // POLICE SUBTITLE
            entropyLabel.setFont(FONT_SUBTITLE);

            strengthBar = new JProgressBar(0, 100);
            strengthBar.setValue(0);
            strengthBar.setBorderPainted(false);
            strengthBar.setForeground(accent);
            strengthBar.setBackground(new Color(0x353849));
            strengthBar.setPreferredSize(new Dimension(0, 12));
            strengthBar.putClientProperty("JProgressBar.flat", Boolean.TRUE);

            historyModel = new DefaultListModel<>();
            historyList = new JList<>(historyModel);
            historyList.setBackground(bgSecondary);
            historyList.setForeground(textSecondary);
            historyList.setSelectionBackground(accentSoft.darker());
            historyList.setSelectionForeground(textPrimary);
            // POLICE MONOSPACE PETITE
            historyList.setFont(FONT_MONO_SMALL);
            historyList.setBorder(new EmptyBorder(4, 0, 4, 0));
        }

        private JTextField createTextField(String initialValue) {
             JTextField field = new JTextField();
            // POLICE LABEL
            field.setFont(FONT_LABEL);
            field.setForeground(textPrimary);
            field.setBackground(bgSecondary.darker());
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor.darker(), 1),
                    new EmptyBorder(6, 8, 6, 8)
            ));
            field.setText(initialValue);
            return field;
        }

        private JCheckBox createCheckbox(String text, boolean selected) {
            JCheckBox checkBox = new JCheckBox(text, selected);
            checkBox.setOpaque(false);
            checkBox.setForeground(textSecondary);
            // POLICE LABEL
            checkBox.setFont(FONT_LABEL);
            return checkBox;
        }

        private JButton createIconButton(String text) {
            RoundedButton button = new RoundedButton(text, bgSecondary.darker(), borderColor);
            // POLICE LABEL
            button.setFont(FONT_LABEL);
            button.setForeground(textSecondary);
            button.setBorder(BorderFactory.createLineBorder(borderColor.darker(), 1));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            button.baseColor = bgSecondary.darker(); 
            button.hoverColor = borderColor;

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setForeground(textPrimary);
                    button.baseColor = borderColor;
                    button.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setForeground(textSecondary);
                    button.baseColor = bgSecondary.darker();
                    button.repaint();
                }
            });
            return button;
        }

        private JButton createPrimaryButton(String text) {
            RoundedButton button = new RoundedButton(text, accent, accentSoft);
            // POLICE BOUTON
            button.setFont(FONT_BUTTON);
            button.setForeground(Color.WHITE);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setBorder(new EmptyBorder(8, 18, 8, 18));
            return button;
        }

        private JButton createSecondaryButton(String text) {
            RoundedButton button = new RoundedButton(text, bgSecondary, borderColor);
            // POLICE LABEL
            button.setFont(FONT_LABEL);
            button.setForeground(textSecondary);
            button.setBorder(BorderFactory.createLineBorder(borderColor.darker(), 1));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            button.baseColor = bgSecondary; 
            button.hoverColor = bgSecondary.darker();

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setForeground(textPrimary);
                    button.baseColor = bgSecondary.darker();
                    button.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setForeground(textSecondary);
                    button.baseColor = bgSecondary;
                    button.repaint();
                }
            });
            return button;
        }

        private JPanel createCardPanel(String title, String subtitle) {
            JPanel panel = new JPanel();
            panel.setBackground(bgSecondary);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1, true),
                    new EmptyBorder(18, 18, 18, 18)
            ));
            panel.setLayout(new BorderLayout(0, 15));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(textPrimary);
            // POLICE ENTÊTE DE CARTE
            titleLabel.setFont(FONT_CARD_HEADER);

            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setForeground(textSecondary.darker());
            // POLICE SUBTITLE
            subtitleLabel.setFont(FONT_SUBTITLE);

            header.add(titleLabel, BorderLayout.NORTH);
            header.add(subtitleLabel, BorderLayout.SOUTH);

            panel.add(header, BorderLayout.NORTH);

            return panel;
        }

        private void layoutComponents() {
            JPanel content = new JPanel();
            content.setBackground(bgPrimary);
            content.setLayout(new BorderLayout());
            content.setBorder(new EmptyBorder(24, 24, 16, 24));
            add(content, BorderLayout.CENTER);

            // TOP BAR
            JPanel appBar = new JPanel(new BorderLayout());
            appBar.setOpaque(false);
            
            JLabel title = new JLabel("Ultra Password Generator");
            title.setForeground(textPrimary);
            // POLICE TITRE PRINCIPAL
            title.setFont(FONT_TITLE);

            JLabel version = new JLabel("Édition Lifestyle 2026 • Java SE Desktop");
            version.setForeground(textSecondary);
            // POLICE SUBTITLE
            version.setFont(FONT_SUBTITLE);

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
            titlePanel.add(title);
            titlePanel.add(Box.createVerticalStrut(4));
            titlePanel.add(version);

            appBar.add(titlePanel, BorderLayout.WEST);
            appBar.add(newConfigButton, BorderLayout.EAST);

            content.add(appBar, BorderLayout.NORTH);

            // CENTER: Main split
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setResizeWeight(0.60);
            splitPane.setBorder(null);
            splitPane.setBackground(bgPrimary);
            splitPane.setDividerSize(10);

            JPanel leftPanel = new JPanel();
            leftPanel.setOpaque(false);
            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            leftPanel.add(createGeneratorPanel());
            leftPanel.add(Box.createVerticalStrut(20));
            leftPanel.add(createStrengthPanel());

            JPanel rightPanel = new JPanel();
            rightPanel.setOpaque(false);
            rightPanel.setLayout(new BorderLayout());
            rightPanel.add(createHistoryPanel(), BorderLayout.CENTER);

            splitPane.setLeftComponent(leftPanel);
            splitPane.setRightComponent(rightPanel);

            content.add(splitPane, BorderLayout.CENTER);

            // Status bar
            JPanel statusBar = new JPanel(new BorderLayout());
            statusBar.setBackground(bgPrimary);
            statusBar.setBorder(new EmptyBorder(10, 0, 0, 0));

            JLabel hint = new JLabel("Astuce : Ctrl/Cmd + G pour Générer | Ctrl/Cmd + C pour Copier | Double-clic sur l'historique pour copier.");
            hint.setForeground(textSecondary.darker());
            // POLICE SUBTITLE
            hint.setFont(FONT_SUBTITLE);

            statusBar.add(hint, BorderLayout.WEST);

            content.add(statusBar, BorderLayout.SOUTH);
        }

        private JPanel createGeneratorPanel() {
            JPanel card = createCardPanel("Générateur de mot de passe", "Critères robustes pour une sécurité maximale.");

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBorder(new EmptyBorder(10, 0, 0, 0));

            // Row: password field + buttons
            JPanel rowPassword = new JPanel(new BorderLayout(10, 0));
            rowPassword.setOpaque(false);
            rowPassword.add(passwordField, BorderLayout.CENTER);

            JPanel btnPanel = new JPanel();
            btnPanel.setOpaque(false);
            btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.X_AXIS));
            btnPanel.add(hideShowButton);
            btnPanel.add(Box.createHorizontalStrut(10));
            btnPanel.add(generateButton);
            btnPanel.add(Box.createHorizontalStrut(10));
            btnPanel.add(copyButton);

            rowPassword.add(btnPanel, BorderLayout.EAST);

            center.add(rowPassword);
            center.add(Box.createVerticalStrut(18));

            // Length row
            JPanel lengthRow = new JPanel(new BorderLayout(10, 0));
            lengthRow.setOpaque(false);

            JLabel lengthLabel = new JLabel("Longueur (Min 6, Max 64)");
            lengthLabel.setForeground(textSecondary);
            // POLICE LABEL
            lengthLabel.setFont(FONT_LABEL);

            lengthRow.add(lengthLabel, BorderLayout.WEST);
            lengthRow.add(lengthSlider, BorderLayout.CENTER);
            lengthRow.add(lengthValueLabel, BorderLayout.EAST);

            center.add(lengthRow);
            center.add(Box.createVerticalStrut(18));

            // Options: character types
            JPanel optionsPanel = new JPanel();
            optionsPanel.setOpaque(false);
            optionsPanel.setLayout(new GridLayout(2, 2, 12, 6));
            optionsPanel.add(uppercaseCheck);
            optionsPanel.add(lowercaseCheck);
            optionsPanel.add(digitsCheck);
            optionsPanel.add(symbolsCheck);

            center.add(optionsPanel);
            center.add(Box.createVerticalStrut(12));

            center.add(avoidAmbiguousCheck);
            center.add(Box.createVerticalStrut(6));
            center.add(requireEachSetCheck);
            center.add(Box.createVerticalStrut(6));
            center.add(pronounceableCheck);
            center.add(Box.createVerticalStrut(15));

            // Custom character fields
            
            // Custom characters (Include)
            JPanel customRow = new JPanel(new BorderLayout(8, 0));
            customRow.setOpaque(false);
            JLabel customLabel = new JLabel("Caractères à INCLURE (Optionnel)");
            customLabel.setForeground(textSecondary);
            // POLICE LABEL
            customLabel.setFont(FONT_LABEL);
            customRow.add(customLabel, BorderLayout.NORTH);
            customRow.add(Box.createVerticalStrut(4), BorderLayout.CENTER);
            customRow.add(customCharactersField, BorderLayout.SOUTH);
            center.add(customRow);
            center.add(Box.createVerticalStrut(10));
            
            // Exclude characters
            JPanel excludeRow = new JPanel(new BorderLayout(8, 0));
            excludeRow.setOpaque(false);
            JLabel excludeLabel = new JLabel("Caractères à EXCLURE (Optionnel)");
            excludeLabel.setForeground(textSecondary);
            // POLICE LABEL
            excludeLabel.setFont(FONT_LABEL);
            excludeRow.add(excludeLabel, BorderLayout.NORTH);
            excludeRow.add(Box.createVerticalStrut(4), BorderLayout.CENTER);
            excludeRow.add(excludeCharactersField, BorderLayout.SOUTH);
            center.add(excludeRow);


            card.add(center, BorderLayout.CENTER);

            return card;
        }

        private JPanel createStrengthPanel() {
            JPanel card = createCardPanel("Analyse de la force", "Entropie cryptographique et évaluation NIST-compatible.");

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBorder(new EmptyBorder(10, 0, 0, 0));

            center.add(strengthLabel);
            center.add(Box.createVerticalStrut(4));
            center.add(entropyLabel);
            center.add(Box.createVerticalStrut(8));
            center.add(strengthBar);

            card.add(center, BorderLayout.CENTER);

            return card;
        }

        private JPanel createHistoryPanel() {
            JPanel card = createCardPanel("Historique des mots de passe", "Stockage sécurisé pour la session en cours uniquement.");

            JScrollPane scroll = new JScrollPane(historyList);
            scroll.setBorder(BorderFactory.createLineBorder(borderColor.darker(), 1));
            scroll.getViewport().setBackground(bgSecondary);
            scroll.setBackground(bgSecondary);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            card.add(scroll, BorderLayout.CENTER);

            JButton clearHistoryButton = createSecondaryButton("Effacer l'historique");
            // POLICE LABEL
            clearHistoryButton.setFont(FONT_LABEL);
            clearHistoryButton.setBorder(new EmptyBorder(8, 12, 8, 12));
            clearHistoryButton.addActionListener(e -> {
                historyModel.clear();
                Toolkit.getDefaultToolkit().beep();
            });

            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            footer.setBorder(new EmptyBorder(15, 0, 0, 0));
            footer.add(clearHistoryButton, BorderLayout.EAST);

            card.add(footer, BorderLayout.SOUTH);

            return card;
        }

        private void attachListeners() {
            generateButton.addActionListener(e -> generatePassword());

            copyButton.addActionListener(e -> {
                String pwd = passwordField.getText();
                if (pwd != null && !pwd.isEmpty()) {
                    copyToClipboard(currentPassword);
                    showTransientMessage("Mot de passe copié dans le presse-papiers.");
                } else {
                    showTransientMessage("Aucun mot de passe à copier.");
                }
            });
            
            hideShowButton.addActionListener(e -> togglePasswordVisibility());
            
            newConfigButton.addActionListener(e -> resetConfigToDefault());

            lengthSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    cfg = updateConfigState();
                    lengthValueLabel.setText(cfg.length() + " caractères");
                    onPasswordChanged();
                }
            });

            ActionListener configUpdater = e -> {
                cfg = updateConfigState();
                onPasswordChanged();
            };
            uppercaseCheck.addActionListener(configUpdater);
            lowercaseCheck.addActionListener(configUpdater);
            digitsCheck.addActionListener(configUpdater);
            symbolsCheck.addActionListener(configUpdater);
            avoidAmbiguousCheck.addActionListener(configUpdater);
            requireEachSetCheck.addActionListener(configUpdater);
            pronounceableCheck.addActionListener(configUpdater);

            SimpleDocumentListener docListener = new SimpleDocumentListener(() -> {
                cfg = updateConfigState();
                onPasswordChanged();
            });
            customCharactersField.getDocument().addDocumentListener(docListener);
            excludeCharactersField.getDocument().addDocumentListener(docListener);

            historyList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = historyList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            String line = historyModel.get(index);
                            String pwd = extractPasswordFromHistoryLine(line);
                            if (!pwd.isEmpty()) {
                                copyToClipboard(pwd);
                                currentPassword = pwd;
                                passwordField.setText(isPasswordHidden ? repeatChar('•', pwd.length()) : pwd);
                                onPasswordChanged();
                                showTransientMessage("Mot de passe copié depuis l'historique.");
                            }
                        }
                    }
                }
            });

            InputMap im = passwordField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = passwordField.getActionMap();
            final int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, shortcutMask), "generate");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask), "copy");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutMask), "toggleVisibility");

            am.put("generate", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { generatePassword(); }
            });
            am.put("copy", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { 
                    copyToClipboard(currentPassword);
                    showTransientMessage("Copié avec Ctrl/Cmd + C.");
                }
            });
            am.put("toggleVisibility", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { togglePasswordVisibility(); }
            });

            JPopupMenu popup = new JPopupMenu();
            JMenuItem copyItem = new JMenuItem(new DefaultEditorKit.CopyAction());
            copyItem.setText("Copier");
            popup.add(copyItem);

            passwordField.setComponentPopupMenu(popup);
        }

        private PasswordGenerationConfig updateConfigState() {
            return new PasswordGenerationConfig(
                    lengthSlider.getValue(),
                    uppercaseCheck.isSelected(),
                    lowercaseCheck.isSelected(),
                    digitsCheck.isSelected(),
                    symbolsCheck.isSelected(),
                    avoidAmbiguousCheck.isSelected(),
                    requireEachSetCheck.isSelected(),
                    pronounceableCheck.isSelected(),
                    customCharactersField.getText(),
                    excludeCharactersField.getText()
            );
        }

        private void resetConfigToDefault() {
            cfg = new PasswordGenerationConfig();
            
            lengthSlider.setValue(cfg.length());
            lengthValueLabel.setText(cfg.length() + " caractères");
            uppercaseCheck.setSelected(cfg.includeUppercase());
            lowercaseCheck.setSelected(cfg.includeLowercase());
            digitsCheck.setSelected(cfg.includeDigits());
            symbolsCheck.setSelected(cfg.includeSymbols());
            avoidAmbiguousCheck.setSelected(cfg.avoidAmbiguous());
            requireEachSetCheck.setSelected(cfg.requireEachSelectedSet());
            pronounceableCheck.setSelected(cfg.pronounceableLike());
            customCharactersField.setText(cfg.customCharacters());
            excludeCharactersField.setText(cfg.excludeCharacters());
            
            generatePassword(); 
            showTransientMessage("Configuration réinitialisée aux valeurs par défaut.");
        }

        private void generateInitialPassword() {
            generatePassword();
        }

        private void generatePassword() {
            try {
                cfg = updateConfigState(); 
                PasswordGenerationConfig configSnapshot = cfg.cloneConfig();
                String pwd = generator.generate(configSnapshot);
                currentPassword = pwd;
                
                passwordField.setText(isPasswordHidden ? repeatChar('•', pwd.length()) : pwd);
                
                onPasswordChanged();
                addToHistory(pwd, configSnapshot);
            } catch (IllegalArgumentException ex) {
                passwordField.setText("ERREUR");
                currentPassword = "";
                onPasswordChanged();
                showTransientMessage(ex.getMessage());
            }
        }
        
        private void togglePasswordVisibility() {
            isPasswordHidden = !isPasswordHidden;
            if (currentPassword.isEmpty()) return;
            
            if (isPasswordHidden) {
                passwordField.setText(repeatChar('•', currentPassword.length()));
                hideShowButton.setText("🙈");
                showTransientMessage("Mot de passe masqué.");
            } else {
                passwordField.setText(currentPassword);
                hideShowButton.setText("👁️");
                showTransientMessage("Mot de passe affiché.");
            }
        }
        
        private String repeatChar(char c, int count) {
            StringBuilder sb = new StringBuilder(count);
            for (int i = 0; i < count; i++) {
                sb.append(c);
            }
            return sb.toString();
        }

        private void onPasswordChanged() {
            String pwd = currentPassword;
            if (pwd.isEmpty()) {
                strengthLabel.setText("Force du mot de passe : -");
                entropyLabel.setText("Entropie : 0.0 bits");
                strengthBar.setValue(0);
                strengthBar.setForeground(new Color(0x353849));
                return;
            }
            
            double entropy = EntropyCalculator.calculate(pwd, cfg);
            PasswordStrengthEvaluator.StrengthLevel level = strengthEvaluator.evaluate(entropy, cfg);

            strengthLabel.setText("Force du mot de passe : " + level.label.split("\\s+\\(")[0]);
            entropyLabel.setText(String.format("Entropie : %.2f bits", entropy));

            int value;
            switch (level) {
                case VERY_WEAK:
                    value = 10;
                    break;
                case WEAK:
                    value = 30;
                    break;
                case MEDIUM:
                    value = 55;
                    break;
                case STRONG:
                    value = 80;
                    break;
                case VERY_STRONG:
                default:
                    value = 100;
                    break;
            }

            strengthBar.setValue(value);
            strengthBar.setForeground(level.color);
        }

        private void addToHistory(String pwd, PasswordGenerationConfig configSnapshot) {
            if (pwd == null || pwd.isEmpty()) return;

            PasswordEntry entry = new PasswordEntry(pwd, configSnapshot);
            historyModel.add(0, entry.toDisplayString());

            int maxHistory = 100;
            if (historyModel.getSize() > maxHistory) {
                historyModel.removeElementAt(historyModel.size() - 1);
            }
        }

        private String extractPasswordFromHistoryLine(String line) {
            int idx = line.indexOf("]  ");
            if (idx >= 0 && idx + 3 <= line.length()) {
                return line.substring(idx + 3).trim();
            }
            return "";
        }

        private void copyToClipboard(String text) {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }

        private void showTransientMessage(String message) {
            final JDialog dialog = new JDialog(this, false);
            dialog.setUndecorated(true);
            dialog.setBackground(new Color(0, 0, 0, 0));

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(40, 42, 54, 240)); 
            panel.setBorder(new EmptyBorder(10, 16, 10, 16));

            JLabel label = new JLabel(message);
            label.setForeground(textPrimary);
            // POLICE LABEL
            label.setFont(FONT_LABEL);
            panel.add(label, BorderLayout.CENTER);

            dialog.getContentPane().add(panel);
            dialog.pack();

            Point p = getLocationOnScreen();
            int x = p.x + (getWidth() - dialog.getWidth()) / 2;
            int y = p.y + getHeight() - dialog.getHeight() - 40;
            dialog.setLocation(x, y);

            dialog.setVisible(true);

            new Timer(2000, e -> dialog.dispose()).start();
        }

    }
}