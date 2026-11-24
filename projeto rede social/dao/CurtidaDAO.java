package dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import dados.Usuario;
import org.bson.Document;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

public class CurtidaDAO {
    private static final String COLLECTION = "curtidas";

    public void curtir(int idUsuario, int idFoto) throws SQLException {
        MongoSupport.requireExists("usuarios", idUsuario, "Usuário");
        MongoSupport.requireExists("fotos", idFoto, "Publicação");
        collection().updateOne(
            new Document("id_usuario", idUsuario).append("id_foto", idFoto),
            new Document("$setOnInsert", new Document("id_usuario", idUsuario).append("id_foto", idFoto)),
            new UpdateOptions().upsert(true));
    }

    public void descurtir(int idUsuario, int idFoto) throws SQLException {
        collection().deleteOne(new Document("id_usuario", idUsuario).append("id_foto", idFoto));
    }

    public List<Usuario> listarUsuarios(int idFoto) throws SQLException {
        List<Integer> userIds = new ArrayList<>();
        for (Document document : collection().find(eq("id_foto", idFoto))) {
            userIds.add(MongoSupport.inteiro(document, "id_usuario"));
        }
        List<Usuario> result = new ArrayList<>();
        if (userIds.isEmpty()) {
            return result;
        }
        for (Document document : ConnectionFactory.getCollection("usuarios").find(in("id", userIds))) {
            result.add(MongoSupport.usuario(document));
        }
        return result;
    }

    private MongoCollection<Document> collection() {
        return ConnectionFactory.getCollection(COLLECTION);
    }
}
