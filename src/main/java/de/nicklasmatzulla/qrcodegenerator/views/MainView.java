/*
 * Copyright 2023 Nicklas Matzulla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.nicklasmatzulla.qrcodegenerator.views;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import de.nicklasmatzulla.qrcodegenerator.QrCodeGenerator;
import de.nicklasmatzulla.qrcodegenerator.util.ImageTransferable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainView extends JFrame {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 300;
    private static final int QR_CODE_SIZE = 200;

    private ImageIcon qrCodeImageIcon;
    private JTextField urlField;
    private boolean generated = false;

    public MainView() {
        super("QRCode generieren");
        createAndShowGui();
    }

    private void createAndShowGui() {
        setLayout(new BorderLayout());

        createLeftPanel();
        createRightPanel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMaximumSize(new Dimension(WIDTH * 35 / 100, HEIGHT));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));

        qrCodeImageIcon = new ImageIcon(getLogoImage().getScaledInstance(QR_CODE_SIZE, QR_CODE_SIZE, Image.SCALE_SMOOTH));
        JLabel qrCodeLabel = new JLabel(qrCodeImageIcon);
        leftPanel.add(qrCodeLabel, BorderLayout.CENTER);

        JButton saveButton = createButton("Speichern", this::handleSave);
        JButton copyButton = createButton("Kopieren", this::handleCopy);
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(saveButton);
        buttonPanel.add(copyButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
    }

    private void createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setMaximumSize(new Dimension(WIDTH * 65 / 100, HEIGHT));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        JPanel urlPanel = new JPanel(new BorderLayout());
        urlField = new JTextField();
        urlField.setToolTipText("Link oder Text eingeben");
        urlPanel.add(urlField, BorderLayout.CENTER);

        JButton generateButton = createButton("QRCode generieren", this::handleGenerate);
        urlPanel.add(generateButton, BorderLayout.SOUTH);

        rightPanel.add(urlPanel, BorderLayout.NORTH);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JButton createButton(final String text, final ActionListener actionListener) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
        return button;
    }

    private void handleGenerate(final ActionEvent e) {
        if (urlField.getText().isEmpty()) {
            return;
        }
        try {
            final String url = urlField.getText();
            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
            final BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            qrCodeImageIcon.setImage(bufferedImage);
            generated = true;
            repaint();
        } catch (final WriterException | RuntimeException ignored) {
            showErrorDialog();
        }
    }

    private void handleSave(final ActionEvent e) {
        if (!generated) {
            return;
        }
        try {
            final Image image = qrCodeImageIcon.getImage();
            final JFileChooser fileChooser = createFileChooser();
            final int userSelection = fileChooser.showSaveDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = getFileToSave(fileChooser.getSelectedFile());
                ImageIO.write((BufferedImage) image, "png", fileToSave);
            }
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handleCopy(final ActionEvent e) {
        if (!generated) {
            return;
        }
        final Image image = qrCodeImageIcon.getImage();
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final ImageTransferable imageTransferable = new ImageTransferable(image);
        clipboard.setContents(imageTransferable, null);
        showMessageDialog();
    }

    private JFileChooser createFileChooser() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("png", "png"));
        return fileChooser;
    }

    private File getFileToSave(final File selectedFile) {
        if (!selectedFile.getName().toLowerCase().endsWith(".png")) {
            return new File(selectedFile.getParentFile(), selectedFile.getName() + ".png");
        }
        return selectedFile;
    }

    private void showErrorDialog() {
        JOptionPane.showMessageDialog(null, "Der QRCode konnte nicht generiert werden!", "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessageDialog() {
        JOptionPane.showMessageDialog(null, "Der QRCode wurde erfolgreich in die Zwischenablage kopiert.", "QRCode kopiert", JOptionPane.INFORMATION_MESSAGE);
    }

    @SuppressWarnings("DataFlowIssue")
    private Image getLogoImage() {
        try (final InputStream imageInputStream = QrCodeGenerator.class.getClassLoader().getResourceAsStream("logo.png")) {
            return ImageIO.read(imageInputStream);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
