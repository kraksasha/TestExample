package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApi {

     private TimeUnit timeUnit;
     private int requestLimit;
     private HttpClient httpClient;
     private long timeUn;
     private ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) throws InterruptedException {
        timeUn = timeUnit.toSeconds(FinalParameter.interval);
        this.requestLimit = requestLimit;
        httpClient = HttpClient.newBuilder().build();
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        startTimer(timeUn,requestLimit);
    }

    public synchronized void createDocument(ProductDocument productDocument, String sig) throws IOException, InterruptedException {
        if (requestLimit > 0){
            productDocument.setSignature(sig);
            String body = objectMapper.writeValueAsString(productDocument);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<Void> response = httpClient.send(request,HttpResponse.BodyHandlers.discarding());
            requestLimit = requestLimit - 1;
        } else {
            System.out.println("Превышен лимит запросов");
        }
    }
    //Метод запускающий бесконечно таймер в отдельном потоке на заданный интервал времени.
    //По истечению таймера лимит запросов восстанавливается.
    private void startTimer(long time, int limit) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    for (int i = 0; i < time; i++){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    requestLimit = limit;
                }
            }
        }).start();
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ProductDocument{
    private Description description;
    private String dock_id;
    private String dock_status;
    private String dock_type;
    private boolean importRequest;
    private String owner_inn;
    private String participant_inn;
    private String producer_inn;
    private String production_type;
    private List<ProductCertificateDoc> products;
    private String reg_date;
    private String reg_number;
    private String signature;
}

class Main{
    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES,5);
        ProductDocument productDocument = new ProductDocument();
        // Имитация запросов на создание документа которые можно отправить с консоли для наглядности
        InputStreamReader inputStreamReader = new InputStreamReader(System.in);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        for (int i = 0; i < 1000; i++){
            String readData = bufferedReader.readLine();
            if (readData.equals("запустить")){
                crptApi.createDocument(productDocument,"fdfd");
            }
        }

    }
}

class FinalParameter{
    static final int interval = 1;
}

@Data
class Description{
    private String participantInn;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ProductCertificateDoc{
    private String certificate_document;
    private String certificate_document_date;
    private String certificate_document_number;
    private String owner_inn;
    private String producer_inn;
    private String production_date;
    private String tnved_code;
    private String uit_code;
    private String uitu_code;
}

