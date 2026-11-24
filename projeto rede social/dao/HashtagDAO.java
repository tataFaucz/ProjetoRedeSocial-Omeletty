package dao;

import dados.Foto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HashtagDAO {
    public List<String> listarPorFoto(int idFoto) throws SQLException {
        List<String> result = new ArrayList<>();
        String sql = "SELECT h.texto FROM fotos_hashtags fh "
            + "JOIN hashtags h ON h.id = fh.id_hashtag WHERE fh.id_foto = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idFoto);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(rows.getString("texto"));
                }
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
        for (String text : values) {
            PostgresSupport.requireExists("fotos", idFoto, "Publicação");
            int hashtagId = obterOuCriar(text);
            String sql = "INSERT INTO fotos_hashtags (id_foto, id_hashtag) VALUES (?, ?) "
                + "ON CONFLICT (id_foto, id_hashtag) DO NOTHING";
            try (Connection connection = ConnectionFactory.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, idFoto);
                statement.setInt(2, hashtagId);
                statement.executeUpdate();
            }
        }
    }

    private int obterOuCriar(String text) throws SQLException {
        String insert = "INSERT INTO hashtags (texto) VALUES (?) "
            + "ON CONFLICT (texto) DO NOTHING RETURNING id";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, text);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id");
                }
            }
        }
        String select = "SELECT id FROM hashtags WHERE texto = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, text);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id");
                }
            }
        }
        throw new SQLException("Não foi possível obter a hashtag criada.");
    }

    public List<Foto> listarFotosPorHashtag(String textoHashtag) throws SQLException {
        List<Foto> result = new ArrayList<>();
        String text = textoHashtag.trim().replaceFirst("^#+", "").toLowerCase(Locale.ROOT);
        String sql = "SELECT f.id, f.legenda, f.caminho_arquivo, f.data_postagem "
            + "FROM hashtags h JOIN fotos_hashtags fh ON fh.id_hashtag = h.id "
            + "JOIN fotos f ON f.id = fh.id_foto WHERE h.texto = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, text);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new Foto(
                        rows.getInt("id"),
                        rows.getString("legenda"),
                        rows.getString("caminho_arquivo"),
                        rows.getTimestamp("data_postagem")
                    ));
                }
            }
        }
        result.sort((left, right) ->
            right.getDataPostagem().compareTo(left.getDataPostagem()));
        return result;
    }
}
