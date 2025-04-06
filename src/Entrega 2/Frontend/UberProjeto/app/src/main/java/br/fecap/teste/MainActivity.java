package br.fecap.teste;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.animation.ObjectAnimator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import br.fecap.teste.modelos.Alerta;
import br.fecap.teste.modelos.GeoJson;
import br.fecap.teste.modelos.LogErros;
import br.fecap.teste.modelos.usuario;
import br.fecap.teste.network.ApiClient;
import br.fecap.teste.network.ApiService;
import br.fecap.teste.network.Criptografia;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    public ApiService apiService;
    private usuario usuarioAtual;
    private static final String ORS_API_KEY = "5b3ce3597851110001cf6248df9d8db9188a4b97ab8ff08477a8797d";
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint myLocation;
    private TextView alertaPopUpText;
    private CardView alertaPopUp;
    private TextView btnFecharAlertaPopUp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apiService = ApiClient.getClient().create(ApiService.class);

        //Pegar a localização atual do usuario
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getCurrentLocation();

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

        BuscarAlerta();


        // Carrega o geoJson
        String geoJson = loadGeoJsonFromAssets();
        parseGeoJson(geoJson);

        // Pede as permições necessarias
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        });
        //Essa chamada garante que a conexão com o backend esteja funcionando corretamente evitando erros como timeout
        callRootApi();

        //btn Listeners
        ImageView btnAlerta = findViewById(R.id.alertBtn);
        btnAlerta.setOnClickListener(view ->{
            Intent intent = new Intent(this, Alertas.class);
            intent.putExtra("usuarioatual", usuarioAtual.getNome());
            startActivity(intent);
        });

        //Mensagem pop up alerta
        alertaPopUp = findViewById(R.id.alertaInformacao);
        alertaPopUpText = findViewById(R.id.textAlertaTipo);
        btnFecharAlertaPopUp = findViewById(R.id.btnFecharAlertaPopUp);

        btnFecharAlertaPopUp.setOnClickListener(view->{
            alertaPopUp.setVisibility(View.GONE);
        });

        //Botões do SideMenu
        CardView sideMenu = findViewById(R.id.sideMenuBack);
        ImageView btnSideHome = findViewById(R.id.btnSideHome);
        ImageView btnSideAlertas = findViewById(R.id.btnSideAlertas);
        ImageView btnSideMeuPerfil = findViewById(R.id.btnSideMeuPerfil);
        ImageView btnConfiguracao = findViewById(R.id.btnSideConfig);

        btnSideAlertas.setOnClickListener(view ->{
            if(usuarioAtual != null){
                Intent intent = new Intent(this, Alertas.class);
                intent.putExtra("usuarioatual", usuarioAtual.getNome());
                startActivity(intent);
                sideMenu.setVisibility(View.GONE);
            }else {
                Toast.makeText(MainActivity.this, "Você não está logado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callRootApi() {
        apiService.getRoot().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("MainActivity", "Sucesso!!");
                } else {
                    Log.e("MainActivity", "Falha ao conectar com o servidor");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MainActivity", "Erro: " + t.getMessage());
            }
        });
    }

    public void buscarLocal(View view){
        //Busca pelo local que o usuario digitou
        EditText destinationInput = findViewById(R.id.textDestino);
        String destination = destinationInput.getText().toString();
        if (!destination.isEmpty()) {
            GeoPoint destinationPoint = getLocationFromAddress(destination);
            if (destinationPoint != null) {
                drawRoute(myLocation, destinationPoint);
            } else {
                Toast.makeText(MainActivity.this, "Endereço invalido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private GeoPoint getLocationFromAddress(String address) {
        //Busca o endereço que o usuario digitou e pega o primeiro
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (!addresses.isEmpty()) {
                Address location = addresses.get(0);
                return new GeoPoint(location.getLatitude(), location.getLongitude());
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Erro no getLocation", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return null;
    }

    private void getCurrentLocation() {
        //Verifica se o app tem permisão para pegar a localização atual e caso verdadeiro, pega a latitude e longitude do usuario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                myLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                                map.getController().setCenter(myLocation);
                                map.getController().setZoom(15);
                                addMarkerToMap(myLocation.getLatitude(), myLocation.getLongitude(), "My Location");
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }}

    private void drawRoute(GeoPoint start, GeoPoint end) {
        //Logica para desenhar a rota da localização atual do usuario até o destino especificado
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlString = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=" + ORS_API_KEY +
                            "&start=" + start.getLongitude() + "," + start.getLatitude() +
                            "&end=" + end.getLongitude() + "," + end.getLatitude();

                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Error: HTTP " + responseCode, Toast.LENGTH_LONG).show()
                        );
                        return;
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());

                    // Garante que a resposta tem uma rota valida
                    if (!jsonResponse.has("features") || jsonResponse.getJSONArray("features").length() == 0) {
                        Log.e("API_RESPONSE", "Response: " + jsonResponse.toString());
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Nenhuma rota encontrada" + jsonResponse.toString(), Toast.LENGTH_LONG).show()
                        );
                        return;
                    }

                    JSONArray coordinates = jsonResponse.getJSONArray("features")
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONArray("coordinates");

                    List<GeoPoint> routePoints = new ArrayList<>();
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray point = coordinates.getJSONArray(i);
                        double lon = point.getDouble(0);
                        double lat = point.getDouble(1);
                        routePoints.add(new GeoPoint(lat, lon));
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Polyline line = new Polyline();
                            line.setPoints(routePoints);
                            line.setColor(0x800000FF);
                            line.setWidth(10);
                            map.getOverlays().add(line);
                            map.invalidate();
                            Toast.makeText(MainActivity.this, "Rota desenhada com sucesso", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Erro de Network: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Erro no Json " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }


    private String loadGeoJsonFromAssets() {
        //Carrega os dados do geoJson do arquivo map.geojson na pasta assets, este arquivo é responsavel pelo mapeamento das áreas de risco em São Paulo
        String geoJson = "";
        try {
            InputStream inputStream = getAssets().open("map.geojson");
            geoJson = convertStreamToString(inputStream);
            if (geoJson.isEmpty()) {
                Log.e("GeoJson", "O GeoJson está vazio.");
                Toast.makeText(this, "O GeoJson está vazio.", Toast.LENGTH_LONG).show();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GeoJson", "Erro ao carregar o arquivo geoJson: " + e.getMessage());
            Toast.makeText(this, "Erro ao carregar o arquivo geoJson: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return geoJson;
    }



    private String convertStreamToString(InputStream is) {
        //Converte os dados do geojson para string
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void parseGeoJson(String geoJson) {
        //Carrega e passa as informações do geoJson para desenhar as áreas de risco no mapa
        try {
            Gson gson = new Gson();
            GeoJson geoJsonObject = gson.fromJson(geoJson, GeoJson.class);

            if (geoJsonObject == null || geoJsonObject.features == null || geoJsonObject.features.isEmpty()) {
                Log.e("GeoJson", "Erro ao carregar informações do GeoJson ou lista de features está vazia");
                Toast.makeText(this, "Erro ao carregar informações do GeoJson ou lista de features está vazia", Toast.LENGTH_LONG).show();
                return;
            }

            for (GeoJson.Feature feature : geoJsonObject.features) {
                if (feature == null || feature.getGeometry() == null) {
                    Log.e("GeoJson", "Feature ou geometria está vazia");
                    Toast.makeText(this, "Feature ou geometria está vazia", Toast.LENGTH_LONG).show();
                    continue; // Pula features invalidas
                }

                GeoJson.Geometry geometry = feature.getGeometry();

                // Lida com pontos no mapa
                if ("Point".equals(geometry.getType())) {
                    if (geometry.getCoordinates() instanceof List<?>) {
                        List<?> coordinates = (List<?>) geometry.getCoordinates();
                        if (coordinates.size() == 2) {
                            double longitude = ((Double) coordinates.get(0));
                            double latitude = ((Double) coordinates.get(1));
                            addMarkerToMap(latitude, longitude, feature.getProperties().getName());
                        } else {
                            Log.e("GeoJson", "Coordenadas do ponto invalidas");
                            Toast.makeText(this, "Coordenadas do ponto invalidas", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e("GeoJson", "Coordenadas do ponto não são uma lista");
                        Toast.makeText(this, "Coordenadas do ponto não são uma lista", Toast.LENGTH_LONG).show();
                    }
                }
                // Lida com a geometria dos poligonos
                else if ("Polygon".equals(geometry.getType())) {
                    if (geometry.getCoordinates() instanceof List<?>) {
                        List<?> coordinatesList = (List<?>) geometry.getCoordinates();

                        for (Object coordinatesObj : coordinatesList) {
                            if (coordinatesObj instanceof List<?>) {
                                List<?> coordinates = (List<?>) coordinatesObj;

                                // Garante que existe ao menos três pontos para formar um poligono
                                if (coordinates.size() >= 3) {
                                    // Cria uma lista de GeoPoints para o poligono
                                    List<GeoPoint> geoPoints = new ArrayList<>();
                                    for (Object coordinate : coordinates) {
                                        if (coordinate instanceof List<?>) {
                                            List<?> coordinatePair = (List<?>) coordinate;
                                            if (coordinatePair.size() == 2) {
                                                double longitude = ((Double) coordinatePair.get(0));
                                                double latitude = ((Double) coordinatePair.get(1));
                                                geoPoints.add(new GeoPoint(latitude, longitude));
                                            }
                                        }
                                    }

                                    // Cria e adiciona o poligono no mapa
                                    if (geoPoints.size() > 2) {
                                        Polygon polygon = new Polygon();
                                        polygon.setPoints(geoPoints);
                                        polygon.setFillColor(0x5000FF00);
                                        polygon.setStrokeColor(0xFF0000FF);
                                        polygon.setStrokeWidth(2);
                                        map.getOverlays().add(polygon);
                                    } else {
                                        Log.e("GeoJson", "Coordenadas do poligono estão incompletas.");
                                        Toast.makeText(this, "Coordenadas do poligono estão incompletas.", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Log.e("GeoJson", "Coordenadas do poligono tem menos de três pontos");
                                    Toast.makeText(this, "Coordenadas do poligono tem menos de três pontos.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.e("GeoJson", "Coodernadas do poligono invalidas");
                                Toast.makeText(this, "Coodernadas do poligono invalidas", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Log.e("GeoJson", "Coodernadas do poligono não são uma lista");
                        Toast.makeText(this, "Coodernadas do poligono não são uma lista", Toast.LENGTH_LONG).show();
                    }
                }else if ("MultiPolygon".equals(geometry.getType())) {
                    if (geometry.getCoordinates() instanceof List<?>) {
                        List<?> multipolygonCoordinates = (List<?>) geometry.getCoordinates();

                        for (Object polygonObj : multipolygonCoordinates) {
                            if (polygonObj instanceof List<?>) {
                                List<?> polygonCoordinates = (List<?>) polygonObj;

                                for (Object coordinatesObj : polygonCoordinates) {
                                    if (coordinatesObj instanceof List<?>) {
                                        List<?> coordinates = (List<?>) coordinatesObj;

                                        if (coordinates.size() >= 3) {
                                            List<GeoPoint> geoPoints = new ArrayList<>();
                                            for (Object coordinate : coordinates) {
                                                if (coordinate instanceof List<?>) {
                                                    List<?> coordinatePair = (List<?>) coordinate;
                                                    if (coordinatePair.size() == 2) {
                                                        double longitude = ((Double) coordinatePair.get(0));
                                                        double latitude = ((Double) coordinatePair.get(1));
                                                        geoPoints.add(new GeoPoint(latitude, longitude));
                                                    }
                                                }
                                            }

                                            // Create and add the polygon to the map
                                            if (geoPoints.size() > 2) {
                                                Polygon polygon = new Polygon();
                                                polygon.setPoints(geoPoints);
                                                polygon.setFillColor(0x5000FF00);
                                                polygon.setStrokeColor(0xFF0000FF);
                                                polygon.setStrokeWidth(2);
                                                map.getOverlays().add(polygon);
                                            } else {
                                                Log.e("GeoJson", "Polygon coordinates are incomplete.");
                                                Toast.makeText(this, "Polygon coordinates are incomplete.", Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            Log.e("GeoJson", "Polygon coordinates have less than three points.");
                                            Toast.makeText(this, "Polygon coordinates have less than three points.", Toast.LENGTH_LONG).show();
                                        }
                                    } else {
                                        Log.e("GeoJson", "Invalid polygon coordinates");
                                        Toast.makeText(this, "Invalid polygon coordinates", Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                Log.e("GeoJson", "Invalid multipolygon structure");
                                Toast.makeText(this, "Invalid multipolygon structure", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        Log.e("GeoJson", "Multipolygon coordinates are not a list");
                        Toast.makeText(this, "Multipolygon coordinates are not a list", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Log.e("GeoJson", "Tipo de geometria invalida:" + geometry.getType());
                    Toast.makeText(this, "Tipo de geometria invalida: " + geometry.getType(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GeoJson", "Erro no GeoJson: " + e.getMessage());
            Toast.makeText(this, "Erro no GeoJson: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }




    private void addMarkerToMap(double latitude, double longitude, String title) {
        try {
            if (map == null) {
                Log.e("GeoJson", "Mapa não foi inicializado.");
                Toast.makeText(this, "Mapa não foi inicializado.", Toast.LENGTH_LONG).show();
                return;
            }

            GeoPoint point = new GeoPoint(latitude, longitude);
            OverlayItem overlayItem = new OverlayItem(title, "", point);

            List<OverlayItem> items = new ArrayList<>();
            items.add(overlayItem);

            ItemizedOverlayWithFocus<OverlayItem> overlay = new ItemizedOverlayWithFocus<>(this, items, new ItemizedOverlayWithFocus.OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(int index, OverlayItem item) {
                    String clickedTitle = item.getTitle();
                    alertaPopUp.setVisibility(View.VISIBLE);
                    alertaPopUpText.setText(clickedTitle);
                    return true;
                }

                @Override
                public boolean onItemLongPress(int index, OverlayItem item) {
                    return false;
                }
            });

            map.getOverlays().add(overlay);
            map.invalidate();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GeoJson", "Erro ao adicionar marcador ao mapa: " + e.getMessage());
            Toast.makeText(this, "Erro ao adicionar marcador ao mapa: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }





    @Override
    public void onResume() {
        super.onResume();
        //Atualiza o osmdroid onResume
        map.onResume();
        BuscarAlerta();
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
        CardView menuTop = findViewById(R.id.topMenuLayout);
        Button deletar = findViewById(R.id.deletarBtn);

        if (usuarioAtual != null){
                deletar.setVisibility(View.VISIBLE);
        }else{
            deletar.setVisibility(View.GONE);
        }

        credencias.setVisibility(View.GONE);
        menuPrincipal.setVisibility(View.GONE);
        menuTop.setVisibility(View.GONE);
        menuBack.setVisibility(View.VISIBLE);
    }

    public void voltarMenuBtn(View view){
        CardView credenciais = findViewById(R.id.layoutCredenciais);
        CardView menuBack = findViewById(R.id.sideMenuBack);
        CardView menuPrincipal = findViewById(R.id.mainMenu);
        CardView menuTop = findViewById(R.id.topMenuLayout);

        if (usuarioAtual == null){
            credenciais.setVisibility(View.VISIBLE);
        }else{
                menuPrincipal.setVisibility(View.VISIBLE);
        }
        menuTop.setVisibility(View.VISIBLE);
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
        Button cadastroBtn = findViewById(R.id.btnCadastrar2);

        //Criptografa a senha
        String senhaCriptografada = Criptografia.Criptografar(senhaText, emailText);

        CardView cadastroLayout = findViewById(R.id.cadastrarLayout);
        CardView menuPrincipal = findViewById(R.id.mainMenu);

        TextView sideBarTesteNome = findViewById(R.id.testeNomeUsuario);

        usuario user = new usuario(nomeText,senhaCriptografada,emailText,telefoneText);

        Call<Void> call = apiService.addUser(user);
        cadastroBtn.setText(getResources().getString(R.string.carregarBtn));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    cadastroBtn.setText(getResources().getString(R.string.botao_cadastro));
                    Toast.makeText(MainActivity.this, "Usuario Cadastrado!", Toast.LENGTH_SHORT).show();
                    usuarioAtual = new usuario(nomeText,"",emailText,telefoneText);

                    sideBarTesteNome.setText(usuarioAtual.getNome());
                    cadastroLayout.setVisibility(View.GONE);
                    menuPrincipal.setVisibility(View.VISIBLE);
                } else {
                    cadastroBtn.setText(getResources().getString(R.string.botao_cadastro));
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
                cadastroBtn.setText(getResources().getString(R.string.botao_cadastro));
                Toast.makeText(MainActivity.this, "Erro: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deletarUsuario(View view){
        TextView sideBarTesteNome = findViewById(R.id.testeNomeUsuario);
        Button deletarBtn = findViewById(R.id.deletarBtn);

        Call<usuario> call = apiService.deletarUser(usuarioAtual);
        call.enqueue(new Callback<usuario>() {
            @Override
            public void onResponse(Call<usuario> call, Response<usuario> response) {
                if (response.isSuccessful()) {
                    try {
                        usuarioAtual = null;
                        sideBarTesteNome.setText("");
                        deletarBtn.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Usuario deletado com sucesso!", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Erro ao processar a resposta", Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorMessage = response.errorBody() != null ? response.errorBody().string() : "Erro desconhecido";
                        Log.e("API_ERROR", "Erro ao deletar usuarios " + errorMessage);
                        Toast.makeText(MainActivity.this, "Erro ao deletar usuarios ", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Erro ao carregar informações", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<usuario> call, Throwable t) {
                // Log dos erros
                t.printStackTrace();
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
    });}

    public void loginUsuario(View view) {
        EditText email = findViewById(R.id.editEmailLogin);
        EditText senha = findViewById(R.id.editSenhaLogin);
        Button loginBtn = findViewById(R.id.loginSendBtn);

        TextView sideBarTesteNome = findViewById(R.id.testeNomeUsuario);
        CardView loginLayout = findViewById(R.id.layoutLogin);
        CardView menuPrincipal = findViewById(R.id.mainMenu);

        String emailText = email.getText().toString();
        String senhaText = senha.getText().toString();

        //Criptografa a senha
        String senhaCriptografada = Criptografia.Criptografar(senhaText, emailText);

        // Cria um objeto usuario
        usuario user = new usuario(senhaCriptografada, emailText);

        // Faz a chamada da API
        Call<usuario> call = apiService.validarUser(user);
        loginBtn.setText(getResources().getString(R.string.carregarBtn));
        call.enqueue(new Callback<usuario>() {
            @Override
            public void onResponse(Call<usuario> call, Response<usuario> response) {
                if (response.isSuccessful()) {
                    loginBtn.setText(getResources().getString(R.string.botao_entrar));
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
                    loginBtn.setText(getResources().getString(R.string.botao_entrar));
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
                loginBtn.setText(getResources().getString(R.string.botao_entrar));
                // Log dos erros
                t.printStackTrace();
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void BuscarAlerta() {
        apiService.buscarAlerta().enqueue(new Callback<List<Alerta>>() {
            @Override
            public void onResponse(Call<List<Alerta>> call, Response<List<Alerta>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<Alerta> alertas = response.body();
                        for (Alerta alerta : alertas) {
                            addMarkerToMap(alerta.getLatitude(), alerta.getLongitude(), alerta.getTipoAlerta());
                        }
                    } catch (Exception e) {
                        LogErros log = new LogErros(e);
                        e.printStackTrace();
                    }
                } else {
                    Log.e("API", "Response not successful or body is null");
                    Toast.makeText(MainActivity.this, "Error: Response body is null", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Alerta>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }




}
