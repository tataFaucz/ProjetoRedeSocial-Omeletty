package ui;

import dados.Foto;
import dados.Usuario;
import negocios.Sistema;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;

public class FeedPanel extends JPanel {
    private Sistema sistema;
    private MainFrame mainFrame;
    private JPanel feedArea;

    public FeedPanel(Sistema sistema, MainFrame mainFrame) {
        this.sistema = sistema;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(MainFrame.AMARELO_CLARO);

        criarMenuLateral();
        criarAreaFeed();
        carregarPublicacoes();
    }

    private void criarMenuLateral() {
        JPanel menu = new JPanel(new GridLayout(7, 1));
        menu.setPreferredSize(new Dimension(150, getHeight()));
        menu.setBackground(MainFrame.AMARELO_MEDIO);

        JButton btnPublicar = new JButton("Nova Publicação");
        JButton btnExplorar = new JButton("Explorar");
        JButton btnMensagens = new JButton("Mensagens");
        JButton btnPerfil = new JButton("Perfil");
        JButton btnLogout = new JButton("Logout");

        for (JButton btn : new JButton[]{btnPublicar, btnExplorar, btnMensagens, btnPerfil, btnLogout}) {
            btn.setBackground(MainFrame.AMARELO_BOTAO);
            btn.setForeground(Color.DARK_GRAY);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2));
            btn.setFont(new Font("Arial", Font.BOLD, 13));
        }

        btnPublicar.addActionListener(e -> publicarFoto());
        btnExplorar.addActionListener(e -> explorarUsuarios());
        btnMensagens.addActionListener(e -> mensagensUsuarios());
        btnPerfil.addActionListener(e -> mostrarPerfil());
        btnLogout.addActionListener(e -> {
            sistema.logout();
            mainFrame.mostrarLogin();
        });

        menu.add(MainFrame.criarLogo(132, 132));
        menu.add(new JLabel(" MENU ", SwingConstants.CENTER));
        menu.add(btnPublicar);
        menu.add(btnExplorar);
        menu.add(btnMensagens);
        menu.add(btnPerfil);
        menu.add(btnLogout);

        add(menu, BorderLayout.WEST);
    }

    private void criarAreaFeed() {
        feedArea = new JPanel();
        feedArea.setLayout(new BoxLayout(feedArea, BoxLayout.Y_AXIS));
        feedArea.setBackground(MainFrame.AMARELO_CLARO);
        JScrollPane scrollPane = new JScrollPane(feedArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void carregarPublicacoes() {
        feedArea.removeAll();
        try {
            List<Foto> publicacoes = sistema.listarPublicacoesDoFeed();
            if (publicacoes.isEmpty()) {
                JLabel vazio = new JLabel("Nenhuma publicação sua ou de quem você segue.");
                vazio.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                feedArea.add(vazio);
            } else {
                for (Foto foto : publicacoes) {
                feedArea.add(criarCardPublicacao(foto));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Não foi possível carregar as publicações.\n" + e.getMessage(),
                "Erro ao carregar feed", JOptionPane.ERROR_MESSAGE);
        }

        revalidate();
        repaint();
    }

    private JPanel criarCardPublicacao(Foto foto) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2),
            foto.getAutor().getNome()
        ));
        card.setBackground(MainFrame.AMARELO_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 2),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 1),
                foto.getAutor().getNome()
            )
        ));

        JLabel imagemLabel = criarImagemPublicacao(foto.getCaminhoArquivo());
        imagemLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel imagemPanel = new JPanel(new BorderLayout());
        imagemPanel.setBackground(MainFrame.AMARELO_PALE);
        imagemPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        imagemPanel.add(imagemLabel, BorderLayout.CENTER);

        JLabel legendaLabel = new JLabel("<html><b>" + foto.getLegenda() + "</b></html>");
        legendaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel curtidasLabel = new JLabel("Curtidas: " + foto.getCurtidas().size());
        JLabel compartilhamentosLabel = new JLabel("Compartilhamentos: " + foto.getCompartilhamentos().size());

        Usuario atual = sistema.getUsuarioLogado();
        boolean jaCurtiu = foto.getCurtidas().stream().anyMatch(usuario -> usuario.getId() == atual.getId());
        boolean jaCompartilhou = foto.getCompartilhamentos().stream()
            .anyMatch(usuario -> usuario.getId() == atual.getId());
        JButton btnCurtir = new JButton(jaCurtiu ? "Descurtir" : "Curtir");
        JButton btnCompartilhar = new JButton(jaCompartilhou ? "Remover compartilhamento" : "Compartilhar");
        JButton btnComentarios = new JButton("Comentários");

        for (JButton btn : new JButton[]{btnCurtir, btnCompartilhar, btnComentarios}) {
            btn.setBackground(MainFrame.AMARELO_BOTAO);
            btn.setForeground(Color.DARK_GRAY);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 1));
            btn.setFont(new Font("Arial", Font.BOLD, 12));
        }

        btnCurtir.addActionListener(e -> {
            if (sistema.curtirFoto(foto)) {
                curtidasLabel.setText("Curtidas: " + foto.getCurtidas().size());
                boolean curtiu = foto.getCurtidas().stream()
                    .anyMatch(usuario -> usuario.getId() == atual.getId());
                btnCurtir.setText(curtiu ? "Descurtir" : "Curtir");
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível atualizar a curtida.");
            }
        });

        btnCompartilhar.addActionListener(e -> {
            if (sistema.compartilharFoto(foto)) {
                compartilhamentosLabel.setText("Compartilhamentos: " + foto.getCompartilhamentos().size());
                boolean compartilhou = foto.getCompartilhamentos().stream()
                    .anyMatch(usuario -> usuario.getId() == atual.getId());
                btnCompartilhar.setText(compartilhou ? "Remover compartilhamento" : "Compartilhar");
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível atualizar o compartilhamento.");
            }
        });

        btnComentarios.addActionListener(e -> mostrarComentarios(foto));

        JPanel botoes = new JPanel();
        botoes.setBackground(MainFrame.AMARELO_CARD);
        botoes.add(btnCurtir);
        botoes.add(btnCompartilhar);
        botoes.add(btnComentarios);

        JPanel centro = new JPanel();
        centro.setBackground(MainFrame.AMARELO_CARD);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(imagemPanel);
        centro.add(Box.createVerticalStrut(8));
        centro.add(legendaLabel);
        if (!foto.getHashtags().isEmpty()) {
            JLabel hashtagsLabel = new JLabel("#" + String.join("  #", foto.getHashtags()));
            hashtagsLabel.setForeground(new Color(143, 83, 0));
            centro.add(hashtagsLabel);
        }
        centro.add(Box.createVerticalStrut(8));
        centro.add(curtidasLabel);
        centro.add(compartilhamentosLabel);

        card.add(centro, BorderLayout.CENTER);
        card.add(botoes, BorderLayout.SOUTH);

        return card;
    }

    private JLabel criarImagemPublicacao(String caminho) {
        if (caminho == null || caminho.trim().isEmpty()) {
            return new JLabel("[Caminho da imagem vazio]");
        }
        try {
            String valor = caminho.trim();
            Image imagem;
            if (valor.startsWith("http://") || valor.startsWith("https://")) {
                imagem = ImageIO.read(new URL(valor));
            } else {
                File arquivo = new File(valor);
                if (!arquivo.isAbsolute()) {
                    arquivo = arquivo.getAbsoluteFile();
                }
                imagem = ImageIO.read(arquivo);
            }
            if (imagem == null) {
                return new JLabel("[O arquivo não é uma imagem compatível]");
            }
            return new JLabel(new ImageIcon(imagem.getScaledInstance(200, 200, Image.SCALE_SMOOTH)));
        } catch (Exception ex) {
            JLabel erro = new JLabel("[Imagem não encontrada ou inacessível]");
            erro.setToolTipText(caminho);
            return erro;
        }
    }

    private void mostrarComentarios(Foto foto) {
        try {
            sistema.atualizarComentarios(foto);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível carregar comentários:\n" + ex.getMessage());
            return;
        }
        JPanel painelComentarios = new JPanel();
        painelComentarios.setLayout(new BorderLayout());
        painelComentarios.setPreferredSize(new Dimension(350, 250));

        JTextArea areaComentarios = new JTextArea();
        areaComentarios.setEditable(false);
        areaComentarios.setBackground(MainFrame.AMARELO_PALE);
        areaComentarios.setForeground(Color.DARK_GRAY);

        StringBuilder comentariosTexto = new StringBuilder();
        List<dados.Comentario> comentarios = foto.getComentarios();
        if (comentarios.isEmpty()) {
            comentariosTexto.append("Seja o primeiro a comentar!");
        } else {
            for (dados.Comentario c : comentarios) {
                comentariosTexto.append(c.getAutor()).append(": ").append(c.getMensagem()).append("\n");
            }
        }
        areaComentarios.setText(comentariosTexto.toString());

        JScrollPane scrollComentarios = new JScrollPane(areaComentarios);
        painelComentarios.add(scrollComentarios, BorderLayout.CENTER);

        JPanel painelNovoComentario = new JPanel(new BorderLayout());
        JTextField campoNovoComentario = new JTextField();
        JButton btnEnviarComentario = new JButton("Comentar");
          btnEnviarComentario.setBackground(MainFrame.AMARELO_BOTAO);
        btnEnviarComentario.setForeground(Color.DARK_GRAY);
        btnEnviarComentario.setFocusPainted(false);
        btnEnviarComentario.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 1));
        btnEnviarComentario.setFont(new Font("Arial", Font.BOLD, 12));

        painelNovoComentario.add(campoNovoComentario, BorderLayout.CENTER);
        painelNovoComentario.add(btnEnviarComentario, BorderLayout.EAST);

        painelComentarios.add(painelNovoComentario, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(mainFrame, "Comentários", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(painelComentarios);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        btnEnviarComentario.addActionListener(e -> {
            String texto = campoNovoComentario.getText().trim();
            if (!texto.isEmpty()) {
                if (!sistema.comentarFoto(foto, texto)) {
                    JOptionPane.showMessageDialog(dialog, "Não foi possível salvar o comentário.");
                    return;
                }
                try {
                    sistema.atualizarComentarios(foto);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "O comentário foi enviado, mas não foi possível atualizar a lista.");
                    return;
                }
                if (areaComentarios.getText().equals("Seja o primeiro a comentar!")) {
                    areaComentarios.setText("");
                }
                StringBuilder comentariosAtualizados = new StringBuilder();
                for (dados.Comentario comentario : foto.getComentarios()) {
                    comentariosAtualizados.append(comentario.getAutor()).append(": ")
                        .append(comentario.getMensagem()).append("\n");
                }
                areaComentarios.setText(comentariosAtualizados.toString());
                campoNovoComentario.setText("");
            }
        });

        dialog.setVisible(true);
    }


    private void publicarFoto() {
        JTextField legendaField = new JTextField();
        JTextField hashtagsField = new JTextField();
        JTextField caminhoField = new JTextField();
        JButton escolherArquivo = new JButton("Selecionar arquivo...");
        escolherArquivo.addActionListener(event -> {
            JFileChooser seletor = new JFileChooser();
            seletor.setDialogTitle("Selecionar imagem para publicação");
            int escolha = seletor.showOpenDialog(this);
            if (escolha == JFileChooser.APPROVE_OPTION) {
                caminhoField.setText(seletor.getSelectedFile().getAbsolutePath());
            }
        });

        JPanel campoCaminho = new JPanel(new BorderLayout(6, 0));
        campoCaminho.add(caminhoField, BorderLayout.CENTER);
        campoCaminho.add(escolherArquivo, BorderLayout.EAST);

        Object[] fields = {
            "Legenda:", legendaField,
            "Hashtags (ex.: #viagem #praia):", hashtagsField,
            "Imagem (caminho local ou URL):", campoCaminho
        };

        int result = JOptionPane.showConfirmDialog(null, fields, "Nova Publicação", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String legenda = legendaField.getText();
            String caminho = caminhoField.getText().trim();

            if (!legenda.isEmpty() && !caminho.isEmpty()) {
                if (!caminho.startsWith("http://") && !caminho.startsWith("https://")) {
                    File arquivo = new File(caminho);
                    if (!arquivo.isAbsolute()) {
                        arquivo = arquivo.getAbsoluteFile();
                    }
                    try {
                        if (!arquivo.isFile() || ImageIO.read(arquivo) == null) {
                            JOptionPane.showMessageDialog(this,
                                "O caminho não aponta para um arquivo de imagem legível:\n" + arquivo.getAbsolutePath());
                            return;
                        }
                        caminho = arquivo.getAbsolutePath();
                    } catch (java.io.IOException ex) {
                        JOptionPane.showMessageDialog(this, "Não foi possível ler a imagem: " + ex.getMessage());
                        return;
                    }
                }
                boolean sucesso = sistema.publicarFoto(caminho, legenda, hashtagsField.getText());
                if (sucesso) {
                    JOptionPane.showMessageDialog(null, "Foto publicada!");
                    carregarPublicacoes();
                } else {
                    JOptionPane.showMessageDialog(null, "Erro ao publicar.");
                }
            }
        }
    }

    private void explorarUsuarios() {
        Usuario atual = sistema.getUsuarioLogado();

        while (true) {
            List<Usuario> usuarios;
            try {
                usuarios = sistema.listarUsuariosAtualizados();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Não foi possível atualizar a lista de usuários.\n" + ex.getMessage(),
                    "Erro ao pesquisar usuários", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String termo = JOptionPane.showInputDialog(null, "Pesquisar usuário por nome:", "Explorar usuários da Omeletty", JOptionPane.QUESTION_MESSAGE);
            if (termo == null) return;

            List<Usuario> encontrados = new ArrayList<>();
            for (Usuario u : usuarios) {
                if (!u.equals(atual) && u.getNome().toLowerCase().contains(termo.toLowerCase())) {
                    encontrados.add(u);
                }
            }

            if (encontrados.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nenhum usuário encontrado.");
                continue;
            }

            String[] nomes = encontrados.stream()
                    .map(u -> u.getNome() + " (" + u.getEmail() + ")")
                    .toArray(String[]::new);

            String escolha = (String) JOptionPane.showInputDialog(
                    null,
                    "Selecione um usuário:",
                    "Resultados da pesquisa",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    nomes,
                    nomes[0]
            );

            if (escolha == null) return;

            Usuario selecionado = encontrados.get(Arrays.asList(nomes).indexOf(escolha));
            try {
                mostrarPerfilDeOutroUsuario(atual, selecionado);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Não foi possível carregar o perfil.\n" + ex.getMessage(),
                    "Erro ao carregar perfil", JOptionPane.ERROR_MESSAGE);
            }

            int op = JOptionPane.showConfirmDialog(null, "Deseja voltar para a pesquisa?", "Explorar", JOptionPane.YES_NO_OPTION);
            if (op != JOptionPane.YES_OPTION) break;
        }
    }

    private void mostrarPerfilDeOutroUsuario(Usuario atual, Usuario usuario) throws java.sql.SQLException {
        sistema.carregarDadosPerfil(usuario);
        sistema.carregarDadosPerfil(atual);

        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(MainFrame.AMARELO_CLARO);

        if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
            try {
                ImageIcon icon;
                String caminho = usuario.getFotoPerfil();
                if (caminho.startsWith("http://") || caminho.startsWith("https://")) {
                    java.net.URL url = new java.net.URL(caminho);
                    icon = new ImageIcon(url);
                } else {
                    java.io.File file = new java.io.File(caminho);
                    if (file.exists()) {
                        icon = new ImageIcon(file.getAbsolutePath());
                    } else {
                        icon = new ImageIcon(caminho);
                    }
                }
                Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                JLabel fotoPerfilLabel = new JLabel(new ImageIcon(img));
                fotoPerfilLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                painel.add(fotoPerfilLabel);
            } catch (Exception e) {
                painel.add(new JLabel("[Foto de perfil não encontrada]"));
            }
        }

        StringBuilder info = new StringBuilder();
        info.append("<html>");
        info.append("<h2>").append(usuario.getNome()).append("</h2>");
        info.append("<b>Email:</b> ").append(usuario.getEmail()).append("<br>");
        info.append("<b>Biografia:</b> ").append(usuario.getBiografia() != null ? usuario.getBiografia() : "Nenhuma").append("<br>");
        info.append("<b>Seguidores:</b> ").append(usuario.getSeguidores().size()).append("<br>");
        info.append("<b>Seguindo:</b> ").append(usuario.getSeguindo().size()).append("<br>");
        info.append("</html>");

        JLabel label = new JLabel(info.toString());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(label);

        painel.add(Box.createVerticalStrut(10));
        JLabel pubLabel = new JLabel("<html><b>Publicações:</b></html>");
        pubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(pubLabel);

        if (usuario.getPublicacoes().isEmpty()) {
            painel.add(new JLabel("Nenhuma publicação ainda."));
        } else {
            JPanel pubPanel = new JPanel();
            pubPanel.setLayout(new BoxLayout(pubPanel, BoxLayout.Y_AXIS));
            pubPanel.setBackground(MainFrame.AMARELO_CLARO);

            for (dados.Foto f : usuario.getPublicacoes()) {
                JPanel fotoPanel = new JPanel();
                fotoPanel.setLayout(new BoxLayout(fotoPanel, BoxLayout.Y_AXIS));
                fotoPanel.setBackground(MainFrame.AMARELO_CLARO);

                try {
                    ImageIcon icon;
                    String caminho = f.getCaminhoArquivo();
                    if (caminho.startsWith("http://") || caminho.startsWith("https://")) {
                        java.net.URL url = new java.net.URL(caminho);
                        icon = new ImageIcon(url);
                    } else {
                        java.io.File file = new java.io.File(caminho);
                        if (file.exists()) {
                            icon = new ImageIcon(file.getAbsolutePath());
                        } else {
                            icon = new ImageIcon(caminho);
                        }
                    }
                    Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    JLabel fotoLabel = new JLabel(new ImageIcon(img));
                    fotoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    fotoPanel.add(fotoLabel);
                } catch (Exception e) {
                    fotoPanel.add(new JLabel("[Imagem não encontrada]"));
                }

                JLabel legenda = new JLabel("<html><i>" + f.getLegenda() + "</i></html>");
                legenda.setAlignmentX(Component.CENTER_ALIGNMENT);
                fotoPanel.add(legenda);
                adicionarResumoInteracoes(fotoPanel, f);

                fotoPanel.add(Box.createVerticalStrut(10));
                pubPanel.add(fotoPanel);
            }
            JScrollPane scroll = new JScrollPane(pubPanel);
            scroll.setPreferredSize(new Dimension(250, 180));
            painel.add(scroll);
        }

        boolean seguindo = sistema.estaSeguindo(usuario);
        String acao = seguindo ? "Deixar de seguir" : "Seguir";
        Object[] opcoes = {acao, "Voltar"};

        int op = JOptionPane.showOptionDialog(
                null,
                painel,
                "Perfil de " + usuario.getNome(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                opcoes,
                opcoes[1]
        );

        if (op == 0) {
            if (seguindo) {
                if (sistema.deixarDeSeguirUsuario(usuario)) {
                    JOptionPane.showMessageDialog(null, "Você deixou de seguir " + usuario.getNome() + "!");
                }
            } else {
                if (sistema.seguirUsuario(usuario)) {
                    JOptionPane.showMessageDialog(null, "Agora você segue " + usuario.getNome() + "!");
                }
            }
            mostrarPerfilDeOutroUsuario(atual, usuario);
        }
    }

    private void mensagensUsuarios() {
        Usuario atual = sistema.getUsuarioLogado();
        List<Usuario> usuarios = sistema.getUsuarios();
        DefaultListModel<String> model = new DefaultListModel<>();

        for (Usuario u : usuarios) {
            if (!u.equals(atual)) {
                model.addElement(u.getNome() + " (" + u.getEmail() + ")");
            }
        }

        JList<String> lista = new JList<>(model);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int result = JOptionPane.showConfirmDialog(
            null,
            new JScrollPane(lista),
            "Mensagens - Escolha um usuário",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION && lista.getSelectedIndex() != -1) {
            Usuario selecionado = null;
            int idx = 0;
            for (Usuario u : usuarios) {
                if (!u.equals(atual)) {
                    if (idx == lista.getSelectedIndex()) {
                        selecionado = u;
                        break;
                    }
                    idx++;
                }
            }
            if (selecionado != null) {
                List<dados.Mensagem> conversas = sistema.visualizarMensagens(selecionado.getEmail());
                StringBuilder historico = new StringBuilder();
                if (conversas != null && !conversas.isEmpty()) {
                    for (dados.Mensagem m : conversas) {
                        historico.append(m.getRemetente().getNome())
                                 .append(": ")
                                 .append(m.getConteudo())
                                 .append("\n");
                    }
                } else {
                    historico.append("Nenhuma mensagem ainda.\n");
                }

                JTextArea area = new JTextArea(historico.toString(), 10, 30);
                area.setEditable(false);
                JTextField novaMsg = new JTextField();

                Object[] fields = {
                    new JLabel("Histórico:"), new JScrollPane(area),
                    new JLabel("Nova mensagem:"), novaMsg
                };

                int enviar = JOptionPane.showConfirmDialog(
                    null, fields, "Conversando com " + selecionado.getNome(), JOptionPane.OK_CANCEL_OPTION
                );

                if (enviar == JOptionPane.OK_OPTION) {
                    String conteudo = novaMsg.getText();
                    if (!conteudo.isEmpty()) {
                        sistema.enviarMensagem(selecionado.getEmail(), conteudo);
                        JOptionPane.showMessageDialog(null, "Mensagem enviada!");
                    }
                }
            }
        }
    }

    private void mostrarPerfil() {
        Usuario atual = sistema.getUsuarioLogado();

        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(MainFrame.AMARELO_CLARO);

        if (atual.getFotoPerfil() != null && !atual.getFotoPerfil().isEmpty()) {
            try {
                ImageIcon icon;
                String caminho = atual.getFotoPerfil();
                if (caminho.startsWith("http://") || caminho.startsWith("https://")) {
                    java.net.URL url = new java.net.URL(caminho);
                    icon = new ImageIcon(url);
                } else {
                    java.io.File file = new java.io.File(caminho);
                    if (file.exists()) {
                        icon = new ImageIcon(file.getAbsolutePath());
                    } else {
                        icon = new ImageIcon(caminho);
                    }
                }
                Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                JLabel fotoPerfilLabel = new JLabel(new ImageIcon(img));
                fotoPerfilLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                painel.add(fotoPerfilLabel);
            } catch (Exception e) {
                painel.add(new JLabel("[Foto de perfil não encontrada]"));
            }
        }

        // Informações do usuário
        StringBuilder info = new StringBuilder();
        info.append("<html>");
        info.append("<h2>Perfil de ").append(escaparHtml(atual.getNome())).append("</h2>");
        info.append("<b>Email:</b> ").append(escaparHtml(atual.getEmail())).append("<br>");
        info.append("<b>Biografia:</b> ").append(escaparHtml(atual.getBiografia() != null ? atual.getBiografia() : "Nenhuma")).append("<br>");
        info.append("<b>Seguidores:</b> ").append(atual.getSeguidores().size()).append("<br>");
        info.append("<b>Seguindo:</b> ").append(atual.getSeguindo().size()).append("<br>");
        info.append("</html>");

        JLabel label = new JLabel(info.toString());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(label);

        painel.add(Box.createVerticalStrut(10));
        JLabel pubLabel = new JLabel("<html><b>Publicações:</b></html>");
        pubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(pubLabel);

        if (atual.getPublicacoes().isEmpty()) {
            painel.add(new JLabel("Nenhuma publicação ainda."));
        } else {
            JPanel pubPanel = new JPanel();
            pubPanel.setLayout(new BoxLayout(pubPanel, BoxLayout.Y_AXIS));
            pubPanel.setBackground(MainFrame.AMARELO_CLARO);

            for (dados.Foto f : atual.getPublicacoes()) {
                JPanel fotoPanel = new JPanel();
                fotoPanel.setLayout(new BoxLayout(fotoPanel, BoxLayout.Y_AXIS));
                fotoPanel.setBackground(MainFrame.AMARELO_CLARO);

                try {
                    ImageIcon icon;
                    String caminho = f.getCaminhoArquivo();
                    if (caminho.startsWith("http://") || caminho.startsWith("https://")) {
                        java.net.URL url = new java.net.URL(caminho);
                        icon = new ImageIcon(url);
                    } else {
                        java.io.File file = new java.io.File(caminho);
                        if (file.exists()) {
                            icon = new ImageIcon(file.getAbsolutePath());
                        } else {
                            icon = new ImageIcon(caminho);
                        }
                    }
                    Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    JLabel fotoLabel = new JLabel(new ImageIcon(img));
                    fotoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    fotoPanel.add(fotoLabel);
                } catch (Exception e) {
                    fotoPanel.add(new JLabel("[Imagem não encontrada]"));
                }

                JLabel legenda = new JLabel("<html><i>" + f.getLegenda() + "</i></html>");
                legenda.setAlignmentX(Component.CENTER_ALIGNMENT);
                fotoPanel.add(legenda);
                adicionarResumoInteracoes(fotoPanel, f);

                fotoPanel.add(Box.createVerticalStrut(10));
                pubPanel.add(fotoPanel);
            }
            JScrollPane scroll = new JScrollPane(pubPanel);
            scroll.setPreferredSize(new Dimension(250, 180));
            painel.add(scroll);
        }

        int op = JOptionPane.showOptionDialog(
            null,
            painel,
            "Meu Perfil - Omellety",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[]{"Editar Conta", "Excluir Conta", "Fechar"},
            "Fechar"
        );

        if (op == 0) {
            JTextField nomeField = new JTextField(atual.getNome());
            JTextField emailField = new JTextField(atual.getEmail());
            JTextField bioField = new JTextField(atual.getBiografia() != null ? atual.getBiografia() : "");
            JTextField fotoField = new JTextField(atual.getFotoPerfil() != null ? atual.getFotoPerfil() : "");
            JButton escolherFotoPerfil = new JButton("Selecionar arquivo...");
            escolherFotoPerfil.addActionListener(event -> {
                JFileChooser seletor = new JFileChooser();
                seletor.setDialogTitle("Selecionar foto de perfil");
                if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    fotoField.setText(seletor.getSelectedFile().getAbsolutePath());
                }
            });
            JPanel campoFotoPerfil = new JPanel(new BorderLayout(6, 0));
            campoFotoPerfil.add(fotoField, BorderLayout.CENTER);
            campoFotoPerfil.add(escolherFotoPerfil, BorderLayout.EAST);
            JPasswordField senhaAtualField = new JPasswordField();
            JPasswordField novaSenhaField = new JPasswordField();

            Object[] fields = {
                "Nome:", nomeField,
                "E-mail:", emailField,
                "Biografia:", bioField,
                "Foto de perfil:", campoFotoPerfil,
                "Senha atual (obrigatória para trocar senha):", senhaAtualField,
                "Nova senha (opcional):", novaSenhaField
            };

            int res = JOptionPane.showConfirmDialog(null, fields, "Editar Conta", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                String caminhoFoto = fotoField.getText().trim();
                if (!caminhoFoto.isEmpty() && !caminhoFoto.startsWith("http://") && !caminhoFoto.startsWith("https://")) {
                    try {
                        File arquivo = new File(caminhoFoto).getAbsoluteFile();
                        if (!arquivo.isFile() || ImageIO.read(arquivo) == null) {
                            JOptionPane.showMessageDialog(this, "Selecione um arquivo de imagem válido para a foto de perfil.");
                            return;
                        }
                        caminhoFoto = arquivo.getAbsolutePath();
                    } catch (java.io.IOException ex) {
                        JOptionPane.showMessageDialog(this, "Não foi possível ler a foto de perfil: " + ex.getMessage());
                        return;
                    }
                }
                String erro = negocios.ValidacaoConta.validar(nomeField.getText(),
                    emailField.getText(), bioField.getText(), caminhoFoto,
                    new String(novaSenhaField.getPassword()), false);
                if (erro != null) {
                    JOptionPane.showMessageDialog(this, erro);
                    return;
                }
                boolean atualizado = sistema.editarConta(nomeField.getText(),
                    emailField.getText(), bioField.getText(), caminhoFoto,
                    new String(senhaAtualField.getPassword()), new String(novaSenhaField.getPassword()));
                JOptionPane.showMessageDialog(this, atualizado
                    ? "Conta atualizada com sucesso."
                    : "Não foi possível atualizar. Verifique a senha atual e se usuário/e-mail estão disponíveis.");
            }
        } else if (op == 1) {
            JPasswordField senhaField = new JPasswordField();
            int confirmar = JOptionPane.showConfirmDialog(this, new Object[]{
                "A exclusão da conta não pode ser desfeita.", "Digite sua senha para confirmar:", senhaField
            }, "Excluir Conta", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmar == JOptionPane.OK_OPTION) {
                if (sistema.excluirConta(new String(senhaField.getPassword()))) {
                    JOptionPane.showMessageDialog(this, "Conta excluída.");
                    mainFrame.mostrarLogin();
                } else {
                    JOptionPane.showMessageDialog(this, "Não foi possível excluir a conta. Verifique sua senha.");
                }
            }
        }
    }

    private String escaparHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void adicionarResumoInteracoes(JPanel painel, dados.Foto foto) {
        JLabel resumo = new JLabel("Curtidas: " + foto.getCurtidas().size()
            + " | Comentários: " + foto.getComentarios().size()
            + " | Compartilhamentos: " + foto.getCompartilhamentos().size());
        resumo.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(resumo);
        if (!foto.getHashtags().isEmpty()) {
            JLabel hashtags = new JLabel("#" + String.join("  #", foto.getHashtags()));
            hashtags.setForeground(new Color(143, 83, 0));
            hashtags.setAlignmentX(Component.CENTER_ALIGNMENT);
            painel.add(hashtags);
        }
    }
}    