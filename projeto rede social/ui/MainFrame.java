package ui;

import negocios.Sistema;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MainFrame extends JFrame {
    private Sistema sistema;

    public static final Color AMARELO_CLARO = new Color(255, 249, 196);
    public static final Color AMARELO_MEDIO = new Color(255, 235, 59);
    public static final Color AMARELO_ESCURO = new Color(255, 193, 7);
    public static final Color AMARELO_BOTAO = new Color(255, 213, 79);
    public static final Color AMARELO_BORDA = new Color(255, 179, 0);
    public static final Color AMARELO_PALE = new Color(255, 252, 224);
    public static final Color AMARELO_CARD = new Color(255, 241, 168);
    public static final Color AMARELO_DOURADO = new Color(255, 193, 7);

    public static JLabel criarLogo(int largura, int altura) {
        BufferedImage imagem = null;
        String[] caminhos = {"projeto rede social/images/logo.png", "images/logo.png"};
        for (String caminho : caminhos) {
            try {
                File arquivo = new File(caminho);
                if (arquivo.isFile()) {
                    imagem = ImageIO.read(arquivo);
                    if (imagem != null) {
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Tenta o próximo caminho conhecido.
            }
        }
        if (imagem == null) {
            java.net.URL recurso = MainFrame.class.getResource("/images/logo.png");
            if (recurso != null) {
                try {
                    imagem = ImageIO.read(recurso);
                } catch (Exception ignored) {
                    imagem = null;
                }
            }
        }

        JLabel logo = new JLabel();
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(largura, altura));
        if (imagem != null) {
            Image escalada = imagem.getScaledInstance(largura, altura, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(escalada));
        } else {
            logo.setText("OMELETTY");
            logo.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, altura / 3)));
            logo.setForeground(new Color(143, 83, 0));
        }
        return logo;
    }

    public MainFrame(Sistema sistema) {
        this.sistema = sistema;
        setTitle("Omellety - Rede Social");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        getContentPane().setBackground(AMARELO_CLARO);
        mostrarInicio();
    }

    public void mostrarInicio() {
        setSize(560, 620);
        setMinimumSize(new Dimension(480, 540));
        setLocationRelativeTo(null);
        setContentPane(new InicioPanel(sistema, this));
        revalidate();
        repaint();
    }

    public void mostrarCadastro() {
        setContentPane(new CadastroPanel(sistema, this));
        revalidate();
        repaint();
        setSize(680, 500);
        setMinimumSize(new Dimension(560, 440));
        setLocationRelativeTo(null);
    }

    public void mostrarLogin() {
        setContentPane(new LoginPanel(sistema, this));
        revalidate();
        repaint();
        setSize(620, 440);
        setMinimumSize(new Dimension(500, 380));
        setLocationRelativeTo(null);
    }

    public void mostrarFeed() {
        setSize(1100, 760);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);
        setContentPane(new FeedPanel(sistema, this));
        revalidate();
        repaint();
    }

    public void mostrarGestao() {
        setSize(1180, 780);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        setContentPane(new GestaoPanel(this));
        revalidate();
        repaint();
    }
}