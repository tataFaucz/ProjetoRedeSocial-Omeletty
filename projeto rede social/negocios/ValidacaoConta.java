package negocios;

public final class ValidacaoConta {
    private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private ValidacaoConta() {
    }

    public static String validar(String nome, String email, String biografia,
                                  String fotoPerfil, String senha, boolean senhaObrigatoria) {
        if (nome == null || nome.trim().isEmpty() || nome.trim().length() > 100) {
            return "O nome deve ter entre 1 e 100 caracteres.";
        }
        if (email == null || email.trim().length() > 150 || !EMAIL.matcher(email.trim()).matches()) {
            return "Informe um e-mail válido com até 150 caracteres.";
        }
        if (biografia != null && biografia.length() > 500) {
            return "A biografia pode ter no máximo 500 caracteres.";
        }
        if (fotoPerfil != null && fotoPerfil.length() > 255) {
            return "O caminho da foto de perfil pode ter no máximo 255 caracteres.";
        }
        if (senhaObrigatoria || (senha != null && !senha.isEmpty())) {
            if (senha == null || senha.length() < 8 || senha.length() > 128) {
                return "A senha deve ter entre 8 e 128 caracteres.";
            }
        }
        return null;
    }
}