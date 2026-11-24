package dao;

import dados.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CurtidaDAO {
    public void curtir(int idUsuario, int idFoto) throws SQLException {
        PostgresSupport.requireExists("usuarios", idUsuario, "Usuário");
        PostgresSupport.requireExists("fotos", idFoto, "Publicação");
        String sql = "INSERT INTO curtidas (id_usuario, id_foto) VALUES (?, ?) "
            + "ON CONFLICT (id_usuario, id_foto) DO NOTHING";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idFoto);
            statement.executeUpdate();
        }
    }

    public void descurtir(int idUsuario, int idFoto) throws SQLException {
        String sql = "DELETE FROM curtidas WHERE id_usuario = ? AND id_foto = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idFoto);
            statement.executeUpdate();
        }
    }

    public List<Usuario> listarUsuarios(int idFoto) throws SQLException {
        List<Usuario> result = new ArrayList<>();
        String sql = "SELECT u.id, u.nome, u.login, u.caminho_foto_perfil, u.biografia "
            + "FROM curtidas c JOIN usuarios u ON u.id = c.id_usuario WHERE c.id_foto = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idFoto);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(PostgresSupport.usuario(rows));
                }
            }
        }
        return result;
    }
}
