package negocios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class SegurancaSenha {
    private static final int ITERACOES = 120000;
    private static final int TAMANHO_SALT = 16;
    private static final int TAMANHO_HASH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SegurancaSenha() {
    }

    public static String gerarHash(String senha) {
        byte[] salt = new byte[TAMANHO_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(senha.toCharArray(), salt, ITERACOES);
        return "pbkdf2-sha256$" + ITERACOES + "$" + Base64.getEncoder().encodeToString(salt) + "$" +
            Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificar(String senha, String armazenada) {
        if (senha == null || armazenada == null) {
            return false;
        }
        if (!armazenada.startsWith("pbkdf2-sha256$")) {
            return MessageDigest.isEqual(senha.getBytes(StandardCharsets.UTF_8),
                armazenada.getBytes(StandardCharsets.UTF_8));
        }

        try {
            String[] partes = armazenada.split("\\$");
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] hashArmazenado = Base64.getDecoder().decode(partes[3]);
            byte[] hashCalculado = derivar(senha.toCharArray(), salt, iteracoes);
            return MessageDigest.isEqual(hashArmazenado, hashCalculado);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static boolean jaEstaProtegida(String senhaArmazenada) {
        return senhaArmazenada != null && senhaArmazenada.startsWith("pbkdf2-sha256$");
    }

    private static byte[] derivar(char[] senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, TAMANHO_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível proteger a senha.", ex);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(senha, '\0');
        }
    }
}