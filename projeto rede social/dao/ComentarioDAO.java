package dao;

import dados.Comentario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ComentarioDAO {
    public void inserir(Comentario comentario, int idUsuario, int idFoto) throws SQLException {
        PostgresSupport.requireExists("usuarios", idUsuario, "Usuário");
        PostgresSupport.requireExists("fotos", idFoto, "Publicação");
        Timestamp timestamp = comentario.getTimestamp();
        if (timestamp == null) {
            timestamp = new Timestamp(System.currentTimeMillis());
        }
        String sql = "INSERT INTO comentarios "
            + "(id_usuario, id_foto, conteudo, data_comentario) VALUES (?, ?, ?, ?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idFoto);
            statement.setString(3, comentario.getMensagem());
            statement.setTimestamp(4, timestamp);
            statement.executeUpdate();
        }
    }

    public List<Comentario> listarPorFoto(int idFoto) throws SQLException {
        List<Comentario> result = new ArrayList<>();
        String sql = "SELECT c.id, c.conteudo, c.data_comentario, u.nome "
            + "FROM comentarios c LEFT JOIN usuarios u ON u.id = c.id_usuario "
            + "WHERE c.id_foto = ? ORDER BY c.data_comentario";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idFoto);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Timestamp timestamp = rows.getTimestamp("data_comentario");
                    LocalDateTime date = timestamp == null ? null
                        : LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault());
                    String author = rows.getString("nome");
                    result.add(new Comentario(
                        rows.getInt("id"),
                        author == null ? "" : author,
                        rows.getString("conteudo"),
                        date
                    ));
                }
            }
        }
        return result;
    }
}
