package br.fecap.teste.modelos;

import br.fecap.teste.network.Criptografia;

public class usuario {

    private String nome;
    private String senha;
    private String email;
    private String celular;

    public usuario(String nome, String senha, String email, String celular) {
        this.email = email;
        this.nome = nome;
        this.senha = senha;
        this.celular =celular;
    }

    public usuario(String senha, String email){
        this.email = email;
        this.senha = senha;
    }



    // Getter e Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }
}

