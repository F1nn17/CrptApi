import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

class Launcher{
    static ExecutorService pool;
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);
        pool = Executors.newFixedThreadPool(50);
        Document document = new Document.DocumentBuilder().build();
        String signature = "";
        for (int i = 0; i < 15; i++) {
            pool.execute(() -> crptApi.register(document, signature));
        }
        pool.shutdown();
    }
}

interface PublicApi{
     void register(Document document, String signature);
}

class PublicApiImp implements PublicApi{
    @Override
    public void register(Document document, String signature) {
        try {
            String post = "https://ismp.crpt.ru/api/v3/lk/documents/create";
            ObjectMapper mapper = new ObjectMapper();
            String resultJson;
            try {
                resultJson = mapper.writeValueAsString(document);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(post))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(resultJson))
                    .build();
            var client = HttpClient.newHttpClient();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}

public class CrptApi implements PublicApi {
    private final PublicApiImp publicApiImp;
    private final int requestLimit;
    private final Semaphore semaphore;

    CrptApi(TimeUnit timeUnit, int requestLimit){
        publicApiImp = new PublicApiImp();
        this.requestLimit = requestLimit;
        semaphore = new Semaphore(requestLimit, true);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new CounterReset(), 60000, 1, timeUnit);
    }

    @Override
    public void register(Document document, String signature) {
        try {
            semaphore.acquire();
            publicApiImp.register(document, signature);
        }
        catch (InterruptedException ignored){}
        finally {
            semaphore.release();
        }
    }

     class CounterReset implements Runnable {
        @Override
        public void run() {
            semaphore.release(requestLimit);
        }
    }
}

@Builder
@Data
class Document{
    @JsonProperty("description")
    private Description description;

    @JsonProperty("doc_id")
    private String docId;

    @JsonProperty("doc_status")
    private String docStatus;

    @JsonProperty("doc_type")
    private String docType;

    @JsonProperty("importRequest")
    private boolean importRequest;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("participant_inn")
    private String participantInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private String productionDate;

    @JsonProperty("production_type")
    private String productionType;

    @JsonProperty("products")
    private Products[] products;

    @JsonProperty("reg_date")
    private String regDate;

    @JsonProperty("reg_number")
    private String regNumber;

    Document(){

    }

}

@Data
class Products{
    @JsonProperty("certificate_document")
    private String certificateDocument;

    @JsonProperty("certificate_document_date")
    private String certificateDocumentDate = "2020-01-23";

    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private String productionDate = "2020-01-23";

    @JsonProperty("tnved_code")
    private String tnvedCode;

    @JsonProperty("uit_code")
    private String uitCode;

    @JsonProperty("uitu_code")
    private String uituCode;

    Products(){

    }
}

@Data
class Description{
    @JsonProperty("participantInn")
    private String participantInn;
    Description(){

    }
}