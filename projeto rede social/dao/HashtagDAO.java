package dao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import dados.Foto;
import org.bson.Document;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Updates.addToSet;

public class HashtagDAO {
    private static final String TAGS = "hashtags";

    public List<String> listarPorFoto(int idFoto) throws SQLException {
        Document photo = MongoSupport.buscar("fotos", idFoto);
        if (photo == null) {
            return new ArrayList<>();
        }
        List<Integer> ids = photo.getList("hashtagIds", Integer.class, new ArrayList<>());
        List<String> result = new ArrayList<>();
        if (!ids.isEmpty()) {
            for (Document hashtag : collection().find(in("id", ids))) {
                result.add(hashtag.getString("texto"));
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    public void associar(int idFoto, String textoHashtags) throws SQLException {
        Set<String> values = new LinkedHashSet<>();
        if (textoHashtags != null) {
            for (String value : textoHashtags.split("[,\\s]+")) {
                String text = value.trim().replaceFirst("^#+", "").toLowerCase(Locale.ROOT);
                if (!text.isEmpty()) {
                    if (!text.matches("[\\p{L}\\p{N}_]{1,50}")) {
                        throw new SQLException("Hashtag inválida: #" + text
                            + ". Use até 50 letras, números ou sublinhados.");
                    }
                    values.add(text);
                }
            }
        }
        MongoSupport.requireExists("fotos", idFoto, "Publicação");
        for (String text : values) {
            int idHashtag = obterOuCriar(text);
            ConnectionFactory.getCollection("fotos").updateOne(
                eq("id", idFoto), addToSet("hashtagIds", idHashtag));
        }
    }

    private int obterOuCriar(String text) throws SQLException {
        Document existing = collection().find(eq("texto", text)).first();
        if (existing != null) {
            return MongoSupport.inteiro(existing, "id");
        }

        Document newTag = new Document("id", MongoSupport.proximoId(TAGS)).append("texto", text);
        try {
            collection().insertOne(newTag);
            return MongoSupport.inteiro(newTag, "id");
        } catch (MongoException exception) {
            if (!MongoSupport.chaveDuplicada(exception)) {
                throw exception;
            }
            Document raced = collection().find(eq("texto", text)).first();
            if (raced != null) {
                return MongoSupport.inteiro(raced, "id");
            }
            throw new SQLException("Não foi possível obter a hashtag criada.", exception);
        }
    }

    public List<Foto> listarFotosPorHashtag(String textoHashtag) throws SQLException {
        String text = textoHashtag.trim().replaceFirst("^#+", "").toLowerCase(Locale.ROOT);
        Document hashtag = collection().find(eq("texto", text)).first();
        List<Foto> result = new ArrayList<>();
        if (hashtag == null) {
            return result;
        }
        int hashtagId = MongoSupport.inteiro(hashtag, "id");
        for (Document document : ConnectionFactory.getCollection("fotos").find(eq("hashtagIds", hashtagId))) {
            result.add(MongoSupport.foto(document));
        }
        result.sort((left, right) -> right.getDataPostagem().compareTo(left.getDataPostagem()));
        return result;
    }

    private MongoCollection<Document> collection() {
        return ConnectionFactory.getCollection(TAGS);
    }
}
