package kz.talipovsn.json_micro;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Главная активность приложения для отображения данных
 */
public class MainActivity extends AppCompatActivity {

    private TextView textView; // Компонент для отображения данных
    // Надежный открытый API без ключей и жестких лимитов
    private static final String URL = "https://blockchain.info/ticker";
    private long lastRequestTime = 0; // Время последнего запроса
    private static final long MIN_REQUEST_INTERVAL = 1000; // Минимальный интервал (1 секунда)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        fetchPriceData(); // Загружаем данные при запуске приложения
    }

    /**
     * Обработчик нажатия кнопки "Обновить".
     */
    public void onClick(View view) {
        fetchPriceData(); // Загружаем данные при нажатии на кнопку
    }

    /**
     * Запускает загрузку данных о цене в фоновом потоке, если не слишком частые запросы.
     */
    private void fetchPriceData() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime < MIN_REQUEST_INTERVAL) {
            textView.setText("Слишком частые запросы, подождите...");
            return;
        }

        textView.setText(R.string.not_data); // Устанавливаем текст "Нет данных"
        lastRequestTime = currentTime; // Обновляем время последнего запроса
        new FetchPriceTask().execute(URL); // Запускаем AsyncTask
    }

    /**
     * AsyncTask для выполнения сетевых операций в фоновом потоке.
     */
    private class FetchPriceTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            return getHTMLData(urls[0]);
        }

        @Override
        protected void onPostExecute(String json) {
            if (json != null) {
                try {
                    // Парсим JSON от blockchain.info
                    JSONObject root = new JSONObject(json);
                    double usdRate = root.getJSONObject("USD").getDouble("last");
                    double eurRate = root.getJSONObject("EUR").getDouble("last");

                    // Форматируем и отображаем цены
                    textView.setText("");
                    textView.append(String.format(Locale.US, "1 BTC = %.2f USD\n", usdRate));
                    textView.append(String.format(Locale.US, "1 BTC = %.2f EUR", eurRate));
                } catch (Exception e) {
                    textView.setText(R.string.error); // Ошибка парсинга
                }
            } else {
                textView.setText(R.string.error); // Ошибка загрузки данных
            }
        }
    }

    /**
     * Метод для загрузки данных по HTTP/HTTPS.
     *
     * @param urlString Адрес API.
     * @return Строка с JSON или null в случае ошибки.
     */
    private static String getHTMLData(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // Маскируем запрос под обычный браузер, чтобы избежать блокировки
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder data = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data.append(line);
                    }
                }
                return data.toString();
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}