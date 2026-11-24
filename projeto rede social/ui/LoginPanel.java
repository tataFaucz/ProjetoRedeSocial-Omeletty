package ui;

import negocios.Sistema;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private Sistema sistema;
    private MainFrame mainFrame;

    public LoginPanel(Sistema sistema, MainFrame mainFrame) {
        this.sistema = sistema;
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setBackground(MainFrame.AMARELO_CLARO);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(MainFrame.criarLogo(150, 150), gbc);
        gbc.gridwidth = 1;

        JLabel lblEmail = new JLabel("E-mail:");
        JTextField txtEmail = new JTextField(20);

        JLabel lblSenha = new JLabel("Senha:");
        JPasswordField txtSenha = new JPasswordField(20);

        JButton btnEntrar = new JButton("Entrar");
        JButton btnVoltar = new JButton("Voltar");

        lblEmail.setForeground(Color.DARK_GRAY);
        lblSenha.setForeground(Color.DARK_GRAY);

        txtEmail.setBackground(MainFrame.AMARELO_CLARO);
        txtSenha.setBackground(MainFrame.AMARELO_CLARO);

        txtEmail.setForeground(Color.DARK_GRAY);
        txtSenha.setForeground(Color.DARK_GRAY);

        btnEntrar.setBackground(MainFrame.AMARELO_BOTAO);
        btnEntrar.setForeground(Color.DARK_GRAY);
        btnEntrar.setFocusPainted(false);
        btnEntrar.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
        btnEntrar.setFont(new Font("Arial", Font.BOLD, 13));

        btnVoltar.setBackground(MainFrame.AMARELO_BOTAO);
        btnVoltar.setForeground(Color.DARK_GRAY);
        btnVoltar.setFocusPainted(false);
        btnVoltar.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
        btnVoltar.setFont(new Font("Arial", Font.BOLD, 13));

        gbc.gridx = 0; gbc.gridy = 1; add(lblEmail, gbc);
        gbc.gridx = 1; gbc.gridy = 1; add(txtEmail, gbc);
        gbc.gridx = 0; gbc.gridy = 2; add(lblSenha, gbc);
        gbc.gridx = 1; gbc.gridy = 2; add(txtSenha, gbc);
        gbc.gridx = 0; gbc.gridy = 3; add(btnEntrar, gbc);
        gbc.gridx = 1; gbc.gridy = 3; add(btnVoltar, gbc);

        btnEntrar.addActionListener(e -> {
            String identificador = txtEmail.getText().trim();
            String senha = new String(txtSenha.getPassword());

            try {
                if (sistema.login(identificador, senha)) {
                    mainFrame.mostrarFeed();
                } else {
                    JOptionPane.showMessageDialog(this, "Credenciais inválidas.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Não foi possível consultar a conta no PostgreSQL.\nDetalhe: " + ex.getMessage() +
                    "\nConfira se o PostgreSQL está ativo e se a URL e as credenciais estão corretas.",
                    "Erro de banco de dados", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnVoltar.addActionListener(e -> mainFrame.mostrarInicio());
    }
}