package dao;

import dados.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

final class PostgresSupport {
    private PostgresSupport() {
    }

    static void requireExists(String table, int id, String entity) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException(entity + " informado não existe.");
                }
            }
        }
    }

    static Usuario usuario(ResultSet result) throws SQLException {
        return new Usuario(
            result.getInt("id"),
            result.getString("nome"),
            result.getString("login"),
            null,
            result.getString("caminho_foto_perfil"),
            result.getString("biografia")
        );
    }

    static Timestamp timestamp(ResultSet result, String column) throws SQLException {
        return result.getTimestamp(column);
    }

    static boolean duplicidade(SQLException exception) {
        return "23505".equals(exception.getSQLState());
    }
}
