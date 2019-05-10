import Services.CategoryService;
import Services.ItemService;
import Services.SiteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import Domain.*;

public class Application {
    //The config parameters for the connection
    private static final String HOST = "localhost";
    private static final int PORT_ONE = 9200;
    private static final int PORT_TWO = 9201;
    private static final String SCHEME = "http";
    private static RestHighLevelClient restHighLevelClient;
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final String INDEX = "persondata";
    private static final String TYPE = "person";

    /**
     * Implemented Singleton pattern here
     * so that there is just one connection at a time.
     * @return RestHighLevelClient
     */
    private static synchronized RestHighLevelClient makeConnection() {

        if(restHighLevelClient == null) {
            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(HOST, PORT_ONE, SCHEME),
                            new HttpHost(HOST, PORT_TWO, SCHEME)));
        }

        return restHighLevelClient;
    }

    private static synchronized void closeConnection() throws IOException {
        restHighLevelClient.close();
        restHighLevelClient = null;
    }

    private static Person insertPerson(Person person){
        person.setPersonId(UUID.randomUUID().toString());
        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("personId", person.getPersonId());
        dataMap.put("name", person.getName());
        IndexRequest indexRequest = new IndexRequest(INDEX, TYPE, person.getPersonId())
                .source(dataMap);
        try {
            IndexResponse response = restHighLevelClient.index(indexRequest,RequestOptions.DEFAULT);
        } catch(ElasticsearchException e) {
            e.getDetailedMessage();
        } catch (IOException ex){
            ex.getLocalizedMessage();
        }
        return person;
    }

    private static Person getPersonById(String id){
        GetRequest getPersonRequest = new GetRequest(INDEX, TYPE, id);
        GetResponse getResponse = null;
        try {
            getResponse = restHighLevelClient.get(getPersonRequest, RequestOptions.DEFAULT);
        } catch (IOException e){
            e.getLocalizedMessage();
        }
        return getResponse != null ?
                objectMapper.convertValue(getResponse.getSourceAsMap(), Person.class) : null;
    }

    private static Person updatePersonById(String id, Person person){
        UpdateRequest updateRequest = new UpdateRequest(INDEX, TYPE, id)
                .fetchSource(true);    // Fetch Object after its update
        try {
            String personJson = objectMapper.writeValueAsString(person);
            updateRequest.doc(personJson, XContentType.JSON);
            UpdateResponse updateResponse = restHighLevelClient.update(updateRequest,RequestOptions.DEFAULT);
            return objectMapper.convertValue(updateResponse.getGetResult().sourceAsMap(), Person.class);
        }catch (JsonProcessingException e){
            e.getMessage();
        } catch (IOException e){
            e.getLocalizedMessage();
        }
        System.out.println("Unable to update person");
        return null;
    }

    private static void deletePersonById(String id) {
        DeleteRequest deleteRequest = new DeleteRequest(INDEX, TYPE, id);
        try {
            DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest,RequestOptions.DEFAULT);
        } catch (IOException e){
            e.getLocalizedMessage();
        }
    }

    public static void main(String[] args) throws IOException {

        makeConnection();
        SiteService siteService = new SiteService (restHighLevelClient) ;
        CategoryService categoryService = new CategoryService(restHighLevelClient);
        ItemService itemService = new ItemService(restHighLevelClient);

        System.out.println("Inserting a new Site MLA");
        Site site = new Site();
        site.setName("Argentina");
        site.setId("MLA");
        site = Services.SiteService.insertSite(site);
        System.out.println("Site inserted --> " + site);

        System.out.println("Inserting a new Site MBO");
        Site site2 = new Site();
        site2.setName("Bolivia");
        site2.setId("MBO");
        site2 = Services.SiteService.insertSite(site2);
        System.out.println("Site inserted --> " + site2);

        System.out.println("Inserting a new Site MLM");
        Site site3 = new Site();
        site3.setName("Mexico");
        site3.setId("MLM");
        site3 = Services.SiteService.insertSite(site3);
        System.out.println("Site inserted --> " + site3);

        System.out.println("Inserting a new Site MLB");
        Site site4 = new Site();
        site4.setName("Brasil");
        site4.setId("MLB");
        site4 = Services.SiteService.insertSite(site4);
        System.out.println("Site inserted --> " + site4);

        System.out.println("Inserting a new Category");
        Category category = new Category();
        category.setName("Objectos para matar tigres");
        category.setId("MLA111");
        category.setSite(site);
        category = categoryService.insertCategory(category);
        System.out.println("Category inserted --> " + category);

        System.out.println("Inserting a new Category");
        Item item = new Item();
        item.setName("Martillo de thor");
        item.setId("MLATHOR12312");
        item.setCategory(category);
        item = itemService.insertItem(item);
        System.out.println("Item inserted --> " + item);

        closeConnection();
    }
}
