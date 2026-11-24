package dao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import dados.Usuario;
import negocios.SegurancaSenha;
import org.bson.Document;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class UsuarioDAO {
    private static final String COLLECTION = "usuarios";

    public void inserir(Usuario usuario) throws SQLException {
        String login = usuario.getEmail().trim().toLowerCase(Locale.ROOT);
        Document document = new Document("nome", usuario.getNome())
            .append("login", login)
            .append("senha", SegurancaSenha.gerarHash(usuario.getSenha()))
            .append("caminho_foto_perfil", usuario.getFotoPerfil())
            .append("biografia", usuario.getBiografia());
        try {
            usuario.setId(MongoSupport.inserir(COLLECTION, document));
            usuario.setSenha(null);
        } catch (MongoException exception) {
            if (MongoSupport.chaveDuplicada(exception)) {
                throw new SQLException("Já existe uma conta com este e-mail.", "23505", exception);
            }
            throw exception;
        }
    }

    public Usuario buscarPorLogin(String identificador, String senha) throws SQLException {
        String login = identificador.trim().toLowerCase(Locale.ROOT);
        Document document = collection().find(eq("login", login)).first();
        if (document == null) {
            return null;
        }
        String storedPassword = document.getString("senha");
        if (!SegurancaSenha.verificar(senha, storedPassword)) {
            return null;
        }
        int id = MongoSupport.inteiro(document, "id");
        if (!SegurancaSenha.jaEstaProtegida(storedPassword)) {
            atualizarSenha(id, SegurancaSenha.gerarHash(senha));
        }
        return MongoSupport.usuario(document);
    }

    public boolean existeLogin(String email) throws SQLException {
        return existeLogin(email, null);
    }

    public boolean existeEmailEmOutroUsuario(String email, int idUsuario) throws SQLException {
        return existeLogin(email, idUsuario);
    }

    private boolean existeLogin(String email, Integer ignorarId) throws SQLException {
        String login = email.trim().toLowerCase(Locale.ROOT);
        Document filter = new Document("login", login);
        if (ignorarId != null) {
            filter.append("id", new Document("$ne", ignorarId));
        }
        return collection().find(filter).limit(1).first() != null;
    }

    public boolean senhaCorreta(int idUsuario, String senha) throws SQLException {
        Document document = MongoSupport.buscar(COLLECTION, idUsuario);
        if (document == null) {
            return false;
        }
        String storedPassword = document.getString("senha");
        boolean valid = SegurancaSenha.verificar(senha, storedPassword);
        if (valid && !SegurancaSenha.jaEstaProtegida(storedPassword)) {
            atualizarSenha(idUsuario, SegurancaSenha.gerarHash(senha));
        }
        return valid;
    }

    private void atualizarSenha(int idUsuario, String senhaProtegida) {
        collection().updateOne(eq("id", idUsuario), set("senha", senhaProtegida));
    }

    public void atualizar(Usuario usuario) throws SQLException {
        collection().updateOne(eq("id", usuario.getId()), combine(
            set("nome", usuario.getNome()),
            set("senha", usuario.getSenha()),
            set("caminho_foto_perfil", usuario.getFotoPerfil()),
            set("biografia", usuario.getBiografia())));
    }

    public void atualizarConta(Usuario usuario, String novaSenha) throws SQLException {
        String login = usuario.getEmail().trim().toLowerCase(Locale.ROOT);
        List<org.bson.conversions.Bson> updates = new ArrayList<>();
        updates.add(set("nome", usuario.getNome()));
        updates.add(set("login", login));
        updates.add(set("caminho_foto_perfil", usuario.getFotoPerfil()));
        updates.add(set("biografia", usuario.getBiografia()));
        if (novaSenha != null && !novaSenha.isEmpty()) {
            updates.add(set("senha", SegurancaSenha.gerarHash(novaSenha)));
        }
        try {
            collection().updateOne(eq("id", usuario.getId()), combine(updates));
        } catch (MongoException exception) {
            if (MongoSupport.chaveDuplicada(exception)) {
                throw new SQLException("Já existe uma conta com este e-mail.", "23505", exception);
            }
            throw exception;
        }
    }

    public void excluir(int id) throws SQLException {
        MongoSupport.excluirUsuario(id);
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> result = new ArrayList<>();
        for (Document document : collection().find().sort(new Document("id", 1))) {
            result.add(MongoSupport.usuario(document));
        }
        return result;
    }

    private MongoCollection<Document> collection() {
        return ConnectionFactory.getCollection(COLLECTION);
    }
}
