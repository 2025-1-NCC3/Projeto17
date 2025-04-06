package br.fecap.teste.modelos;

public class Alerta {
    private String nomeUsuario;
    private double longitude;
    private double latitude;
    private String tipoAlerta;

    public Alerta(String nomeUsuario, double longitude, double latitude, String tipoAlerta){
        this.nomeUsuario = nomeUsuario;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tipoAlerta = tipoAlerta;
    }

    public Alerta(double latitude, double longitude, String tipoAlerta){
        this.latitude = latitude;
        this.longitude = longitude;
        this.tipoAlerta = tipoAlerta;
    }

    //Getters e Setters
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getTipoAlerta() {
        return tipoAlerta;
    }

    public void setTipoAlerta(String tipoAlerta) {
        this.tipoAlerta = tipoAlerta;
    }
}
