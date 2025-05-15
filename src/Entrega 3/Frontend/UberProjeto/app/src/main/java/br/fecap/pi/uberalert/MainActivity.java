package br.fecap.pi.uberalert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
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

import br.fecap.pi.uberalert.modelos.Alerta;
import br.fecap.pi.uberalert.modelos.GeoJson;
import br.fecap.pi.uberalert.modelos.LogErros;
import br.fecap.pi.uberalert.modelos.usuario;
import br.fecap.pi.uberalert.network.ApiClient;
import br.fecap.pi.uberalert.network.ApiService;
import br.fecap.pi.uberalert.network.Criptografia;
import br.fecap.pi.uberalert.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity{
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    public ApiService apiService;
    public static usuario usuarioAtual;
    private static final String ORS_API_KEY = "5b3ce3597851110001cf6248df9d8db9188a4b97ab8ff08477a8797d";
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint myLocation, antigaLocalizacao;
    private ItemizedOverlayWithFocus<OverlayItem> minhaLocalizacaoOverlayItem = null;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private TextView alertaPopUpText;
    private CardView alertaPopUp;
    private TextView btnFecharAlertaPopUp;
    private List<OverlayItem> muitoLonge , longe, perto;
    private Polyline previousLine;
    private GeoPoint destinationPoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        apiService = ApiClient.getClient().create(ApiService.class);

        //Pegar a localização atual do usuario
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
        //String geoJson = loadGeoJsonFromAssets();
        //parseGeoJson(geoJson);

        // Pede as permições necessarias

        //Essa chamada garante que a conexão com o backend esteja funcionando corretamente evitando erros como timeout
        callRootApi();

        //Botões do Menu Principal
        ImageView btnAlerta = findViewById(R.id.alertBtn);
        ImageView btnConfig = findViewById(R.id.configBtn);
        ImageView btnMeuPerfil = findViewById(R.id.perfilBtn);
        //btn Listerners
        btnAlerta.setOnClickListener(view ->{
            Intent intent = new Intent(this, Alertas.class);
            intent.putExtra("usuarioatual", usuarioAtual.getNome());
            startActivity(intent);
        });
        btnMeuPerfil.setOnClickListener(view ->{
            Intent intent = new Intent(this, MeuPerfil.class);
            intent.putExtra("usuarioatualnome", usuarioAtual.getNome());
            intent.putExtra("usuarioatualemail", usuarioAtual.getEmail());
            intent.putExtra("usuarioatualtelefone", usuarioAtual.getCelular());
            intent.putExtra("usuarioatualsenha", usuarioAtual.getSenha());
            startActivity(intent);
        });
        try {
            btnConfig.setOnClickListener(view ->{
                Intent intent = new Intent(this, Configuracoes.class);
                startActivity(intent);
            });
        } catch (Exception e) {
            LogErros log = new LogErros(e);
        }

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
        ImageView btnSideConfiguracao = findViewById(R.id.btnSideConfig);

        btnSideAlertas.setOnClickListener(view ->{
            if(usuarioAtual != null){
                Intent intent = new Intent(this, Alertas.class);
                intent.putExtra("usuarioatual", usuarioAtual.getNome());
                startActivity(intent);
            }else {
                Toast.makeText(MainActivity.this, "Você não está logado", Toast.LENGTH_SHORT).show();
            }
        });
        btnSideMeuPerfil.setOnClickListener(view->{
            if(usuarioAtual !=null){
                Intent intent = new Intent(this, MeuPerfil.class);
                intent.putExtra("usuarioatualnome", usuarioAtual.getNome());
                intent.putExtra("usuarioatualemail", usuarioAtual.getEmail());
                intent.putExtra("usuarioatualtelefone", usuarioAtual.getCelular());
                intent.putExtra("usuarioatualsenha", usuarioAtual.getSenha());
                startActivity(intent);
            }else{
                Toast.makeText(MainActivity.this, "Você não está logado", Toast.LENGTH_SHORT).show();
            }
        });
        try {
            btnSideConfiguracao.setOnClickListener(view->{
                Intent intent = new Intent(this, Configuracoes.class);
                startActivity(intent);
            });
        } catch (Exception e) {
            LogErros log = new LogErros(e);
        }

        ImageView btnCentralizar = findViewById(R.id.btnCentralizarLocal);
        btnCentralizar.setOnClickListener(view->{
            map.getController().setCenter(myLocation);
            map.getController().setZoom(10);
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
            destinationPoint = getLocationFromAddress(destination);
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
                                antigaLocalizacao = myLocation;
                                map.getController().setCenter(myLocation);
                                map.getController().setZoom(10);
                                addMarkerToMap(myLocation.getLatitude(), myLocation.getLongitude(), "Minha localização", "");
                                muitoLonge = new ArrayList<>();
                                longe = new ArrayList<>();
                                perto = new ArrayList<>();
                                    for (int i = 0; i < map.getOverlays().size(); i++) {
                                        Overlay overlay = map.getOverlays().get(i);

                                        // Check if the overlay is an instance of ItemizedOverlayWithFocus
                                        if (overlay instanceof ItemizedOverlayWithFocus) {
                                            ItemizedOverlayWithFocus<OverlayItem> itemizedOverlay = (ItemizedOverlayWithFocus<OverlayItem>) overlay;

                                            // Iterate through the items in the ItemizedOverlayWithFocus
                                            for (int j = 0; j < itemizedOverlay.size(); j++) {
                                                OverlayItem item = itemizedOverlay.getItem(j);
                                                GeoPoint point = (GeoPoint) item.getPoint(); // Get the GeoPoint of the overlay item
                                                double distancia = myLocation.distanceToAsDouble(point); // Calculate the distance
                                                // Check the distance range and categorize
                                                if(item.getTitle() !="Minha localização"){
                                                    if (distancia < 100000 && distancia > 10000) {
                                                        muitoLonge.add(item);
                                                    } else if (distancia < 10000 && distancia > 1000) {
                                                        muitoLonge.add(item);
                                                        longe.add(item);
                                                    } else if (distancia < 1000 && distancia > 250) {
                                                        muitoLonge.add(item);
                                                        perto.add(item);
                                                    } else if (distancia <= 250) {
                                                        muitoLonge.add(item);
                                                        if(distancia>5) {
                                                            alertaPerto(item.getTitle(), distancia, item.getSnippet());
                                                        }
                                                    }
                                                }else{

                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }}

    private void alertaPerto(String tipoAlerta, Double distancia, String data){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        String formattedTime;
        String formattedDate;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Input format (2025-04-21T17:29:04.000Z)
            Date date = inputFormat.parse(data);

            // Format to just time
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            formattedTime = timeFormat.format(date);
            formattedDate = dateFormat.format(date);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        alertaPopUp.setVisibility(View.VISIBLE);
        DecimalFormat numeroFormatado = new DecimalFormat("#.00");
        String alerta = tipoAlerta+".\nO alerta foi emitido à menos de "+ numeroFormatado.format(distancia)+" metros da sua localização atual, às "+formattedTime+" de "+formattedDate+".";
        alertaPopUpText.setText(alerta);
        v.vibrate(400);
    }

    private void startTrackingUserLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create LocationRequest with desired parameters
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Create LocationCallback to handle location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    Location location = locationResult.getLastLocation();
                    GeoPoint novaLocalizacao = new GeoPoint(location.getLatitude(), location.getLongitude());
                    if (location != null && myLocation.distanceToAsDouble(novaLocalizacao)<50) {
                        myLocation = novaLocalizacao;
                        Double distanciaLocalizacoes = myLocation.distanceToAsDouble(antigaLocalizacao);
                        if(distanciaLocalizacoes<750){
                            for (int i = 0; i<perto.size();i++){
                                GeoPoint ponto = (GeoPoint) perto.get(i).getPoint();
                                Double distancia = myLocation.distanceToAsDouble(ponto);
                                if (distancia<=250){
                                    alertaPerto(perto.get(i).getTitle(),distancia,perto.get(i).getSnippet());
                                    perto.remove(i);
                                }
                            }
                        } else if (distanciaLocalizacoes>=750) {
                            antigaLocalizacao = myLocation;
                            perto.clear();
                            for (int i = 0;i<longe.size();i++){
                                GeoPoint ponto = (GeoPoint) longe.get(i).getPoint();
                                Double distancia = myLocation.distanceToAsDouble(ponto);
                                if(distancia<1000 && distancia>250){
                                    perto.add(longe.get(i));
                                } else if (distancia<=250) {
                                    perto.add(longe.get(i));
                                    alertaPerto(longe.get(i).getTitle(), distancia,longe.get(i).getSnippet());
                                }
                            }
                            longe.clear();
                            for (int i = 0; i<muitoLonge.size();i++){
                                GeoPoint ponto = (GeoPoint) muitoLonge.get(i).getPoint();
                                Double distancia = myLocation.distanceToAsDouble(ponto);
                                if(distancia < 10000 && distancia > 1000){
                                    longe.add(muitoLonge.get(i));
                                }
                            }
                        }
                        addMarkerToMap(myLocation.getLatitude(), myLocation.getLongitude(), "Minha localização","");
                        if(previousLine !=null){
                            drawRoute(myLocation,destinationPoint);
                            map.getOverlays().remove(previousLine);
                        }
                    }
                }
            }
        };

        // Check if the app has permission to access the location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

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
                            previousLine = line;
                            map.getOverlays().add(line);
                            map.invalidate();
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
    private void addMarkerToMap(double latitude, double longitude, String title, String data) {
        try {
            if (map == null) {
                Log.e("GeoJson", "Mapa não foi inicializado.");
                Toast.makeText(this, "Mapa não foi inicializado.", Toast.LENGTH_LONG).show();
                return;
            }

            GeoPoint point = new GeoPoint(latitude, longitude);
            OverlayItem overlayItem = new OverlayItem(title, data, point);

            Drawable markerIcon = null;

            List<OverlayItem> items = new ArrayList<>();
            if (title.equals("Roubo de Celular")) {
                markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.alertacelular, null);
                overlayItem.setMarker(markerIcon);
            } else if (title.equals("Roubo de carro")) {
                markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.alertacarro, null);
                overlayItem.setMarker(markerIcon);
            }else if(title.equals("Risco de enchente")){
                markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.enchente, null);
                overlayItem.setMarker(markerIcon);
            } else if (title.equals("Trânsito")) {
                markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.transito, null);
                overlayItem.setMarker(markerIcon);
            } else if (title.equals("Minha localização")) {
                if(minhaLocalizacaoOverlayItem !=null){
                    map.getOverlays().remove(minhaLocalizacaoOverlayItem);
                }
                markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.localatual, null);
                overlayItem.setMarker(markerIcon);
            } else{
                markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.outroalerta, null);
                overlayItem.setMarker(markerIcon);
            }

            if (markerIcon != null) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) markerIcon;
                Bitmap bitmap = bitmapDrawable.getBitmap();

                int newWidth;
                int newHeight;

                if(title.equals("Minha localização")){
                    newWidth = 100;
                    newHeight = 100;
                }else{
                    newWidth = 60;
                    newHeight = 60;
                }

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);

                markerIcon = new BitmapDrawable(getResources(), scaledBitmap);
                overlayItem.setMarker(markerIcon);
            }

            items.add(overlayItem);

            ItemizedOverlayWithFocus<OverlayItem> overlay = new ItemizedOverlayWithFocus<>(this, items, new ItemizedOverlayWithFocus.OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(int index, OverlayItem item) {
                    String clickedTitle = item.getTitle();
                    String clickedData = item.getSnippet();
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Input format (2025-04-21T17:29:04.000Z)
                        Date date = inputFormat.parse(clickedData);

                        // Format to just time
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                        String formattedTime = timeFormat.format(date);
                        String formattedDate = dateFormat.format(date);

                        // Combine title with formatted time
                        String popUpText = clickedTitle + "\n" + formattedDate + "\n" + formattedTime;
                        alertaPopUp.setVisibility(View.VISIBLE);
                        alertaPopUpText.setText(popUpText);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Handle parsing error, maybe use clickedData as-is
                        String popUpText = clickedTitle + "\n" + clickedData;
                        alertaPopUp.setVisibility(View.VISIBLE);
                        alertaPopUpText.setText(popUpText);
                    }
                    return true;
                }

                @Override
                public boolean onItemLongPress(int index, OverlayItem item) {
                    return false;
                }
            });
            if (title.equals("Minha localização")){
                minhaLocalizacaoOverlayItem = overlay;
            }
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
        BuscarAlerta();
        map.onResume();
        TextView sideBarNome = findViewById(R.id.testeNomeUsuario);
        CardView layoutCredenciais = findViewById(R.id.layoutCredenciais);
        CardView layoutMainMenu = findViewById(R.id.mainMenu);
        if (usuarioAtual != null){
            sideBarNome.setText(Criptografia.Descriptografar(usuarioAtual.getNome(),usuarioAtual.getEmail()));
        }else {
            layoutMainMenu.setVisibility(View.GONE);
            layoutCredenciais.setVisibility(View.VISIBLE);
            sideBarNome.setText("");
        }
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

    public void disaparecerLayout(){
        CardView credencias = findViewById(R.id.layoutCredenciais);
        CardView topMenu = findViewById(R.id.topMenuLayout);
        ImageView btnCentralizar = findViewById(R.id.btnCentralizarLocal);
        CardView alertaPopUpLayout = findViewById(R.id.alertaInformacao);
        CardView menuPrincipal = findViewById(R.id.mainMenu);

        topMenu.setVisibility(View.GONE);
        btnCentralizar.setVisibility(View.GONE);
        credencias.setVisibility(View.GONE);
        alertaPopUpLayout.setVisibility(View.GONE);
        menuPrincipal.setVisibility(View.GONE);
    }

    public void aparecerLayout(){
        CardView credenciais = findViewById(R.id.layoutCredenciais);
        CardView menuPrincipal = findViewById(R.id.mainMenu);
        CardView menuTop = findViewById(R.id.topMenuLayout);
        ImageView btnCentralizar = findViewById(R.id.btnCentralizarLocal);
        if (usuarioAtual == null){
            credenciais.setVisibility(View.VISIBLE);
        }else{
            menuPrincipal.setVisibility(View.VISIBLE);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) btnCentralizar.getLayoutParams();
            params.verticalBias = 0.85f;
            btnCentralizar.setLayoutParams(params);
        }
        btnCentralizar.setVisibility(View.VISIBLE);
        menuTop.setVisibility(View.VISIBLE);
    }
    public void entrarTelaBtn(View view){
        CardView loginLayout = findViewById(R.id.layoutLogin);
        // Esconde o layout do card
        disaparecerLayout();
        // Faz o layout do card de login aparecer
        loginLayout.setVisibility(View.VISIBLE);
        // Aplica animação ao card
        ObjectAnimator animator = ObjectAnimator.ofFloat(loginLayout, "translationY", 1000f, 0f);
        animator.setDuration(600);
        animator.start();
    }

    public void cadastroTelaBtn(View view){
        CardView cadastroLayout = findViewById(R.id.cadastrarLayout);
        disaparecerLayout();
        cadastroLayout.setVisibility(View.VISIBLE);

        ObjectAnimator animator = ObjectAnimator.ofFloat(cadastroLayout, "translationY", 1000f, 0f);
        animator.setDuration(600);
        animator.start();
    }

    public void sideMenuBtn(View view){
        CardView menuBack = findViewById(R.id.sideMenuBack);
        disaparecerLayout();
        menuBack.setVisibility(View.VISIBLE);
    }

    public void voltarMenuBtn(View view){
        CardView menuBack = findViewById(R.id.sideMenuBack);
        aparecerLayout();
        menuBack.setVisibility(View.GONE);
    }

    public void sairCadastroBtn(View view){
        CardView cadastroLayout = findViewById(R.id.cadastrarLayout);
        aparecerLayout();
        cadastroLayout.setVisibility(View.GONE);
    }

    public void sairLoginBtn(View view){
        CardView loginLayout = findViewById(R.id.layoutLogin);
        aparecerLayout();
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

        CardView cadastroLayout = findViewById(R.id.cadastrarLayout);

        TextView sideBarTesteNome = findViewById(R.id.testeNomeUsuario);

        if(nomeText.isEmpty() || emailText.isEmpty() || senhaText.isEmpty() || telefoneText.isEmpty()){
            Toast.makeText(this, "Você deve preencher todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailCript = Criptografia.Criptografar(emailText, senhaText);
        String senhaCript = Criptografia.Criptografar(senhaText, emailCript);
        String nomeCript = Criptografia.Criptografar(nomeText, emailCript);
        String telefoneCript = Criptografia.Criptografar(telefoneText, emailCript);

        usuario user = new usuario(nomeCript,senhaCript,emailCript,telefoneCript);

        Call<Void> call = apiService.addUser(user);
        cadastroBtn.setText(getResources().getString(R.string.carregarBtn));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    cadastroBtn.setText(getResources().getString(R.string.botao_cadastro));
                    Toast.makeText(MainActivity.this, "Usuario Cadastrado!", Toast.LENGTH_SHORT).show();
                    usuarioAtual = new usuario(nomeCript,senhaCript,emailCript,telefoneCript);

                    sideBarTesteNome.setText(Criptografia.Descriptografar(usuarioAtual.getNome(), usuarioAtual.getEmail()));
                    cadastroLayout.setVisibility(View.GONE);
                    aparecerLayout();
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

    public void loginUsuario(View view) {
        EditText email = findViewById(R.id.editEmailLogin);
        EditText senha = findViewById(R.id.editSenhaLogin);
        Button loginBtn = findViewById(R.id.loginSendBtn);

        TextView sideBarTesteNome = findViewById(R.id.testeNomeUsuario);
        CardView loginLayout = findViewById(R.id.layoutLogin);

        String emailText = email.getText().toString();
        String senhaText = senha.getText().toString();

        if(emailText.isEmpty() || senhaText.isEmpty()){
            Toast.makeText(this, "Você deve preencher todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailCript = Criptografia.Criptografar(emailText, senhaText);
        String senhaCript = Criptografia.Criptografar(senhaText, emailCript);

        // Cria um objeto usuario
        usuario user = new usuario(senhaCript, emailCript);

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
                        String telefone = loggedInUser.getCelular();

                        usuarioAtual = new usuario(nome,senhaCript,emailCript,telefone);

                        Toast.makeText(MainActivity.this, "Usuario logado com sucesso!!", Toast.LENGTH_SHORT).show();
                        loginLayout.setVisibility(View.GONE);
                        sideBarTesteNome.setText(Criptografia.Descriptografar(usuarioAtual.getNome(),usuarioAtual.getEmail()));
                        aparecerLayout();

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
                            addMarkerToMap(alerta.getLatitude(), alerta.getLongitude(), alerta.getTipoAlerta(), alerta.getDataOcorrencia());
                        }
                        getCurrentLocation();
                        startTrackingUserLocation();
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
