package dao;

import dados.Usuario;
import negocios.SegurancaSenha;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsuarioDAO {
    private static final String COLUNAS_USUARIO =
        "id, nome, login, caminho_foto_perfil, biografia";

    public void inserir(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuarios "
            + "(nome, login, senha, caminho_foto_perfil, biografia) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql,
                 PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, usuario.getNome());
            statement.setString(2, usuario.getEmail().trim());
            statement.setString(3, SegurancaSenha.gerarHash(usuario.getSenha()));
            statement.setString(4, usuario.getFotoPerfil());
            statement.setString(5, usuario.getBiografia());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    usuario.setId(keys.getInt(1));
                }
            }
            usuario.setSenha(null);
        } catch (SQLException exception) {
            if (PostgresSupport.duplicidade(exception)) {
                throw new SQLException("Já existe uma conta com este e-mail.", exception);
            }
            throw exception;
        }
    }

    public Usuario buscarPorLogin(String identificador, String senha) throws SQLException {
        String sql = "SELECT id, nome, login, senha, caminho_foto_perfil, biografia "
            + "FROM usuarios WHERE LOWER(BTRIM(login)) = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identificador.trim().toLowerCase(Locale.ROOT));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                String storedPassword = result.getString("senha");
                if (!SegurancaSenha.verificar(senha, storedPassword)) {
                    return null;
                }
                int id = result.getInt("id");
                if (!SegurancaSenha.jaEstaProtegida(storedPassword)) {
                    atualizarSenha(id, SegurancaSenha.gerarHash(senha));
                }
                return PostgresSupport.usuario(result);
            }
        }
    }

    public boolean existeLogin(String email) throws SQLException {
        return existeLogin(email, null);
    }

    public boolean existeEmailEmOutroUsuario(String email, int idUsuario) throws SQLException {
        return existeLogin(email, idUsuario);
    }

    private boolean existeLogin(String email, Integer ignorarId) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE LOWER(BTRIM(login)) = ?"
            + (ignorarId == null ? "" : " AND id <> ?");
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim().toLowerCase(Locale.ROOT));
            if (ignorarId != null) {
                statement.setInt(2, ignorarId);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean senhaCorreta(int idUsuario, String senha) throws SQLException {
        String sql = "SELECT senha FROM usuarios WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return false;
                }
                String storedPassword = result.getString("senha");
                boolean valid = SegurancaSenha.verificar(senha, storedPassword);
                if (valid && !SegurancaSenha.jaEstaProtegida(storedPassword)) {
                    atualizarSenha(idUsuario, SegurancaSenha.gerarHash(senha));
                }
                return valid;
            }
        }
    }

    private void atualizarSenha(int idUsuario, String senhaProtegida) throws SQLException {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE usuarios SET senha = ? WHERE id = ?")) {
            statement.setString(1, senhaProtegida);
            statement.setInt(2, idUsuario);
            statement.executeUpdate();
        }
    }

    public void atualizar(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuarios SET nome = ?, senha = ?, caminho_foto_perfil = ?, "
            + "biografia = ? WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuario.getNome());
            statement.setString(2, usuario.getSenha());
            statement.setString(3, usuario.getFotoPerfil());
            statement.setString(4, usuario.getBiografia());
            statement.setInt(5, usuario.getId());
            statement.executeUpdate();
        }
    }

    public void atualizarConta(Usuario usuario, String novaSenha) throws SQLException {
        boolean alterarSenha = novaSenha != null && !novaSenha.isEmpty();
        String sql = "UPDATE usuarios SET nome = ?, login = ?, caminho_foto_perfil = ?, "
            + "biografia = ?" + (alterarSenha ? ", senha = ?" : "") + " WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            statement.setString(parameter++, usuario.getNome());
            statement.setString(parameter++, usuario.getEmail().trim());
            statement.setString(parameter++, usuario.getFotoPerfil());
            statement.setString(parameter++, usuario.getBiografia());
            if (alterarSenha) {
                statement.setString(parameter++, SegurancaSenha.gerarHash(novaSenha));
            }
            statement.setInt(parameter, usuario.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (PostgresSupport.duplicidade(exception)) {
                throw new SQLException("Já existe uma conta com este e-mail.", exception);
            }
            throw exception;
        }
    }

    public void excluir(int id) throws SQLException {
        try (Connection connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
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
                execute(connection, "DELETE FROM fotos_hashtags WHERE id_foto "
                    + "IN (SELECT id FROM fotos WHERE id_usuario = ?)", id);
                execute(connection, "DELETE FROM fotos WHERE id_usuario = ?", id);
                execute(connection, "DELETE FROM usuarios WHERE id = ?", id);
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

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> result = new ArrayList<>();
        String sql = "SELECT " + COLUNAS_USUARIO + " FROM usuarios ORDER BY id";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(PostgresSupport.usuario(rows));
            }
        }
        return result;
    }
}
