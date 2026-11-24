package ui;

import negocios.Sistema;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;

public class CadastroPanel extends JPanel {
    private Sistema sistema;
    private MainFrame mainFrame;

    public CadastroPanel(Sistema sistema, MainFrame mainFrame) {
        this.sistema = sistema;
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setBackground(MainFrame.AMARELO_CLARO);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel logo = MainFrame.criarLogo(145, 145);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(logo, gbc);
        gbc.gridwidth = 1;

        JLabel lblNome = new JLabel("Nome:");
        JTextField txtNome = new JTextField(20);

        JLabel lblEmail = new JLabel("Email:");
        JTextField txtEmail = new JTextField(20);

        JLabel lblSenha = new JLabel("Senha:");
        JPasswordField txtSenha = new JPasswordField(20);

        JLabel lblFoto = new JLabel("Foto de Perfil (opcional):");
        JTextField txtFoto = new JTextField(20);
        JButton btnEscolherFoto = new JButton("Selecionar...");
        btnEscolherFoto.setPreferredSize(new Dimension(112, 28));
        btnEscolherFoto.addActionListener(event -> {
            JFileChooser seletor = new JFileChooser();
            seletor.setDialogTitle("Selecionar foto de perfil");
            if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtFoto.setText(seletor.getSelectedFile().getAbsolutePath());
            }
        });
        JPanel campoFoto = new JPanel(new BorderLayout(5, 0));
        campoFoto.add(txtFoto, BorderLayout.CENTER);
        campoFoto.add(btnEscolherFoto, BorderLayout.EAST);
        campoFoto.setOpaque(false);
        campoFoto.setMinimumSize(new Dimension(280, 28));
        campoFoto.setPreferredSize(new Dimension(340, 28));

        JLabel lblBiografia = new JLabel("Biografia (opcional):");
        JTextField txtBiografia = new JTextField(20);

        JButton btnCadastrar = new JButton("Cadastrar");
        JButton btnVoltar = new JButton("Voltar");

        lblNome.setForeground(Color.DARK_GRAY);
        lblEmail.setForeground(Color.DARK_GRAY);
        lblSenha.setForeground(Color.DARK_GRAY);
        lblFoto.setForeground(Color.DARK_GRAY);
        lblBiografia.setForeground(Color.DARK_GRAY);

        txtNome.setBackground(MainFrame.AMARELO_CLARO);
        txtEmail.setBackground(MainFrame.AMARELO_CLARO);
        txtSenha.setBackground(MainFrame.AMARELO_CLARO);
        txtFoto.setBackground(MainFrame.AMARELO_CLARO);
        txtBiografia.setBackground(MainFrame.AMARELO_CLARO);

        txtNome.setForeground(Color.DARK_GRAY);
        txtEmail.setForeground(Color.DARK_GRAY);
        txtSenha.setForeground(Color.DARK_GRAY);
        txtFoto.setForeground(Color.DARK_GRAY);
        txtBiografia.setForeground(Color.DARK_GRAY);

        btnCadastrar.setBackground(MainFrame.AMARELO_BOTAO);
        btnCadastrar.setForeground(Color.DARK_GRAY);
        btnCadastrar.setFocusPainted(false);
        btnCadastrar.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
        btnCadastrar.setFont(new Font("Arial", Font.BOLD, 13));

        btnVoltar.setBackground(MainFrame.AMARELO_BOTAO);
        btnVoltar.setForeground(Color.DARK_GRAY);
        btnVoltar.setFocusPainted(false);
        btnVoltar.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
        btnVoltar.setFont(new Font("Arial", Font.BOLD, 13));

        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridx = 0; gbc.gridy = 1; add(lblNome, gbc);
        gbc.gridy = 2; add(lblEmail, gbc);
        gbc.gridy = 3; add(lblSenha, gbc);
        gbc.gridy = 4; add(lblFoto, gbc);
        gbc.gridy = 5; add(lblBiografia, gbc);

        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 1; gbc.gridy = 1; add(txtNome, gbc);
        gbc.gridy = 2; add(txtEmail, gbc);
        gbc.gridy = 3; add(txtSenha, gbc);
        gbc.gridy = 4; add(campoFoto, gbc);
        gbc.gridy = 5; add(txtBiografia, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 1; gbc.gridy = 6; add(btnCadastrar, gbc);
        gbc.gridx = 0; add(btnVoltar, gbc);

        btnCadastrar.addActionListener(e -> {
            String nome = txtNome.getText().trim();
            String email = txtEmail.getText().trim();
            String senha = new String(txtSenha.getPassword());
            String fotoPerfil = txtFoto.getText().trim();
            String biografia = txtBiografia.getText().trim();

            if (!fotoPerfil.isEmpty() && !fotoPerfil.startsWith("http://") && !fotoPerfil.startsWith("https://")) {
                try {
                    File arquivo = new File(fotoPerfil).getAbsoluteFile();
                    if (!arquivo.isFile() || ImageIO.read(arquivo) == null) {
                        JOptionPane.showMessageDialog(this, "Selecione um arquivo de imagem válido para a foto de perfil.");
                        return;
                    }
                    fotoPerfil = arquivo.getAbsolutePath();
                } catch (java.io.IOException ex) {
                    JOptionPane.showMessageDialog(this, "Não foi possível ler a foto de perfil: " + ex.getMessage());
                    return;
                }
            }

            String erroValidacao = negocios.ValidacaoConta.validar(nome, email,
                biografia, fotoPerfil, senha, true);
            if (erroValidacao != null) {
                JOptionPane.showMessageDialog(this, erroValidacao);
                return;
            }

            try {
                boolean sucesso = sistema.cadastrarUsuario(nome, email, senha,
                    fotoPerfil, biografia);
                if (sucesso) {
                    JOptionPane.showMessageDialog(this, "Cadastro realizado com sucesso!");
                    mainFrame.mostrarInicio();
                } else {
                    JOptionPane.showMessageDialog(this, "E-mail inválido ou já cadastrado.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível acessar o banco de dados.\nDetalhe: " + ex.getMessage(),
                    "Erro de banco de dados",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        btnVoltar.addActionListener(e -> mainFrame.mostrarInicio());
    }
}
