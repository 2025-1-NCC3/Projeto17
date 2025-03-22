package br.fecap.teste.network;

import java.util.List;

import br.fecap.teste.modelos.usuario;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    // Endpoint para pegar todos os usuarios
    @GET("tudo")
    Call<List<usuario>> getUsers();

    // Endpoint para adicionar usuarios
    @POST("usuario")
    Call<Void> addUser(@Body usuario user);

    //Endpoint para pegar um usuario
    @POST("login")
    Call<usuario> validarUser(@Body usuario user);

    //Endpoint para deletar um usuario
    @POST("deletar")
    Call<usuario> deletarUser(@Body usuario user);
}
