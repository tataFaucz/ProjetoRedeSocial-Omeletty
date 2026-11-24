const databaseName = process.env.MONGODB_DATABASE || "rede_social_nosql";
const appDb = db.getSiblingDB(databaseName);
const collections = [
    "usuarios",
    "fotos",
    "comentarios",
    "mensagens",
    "curtidas",
    "compartilhamentos",
    "seguidores",
    "hashtags",
    "counters"
];

const existingCollections = appDb.getCollectionNames();
collections.forEach((name) => {
    if (!existingCollections.includes(name)) {
        appDb.createCollection(name);
    }
});

appDb.usuarios.createIndex({ id: 1 }, { unique: true });
appDb.usuarios.createIndex({ login: 1 }, { unique: true });
appDb.fotos.createIndex({ id: 1 }, { unique: true });
appDb.fotos.createIndex({ id_usuario: 1, data_postagem: -1 });
appDb.fotos.createIndex({ hashtagIds: 1 });
appDb.comentarios.createIndex({ id: 1 }, { unique: true });
appDb.comentarios.createIndex({ id_foto: 1, data_comentario: 1 });
appDb.mensagens.createIndex({ id: 1 }, { unique: true });
appDb.mensagens.createIndex({ id_remetente: 1, id_destinatario: 1, data_envio: 1 });
appDb.curtidas.createIndex({ id_usuario: 1, id_foto: 1 }, { unique: true });
appDb.compartilhamentos.createIndex({ id_usuario: 1, id_foto: 1 }, { unique: true });
appDb.seguidores.createIndex({ id_seguidor: 1, id_seguido: 1 }, { unique: true });
appDb.seguidores.createIndex({ id_seguido: 1 });
appDb.hashtags.createIndex({ id: 1 }, { unique: true });
appDb.hashtags.createIndex({ texto: 1 }, { unique: true });

print(`Coleções e índices MongoDB prontos no banco '${databaseName}'.`);
