package dao;

import com.mongodb.client.MongoCollection;
import dados.Foto;
import dados.Usuario;
import org.bson.Document;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

public class FotoDAO {
    private static final String COLLECTION = "fotos";

    public void inserir(Foto foto, int idUsuario) throws SQLException {
        MongoSupport.requireExists("usuarios", idUsuario, "Usuário");
        Document document = new Document("id_usuario", idUsuario)
            .append("legenda", foto.getLegenda())
            .append("caminho_arquivo", foto.getCaminhoArquivo())
            .append("data_postagem", new Date())
            .append("hashtagIds", new ArrayList<Integer>());
        foto.setId(MongoSupport.inserir(COLLECTION, document));
    }

    public List<Foto> listarPorUsuario(int idUsuario) throws SQLException {
        List<Foto> result = new ArrayList<>();
        for (Document document : collection().find(eq("id_usuario", idUsuario))
                .sort(new Document("data_postagem", -1))) {
            result.add(MongoSupport.foto(document));
        }
        return result;
    }

    public List<Foto> listarFeed(int idUsuario) throws SQLException {
        List<Integer> authorIds = new ArrayList<>();
        authorIds.add(idUsuario);
        for (Document relationship : ConnectionFactory.getCollection("seguidores")
                .find(eq("id_seguidor", idUsuario))) {
            authorIds.add(MongoSupport.inteiro(relationship, "id_seguido"));
        }

        List<Foto> result = new ArrayList<>();
        for (Document document : collection().find(in("id_usuario", authorIds))
                .sort(new Document("data_postagem", -1))) {
            Foto foto = MongoSupport.foto(document);
            foto.setAutor(MongoSupport.usuario(
                MongoSupport.buscar("usuarios", MongoSupport.inteiro(document, "id_usuario"))));
            result.add(foto);
        }
        return result;
    }

    public void excluir(int idFoto) throws SQLException {
        MongoSupport.excluirFoto(idFoto);
    }

    private MongoCollection<Document> collection() {
        return ConnectionFactory.getCollection(COLLECTION);
    }
}
