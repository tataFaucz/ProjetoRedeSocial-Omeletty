package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GestaoDAO {
    private static final Map<String, List<ColumnInfo>> ESQUEMAS = criarEsquemas();
    private static final List<String> RELATORIOS = Arrays.asList(
        "Publicações por usuário",
        "Comentários por publicação",
        "Publicações por hashtag"
    );

    public static class ColumnInfo {
        private final String name;
        private final int sqlType;
        private final String typeName;
        private final boolean nullable;
        private final boolean autoIncrement;
        private final boolean primaryKey;
        private final String defaultValue;

        ColumnInfo(String name, int sqlType, String typeName, boolean nullable,
                   boolean autoIncrement, boolean primaryKey, String defaultValue) {
            this.name = name;
            this.sqlType = sqlType;
            this.typeName = typeName;
            this.nullable = nullable;
            this.autoIncrement = autoIncrement;
            this.primaryKey = primaryKey;
            this.defaultValue = defaultValue;
        }

        public String getName() { return name; }
        public String getTypeName() { return typeName; }
        public boolean isNullable() { return nullable; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public boolean isPrimaryKey() { return primaryKey; }
        public String getDefaultValue() { return defaultValue; }
    }

    public static class QueryResult {
        private final String[] columns;
        private final Object[][] rows;

        QueryResult(String[] columns, Object[][] rows) {
            this.columns = columns;
            this.rows = rows;
        }

        public String[] getColumns() { return columns; }
        public Object[][] getRows() { return rows; }
    }

    private static Map<String, List<ColumnInfo>> criarEsquemas() {
        Map<String, List<ColumnInfo>> result = new LinkedHashMap<>();
        result.put("usuarios", colunas(
            coluna("id", Types.INTEGER, "integer", false, true, true, null),
            coluna("nome", Types.VARCHAR, "varchar(100)", false, false, false, null),
            coluna("login", Types.VARCHAR, "varchar(150)", false, false, false, null),
            coluna("senha", Types.VARCHAR, "varchar(255)", false, false, false, null),
            coluna("caminho_foto_perfil", Types.VARCHAR, "varchar(255)", true, false, false, null),
            coluna("biografia", Types.VARCHAR, "text", true, false, false, null)
        ));
        result.put("fotos", colunas(
            coluna("id", Types.INTEGER, "integer", false, true, true, null),
            coluna("id_usuario", Types.INTEGER, "integer", false, false, false, null),
            coluna("legenda", Types.VARCHAR, "text", true, false, false, null),
            coluna("caminho_arquivo", Types.VARCHAR, "varchar(255)", false, false, false, null),
            coluna("data_postagem", Types.TIMESTAMP, "timestamp", false, false, false, "CURRENT_TIMESTAMP")
        ));
        result.put("comentarios", colunas(
            coluna("id", Types.INTEGER, "integer", false, true, true, null),
            coluna("id_usuario", Types.INTEGER, "integer", false, false, false, null),
            coluna("id_foto", Types.INTEGER, "integer", false, false, false, null),
            coluna("conteudo", Types.VARCHAR, "text", false, false, false, null),
            coluna("data_comentario", Types.TIMESTAMP, "timestamp", false, false, false, "CURRENT_TIMESTAMP")
        ));
        result.put("mensagens", colunas(
            coluna("id", Types.INTEGER, "integer", false, true, true, null),
            coluna("id_remetente", Types.INTEGER, "integer", false, false, false, null),
            coluna("id_destinatario", Types.INTEGER, "integer", false, false, false, null),
            coluna("conteudo", Types.VARCHAR, "text", false, false, false, null),
            coluna("data_envio", Types.TIMESTAMP, "timestamp", false, false, false, "CURRENT_TIMESTAMP")
        ));
        result.put("hashtags", colunas(
            coluna("id", Types.INTEGER, "integer", false, true, true, null),
            coluna("texto", Types.VARCHAR, "varchar(50)", false, false, false, null)
        ));
        return result;
    }

    private static ColumnInfo coluna(String name, int type, String typeName, boolean nullable,
                                     boolean autoIncrement, boolean primaryKey, String defaultValue) {
        return new ColumnInfo(name, type, typeName, nullable, autoIncrement, primaryKey, defaultValue);
    }

    private static List<ColumnInfo> colunas(ColumnInfo... columns) {
        return Arrays.asList(columns);
    }

    private List<ColumnInfo> esquema(String entidade) {
        List<ColumnInfo> result = ESQUEMAS.get(entidade);
        if (result == null) {
            throw new IllegalArgumentException("Entidade não permitida: " + entidade);
        }
        return result;
    }

    public List<ColumnInfo> listarColunas(String tabela) throws SQLException {
        return esquema(tabela);
    }

    public QueryResult listar(String tabela) throws SQLException {
        List<ColumnInfo> columns = esquema(tabela);
        String[] names = columns.stream().map(ColumnInfo::getName).toArray(String[]::new);
        String sql = "SELECT " + nomesColunas(columns) + " FROM " + tabela + " ORDER BY id";
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Object[] row = new Object[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    ColumnInfo column = columns.get(i);
                    row[i] = column.sqlType == Types.TIMESTAMP
                        ? result.getTimestamp(column.name)
                        : result.getObject(column.name);
                }
                rows.add(row);
            }
        }
        return new QueryResult(names, rows.toArray(new Object[0][]));
    }

    public void inserir(String tabela, Map<String, String> values) throws SQLException {
        List<ColumnInfo> columns = esquema(tabela);
        Map<String, Object> valuesToInsert = new LinkedHashMap<>();
        for (ColumnInfo column : columns) {
            if (column.autoIncrement) {
                continue;
            }
            String raw = values.get(column.name);
            if (isBlank(raw) && column.defaultValue != null) {
                continue;
            }
            Object value = isBlank(raw) && column.nullable ? null : parseValue(column, raw);
            valuesToInsert.put(column.name, value);
        }
        normalizarHashtag(tabela, valuesToInsert);
        validarReferencias(tabela, valuesToInsert);

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tabela).append(" (");
        sql.append(String.join(", ", valuesToInsert.keySet())).append(") VALUES (");
        sql.append(String.join(", ", java.util.Collections.nCopies(valuesToInsert.size(), "?")));
        sql.append(')');
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            for (Object value : valuesToInsert.values()) {
                statement.setObject(parameter++, value);
            }
            statement.executeUpdate();
        }
    }

    public void atualizar(String tabela, Map<String, String> values) throws SQLException {
        List<ColumnInfo> columns = esquema(tabela);
        int id = parseId(values.get("id"));
        Map<String, Object> updates = new LinkedHashMap<>();
        for (ColumnInfo column : columns) {
            if (column.primaryKey) {
                continue;
            }
            String raw = values.get(column.name);
            Object value = isBlank(raw) && column.nullable ? null : parseValue(column, raw);
            updates.put(column.name, value);
        }
        normalizarHashtag(tabela, updates);
        validarReferencias(tabela, updates);
        StringBuilder sql = new StringBuilder("UPDATE ").append(tabela).append(" SET ");
        List<String> assignments = new ArrayList<>();
        for (String column : updates.keySet()) {
            assignments.add(column + " = ?");
        }
        sql.append(String.join(", ", assignments)).append(" WHERE id = ?");
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            for (Object value : updates.values()) {
                statement.setObject(parameter++, value);
            }
            statement.setInt(parameter, id);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("O registro não foi encontrado para atualização.");
            }
        }
    }

    private String nomesColunas(List<ColumnInfo> columns) {
        List<String> names = new ArrayList<>();
        for (ColumnInfo column : columns) {
            names.add(column.name);
        }
        return String.join(", ", names);
    }

    private void normalizarHashtag(String table, Map<String, Object> values) throws SQLException {
        if (!"hashtags".equals(table)) {
            return;
        }
        Object raw = values.get("texto");
        String text = raw == null ? "" : raw.toString().trim().toLowerCase(Locale.ROOT);
        if (text.length() > 50) {
            throw new SQLException("A hashtag deve ter no máximo 50 caracteres.");
        }
        values.put("texto", text);
    }

    private void validarReferencias(String table, Map<String, Object> values) throws SQLException {
        switch (table) {
            case "fotos":
                PostgresSupport.requireExists("usuarios", (Integer) values.get("id_usuario"), "Usuário");
                break;
            case "comentarios":
                PostgresSupport.requireExists("usuarios", (Integer) values.get("id_usuario"), "Usuário");
                PostgresSupport.requireExists("fotos", (Integer) values.get("id_foto"), "Publicação");
                break;
            case "mensagens":
                int sender = (Integer) values.get("id_remetente");
                int recipient = (Integer) values.get("id_destinatario");
                if (sender == recipient) {
                    throw new SQLException("O remetente e o destinatário devem ser usuários diferentes.");
                }
                PostgresSupport.requireExists("usuarios", sender, "Remetente");
                PostgresSupport.requireExists("usuarios", recipient, "Destinatário");
                break;
            default:
                break;
        }
    }

    public void excluir(String tabela, Map<String, String> values) throws SQLException {
        esquema(tabela);
        int id = parseId(values.get("id"));
        try (Connection connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                switch (tabela) {
                    case "usuarios":
                        execute(connection, "DELETE FROM comentarios WHERE id_usuario = ? "
                            + "OR id_foto IN (SELECT id FROM fotos WHERE id_usuario = ?)", id, id);
                        execute(connection, "DELETE FROM mensagens WHERE id_remetente = ? "
                            + "OR id_destinatario = ?", id, id);
                        execute(connection, "DELETE FROM curtidas WHERE id_usuario = ? "
                            + "OR id_foto IN (SELECT id FROM fotos WHERE id_usuario = ?)", id, id);
                        execute(connection, "DELETE FROM compartilhamentos WHERE id_usuario = ? "
                            + "OR id_foto IN (SELECT id FROM fotos WHERE id_usuario = ?)", id, id);
                        execute(connection, "DELETE FROM seguidores WHERE id_seguidor = ? "
                            + "OR id_seguido = ?", id, id);
                        execute(connection, "DELETE FROM fotos_hashtags WHERE id_foto IN "
                            + "(SELECT id FROM fotos WHERE id_usuario = ?)", id);
                        execute(connection, "DELETE FROM fotos WHERE id_usuario = ?", id);
                        break;
                    case "fotos":
                        execute(connection, "DELETE FROM comentarios WHERE id_foto = ?", id);
                        execute(connection, "DELETE FROM curtidas WHERE id_foto = ?", id);
                        execute(connection, "DELETE FROM compartilhamentos WHERE id_foto = ?", id);
                        execute(connection, "DELETE FROM fotos_hashtags WHERE id_foto = ?", id);
                        break;
                    case "hashtags":
                        execute(connection, "DELETE FROM fotos_hashtags WHERE id_hashtag = ?", id);
                        break;
                    default:
                        break;
                }
                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM " + tabela + " WHERE id = ?")) {
                    statement.setInt(1, id);
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("O registro não foi encontrado para exclusão.");
                    }
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void execute(Connection connection, String sql, int... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setInt(i + 1, parameters[i]);
            }
            statement.executeUpdate();
        }
    }

    private int parseId(String value) throws SQLException {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException exception) {
            throw new SQLException("Informe um ID numérico válido.", exception);
        }
    }

    private Object parseValue(ColumnInfo column, String value) throws SQLException {
        if (isBlank(value)) {
            if (column.nullable) {
                return null;
            }
            throw new SQLException("O campo " + column.name + " é obrigatório.");
        }
        try {
            switch (column.sqlType) {
                case Types.INTEGER:
                case Types.SMALLINT:
                case Types.TINYINT:
                    return Integer.valueOf(value.trim());
                case Types.BIGINT:
                    return Long.valueOf(value.trim());
                case Types.NUMERIC:
                case Types.DECIMAL:
                    return new BigDecimal(value.trim());
                case Types.BOOLEAN:
                case Types.BIT:
                    if (!"true".equalsIgnoreCase(value.trim())
                            && !"false".equalsIgnoreCase(value.trim())) {
                        throw new IllegalArgumentException("Use true ou false.");
                    }
                    return Boolean.valueOf(value.trim());
                case Types.TIMESTAMP:
                case Types.TIMESTAMP_WITH_TIMEZONE:
                    return Timestamp.valueOf(value.trim());
                case Types.DATE:
                    return java.sql.Date.valueOf(value.trim());
                default:
                    return value;
            }
        } catch (IllegalArgumentException exception) {
            throw new SQLException("Valor inválido para " + column.name + " (" + column.typeName + "): "
                + value, exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public void executarAssociacao(String tipo, int primeiroId, int segundoId, boolean adicionar)
            throws SQLException {
        switch (tipo) {
            case "seguir":
                if (adicionar) {
                    new SeguidorDAO().seguir(primeiroId, segundoId);
                } else {
                    new SeguidorDAO().deixarDeSeguir(primeiroId, segundoId);
                }
                return;
            case "curtir":
                if (adicionar) {
                    new CurtidaDAO().curtir(primeiroId, segundoId);
                } else {
                    new CurtidaDAO().descurtir(primeiroId, segundoId);
                }
                return;
            case "compartilhar":
                if (adicionar) {
                    new CompartilhamentoDAO().compartilhar(primeiroId, segundoId);
                } else {
                    new CompartilhamentoDAO().removerCompartilhamento(primeiroId, segundoId);
                }
                return;
            case "hashtag":
                if (adicionar) {
                    associarHashtagExistente(primeiroId, segundoId);
                } else {
                    try (Connection connection = ConnectionFactory.getConnection();
                         PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM fotos_hashtags WHERE id_foto = ? AND id_hashtag = ?")) {
                        statement.setInt(1, primeiroId);
                        statement.setInt(2, segundoId);
                        statement.executeUpdate();
                    }
                }
                return;
            default:
                throw new IllegalArgumentException("Processo associativo inválido: " + tipo);
        }
    }

    private void associarHashtagExistente(int photoId, int hashtagId) throws SQLException {
        PostgresSupport.requireExists("fotos", photoId, "Publicação");
        PostgresSupport.requireExists("hashtags", hashtagId, "Hashtag");
        String sql = "INSERT INTO fotos_hashtags (id_foto, id_hashtag) VALUES (?, ?) "
            + "ON CONFLICT (id_foto, id_hashtag) DO NOTHING";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, photoId);
            statement.setInt(2, hashtagId);
            statement.executeUpdate();
        }
    }

    public QueryResult executarRelatorio(String nome) throws SQLException {
        switch (nome) {
            case "Publicações por usuário":
                return relatorioPublicacoesPorUsuario();
            case "Comentários por publicação":
                return relatorioComentariosPorPublicacao();
            case "Publicações por hashtag":
                return relatorioPublicacoesPorHashtag();
            default:
                throw new IllegalArgumentException("Relatório desconhecido: " + nome);
        }
    }

    public List<String> listarRelatorios() {
        return new ArrayList<>(RELATORIOS);
    }

    private QueryResult relatorioPublicacoesPorUsuario() throws SQLException {
        String sql = "SELECT u.id, u.nome, u.login, COUNT(f.id) AS total_publicacoes "
            + "FROM usuarios u LEFT JOIN fotos f ON f.id_usuario = u.id "
            + "GROUP BY u.id, u.nome, u.login ORDER BY u.nome";
        return consultar(sql, new String[]{"id", "nome", "login", "total_publicacoes"});
    }

    private QueryResult relatorioComentariosPorPublicacao() throws SQLException {
        String sql = "SELECT f.id AS id_foto, f.legenda, u.nome AS autor, "
            + "COUNT(c.id) AS total_comentarios FROM fotos f "
            + "JOIN usuarios u ON u.id = f.id_usuario "
            + "LEFT JOIN comentarios c ON c.id_foto = f.id "
            + "GROUP BY f.id, f.legenda, u.nome "
            + "ORDER BY total_comentarios DESC, f.id";
        return consultar(sql,
            new String[]{"id_foto", "legenda", "autor", "total_comentarios"});
    }

    private QueryResult relatorioPublicacoesPorHashtag() throws SQLException {
        String sql = "SELECT h.texto AS hashtag, f.id AS id_foto, u.nome AS autor, "
            + "f.legenda, f.data_postagem FROM hashtags h "
            + "JOIN fotos_hashtags fh ON fh.id_hashtag = h.id "
            + "JOIN fotos f ON f.id = fh.id_foto "
            + "JOIN usuarios u ON u.id = f.id_usuario "
            + "ORDER BY h.texto, f.data_postagem DESC";
        return consultar(sql,
            new String[]{"hashtag", "id_foto", "autor", "legenda", "data_postagem"});
    }

    private QueryResult consultar(String sql, String[] columns) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Object[] row = new Object[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    row[i] = result.getObject(columns[i]);
                }
                rows.add(row);
            }
        }
        return new QueryResult(columns, rows.toArray(new Object[0][]));
    }
}
