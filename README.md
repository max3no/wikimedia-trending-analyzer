# Wikipedia Trending Analyzer 🚀

![GCP](https://img.shields.io/badge/Platform-Google%20Cloud-blue?logo=googlecloud)
![Java](https://img.shields.io/badge/Language-Java-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot-green?logo=springboot)
![Apache Beam](https://img.shields.io/badge/Dataflow-Apache%20Beam-red?logo=apachebeam)
![BigQuery](https://img.shields.io/badge/Storage-BigQuery-yellow?logo=googlebigquery)

> A real-time data pipeline on Google Cloud Platform (GCP) that fetches trending Wikipedia articles, streams data via Pub/Sub, enriches it with Apache Beam & Dataflow, and stores it in BigQuery for analytics and visualization.

---

## Project Overview

This project demonstrates an end-to-end cloud data engineering workflow using Google Cloud Platform and Spring Boot applications:

- **Media App**: A Spring Boot service that fetches trending Wikipedia articles via Wikimedia's REST API and publishes the data as messages to a Google Cloud Pub/Sub topic.
- **Dataflow Pipeline**: Another Spring Boot application that runs an Apache Beam pipeline on Google Cloud Dataflow. It reads from Pub/Sub, adds an ingestion timestamp (`ingested_at`), and writes enriched data to BigQuery in real-time.
- **BigQuery Dataset**: Stores the trending articles data in a table for querying and analysis.
- **Cloud Storage Bucket**: Used by Dataflow as temporary storage during pipeline execution.

---

## Architecture Diagram

```plaintext
[Wikipedia API] --> [Media Spring Boot App] --(Pub/Sub)--> [Dataflow Pipeline] --> [BigQuery]
                                                |
                                     (Temporary Storage on Cloud Storage Bucket)
```

## ⚙️ Setup & Configuration

### 1. Google Cloud Project Setup
- Created a GCP project: **wikipedia-trending-analyzer**.
- Created a Service Account `wiki-data-ingestor` with the following roles:
    - 📤 Pub/Sub Publisher
    - 📥 Pub/Sub Subscriber
    - 👁️ Pub/Sub Viewer
- Generated and downloaded the Service Account key JSON.
- Set the environment variable `GOOGLE_APPLICATION_CREDENTIALS` to point to the service account key file for authentication.

---

### 2. IAM and Permissions
- Assigned the Service Account as a principal with the required roles in IAM.

---

### 3. Pub/Sub Setup
- Created Pub/Sub topics and subscriptions for streaming data between producer and consumer apps.

---

### 4. BigQuery Setup
- Enabled **BigQuery API**.
- Created a dataset named `wikimedia_dataset`.
- Created a table `trending_articles` inside the dataset with the appropriate schema.

---

### 5. Cloud Storage Setup
- Created a bucket: `wikimedia-temp-bucket-110`.
- Created a `temp` folder inside the bucket for Dataflow temporary files.

---

### 6. Dataflow Pipeline
- Enabled **Dataflow API**.
- Configured and ran the Dataflow pipeline with Apache Beam using this run configuration:

```bash
--project=wikipedia-trending-analyzer
--region=us-central1
--runner=DataflowRunner
--tempLocation=gs://wikimedia-temp-bucket-110/temp
--gcpTempLocation=gs://wikimedia-temp-bucket-110/temp
--inputSubscription=projects/wikipedia-trending-analyzer/subscriptions/wikimedia-topic-sub
--outputTable=wikipedia-trending-analyzer:wikimedia_dataset.trending_articles
```

---

### 7. Media App
- Fetches trending articles from Wikimedia API endpoint:
- Publishes the fetched data to Pub/Sub topic for consumption by the Dataflow pipeline.

```plaintext
https://wikimedia.org/api/rest_v1/metrics/pageviews/top/en.wikipedia/all-access/{date}
```


### ▶️ Running the Apps
- Media Producer App: Publishes trending article data to Pub/Sub.

- Dataflow Consumer App: Runs Apache Beam pipeline on Dataflow to process and insert data into BigQuery.
## 📚 References

- [Wikimedia API Documentation](https://wikimedia.org/api/rest_v1/)
- [Apache Beam on Dataflow](https://beam.apache.org/documentation/runners/dataflow/)
- [BigQuery Basics](https://cloud.google.com/bigquery/docs/introduction)
- [Looker Studio](https://datastudio.google.com/)
