package br.fecap.teste;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.animation.ObjectAnimator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import br.fecap.teste.modelos.usuario;
import br.fecap.teste.network.ApiClient;
import br.fecap.teste.network.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    public ApiService apiService;
    private usuario usuarioAtual;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apiService = ApiClient.getClient().create(ApiService.class);

        // Carrega e inicia a configuração do osmdroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Cria o mapa
        setContentView(R.layout.activity_main);

        // Inicializa o map view
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Coloca a posição inicial do mapa no brasil
        map.getController().setZoom(5.5);
        GeoPoint brasil = new GeoPoint(-14.2350, -51.9253);
        map.getController().setCenter(brasil);

        // Pede as permições necessarias
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        });

        ;

    }

    @Override
    public void onResume() {
        super.onResume();
        //Atualiza o osmdroid onResume
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Salva as configurações na pausa
        map.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    public void entrarTelaBtn(View view){
        CardView credencias = findViewById(R.id.layoutCredenciais);
        CardView loginLayout = findViewById(R.id.layoutLogin);

        // Esconde o layout do card
        credencias.setVisibility(View.GONE);

        // Faz o layout do card de login aparecer
        loginLayout.setVisibility(View.VISIBLE);

        // Aplica animação ao card
        ObjectAnimator animator = ObjectAnimator.ofFloat(loginLayout, "translationY", 1000f, 0f);
        animator.setDuration(600);
        animator.start();
    }

    public void cadastroTelaBtn(View view){
        CardView credencias = findViewById(R.id.layoutCredenciais);
        CardView cadastroLayout = findViewById(R.id.cadastrarLayout);

        credencias.setVisibility(View.GONE);

        cadastroLayout.setVisibility(View.VISIBLE);

        ObjectAnimator animator = ObjectAnimator.ofFloat(cadastroLayout, "translationY", 1000f, 0f);
        animator.setDuration(600);
        animator.start();
    }

    public void sideMenuBtn(View view){
        CardView credencias = findViewById(R.id.layoutCredenciais);
        CardView menuBack = findViewById(R.id.sideMenuBack);
        CardView menuPrincipal = findViewById(R.id.mainMenu);

        credencias.setVisibility(View.GONE);
        menuPrincipal.setVisibility(View.GONE);
        menuBack.setVisibility(View.VISIBLE);
    }

    public void voltarMenuBtn(View view){
        CardView credencias = findViewById(R.id.layoutCredenciais);
        CardView menuBack = findViewById(R.id.sideMenuBack);
        CardView menuPrincipal = findViewById(R.id.mainMenu);

        if (usuarioAtual == null){
            credencias.setVisibility(View.VISIBLE);
        }else{
            menuPrincipal.setVisibility(View.VISIBLE);
        }

        menuBack.setVisibility(View.GONE);
    }

    public void sairCadastroBtn(View view){
        CardView credencias = findViewById(R.id.layoutCredenciais);
        CardView cadastroLayout = findViewById(R.id.cadastrarLayout);

        credencias.setVisibility(View.VISIBLE);
        cadastroLayout.setVisibility(View.GONE);
    }

    public void sairLoginBtn(View view){
        CardView credencias = findViewById(R.id.layoutCredenciais);
        CardView loginLayout = findViewById(R.id.layoutLogin);

        credencias.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.GONE);
    }

    public void cadastrarUsuario(View view){
        EditText nome = findViewById(R.id.nomeCadastroInput);
        String nomeText = nome.getText().toString();
        EditText email = findViewById(R.id.emailCadastroInput);
        String emailText = email.getText().toString();
        EditText telefone = findViewById(R.id.telefoneCadastroInput);
        String telefoneText = telefone.getText().toString();
        EditText senha = findViewById(R.id.senhaCadastroInput);
        String senhaText = senha.getText().toString();

        CardView cadastroLayout = findViewById(R.id.cadastrarLayout);
        CardView menuPrincipal = findViewById(R.id.mainMenu);

        TextView sideBarTesteNome = findViewById(R.id.testeNomeUsuario);

        usuario user = new usuario(nomeText,senhaText,emailText,telefoneText);

        Call<Void> call = apiService.addUser(user);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Usuario Cadastrado!", Toast.LENGTH_SHORT).show();
                    usuarioAtual = new usuario(nomeText,"",emailText,telefoneText);

                    sideBarTesteNome.setText(usuarioAtual.getNome());
                    cadastroLayout.setVisibility(View.GONE);
                    menuPrincipal.setVisibility(View.VISIBLE);
                } else {
                    //Loga o erro para mais detalhes
                    try {
                        String errorMessage = response.errorBody() != null ? response.errorBody().string() : "Erro desconhecido";
                        Log.e("API_ERROR", "Erro ao adicionar usuario: " + errorMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(MainActivity.this, "Erro ao adicionar usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Erro: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void testeTudo(View view){
        Call<List<usuario>> call = apiService.getUsers(); //Pega todos os usuarios do Backend
        call.enqueue(new Callback<List<usuario>>() {
            @Override
            public void onResponse(Call<List<usuario>> call, Response<List<usuario>> response) {
                if (response.isSuccessful()) {
                    // Pega usuarios da resposta
                    List<usuario> users = response.body();
                    if (users != null && !users.isEmpty()) {
                        // Mostra os usuarios para teste
                        StringBuilder usersInfo = new StringBuilder();
                        for (usuario user : users) {
                            usersInfo.append("Name: ").append(user.getNome()).append(", ")
                                    .append("Email: ").append(user.getEmail()).append("\n");
                        }
                        Toast.makeText(MainActivity.this, usersInfo.toString(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Nenhum usuario encontrado", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        //Log da mensagem de erro
                        String errorMessage = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e("API_ERROR", "Erro ao buscar usuarios: " + errorMessage);
                        Toast.makeText(MainActivity.this, "Erro ao buscar usuarios", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Erro ao ler o corpo da mensagem", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<usuario>> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(MainActivity.this, "Erro: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loginUsuario(View view) {
        EditText email = findViewById(R.id.editEmailLogin);
        EditText senha = findViewById(R.id.editSenhaLogin);

        TextView sideBarTesteNome = findViewById(R.id.testeNomeUsuario);
        CardView loginLayout = findViewById(R.id.layoutLogin);
        CardView menuPrincipal = findViewById(R.id.mainMenu);

        String emailText = email.getText().toString();
        String senhaText = senha.getText().toString();

        // Cria um objeto usuario
        usuario user = new usuario(senhaText, emailText);

        // Faz a chamada da API
        Call<usuario> call = apiService.validarUser(user);
        call.enqueue(new Callback<usuario>() {
            @Override
            public void onResponse(Call<usuario> call, Response<usuario> response) {
                if (response.isSuccessful()) {
                    // Pega os usuarios da resposta
                    usuario loggedInUser = response.body();
                    if (loggedInUser != null) {
                        // Extrai as informações e armazena em diferentes variaveis
                        String nome = loggedInUser.getNome();
                        String email = loggedInUser.getEmail();
                        String telefone = loggedInUser.getCelular();

                        usuarioAtual = new usuario(nome,"",email,telefone);

                        Toast.makeText(MainActivity.this, "Usuario logado com sucesso!!", Toast.LENGTH_SHORT).show();
                        loginLayout.setVisibility(View.GONE);
                        sideBarTesteNome.setText(usuarioAtual.getNome());
                        menuPrincipal.setVisibility(View.VISIBLE);

                    } else {
                        Toast.makeText(MainActivity.this, "Nenhum usuario encontrado", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        // Log dos erros
                        String errorMessage = response.errorBody() != null ? response.errorBody().string() : "Erro desconhecido";
                        Log.e("API_ERROR", "Erro ao buscar usuarios " + errorMessage);
                        Toast.makeText(MainActivity.this, "Erro ao buscar usuarios", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Erro ao carregar informações", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<usuario> call, Throwable t) {
                // Log dos erros
                t.printStackTrace();
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
