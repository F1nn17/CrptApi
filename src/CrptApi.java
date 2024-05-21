import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private Semaphore semaphore;

    CrptApi(TimeUnit timeUnit, int requestLimit){
        publicApiImp = new PublicApiImp();
        this.requestLimit = requestLimit;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new CounterReset(), 0, 1, timeUnit);
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
            if(semaphore != null)   semaphore.release(requestLimit);
            if(semaphore == null)   semaphore = new Semaphore(requestLimit, true);
        }
    }
}

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

    Document(DocumentBuilder documentBuilder){
         this.description = documentBuilder.description;
         this.docId = documentBuilder.docId;
         this.docStatus = documentBuilder.docStatus;
         this.docType = documentBuilder.docType;
         this.importRequest = documentBuilder.importRequest;
         this.ownerInn = documentBuilder.ownerInn;
         this.participantInn = documentBuilder.participantInn;
         this.producerInn = documentBuilder.producerInn;
         this.productionDate = documentBuilder.productionDate;
         this.productionType = documentBuilder.productionType;
         this.products = documentBuilder.products;
         this.regDate = documentBuilder.regDate;
         this.regNumber = documentBuilder.regNumber;
    }

    public static class DocumentBuilder{
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Products[] products;
        private String regDate;
        private String regNumber;

        public DocumentBuilder(){
            super();
        }

        public DocumentBuilder description(Description description){
            this.description = description;
            return this;
        }

        public DocumentBuilder docId(String docId){
            this.docId = docId;
            return this;
        }

        public DocumentBuilder docStatus(String docStatus){
            this.docStatus = docStatus;
            return this;
        }

        public DocumentBuilder docType(String docType){
            this.docType = docType;
            return this;
        }

        public DocumentBuilder importRequest(boolean importRequest){
            this.importRequest = importRequest;
            return this;
        }

        public DocumentBuilder ownerInn(String ownerInn){
            this.ownerInn = ownerInn;
            return this;
        }

        public DocumentBuilder participantInn(String participantInn){
            this.participantInn = participantInn;
            return this;
        }

        public DocumentBuilder producerInn(String producerInn){
            this.producerInn = producerInn;
            return this;
        }

        public DocumentBuilder productionDate(String productionDate){
            this.productionDate = productionDate;
            return this;
        }

        public DocumentBuilder productionType(String productionType){
            this.productionType = productionType;
            return this;
        }

        public DocumentBuilder products(Products[] products){
            this.products = products;
            return this;
        }

        public DocumentBuilder regDate(String regDate){
            this.regDate = regDate;
            return this;
        }

        public DocumentBuilder regNumber(String regNumber){
            this.regNumber = regNumber;
            return this;
        }

        public Document build(){
            return new Document(this);
        }

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