package org.cdg.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Cartao {
    private int idCartao;
    private String nome;
    private int diaVencimento;

    // Este método é importante para quando o cartão aparecer na caixinha de seleção (ComboBox)
    // do formulário de transações mais tarde, ele mostrar o nome e não o endereço de memória.
    @Override
    public String toString() {
        return nome + " (Vence dia " + diaVencimento + ")";
    }
}