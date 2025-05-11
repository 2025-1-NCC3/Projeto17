package br.fecap.pi.uberalert.modelos;

import br.fecap.pi.uberalert.network.Criptografia;

public class Alerta {
    private String nomeUsuario;
    private String longitude;
    private String latitude;
    private String tipoAlerta;
    private String dataOcorrencia;

    public Alerta(String nomeUsuario, String longitude, String latitude, String tipoAlerta){
        this.nomeUsuario = nomeUsuario;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tipoAlerta = tipoAlerta;
    }

    public Alerta(String nomeUsuario, String longitude, String latitude, String tipoAlerta, String dataOcorrencia){
        this.nomeUsuario = nomeUsuario;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tipoAlerta = tipoAlerta;
        this.dataOcorrencia = dataOcorrencia;
    }

    public Alerta(String latitude, String longitude, String tipoAlerta){
        this.latitude = latitude;
        this.longitude = longitude;
        this.tipoAlerta = tipoAlerta;
    }

    //Getters e Setters
    public double getLongitude() {
        return Double.parseDouble(Criptografia.Descriptografar(longitude, tipoAlerta));
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public double getLatitude() {
        return Double.parseDouble(Criptografia.Descriptografar(latitude, tipoAlerta));
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(String tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }

    public String getDataOcorrencia() {
        return dataOcorrencia;
    }

    public void setDataOcorrencia(String dataOcorrencia) {
        this.dataOcorrencia = dataOcorrencia;
    }
}
