package br.com.fiap.meusdireitos.model.enums;

import org.junit.jupiter.api.Test;

import static br.com.fiap.meusdireitos.model.enums.StatusDenuncia.ABERTA;
import static br.com.fiap.meusdireitos.model.enums.StatusDenuncia.EM_ANDAMENTO;
import static br.com.fiap.meusdireitos.model.enums.StatusDenuncia.RESOLVIDA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusDenunciaTest {

    @Test
    void abertaPodeAndarOuSerResolvida() {
        assertTrue(ABERTA.podeIrPara(EM_ANDAMENTO));
        assertTrue(ABERTA.podeIrPara(RESOLVIDA));
    }

    @Test
    void emAndamentoPodeResolverOuVoltar() {
        assertTrue(EM_ANDAMENTO.podeIrPara(RESOLVIDA));
        assertTrue(EM_ANDAMENTO.podeIrPara(ABERTA));
    }

    @Test
    void resolvidaSoPodeSerReaberta() {
        assertTrue(RESOLVIDA.podeIrPara(ABERTA));
        assertFalse(RESOLVIDA.podeIrPara(EM_ANDAMENTO));
    }

    @Test
    void naoMudaParaOMesmoStatusNemParaNulo() {
        assertFalse(ABERTA.podeIrPara(ABERTA));
        assertFalse(ABERTA.podeIrPara(null));
    }
}
