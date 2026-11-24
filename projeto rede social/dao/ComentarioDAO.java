package dao;

import com.mongodb.client.MongoCollection;
import dados.Comentario;
import org.bson.Document;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class ComentarioDAO {
    private static final String COLLECTION = "comentarios";

    public void inserir(Comentario comentario, int idUsuario, int idFoto) throws SQLException {
        MongoSupport.requireExists("usuarios", idUsuario, "Usuário");
        MongoSupport.requireExists("fotos", idFoto, "Publicação");
        Date date = comentario.getTimestamp() == null
            ? new Date() : new Date(comentario.getTimestamp().getTime());
        MongoSupport.inserir(COLLECTION, new Document("id_usuario", idUsuario)
            .append("id_foto", idFoto)
            .append("conteudo", comentario.getMensagem())
            .append("data_comentario", date));
    }

    public List<Comentario> listarPorFoto(int idFoto) throws SQLException {
        List<Comentario> result = new ArrayList<>();
        for (Document document : collection().find(eq("id_foto", idFoto))
                .sort(new Document("data_comentario", 1))) {
            Document user = MongoSupport.buscar("usuarios", MongoSupport.inteiro(document, "id_usuario"));
            String author = user == null ? "" : user.getString("nome");
            Date timestamp = document.getDate("data_comentario");
            LocalDateTime date = timestamp == null ? null
                : LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
            result.add(new Comentario(
                MongoSupport.inteiro(document, "id"),
                author,
                document.getString("conteudo"),
                date));
        }
        return result;
    }

    private MongoCollection<Document> collection() {
        return ConnectionFactory.getCollection(COLLECTION);
    }
}
