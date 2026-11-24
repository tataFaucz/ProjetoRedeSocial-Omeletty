package dao;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

public final class ConnectionFactory {
    private static final String URI = configuracao("MONGODB_URI", "NOSQL_URI",
        "mongodb://localhost:27017");
    private static final String DATABASE_NAME = configuracao("MONGODB_DATABASE", "NOSQL_DATABASE",
        "rede_social_nosql");
    private static final MongoClient CLIENT = MongoClients.create(
        MongoClientSettings.builder().applyConnectionString(new ConnectionString(URI)).build());
    private static final MongoDatabase DATABASE = CLIENT.getDatabase(DATABASE_NAME);
    private static volatile boolean indexesReady;

    private ConnectionFactory() {
    }

    public static MongoDatabase getDatabase() {
        if (!indexesReady) {
            synchronized (ConnectionFactory.class) {
                if (!indexesReady) {
                    criarIndices();
                    indexesReady = true;
                }
            }
        }
        return DATABASE;
    }

    public static MongoCollection<Document> getCollection(String name) {
        return getDatabase().getCollection(name);
    }

    private static void criarIndices() {
        indiceUnico("usuarios", "id");
        indiceUnico("usuarios", "login");
        indiceUnico("fotos", "id");
        DATABASE.getCollection("fotos").createIndex(
            Indexes.compoundIndex(Indexes.ascending("id_usuario"), Indexes.descending("data_postagem")));
        DATABASE.getCollection("fotos").createIndex(Indexes.ascending("hashtagIds"));
        indiceUnico("comentarios", "id");
        DATABASE.getCollection("comentarios").createIndex(
            Indexes.compoundIndex(Indexes.ascending("id_foto"), Indexes.ascending("data_comentario")));
        indiceUnico("mensagens", "id");
        DATABASE.getCollection("mensagens").createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("id_remetente"),
                Indexes.ascending("id_destinatario"),
                Indexes.ascending("data_envio")));
        indiceCompostoUnico("curtidas", "id_usuario", "id_foto");
        indiceCompostoUnico("compartilhamentos", "id_usuario", "id_foto");
        indiceCompostoUnico("seguidores", "id_seguidor", "id_seguido");
        DATABASE.getCollection("seguidores").createIndex(Indexes.ascending("id_seguido"));
        indiceUnico("hashtags", "id");
        indiceUnico("hashtags", "texto");
    }

    private static void indiceUnico(String collection, String field) {
        DATABASE.getCollection(collection).createIndex(
            Indexes.ascending(field), new IndexOptions().unique(true));
    }

    private static void indiceCompostoUnico(String collection, String first, String second) {
        DATABASE.getCollection(collection).createIndex(
            Indexes.compoundIndex(Indexes.ascending(first), Indexes.ascending(second)),
            new IndexOptions().unique(true));
    }

    private static String configuracao(String principal, String alternativa, String padrao) {
        String valor = configuracao(principal);
        if (valor == null || valor.isBlank()) {
            valor = configuracao(alternativa);
        }
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    private static String configuracao(String nome) {
        String valor = System.getProperty(nome);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(nome);
        }
        return valor;
    }
}
