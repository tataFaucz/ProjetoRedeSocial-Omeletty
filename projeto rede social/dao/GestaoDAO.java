package dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dados.Foto;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

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

    private List<ColumnInfo> esquema(String collection) {
        List<ColumnInfo> result = ESQUEMAS.get(collection);
        if (result == null) {
            throw new IllegalArgumentException("Entidade não permitida: " + collection);
        }
        return result;
    }

    public List<ColumnInfo> listarColunas(String collection) throws SQLException {
        return esquema(collection);
    }

    public QueryResult listar(String collectionName) throws SQLException {
        List<ColumnInfo> columns = esquema(collectionName);
        String[] names = columns.stream().map(ColumnInfo::getName).toArray(String[]::new);
        List<Object[]> rows = new ArrayList<>();
        for (Document document : collection(collectionName).find().sort(new Document("id", 1))) {
            Object[] row = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                Object value = document.get(columns.get(i).name);
                row[i] = value instanceof Date ? new Timestamp(((Date) value).getTime()) : value;
            }
            rows.add(row);
        }
        return new QueryResult(names, rows.toArray(new Object[0][]));
    }

    public void inserir(String collectionName, Map<String, String> values) throws SQLException {
        List<ColumnInfo> columns = esquema(collectionName);
        Document document = new Document();
        for (ColumnInfo column : columns) {
            if (column.autoIncrement) {
                continue;
            }
            String raw = values.get(column.name);
            if (isBlank(raw) && column.defaultValue != null) {
                document.put(column.name, new Date());
                continue;
            }
            document.put(column.name, isBlank(raw) && column.nullable ? null : parseValue(column, raw));
        }
        normalizarHashtag(collectionName, document);
        validarReferencias(collectionName, document);
        MongoSupport.inserir(collectionName, document);
    }

    public void atualizar(String collectionName, Map<String, String> values) throws SQLException {
        List<ColumnInfo> columns = esquema(collectionName);
        int id = parseId(values.get("id"));
        List<Bson> updates = new ArrayList<>();
        Document fields = new Document();
        for (ColumnInfo column : columns) {
            if (column.primaryKey) {
                continue;
            }
            String raw = values.get(column.name);
            Object value = isBlank(raw) && column.nullable ? null : parseValue(column, raw);
            fields.put(column.name, value);
            updates.add(Updates.set(column.name, value));
        }
        normalizarHashtag(collectionName, fields);
        validarReferencias(collectionName, fields);
        if ("hashtags".equals(collectionName)) {
            updates.set(0, Updates.set("texto", fields.getString("texto")));
        }
        if (collection(collectionName).updateOne(eq("id", id), Updates.combine(updates)).getMatchedCount() == 0) {
            throw new SQLException("O registro não foi encontrado para atualização.");
        }
    }

    public void excluir(String collectionName, Map<String, String> values) throws SQLException {
        esquema(collectionName);
        int id = parseId(values.get("id"));
        if (MongoSupport.buscar(collectionName, id) == null) {
            throw new SQLException("O registro não foi encontrado para exclusão.");
        }
        switch (collectionName) {
            case "usuarios":
                MongoSupport.excluirUsuario(id);
                return;
            case "fotos":
                MongoSupport.excluirFoto(id);
                break;
            case "hashtags":
                ConnectionFactory.getCollection("fotos").updateMany(
                    Filters.exists("hashtagIds", true), Updates.pull("hashtagIds", id));
                break;
            default:
                break;
        }
        collection(collectionName).deleteOne(eq("id", id));
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
                associarHashtag(primeiroId, segundoId, adicionar);
                return;
            default:
                throw new IllegalArgumentException("Processo associativo inválido: " + tipo);
        }
    }

    private void associarHashtag(int photoId, int hashtagId, boolean adicionar) throws SQLException {
        MongoSupport.requireExists("fotos", photoId, "Publicação");
        MongoSupport.requireExists("hashtags", hashtagId, "Hashtag");
        if (adicionar) {
            ConnectionFactory.getCollection("fotos").updateOne(
                eq("id", photoId), Updates.addToSet("hashtagIds", hashtagId));
        } else {
            ConnectionFactory.getCollection("fotos").updateOne(
                eq("id", photoId), Updates.pull("hashtagIds", hashtagId));
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

    private QueryResult relatorioPublicacoesPorUsuario() {
        List<Object[]> rows = new ArrayList<>();
        for (Document user : collection("usuarios").find().sort(new Document("nome", 1))) {
            long total = collection("fotos").countDocuments(eq("id_usuario", MongoSupport.inteiro(user, "id")));
            rows.add(new Object[]{user.get("id"), user.get("nome"), user.get("login"), total});
        }
        return new QueryResult(
            new String[]{"id", "nome", "login", "total_publicacoes"},
            rows.toArray(new Object[0][]));
    }

    private QueryResult relatorioComentariosPorPublicacao() {
        List<Object[]> rows = new ArrayList<>();
        for (Document photo : collection("fotos").find()) {
            Document user = MongoSupport.buscar("usuarios", MongoSupport.inteiro(photo, "id_usuario"));
            long total = collection("comentarios").countDocuments(eq("id_foto", MongoSupport.inteiro(photo, "id")));
            rows.add(new Object[]{
                photo.get("id"),
                photo.get("legenda"),
                user == null ? "" : user.getString("nome"),
                total
            });
        }
        rows.sort((left, right) -> {
            int totalOrder = Long.compare(((Number) right[3]).longValue(), ((Number) left[3]).longValue());
            return totalOrder != 0 ? totalOrder : Integer.compare(((Number) left[0]).intValue(),
                ((Number) right[0]).intValue());
        });
        return new QueryResult(
            new String[]{"id_foto", "legenda", "autor", "total_comentarios"},
            rows.toArray(new Object[0][]));
    }

    private QueryResult relatorioPublicacoesPorHashtag() {
        List<Object[]> rows = new ArrayList<>();
        for (Document hashtag : collection("hashtags").find()) {
            int hashtagId = MongoSupport.inteiro(hashtag, "id");
            for (Document photo : collection("fotos").find(eq("hashtagIds", hashtagId))) {
                Document user = MongoSupport.buscar("usuarios", MongoSupport.inteiro(photo, "id_usuario"));
                Object date = photo.get("data_postagem");
                rows.add(new Object[]{
                    hashtag.getString("texto"),
                    photo.get("id"),
                    user == null ? "" : user.getString("nome"),
                    photo.get("legenda"),
                    date instanceof Date ? new Timestamp(((Date) date).getTime()) : date
                });
            }
        }
        rows.sort((left, right) -> {
            int tagOrder = ((String) left[0]).compareTo((String) right[0]);
            if (tagOrder != 0) {
                return tagOrder;
            }
            Date leftDate = (Date) left[4];
            Date rightDate = (Date) right[4];
            return rightDate.compareTo(leftDate);
        });
        return new QueryResult(
            new String[]{"hashtag", "id_foto", "autor", "legenda", "data_postagem"},
            rows.toArray(new Object[0][]));
    }

    private void normalizarHashtag(String collectionName, Document values) throws SQLException {
        if (!"hashtags".equals(collectionName)) {
            return;
        }
        String text = values.getString("texto");
        text = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty() || text.length() > 50) {
            throw new SQLException("A hashtag deve ter entre 1 e 50 caracteres.");
        }
        values.put("texto", text);
    }

    private void validarReferencias(String collectionName, Document values) throws SQLException {
        switch (collectionName) {
            case "fotos":
                MongoSupport.requireExists("usuarios", inteiro(values, "id_usuario"), "Usuário");
                break;
            case "comentarios":
                MongoSupport.requireExists("usuarios", inteiro(values, "id_usuario"), "Usuário");
                MongoSupport.requireExists("fotos", inteiro(values, "id_foto"), "Publicação");
                break;
            case "mensagens":
                int sender = inteiro(values, "id_remetente");
                int recipient = inteiro(values, "id_destinatario");
                if (sender == recipient) {
                    throw new SQLException("O remetente e o destinatário devem ser usuários diferentes.");
                }
                MongoSupport.requireExists("usuarios", sender, "Remetente");
                MongoSupport.requireExists("usuarios", recipient, "Destinatário");
                break;
            default:
                break;
        }
    }

    private int inteiro(Document values, String field) throws SQLException {
        Object value = values.get(field);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new SQLException("Informe um ID numérico válido para " + field + ".");
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

    private MongoCollection<Document> collection(String name) {
        return ConnectionFactory.getCollection(name);
    }
}
