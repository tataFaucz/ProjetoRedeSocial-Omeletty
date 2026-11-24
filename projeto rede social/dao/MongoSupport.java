package dao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import dados.Foto;
import dados.Usuario;
import org.bson.Document;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Updates.inc;

final class MongoSupport {
    private MongoSupport() {
    }

    static int inserir(String collectionName, Document document) {
        int id = proximoId(collectionName);
        document.put("id", id);
        ConnectionFactory.getCollection(collectionName).insertOne(document);
        return id;
    }

    static int proximoId(String sequenceName) {
        Document counter = ConnectionFactory.getCollection("counters").findOneAndUpdate(
            eq("_id", sequenceName),
            inc("value", 1),
            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return counter.getInteger("value");
    }

    static Document buscar(String collection, int id) {
        return ConnectionFactory.getCollection(collection).find(eq("id", id)).first();
    }

    static void requireExists(String collection, int id, String entity) throws SQLException {
        if (buscar(collection, id) == null) {
            throw new SQLException(entity + " informado não existe.");
        }
    }

    static Usuario usuario(Document document) {
        if (document == null) {
            return null;
        }
        return new Usuario(
            inteiro(document, "id"),
            document.getString("nome"),
            document.getString("login"),
            null,
            document.getString("caminho_foto_perfil"),
            document.getString("biografia")
        );
    }

    static Foto foto(Document document) {
        return new Foto(
            inteiro(document, "id"),
            document.getString("legenda"),
            document.getString("caminho_arquivo"),
            timestamp(document, "data_postagem")
        );
    }

    static Timestamp timestamp(Document document, String field) {
        Object value = document.get(field);
        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        return null;
    }

    static int inteiro(Document document, String field) {
        Number value = document.get(field, Number.class);
        return value == null ? 0 : value.intValue();
    }

    static List<Integer> ids(List<Document> documents, String field) {
        List<Integer> result = new ArrayList<>();
        for (Document document : documents) {
            result.add(inteiro(document, field));
        }
        return result;
    }

    static void excluirFoto(int idFoto) {
        ConnectionFactory.getCollection("comentarios").deleteMany(eq("id_foto", idFoto));
        ConnectionFactory.getCollection("curtidas").deleteMany(eq("id_foto", idFoto));
        ConnectionFactory.getCollection("compartilhamentos").deleteMany(eq("id_foto", idFoto));
        ConnectionFactory.getCollection("fotos").deleteOne(eq("id", idFoto));
    }

    static void excluirUsuario(int idUsuario) {
        List<Integer> fotos = ids(
            ConnectionFactory.getCollection("fotos").find(eq("id_usuario", idUsuario)).into(new ArrayList<>()),
            "id");
        ConnectionFactory.getCollection("comentarios").deleteMany(eq("id_usuario", idUsuario));
        ConnectionFactory.getCollection("comentarios").deleteMany(in("id_foto", fotos));
        ConnectionFactory.getCollection("mensagens").deleteMany(eq("id_remetente", idUsuario));
        ConnectionFactory.getCollection("mensagens").deleteMany(eq("id_destinatario", idUsuario));
        ConnectionFactory.getCollection("curtidas").deleteMany(eq("id_usuario", idUsuario));
        ConnectionFactory.getCollection("curtidas").deleteMany(in("id_foto", fotos));
        ConnectionFactory.getCollection("compartilhamentos").deleteMany(eq("id_usuario", idUsuario));
        ConnectionFactory.getCollection("compartilhamentos").deleteMany(in("id_foto", fotos));
        ConnectionFactory.getCollection("seguidores").deleteMany(eq("id_seguidor", idUsuario));
        ConnectionFactory.getCollection("seguidores").deleteMany(eq("id_seguido", idUsuario));
        ConnectionFactory.getCollection("fotos").deleteMany(eq("id_usuario", idUsuario));
        ConnectionFactory.getCollection("usuarios").deleteOne(eq("id", idUsuario));
    }

    static boolean chaveDuplicada(MongoException exception) {
        return exception.getCode() == 11000;
    }
}
