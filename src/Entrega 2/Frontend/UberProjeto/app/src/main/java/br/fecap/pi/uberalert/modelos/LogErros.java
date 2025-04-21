package br.fecap.pi.uberalert.modelos;

import android.util.Log;

import br.fecap.pi.uberalert.network.ApiClient;
import br.fecap.pi.uberalert.network.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogErros {
    private String logErro;
    private ApiService apiService;

    public LogErros(Exception e){
        this.logErro = e.getMessage();
        CriarLogErros();
    }
    private void CriarLogErros(){
        apiService = ApiClient.getClient().create(ApiService.class);
        apiService.addErro(this).enqueue(new Callback<LogErros>() {
            @Override
            public void onResponse(Call<LogErros> call, Response<LogErros> response) {
                if (response.isSuccessful()) {
                    Log.e("Log", "Log foi criado!");
                } else {
                    Log.e("Log", "Falha ao conectar com o servidor");
                }
            }

            @Override
            public void onFailure(Call<LogErros> call, Throwable t) {
                Log.e("Log", "Erro: " + t.getMessage());
            }
        });
    }

    public String getErrosLog() {
        return logErro;
    }

    public void setErrosLog(String errosLog) {
        this.logErro = errosLog;
    }
}
