package ui;

import negocios.Sistema;

import javax.swing.*;
import java.awt.*;

public class InicioPanel extends JPanel {
    public InicioPanel(Sistema sistema, MainFrame mainFrame) {
        setLayout(new GridBagLayout());
        setBackground(MainFrame.AMARELO_CLARO);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel logoLabel = MainFrame.criarLogo(250, 250);
        JButton btnEntrar = new JButton("Entrar");
        JButton btnCadastrar = new JButton("Cadastrar");
        JButton btnGestao = new JButton("Gestão e relatórios");

        btnEntrar.setBackground(MainFrame.AMARELO_BOTAO);
        btnEntrar.setForeground(Color.DARK_GRAY);
        btnEntrar.setFocusPainted(false);
        btnEntrar.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
        btnEntrar.setFont(new Font("Arial", Font.BOLD, 16));

        btnCadastrar.setBackground(MainFrame.AMARELO_BOTAO);
        btnCadastrar.setForeground(Color.DARK_GRAY);
        btnCadastrar.setFocusPainted(false);
        btnCadastrar.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
        btnCadastrar.setFont(new Font("Arial", Font.BOLD, 16));

        btnGestao.setBackground(MainFrame.AMARELO_MEDIO);
        btnGestao.setForeground(Color.DARK_GRAY);
        btnGestao.setFocusPainted(false);
        btnGestao.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
        btnGestao.setFont(new Font("Arial", Font.BOLD, 15));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; add(logoLabel, gbc);
        gbc.gridy = 1; add(btnEntrar, gbc);
        gbc.gridy = 2; add(btnCadastrar, gbc);
        gbc.gridy = 3; add(btnGestao, gbc);

        btnEntrar.addActionListener(e -> mainFrame.mostrarLogin());
        btnCadastrar.addActionListener(e -> mainFrame.mostrarCadastro());
        btnGestao.addActionListener(e -> mainFrame.mostrarGestao());
    }
}