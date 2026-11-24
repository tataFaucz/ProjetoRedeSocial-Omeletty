package ui;

import dao.GestaoDAO;
import dao.GestaoDAO.ColumnInfo;
import dao.GestaoDAO.QueryResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GestaoPanel extends JPanel {
    private static final Color BACKGROUND = MainFrame.AMARELO_CLARO;
    private static final Color INK = new Color(74, 49, 0);
    private static final Color GREEN = new Color(143, 83, 0);
    private static final String[] ENTIDADES = {"usuarios", "fotos", "comentarios", "mensagens", "hashtags"};

    private final GestaoDAO dao = new GestaoDAO();
    private final MainFrame mainFrame;

    public GestaoPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 12));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Gestão de dados", SwingConstants.LEFT);
        title.setForeground(INK);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Operações do banco de dados e relatórios", SwingConstants.LEFT);
        subtitle.setForeground(new Color(100, 111, 104));
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 3));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(subtitle);
        JPanel brandActions = new JPanel(new GridLayout(2, 1, 0, 3));
        brandActions.setOpaque(false);
        brandActions.add(MainFrame.criarLogo(96, 96));
        JButton home = new JButton("Voltar ao início");
        home.setBackground(MainFrame.AMARELO_BOTAO);
        home.setForeground(INK);
        home.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA, 1));
        home.addActionListener(event -> mainFrame.mostrarInicio());
        brandActions.add(home);
        header.add(titles, BorderLayout.WEST);
        header.add(brandActions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setBackground(MainFrame.AMARELO_CLARO);
        tabs.setForeground(INK);
        tabs.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA));
        for (String entidade : ENTIDADES) {
            tabs.addTab(nomeEntidade(entidade), new EntityPanel(entidade));
        }
        tabs.addTab("Processos", new ProcessPanel());
        tabs.addTab("Relatórios", new ReportPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private String nomeEntidade(String tabela) {
        switch (tabela) {
            case "usuarios": return "Usuários";
            case "fotos": return "Publicações";
            case "comentarios": return "Comentários";
            case "mensagens": return "Mensagens";
            case "hashtags": return "Hashtags";
            default: return tabela;
        }
    }

    private void mostrarErro(Exception exception) {
        exception.printStackTrace();
        String message = exception.getMessage() == null ? exception.toString() : exception.getMessage();
        JOptionPane.showMessageDialog(this, message, "Operação não concluída", JOptionPane.ERROR_MESSAGE);
    }

    private JTable criarTabela() {
        JTable table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setBackground(MainFrame.AMARELO_PALE);
        table.setGridColor(new Color(235, 199, 96));
        table.setSelectionBackground(MainFrame.AMARELO_DOURADO);
        table.setSelectionForeground(INK);
        table.getTableHeader().setBackground(MainFrame.AMARELO_MEDIO);
        table.getTableHeader().setForeground(INK);
        return table;
    }

    private void preencherTabela(JTable table, QueryResult result) {
        DefaultTableModel model = new DefaultTableModel(result.getRows(), result.getColumns()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
    }

    private final class EntityPanel extends JPanel {
        private final String tableName;
        private final JTable table = criarTabela();
        private final JPanel fieldsPanel = new JPanel(new GridLayout(0, 2, 10, 7));
        private final Map<String, JTextField> inputs = new LinkedHashMap<>();
        private List<ColumnInfo> columns = new ArrayList<>();
        private boolean editing;

        EntityPanel(String tableName) {
            this.tableName = tableName;
            setLayout(new BorderLayout(0, 10));
            setBackground(BACKGROUND);
            JPanel editor = new JPanel(new BorderLayout(0, 8));
            editor.setBackground(MainFrame.AMARELO_CARD);
            editor.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA), "Dados da entidade"));
            fieldsPanel.setBackground(MainFrame.AMARELO_CARD);
            editor.add(fieldsPanel, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
            actions.setBackground(MainFrame.AMARELO_CARD);
            JButton newButton = new JButton("Novo");
            JButton insertButton = new JButton("Cadastrar");
            JButton updateButton = new JButton("Atualizar selecionado");
            JButton deleteButton = new JButton("Excluir selecionado");
            JButton refreshButton = new JButton("Atualizar lista");
            for (JButton button : new JButton[]{newButton, insertButton, updateButton, deleteButton, refreshButton}) {
                button.setBackground(MainFrame.AMARELO_BOTAO);
                button.setForeground(INK);
                button.setFocusPainted(false);
                button.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA));
                actions.add(button);
            }
            editor.add(actions, BorderLayout.SOUTH);

            add(editor, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                    carregarSelecionado();
                }
            });
            newButton.addActionListener(event -> limparFormulario());
            insertButton.addActionListener(event -> executarInsercao());
            updateButton.addActionListener(event -> executarAtualizacao());
            deleteButton.addActionListener(event -> executarExclusao());
            refreshButton.addActionListener(event -> atualizarLista());
            inicializar();
        }

        private void inicializar() {
            try {
                columns = dao.listarColunas(tableName);
                fieldsPanel.removeAll();
                inputs.clear();
                for (ColumnInfo column : columns) {
                    fieldsPanel.add(new JLabel(rotulo(column.getName(), column)));
                    JTextField input = new JTextField(18);
                    input.setBackground(MainFrame.AMARELO_PALE);
                    input.setForeground(INK);
                    input.setToolTipText("Tipo no banco: " + column.getTypeName() +
                        (column.isNullable() ? " (opcional)" : " (obrigatório)"));
                    input.setEditable(!column.isAutoIncrement());
                    inputs.put(column.getName(), input);
                    fieldsPanel.add(input);
                }
                fieldsPanel.revalidate();
                fieldsPanel.repaint();
                atualizarLista();
                limparFormulario();
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        }

        private String rotulo(String columnName, ColumnInfo column) {
            String result = columnName.replace('_', ' ');
            if (column.isPrimaryKey()) {
                result += " (chave)";
            }
            if (column.getDefaultValue() != null) {
                result += " (opcional, padrão do banco)";
            } else if (column.isNullable()) {
                result += " (opcional)";
            }
            return result;
        }

        private Map<String, String> valores() {
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, JTextField> entry : inputs.entrySet()) {
                values.put(entry.getKey(), entry.getValue().getText());
            }
            return values;
        }

        private void executarInsercao() {
            try {
                dao.inserir(tableName, valores());
                atualizarLista();
                limparFormulario();
                JOptionPane.showMessageDialog(this, "Registro cadastrado.");
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        }

        private void executarAtualizacao() {
            if (!editing) {
                JOptionPane.showMessageDialog(this, "Selecione uma linha para atualizar.");
                return;
            }
            try {
                dao.atualizar(tableName, valores());
                atualizarLista();
                limparFormulario();
                JOptionPane.showMessageDialog(this, "Registro atualizado.");
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        }

        private void executarExclusao() {
            if (!editing) {
                JOptionPane.showMessageDialog(this, "Selecione uma linha para excluir.");
                return;
            }
            int answer = JOptionPane.showConfirmDialog(
                this,
                "Excluir o registro selecionado? Relações dependentes podem ser removidas em cascata.",
                "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                dao.excluir(tableName, valores());
                atualizarLista();
                limparFormulario();
                JOptionPane.showMessageDialog(this, "Registro excluído.");
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        }

        private void carregarSelecionado() {
            int viewRow = table.getSelectedRow();
            int row = table.convertRowIndexToModel(viewRow);
            for (ColumnInfo column : columns) {
                JTextField input = inputs.get(column.getName());
                if (input == null) {
                    continue;
                }
                int modelColumn = table.getColumnModel().getColumnIndex(column.getName());
                Object value = table.getModel().getValueAt(row, table.convertColumnIndexToModel(modelColumn));
                input.setText(value == null ? "" : value.toString());
                input.setEditable(!column.isPrimaryKey());
            }
            editing = true;
        }

        private void limparFormulario() {
            table.clearSelection();
            for (Map.Entry<String, JTextField> entry : inputs.entrySet()) {
                entry.getValue().setText("");
                ColumnInfo column = columns.stream()
                    .filter(candidate -> candidate.getName().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);
                entry.getValue().setEditable(column == null || !column.isAutoIncrement());
            }
            editing = false;
        }

        private void atualizarLista() {
            try {
                preencherTabela(table, dao.listar(tableName));
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        }
    }

    private final class ProcessPanel extends JPanel {
        ProcessPanel() {
            setLayout(new GridLayout(0, 2, 14, 14));
            setBackground(BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
            add(criarProcesso("Seguir usuário", "id_seguidor", "id_seguido", "seguir"));
            add(criarProcesso("Curtir publicação", "id_usuario", "id_foto", "curtir"));
            add(criarProcesso("Compartilhar publicação", "id_usuario", "id_foto", "compartilhar"));
            add(criarProcesso("Associar hashtag à publicação", "id_foto", "id_hashtag", "hashtag"));
        }

        private JPanel criarProcesso(String title, String firstLabel, String secondLabel, String type) {
            JPanel panel = new JPanel(new BorderLayout(8, 10));
            panel.setBackground(MainFrame.AMARELO_PALE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
            ));
            JLabel heading = new JLabel(title);
            heading.setFont(new Font("SansSerif", Font.BOLD, 16));
            heading.setForeground(GREEN);
            panel.add(heading, BorderLayout.NORTH);

            JPanel fields = new JPanel(new GridLayout(2, 2, 8, 8));
            fields.setOpaque(false);
            JTextField first = new JTextField();
            JTextField second = new JTextField();
            first.setBackground(MainFrame.AMARELO_PALE);
            second.setBackground(MainFrame.AMARELO_PALE);
            fields.add(new JLabel(firstLabel));
            fields.add(first);
            fields.add(new JLabel(secondLabel));
            fields.add(second);
            panel.add(fields, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttons.setOpaque(false);
            JButton add = new JButton("Executar");
            JButton remove = new JButton("Remover relação");
            for (JButton button : new JButton[]{add, remove}) {
                button.setBackground(MainFrame.AMARELO_BOTAO);
                button.setForeground(INK);
                button.setFocusPainted(false);
                button.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA));
            }
            buttons.add(add);
            buttons.add(remove);
            panel.add(buttons, BorderLayout.SOUTH);
            add.addActionListener(event -> alterarAssociacao(type, first, second, true));
            remove.addActionListener(event -> alterarAssociacao(type, first, second, false));
            return panel;
        }

        private void alterarAssociacao(String type, JTextField first, JTextField second, boolean adding) {
            try {
                int firstId = Integer.parseInt(first.getText().trim());
                int secondId = Integer.parseInt(second.getText().trim());
                if (firstId <= 0 || secondId <= 0) {
                    throw new NumberFormatException("Os identificadores devem ser positivos.");
                }
                dao.executarAssociacao(type, firstId, secondId, adding);
                JOptionPane.showMessageDialog(this, adding ? "Processo registrado." : "Relação removida.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Informe IDs numéricos positivos para os dois registros.");
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        }
    }

    private final class ReportPanel extends JPanel {
        private final JComboBox<String> reportSelector = new JComboBox<>();
        private final JTable results = criarTabela();

        ReportPanel() {
            setLayout(new BorderLayout(8, 10));
            setBackground(BACKGROUND);
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            toolbar.setBackground(BACKGROUND);
            toolbar.add(new JLabel("Relatório:"));
            for (String report : dao.listarRelatorios()) {
                reportSelector.addItem(report);
            }
            toolbar.add(reportSelector);
            JButton run = new JButton("Executar relatório");
            run.setBackground(MainFrame.AMARELO_BOTAO);
            run.setForeground(INK);
            run.setFocusPainted(false);
            run.setBorder(BorderFactory.createLineBorder(MainFrame.AMARELO_BORDA));
            toolbar.add(run);
            JLabel hint = new JLabel("Cada consulta combina dados de múltiplas tabelas.");
            hint.setForeground(new Color(143, 83, 0));
            toolbar.add(hint);
            add(toolbar, BorderLayout.NORTH);
            add(new JScrollPane(results), BorderLayout.CENTER);
            run.addActionListener(event -> executar());
            reportSelector.addActionListener(event -> executar());
        }

        private void executar() {
            String selected = (String) reportSelector.getSelectedItem();
            if (selected == null) {
                return;
            }
            try {
                preencherTabela(results, dao.executarRelatorio(selected));
            } catch (Exception ex) {
                mostrarErro(ex);
            }
        }
    }
}
