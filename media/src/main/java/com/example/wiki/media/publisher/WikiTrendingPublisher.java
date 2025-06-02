package com.example.wiki.media.publisher;

import com.example.wiki.media.model.WikiArticle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

@Component
class WikiTrendingPublisher implements CommandLineRunner {

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.pubsub.topic}")
    private String topicId;

    private CredentialsProvider credentialsProvider;

    @Override
    public void run(String... args) throws Exception {
        // Load credentials from env variable path
        // update the path with key.json from service accounts, read from secure location
        String credentialsPath = "key.json";
        if (credentialsPath == null) {
            System.err.println("Please set GOOGLE_APPLICATION_CREDENTIALS environment variable");
            return;
        }
        var credentials = ServiceAccountCredentials.fromStream(new FileInputStream(credentialsPath));
        credentialsProvider = FixedCredentialsProvider.create(credentials);

        createTopicIfNotExists();

        List<WikiArticle> trendingArticles = fetchTrendingWikipediaArticles();
        System.out.println("Trending Articles: " + trendingArticles);

        publishMessages(trendingArticles);
    }

    private void createTopicIfNotExists() throws Exception {
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(
                TopicAdminSettings.newBuilder()
                        .setCredentialsProvider(credentialsProvider)
                        .build())) {
            TopicName topicName = TopicName.of(projectId, topicId);
            try {
                topicAdminClient.getTopic(topicName);
                System.out.println("Topic exists: " + topicName);
            } catch (Exception e) {
                topicAdminClient.createTopic(topicName);
                System.out.println("Created topic: " + topicName);
            }
        }
    }

private List<WikiArticle> fetchTrendingWikipediaArticles() {
    LocalDate today = LocalDate.now().minusDays(2L);
    String dateStr = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

    String url = "https://wikimedia.org/api/rest_v1/metrics/pageviews/top/en.wikipedia/all-access/" + dateStr;
    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

    try {
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
        Map<String, Object> firstItem = items.get(0);
        List<Map<String, Object>> articles = (List<Map<String, Object>>) firstItem.get("articles");

        return articles.stream().map(article -> new WikiArticle(
                (String) article.get("article"),
                ((Number) article.get("views")).longValue(),
                ((Number) article.get("rank")).intValue()
        )).toList();
    } catch (Exception e) {
        e.printStackTrace();
        return List.of();
    }
}

    private void publishMessages(List<WikiArticle> articles) throws Exception {
        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        Publisher publisher = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            publisher = Publisher.newBuilder(topicName)
                    .setCredentialsProvider(credentialsProvider)
                    .build();

            for (WikiArticle article : articles) {
                String json = objectMapper.writeValueAsString(article);
                ByteString data = ByteString.copyFromUtf8(json);

                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
                publisher.publish(pubsubMessage).get();  // blocking for simplicity

                System.out.println("Published: " + json);
            }
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

}
