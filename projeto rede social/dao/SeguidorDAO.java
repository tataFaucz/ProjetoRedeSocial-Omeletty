package dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class SeguidorDAO {
    private static final String COLLECTION = "seguidores";

    public void seguir(int idSeguidor, int idSeguido) throws SQLException {
        if (idSeguidor == idSeguido) {
            throw new SQLException("Um usuário não pode seguir a si mesmo.");
        }
        MongoSupport.requireExists("usuarios", idSeguidor, "Seguidor");
        MongoSupport.requireExists("usuarios", idSeguido, "Usuário seguido");
        collection().updateOne(
            new Document("id_seguidor", idSeguidor).append("id_seguido", idSeguido),
            new Document("$setOnInsert",
                new Document("id_seguidor", idSeguidor).append("id_seguido", idSeguido)),
            new UpdateOptions().upsert(true));
    }

    public void deixarDeSeguir(int idSeguidor, int idSeguido) throws SQLException {
        collection().deleteOne(new Document("id_seguidor", idSeguidor).append("id_seguido", idSeguido));
    }

    public List<Integer> listarSeguidores(int idSeguido) throws SQLException {
        return listarIds("id_seguidor", "id_seguido", idSeguido);
    }

    public List<Integer> listarSeguindo(int idSeguidor) throws SQLException {
        return listarIds("id_seguido", "id_seguidor", idSeguidor);
    }

    private List<Integer> listarIds(String resultField, String filterField, int filterId) {
        List<Integer> result = new ArrayList<>();
        for (Document document : collection().find(eq(filterField, filterId))) {
            result.add(MongoSupport.inteiro(document, resultField));
        }
        return result;
    }

    private MongoCollection<Document> collection() {
        return ConnectionFactory.getCollection(COLLECTION);
    }
}
