package dao;

import dados.Foto;
import dados.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FotoDAO {
    public void inserir(Foto foto, int idUsuario) throws SQLException {
        PostgresSupport.requireExists("usuarios", idUsuario, "Usuário");
        String sql = "INSERT INTO fotos (id_usuario, legenda, caminho_arquivo) VALUES (?, ?, ?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql,
                 PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, idUsuario);
            statement.setString(2, foto.getLegenda());
            statement.setString(3, foto.getCaminhoArquivo());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    foto.setId(keys.getInt(1));
                }
            }
        }
    }

    public List<Foto> listarPorUsuario(int idUsuario) throws SQLException {
        List<Foto> result = new ArrayList<>();
        String sql = "SELECT id, legenda, caminho_arquivo, data_postagem "
            + "FROM fotos WHERE id_usuario = ? ORDER BY data_postagem DESC";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(mapear(rows));
                }
            }
        }
        return result;
    }

    public List<Foto> listarFeed(int idUsuario) throws SQLException {
        List<Foto> result = new ArrayList<>();
        String sql = "SELECT f.id, f.legenda, f.caminho_arquivo, f.data_postagem, "
            + "u.id AS autor_id, u.nome AS autor_nome, u.login AS autor_login, "
            + "u.caminho_foto_perfil AS autor_foto, u.biografia AS autor_biografia "
            + "FROM fotos f JOIN usuarios u ON u.id = f.id_usuario "
            + "WHERE f.id_usuario = ? OR f.id_usuario IN "
            + "(SELECT id_seguido FROM seguidores WHERE id_seguidor = ?) "
            + "ORDER BY f.data_postagem DESC";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idUsuario);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Foto foto = mapear(rows);
                    foto.setAutor(new Usuario(
                        rows.getInt("autor_id"),
                        rows.getString("autor_nome"),
                        rows.getString("autor_login"),
                        null,
                        rows.getString("autor_foto"),
                        rows.getString("autor_biografia")
                    ));
                    result.add(foto);
                }
            }
        }
        return result;
    }

    public void excluir(int idFoto) throws SQLException {
        try (Connection connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                deleteByPhoto(connection, "comentarios", idFoto);
                deleteByPhoto(connection, "curtidas", idFoto);
                deleteByPhoto(connection, "compartilhamentos", idFoto);
                deleteByPhoto(connection, "fotos_hashtags", idFoto);
                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM fotos WHERE id = ?")) {
                    statement.setInt(1, idFoto);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void deleteByPhoto(Connection connection, String table, int idFoto) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "DELETE FROM " + table + " WHERE id_foto = ?")) {
            statement.setInt(1, idFoto);
            statement.executeUpdate();
        }
    }

    private Foto mapear(ResultSet result) throws SQLException {
        return new Foto(
            result.getInt("id"),
            result.getString("legenda"),
            result.getString("caminho_arquivo"),
            result.getTimestamp("data_postagem")
        );
    }
}
