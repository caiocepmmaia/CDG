package org.cdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cartao {
    private int idCartao;
    private String nome;
    private int diaVencimento;
    private int diaFechamento;

    @Override
    public String toString() {
        return nome; // <--- Isto força o ComboBox a mostrar apenas o nome limpo do cartão!
    }
}