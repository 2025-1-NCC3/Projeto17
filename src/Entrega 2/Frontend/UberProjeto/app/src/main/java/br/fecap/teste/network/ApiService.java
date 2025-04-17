package br.fecap.teste.network;

import java.util.List;

import br.fecap.teste.Alertas;
import br.fecap.teste.modelos.Alerta;
import br.fecap.teste.modelos.LogErros;
import br.fecap.teste.modelos.usuario;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("logErro")
    Call<LogErros> addErro(@Body LogErros logErros);

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

    @GET("/")
    Call<Void> getRoot();

    @POST("emitirAlerta")
    Call<Alerta> emitirAlerta(@Body Alerta alerta);

    @GET("buscarAlerta")
    Call<List<Alerta>> buscarAlerta();

    @POST("mudarNome")
    Call<usuario> modificarNome(@Body usuario user);

    @POST("mudarTelefone")
    Call<usuario> modificarTelefone(@Body usuario user);

    @POST("mudarEmail")
    Call<usuario> modificarEmail(@Body usuario user);
}
