package br.fecap.pi.uberalert;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

import br.fecap.pi.uberalert.modelos.usuario;
import br.fecap.pi.uberalert.network.ApiClient;
import br.fecap.pi.uberalert.network.ApiService;
import br.fecap.pi.uberalert.network.Criptografia;
import br.fecap.pi.uberalert.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeuPerfil extends AppCompatActivity {

    private String usuarioNome;
    private String usuarioEmail;
    private String usuarioTelefone;
    private String usuarioSenha;
    private TextView textViewNome, textViewEmail, textViewTelefone;
    private ImageView btnEditarNome, btnEditarEmail, btnEditarTelefone, btnVoltar, btnFecharPerfil, btnVoltarDlt;
    private EditText editModificacao;
    private CardView modificarCard, deletarCard;
    private Button btnEnviarModificacao, btnDeletarCard, btnDeletarUsuario;
    public ApiService apiService;
    private String textNome;
    private String textEmail;
    private String textTelefone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meu_perfil);

        apiService = ApiClient.getClient().create(ApiService.class);

        Bundle bundle = getIntent().getExtras();
        usuarioNome = bundle.getString("usuarioatualnome");
        usuarioEmail = bundle.getString("usuarioatualemail");
        usuarioTelefone = bundle.getString("usuarioatualtelefone");
        usuarioSenha = bundle.getString("usuarioatualsenha");

        textViewNome = findViewById(R.id.textNomeUsuario);
        textViewEmail = findViewById(R.id.textEmailUsuario);
        textViewTelefone = findViewById(R.id.textTelefoneUsuario);
        btnEditarNome = findViewById(R.id.editarNome);
        btnEditarEmail = findViewById(R.id.editarEmail);
        btnEditarTelefone = findViewById(R.id.editarTelefone);
        modificarCard = findViewById(R.id.modificarCard);
        editModificacao = findViewById(R.id.editTextModificacao);
        btnEnviarModificacao = findViewById(R.id.btnEnviarModificacao);
        btnVoltar = findViewById(R.id.btnVoltar);
        btnFecharPerfil = findViewById(R.id.btnFecharPerfil);
        btnDeletarCard = findViewById(R.id.btnDeletarUsuario);
        btnDeletarUsuario = findViewById(R.id.btnDeletarDlt);
        deletarCard = findViewById(R.id.deletarCard);
        btnVoltarDlt = findViewById(R.id.btnVoltarDlt);

        textNome = textViewNome.getText().toString();
        textEmail = textViewEmail.getText().toString();
        textTelefone = textViewTelefone.getText().toString();

        textViewNome.setText(textNome + Criptografia.Descriptografar(usuarioNome, usuarioEmail));
        textViewEmail.setText(textEmail + Criptografia.Descriptografar(usuarioEmail,Criptografia.Descriptografar(usuarioSenha,usuarioEmail)));
        textViewTelefone.setText(textTelefone + Criptografia.Descriptografar(usuarioTelefone, usuarioEmail));

        btnEditarNome.setOnClickListener(view ->{
            EditarNomeCard();
        });

        btnEditarTelefone.setOnClickListener(view ->{
            EditarTelefoneCard();
        });

        btnEditarEmail.setOnClickListener(view ->{
            EditarEmailCard();
        });

        btnVoltar.setOnClickListener(view->{
            modificarCard.setVisibility(View.GONE);
        });

        btnFecharPerfil.setOnClickListener(view ->{
            finish();
        });

        btnDeletarCard.setOnClickListener(view ->{
            deletarCard.setVisibility(View.VISIBLE);
        });

        btnDeletarUsuario.setOnClickListener(view ->{
            DeletarUsuario();
        });

        btnVoltarDlt.setOnClickListener(view->{
            deletarCard.setVisibility(View.GONE);
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void EditarNomeCard(){
        modificarCard.setVisibility(View.VISIBLE);
        TextView modificarCardTitulo = findViewById(R.id.tituloCard);
        modificarCardTitulo.setText(getResources().getString(R.string.modifcarCardNomeT3));
        editModificacao.setHint(getResources().getString(R.string.hintModificarCardNomeT3));
        btnEnviarModificacao.setOnClickListener(view->{
            String novoNome = Criptografia.Criptografar(editModificacao.getText().toString(), usuarioEmail);
            usuario novoUsuario = new usuario(novoNome, usuarioSenha,usuarioEmail,usuarioTelefone);
            apiService.modificarNome(novoUsuario).enqueue(new Callback<usuario>() {
                @Override
                public void onResponse(Call<usuario> call, Response<usuario> response) {
                    if (response.isSuccessful()) {
                        MainActivity.usuarioAtual.setNome(novoNome);
                        usuarioNome = novoNome;
                        Toast.makeText(MeuPerfil.this, "O seu nome foi atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                        textViewNome.setText(textNome + Criptografia.Descriptografar(usuarioNome, usuarioEmail));
                        modificarCard.setVisibility(View.GONE);
                        editModificacao.setText("");
                    } else {
                        Log.e("Alerta", "Falha ao conectar com o servidor");
                    }
                }

                @Override
                public void onFailure(Call<usuario> call, Throwable t) {
                    Log.e("Alerta", "Erro: " + t.getMessage());
                }
            });
        });
    }

    private void EditarTelefoneCard(){
        modificarCard.setVisibility(View.VISIBLE);
        TextView modificarCardTitulo = findViewById(R.id.tituloCard);
        modificarCardTitulo.setText(getResources().getString(R.string.modificarCardTelefoneT3));
        editModificacao.setHint(getResources().getString(R.string.hintModificarCardTelefoneT3));
        editModificacao.setInputType(InputType.TYPE_CLASS_PHONE);
        btnEnviarModificacao.setOnClickListener(view->{
            String novoTelefone = Criptografia.Criptografar(editModificacao.getText().toString(), usuarioEmail);
            usuario novoUsuario = new usuario(usuarioNome, usuarioSenha, usuarioEmail, novoTelefone);
            apiService.modificarTelefone(novoUsuario).enqueue(new Callback<usuario>() {
                @Override
                public void onResponse(Call<usuario> call, Response<usuario> response) {
                    if (response.isSuccessful()) {
                        MainActivity.usuarioAtual.setCelular(novoTelefone);
                        usuarioTelefone = novoTelefone;
                        Toast.makeText(MeuPerfil.this, "O seu telefone foi atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                        textViewTelefone.setText(textTelefone + Criptografia.Descriptografar(usuarioTelefone, usuarioEmail));
                        modificarCard.setVisibility(View.GONE);
                        editModificacao.setInputType(InputType.TYPE_CLASS_TEXT);
                        editModificacao.setText("");
                    } else {
                        Log.e("Alerta", "Falha ao conectar com o servidor");
                    }
                }
                @Override
                public void onFailure(Call<usuario> call, Throwable t) {
                    Log.e("Alerta", "Erro: " + t.getMessage());
                }
            });
        });
    }

    private void EditarEmailCard(){
        modificarCard.setVisibility(View.VISIBLE);
        TextView modificarCardTitulo = findViewById(R.id.tituloCard);
        modificarCardTitulo.setText(getResources().getString(R.string.modificarCardEmail));
        editModificacao.setHint(getResources().getString(R.string.hintModificarCardEmailT3));
        btnEnviarModificacao.setOnClickListener(view->{
            String novoEmail = Criptografia.Criptografar(editModificacao.getText().toString(), Criptografia.Descriptografar(usuarioSenha, usuarioEmail));
            usuario novoUsuario = new usuario(novoEmail, usuarioSenha, usuarioEmail, usuarioTelefone);
            apiService.modificarEmail(novoUsuario).enqueue(new Callback<usuario>() {
                @Override
                public void onResponse(Call<usuario> call, Response<usuario> response) {
                    if (response.isSuccessful()) {
                        String novaSenha = Criptografia.Criptografar(Criptografia.Descriptografar(usuarioSenha, usuarioEmail), novoEmail);
                        String novoNome = Criptografia.Criptografar(Criptografia.Descriptografar(usuarioNome,usuarioEmail), novoEmail);
                        String novoTelefone = Criptografia.Criptografar(Criptografia.Descriptografar(usuarioTelefone,usuarioEmail),novoEmail);
                        usuarioEmail = novoEmail;
                        usuarioSenha = novaSenha;
                        usuarioNome = novoNome;
                        usuarioTelefone = novoTelefone;
                        Toast.makeText(MeuPerfil.this, "O seu email foi atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                        textViewEmail.setText(textEmail + Criptografia.Descriptografar(novoEmail, Criptografia.Descriptografar(usuarioSenha, usuarioEmail)));
                        MainActivity.usuarioAtual.setEmail(novoEmail);
                        MainActivity.usuarioAtual.setSenha(novaSenha);
                        MainActivity.usuarioAtual.setNome(novoNome);
                        MainActivity.usuarioAtual.setCelular(novoTelefone);
                        modificarCard.setVisibility(View.GONE);
                        editModificacao.setText("");
                    } else {
                        Log.e("Alerta", "Falha ao conectar com o servidor");
                    }
                }
                @Override
                public void onFailure(Call<usuario> call, Throwable t) {
                    Log.e("Alerta", "Erro: " + t.getMessage());
                }
            });
        });
    }

    private void DeletarUsuario(){
        Call<usuario> call = apiService.deletarUser(MainActivity.usuarioAtual);
        call.enqueue(new Callback<usuario>() {
            @Override
            public void onResponse(Call<usuario> call, Response<usuario> response) {
                if (response.isSuccessful()) {
                    try {
                        MainActivity.usuarioAtual = null;
                        Toast.makeText(MeuPerfil.this, "Usuario deletado com sucesso!", Toast.LENGTH_LONG).show();
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MeuPerfil.this, "Erro ao processar a resposta", Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorMessage = response.errorBody() != null ? response.errorBody().string() : "Erro desconhecido";
                        Log.e("API_ERROR", "Erro ao deletar usuarios " + errorMessage);
                        Toast.makeText(MeuPerfil.this, "Erro ao deletar usuarios ", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MeuPerfil.this, "Erro ao carregar informações", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<usuario> call, Throwable t) {
                // Log dos erros
                t.printStackTrace();
                Toast.makeText(MeuPerfil.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}