package org.cdg.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Titular {
    private Integer idTitular;
    private String nome;
    private boolean reembolsavel;
}