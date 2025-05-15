package br.fecap.pi.uberalert;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
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

import br.fecap.pi.uberalert.modelos.Alerta;
import br.fecap.pi.uberalert.network.ApiClient;
import br.fecap.pi.uberalert.network.ApiService;
import br.fecap.pi.uberalert.network.Criptografia;
import br.fecap.pi.uberalert.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Alertas extends AppCompatActivity {

    private String nomeUsuarioAtual;
    private FusedLocationProviderClient fusedLocationClient;
    private String latitude;
    private String longitude;
    private String tipoAlerta;
    private RadioButton rbCelular, rbCarro, rbEnchente, rbTransito, rbOutro;
    private EditText outroText;
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
        rbTransito = findViewById(R.id.rbTransito);
        rbEnchente = findViewById(R.id.rbEnchente);
        rbOutro = findViewById(R.id.rbOutro);
        outroText = findViewById(R.id.editTextAlertaOutro);

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
                                latitude = String.valueOf(location.getLatitude());
                                longitude = String.valueOf(location.getLongitude());
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
        if(rbEnchente.isChecked()){
            tipoAlerta = "Risco de enchente";
        }
        if (rbTransito.isChecked()){
            tipoAlerta = "Trânsito";
        }
        if (rbOutro.isChecked()){
            tipoAlerta = outroText.getText().toString();
        }
    }

    private void EmitirAlerta(){
        String longitudeCript = Criptografia.Criptografar(longitude, tipoAlerta);
        String latitudeCript = Criptografia.Criptografar(latitude, tipoAlerta);
        Alerta novoAlerta = new Alerta(nomeUsuarioAtual, longitudeCript, latitudeCript, tipoAlerta);
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