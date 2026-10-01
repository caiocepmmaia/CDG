package org.cdg.service;

import org.cdg.model.Categoria;
import org.cdg.repository.CategoriaRepository;

import java.util.List;

public class CategoriaService {

    private final CategoriaRepository repo = new CategoriaRepository();

    public List<Categoria> listarTodas() {
        return repo.listarTodas();
    }

    public void salvar(Categoria categoria, String nome) throws Exception {
        // Regra de negócio: O nome não pode estar vazio
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria não pode estar vazio.");
        }

        if (categoria != null) {
            categoria.setNome(nome.trim());
            repo.atualizar(categoria);
        } else {
            Categoria nova = Categoria.builder().nome(nome.trim()).build();
            repo.salvar(nova);
        }
    }

    public void excluir(int idCategoria) throws Exception {
        repo.excluir(idCategoria);
    }
}