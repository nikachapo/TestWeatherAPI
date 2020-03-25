package com.example.testweatherapi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_LAST_CITY_NAME = "last-city";

    private TextView cityText,
            descriptionText, celsiusText, timeText, windText, sunriseText, sunsetText;
    private ImageView image;
    private ProgressBar progressBar;
    private EditText searchEditText;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private boolean isConnectedToInternet = false;

    public LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sunriseText = findViewById(R.id.sunriseText);
        sunsetText = findViewById(R.id.sunsetText);
        windText = findViewById(R.id.windText);
        timeText = findViewById(R.id.timeText);
        image = findViewById(R.id.weatherImage);
        progressBar = findViewById(R.id.progressBar);
        cityText = findViewById(R.id.cityText);
        descriptionText = findViewById(R.id.descriptionText);
        celsiusText = findViewById(R.id.celsiusText);
        searchEditText = findViewById(R.id.searchEditText);
        Button searchButton = findViewById(R.id.searchButton);

        String lastCityName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(KEY_LAST_CITY_NAME, "London");
        loadInfo(lastCityName);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //stop updates
                locationManager.removeUpdates(locationListener);

                InputMethodManager inputManager =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputManager != null) {
                    inputManager.hideSoftInputFromWindow(v.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }
                if (!isConnectedToInternet) {
                    Toast.makeText(getApplicationContext(), "No internet connection", Toast.LENGTH_LONG).show();
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    String q = searchEditText.getText().toString().trim();
                    if (!q.isEmpty()) {
                        searchEditText.getText().clear();
                        loadInfo(q);
                    } else
                        Toast.makeText(getApplicationContext(), "Enter city name", Toast.LENGTH_LONG).show();
                }

            }
        });

    }


    @SuppressLint("StaticFieldLeak")
    private void loadInfo(String q) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                return NetworkUtils.getWeather(strings[0]);
            }

            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            protected void onPostExecute(String s) {
                processJsonString(s);
            }
        }.execute(q);

    }

    @SuppressLint("StaticFieldLeak")
    private void loadInfo(double lon,double lat) {
        new AsyncTask<Double, Void, String>() {
            @Override
            protected String doInBackground(Double... doubles) {
                return NetworkUtils.getWeather(doubles[0],doubles[1]);
            }

            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            protected void onPostExecute(String s) {
                processJsonString(s);
            }
        }.execute(lon,lat);

    }


    private void processJsonString(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);

            JSONObject mainWeather = jsonObject.getJSONObject("main");
            JSONObject sysObject = jsonObject.getJSONObject("sys");


            try {
                JSONArray weatherArray = jsonObject.getJSONArray("weather");
                JSONObject weather = weatherArray.getJSONObject(0);
                String icon = (String) weather.get("icon");
                String url = "http://openweathermap.org/img/wn/" + icon + "@2x.png";

                Picasso.with(getApplicationContext()).load(url).into(image);

                String description = (String) weather.get("description");
                String cityName = (String) jsonObject.get("name");
                double kelvinToCelsius = mainWeather.getDouble("temp") - 273.15;
                Integer timeMillis = (Integer) jsonObject.get("dt");
                Integer sunriseMillis = (Integer) sysObject.get("sunrise");
                Integer sunsetMillis = (Integer) sysObject.get("sunset");
                String time = getFormattedTime((long) timeMillis);
                String sunrise = getFormattedTime((long) sunriseMillis);
                String sunset = getFormattedTime((long) sunsetMillis);

                double wind = jsonObject.getJSONObject("wind").getDouble("speed");

                descriptionText.setText(description);
                cityText.setText(cityName);
                celsiusText.setText(String.format("%.2f", kelvinToCelsius) + "°C");
                timeText.setText(time);
                windText.setText(wind + " kph");
                sunriseText.setText(sunrise);
                sunsetText.setText(sunset);

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            // If onPostExecute does not receive a proper JSON string,
            // update the UI to show failed results.
            Toast.makeText(getApplicationContext(), "City not found!", Toast.LENGTH_LONG).show();

            e.printStackTrace();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    public static String getFormattedTime(long millis) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        Date date = new Date(millis * 1000);
        return formatter.format(date);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            networkCallback =
                    new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            isConnectedToInternet = true;
                        }

                        @Override
                        public void onLost(@NonNull Network network) {
                            isConnectedToInternet = false;
                        }
                    };
            connectivityManager
                    .registerNetworkCallback(builder.build(), networkCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
        String cityName = cityText.getText().toString().trim();
        if (!cityName.isEmpty()) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putString(KEY_LAST_CITY_NAME, cityName).apply();
        }

        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }




    private Location getLastKnownLocation() {
        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission")
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    @SuppressLint("MissingPermission")
    public void searchWithLocation(View view) {
        progressBar.setVisibility(View.VISIBLE);
        Location location = getLastKnownLocation();
        if (location != null) {
            Log.e("TAG", "GPS is on");
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            loadInfo(longitude,latitude);
        }else
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000,
                    10, locationListener);


    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(getApplicationContext(), "Location found", Toast.LENGTH_LONG).show();
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            loadInfo(longitude,latitude);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Enable GPS", Toast.LENGTH_LONG).show();
        }
    };
}
