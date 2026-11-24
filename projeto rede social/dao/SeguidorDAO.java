package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SeguidorDAO {
    public void seguir(int idSeguidor, int idSeguido) throws SQLException {
        if (idSeguidor == idSeguido) {
            throw new SQLException("Um usuário não pode seguir a si mesmo.");
        }
        PostgresSupport.requireExists("usuarios", idSeguidor, "Seguidor");
        PostgresSupport.requireExists("usuarios", idSeguido, "Usuário seguido");
        String sql = "INSERT INTO seguidores (id_seguidor, id_seguido) VALUES (?, ?) "
            + "ON CONFLICT (id_seguidor, id_seguido) DO NOTHING";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idSeguidor);
            statement.setInt(2, idSeguido);
            statement.executeUpdate();
        }
    }

    public void deixarDeSeguir(int idSeguidor, int idSeguido) throws SQLException {
        String sql = "DELETE FROM seguidores WHERE id_seguidor = ? AND id_seguido = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idSeguidor);
            statement.setInt(2, idSeguido);
            statement.executeUpdate();
        }
    }

    public List<Integer> listarSeguidores(int idSeguido) throws SQLException {
        return listarIds("id_seguidor", idSeguido, "id_seguido");
    }

    public List<Integer> listarSeguindo(int idSeguidor) throws SQLException {
        return listarIds("id_seguido", idSeguidor, "id_seguidor");
    }

    private List<Integer> listarIds(String resultColumn, int filterId, String filterColumn)
            throws SQLException {
        List<Integer> result = new ArrayList<>();
        String sql = "SELECT " + resultColumn + " FROM seguidores WHERE " + filterColumn + " = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, filterId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(rows.getInt(resultColumn));
                }
            }
        }
        return result;
    }
}
