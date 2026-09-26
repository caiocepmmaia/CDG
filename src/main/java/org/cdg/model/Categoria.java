package org.cdg.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Categoria {
    private int idCategoria;
    private String nome;

    @Override
    public String toString() {
        return nome; // Permite que o nome apareça corretamente nas caixas de seleção
    }
}