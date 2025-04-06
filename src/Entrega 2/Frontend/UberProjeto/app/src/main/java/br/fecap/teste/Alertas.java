package br.fecap.teste;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.util.GeoPoint;

import br.fecap.teste.modelos.Alerta;
import br.fecap.teste.modelos.LogErros;
import br.fecap.teste.network.ApiClient;
import br.fecap.teste.network.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Alertas extends AppCompatActivity {

    private String nomeUsuarioAtual;
    private FusedLocationProviderClient fusedLocationClient;
    private Double latitude;
    private Double longitude;
    private String tipoAlerta;
    private RadioButton rbCelular;
    private RadioButton rbCarro;
    public ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alertas);

        apiService = ApiClient.getClient().create(ApiService.class);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Bundle bundle = getIntent().getExtras();

        nomeUsuarioAtual = bundle.getString("usuarioatual");

        rbCelular = findViewById(R.id.rbCelular);
        rbCarro = findViewById(R.id.rbCarro);

        Button btnEnviar = findViewById(R.id.emitirAlertaBtn);
        btnEnviar.setOnClickListener(view ->{
            tipoRadioGroup();
            getCurrentLocation();
            finish();

        });
        ImageView btnVoltar = findViewById(R.id.voltarT2Btn);
        btnVoltar.setOnClickListener(view ->{
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void getCurrentLocation() {
        //Verifica se o app tem permisão para pegar a localização atual e caso verdadeiro, pega a latitude e longitude do usuario
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                EmitirAlerta();
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void tipoRadioGroup(){
        if(rbCelular.isChecked()){
            tipoAlerta = "Roubo de Celular";
        }
        if (rbCarro.isChecked()){
            tipoAlerta = "Roubo de carro";
        }
    }

    private void EmitirAlerta(){
        Alerta novoAlerta = new Alerta(nomeUsuarioAtual, longitude, latitude, tipoAlerta);
        apiService.emitirAlerta(novoAlerta).enqueue(new Callback<Alerta>() {
            @Override
            public void onResponse(Call<Alerta> call, Response<Alerta> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(Alertas.this, "O alerta foi criado com sucesso!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("Alerta", "Falha ao conectar com o servidor");
                }
            }

            @Override
            public void onFailure(Call<Alerta> call, Throwable t) {
                Log.e("Alerta", "Erro: " + t.getMessage());
            }
        });

    }

}