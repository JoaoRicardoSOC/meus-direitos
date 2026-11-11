package br.com.fiap.meusdireitos.service.simulacao;

import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ComentarioPlanejado;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.ConfirmacaoPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.DenunciaPlanejada;
import br.com.fiap.meusdireitos.service.simulacao.PacoteCidade.VotoPlanejado;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transforma um {@link PacoteCidade} em comandos INSERT para o script .sql de carga inicial.
 * <p>
 * Os ids vêm das sequences e as chaves estrangeiras são resolvidas por subconsulta
 * (e-mail do morador; título + autor da denúncia), então o resultado pode ser colado em qualquer banco.
 */
class ExportadorSql {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    String exportar(PacoteCidade pacote, String cidade, String uf, String emailBase) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Conteúdo de demonstração (\"cidade viva\") de ").append(cidade).append('/').append(uf)
                .append("\n-- Gerado pela API Meus Direitos. Moradores simulados não fazem login.\n\n");

        for (int i = 0; i < pacote.moradores().size(); i++) {
            sql.append("INSERT INTO tb_usuario (cd_usuario, nm_usuario, ds_email, ds_senha_hash, nm_cidade, sg_uf, ")
                    .append("dt_cadastro, st_simulado) VALUES (seq_usuario.NEXTVAL, ")
                    .append(texto(pacote.moradores().get(i))).append(", ").append(texto(email(emailBase, i))).append(", ")
                    .append(texto(SimulacaoService.SEM_LOGIN)).append(", ").append(texto(cidade)).append(", ")
                    .append(texto(uf)).append(", ").append(data(pacote.denuncias().get(0).criadaEm().minusDays(3)))
                    .append(", 'S');\n");
        }
        sql.append('\n');

        for (DenunciaPlanejada d : pacote.denuncias()) {
            String autor = usuario(emailBase, d.autor());
            sql.append("INSERT INTO tb_denuncia (cd_denuncia, cd_usuario, ds_titulo, ds_denuncia, tp_categoria, ")
                    .append("st_denuncia, ds_endereco, nm_bairro, nm_cidade, sg_uf, ds_imagem_url, dt_criacao, ")
                    .append("dt_atualizacao) VALUES (seq_denuncia.NEXTVAL, ").append(autor).append(", ")
                    .append(texto(d.titulo())).append(", ").append(texto(d.descricao())).append(", ")
                    .append(texto(d.categoria().name())).append(", ").append(texto(d.status().name())).append(", ")
                    .append(texto(d.endereco())).append(", ").append(texto(d.bairro())).append(", ")
                    .append(texto(cidade)).append(", ").append(texto(uf)).append(", ").append(texto(d.imagemUrl()))
                    .append(", ").append(data(d.criadaEm())).append(", ").append(data(d.criadaEm())).append(");\n");

            String denuncia = "(SELECT cd_denuncia FROM tb_denuncia WHERE ds_titulo = " + texto(d.titulo())
                    + " AND cd_usuario = " + autor + ")";
            for (VotoPlanejado v : d.votos()) {
                sql.append("INSERT INTO tb_voto_denuncia (cd_usuario, cd_denuncia, tp_voto, dt_voto) VALUES (")
                        .append(usuario(emailBase, v.morador())).append(", ").append(denuncia).append(", ")
                        .append(texto(v.tipo().name())).append(", ").append(data(v.quando())).append(");\n");
            }
            for (ComentarioPlanejado c : d.comentarios()) {
                sql.append("INSERT INTO tb_comentario (cd_comentario, cd_denuncia, cd_usuario, ds_comentario, ")
                        .append("dt_criacao) VALUES (seq_comentario.NEXTVAL, ").append(denuncia).append(", ")
                        .append(usuario(emailBase, c.morador())).append(", ").append(texto(c.texto())).append(", ")
                        .append(data(c.quando())).append(");\n");
            }
            for (ConfirmacaoPlanejada c : d.confirmacoes()) {
                sql.append("INSERT INTO tb_confirmacao_resolucao (cd_usuario, cd_denuncia, dt_confirmacao) VALUES (")
                        .append(usuario(emailBase, c.morador())).append(", ").append(denuncia).append(", ")
                        .append(data(c.quando())).append(");\n");
            }
            sql.append('\n');
        }
        return sql.append("COMMIT;\n").toString();
    }

    private static String email(String emailBase, int posicao) {
        return emailBase.replace("{n}", String.valueOf(posicao + 1));
    }

    private static String usuario(String emailBase, int posicao) {
        return "(SELECT cd_usuario FROM tb_usuario WHERE ds_email = " + texto(email(emailBase, posicao)) + ")";
    }

    /** Texto entre aspas simples, com aspas internas duplicadas (escape do SQL). */
    private static String texto(String valor) {
        return valor == null ? "NULL" : "'" + valor.replace("'", "''") + "'";
    }

    private static String data(LocalDateTime valor) {
        return "TO_DATE('" + valor.format(DATA) + "', 'YYYY-MM-DD HH24:MI:SS')";
    }
}
