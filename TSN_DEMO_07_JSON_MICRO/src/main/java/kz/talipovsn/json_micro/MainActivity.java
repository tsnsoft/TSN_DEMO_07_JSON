package kz.talipovsn.json_micro;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

/**
 * Главная активность приложения для отображения цены биткоина в USD и EUR.
 */
public class MainActivity extends AppCompatActivity {

    private TextView textView; // Компонент для отображения данных
    private static final String URL = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD,EUR"; // Адрес API CryptoCompare
    private long lastRequestTime = 0; // Время последнего запроса
    private static final long MIN_REQUEST_INTERVAL = 1000; // Минимальный интервал между запросами (1 секунда)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Фикс для HTTPS на старых устройствах Android
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            sslContext.createSSLEngine();
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException
                 | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        textView = findViewById(R.id.textView);
        fetchPriceData(); // Загружаем данные при запуске приложения
    }

    /**
     * Обработчик нажатия кнопки "Обновить".
     */
    public void onClick(View view) {
        fetchPriceData();
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
        protected String doInBackground(String... urls) { // Фоновая задача
            return getHTMLData(urls[0]); // Загружаем данные по URL
        }

        @Override
        protected void onPostExecute(String json) { // Метод вызывается после завершения фоновой задачи
            if (json != null) {
                try {
                    // Парсим JSON
                    JSONObject root = new JSONObject(json); // Создаем объект JSON
                    double usdRate = root.getDouble("USD"); // Получаем курс USD
                    double eurRate = root.getDouble("EUR"); // Получаем курс EUR

                    // Форматируем и отображаем цены
                    textView.setText(""); // Очищаем текстовое поле
                    textView.append(String.format("1 BTC = %.2f USD", usdRate)); // Добавляем курс USD в текстовое поле
                    textView.append("\n"); // Перенос строки
                    textView.append(String.format("1 BTC = %.2f EUR", eurRate)); // Добавляем курс EUR в текстовое поле
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
            URL url = new URL(urlString); // Создаем объект URL
            conn = (HttpURLConnection) url.openConnection(); // Открываем соединение
            conn.setRequestMethod("GET"); // Устанавливаем метод запроса
            conn.setConnectTimeout(5000); // Таймаут соединения 5 секунд
            conn.setReadTimeout(5000); // Таймаут чтения 5 секунд

            int responseCode = conn.getResponseCode(); // Получаем код ответа
            if (responseCode == HttpURLConnection.HTTP_OK) { // Проверяем код ответа
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
                return null; // Неверный код ответа сервера
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Ошибка сети
        } finally {
            if (conn != null) {
                conn.disconnect(); // Закрываем соединение
            }
        }
    }
}