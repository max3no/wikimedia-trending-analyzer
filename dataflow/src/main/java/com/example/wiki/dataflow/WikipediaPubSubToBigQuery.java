package com.example.wiki.dataflow;

import com.google.api.services.bigquery.model.TableRow;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.*;

import java.time.Instant;

public class WikipediaPubSubToBigQuery {

    public interface WikiPipelineOptions extends DataflowPipelineOptions {
        @Description("Pub/Sub subscription")
        @Validation.Required
        String getInputSubscription();
        void setInputSubscription(String value);

        @Description("BigQuery output table (<project>:<dataset>.<table>)")
        @Validation.Required
        String getOutputTable();
        void setOutputTable(String value);
    }

    public static void main(String[] args) {
        WikiPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(WikiPipelineOptions.class);

        Pipeline pipeline = Pipeline.create(options);

        pipeline
                .apply("ReadFromPubSub", PubsubIO.readStrings().fromSubscription(options.getInputSubscription()))
                .apply("ParseJSON", ParDo.of(new DoFn<String, TableRow>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        try {
                            Gson gson = new Gson();
                            JsonObject json = gson.fromJson(c.element(), JsonObject.class);

                            String article = json.has("article") ? json.get("article").getAsString() : null;
                            Integer views = json.has("views") ? json.get("views").getAsInt() : null;
                            Integer rank = json.has("rank") ? json.get("rank").getAsInt() : null;

                            if (article != null && views != null && rank != null) {
                                TableRow row = new TableRow()
                                        .set("article", article)
                                        .set("views", views)
                                        .set("rank", rank)
                                        .set("ingested_at", Instant.now().toString());
                                c.output(row);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse message: " + c.element());
                            e.printStackTrace();
                        }
                    }
                }))
                .apply("WriteToBigQuery", BigQueryIO.writeTableRows()
                        .to(options.getOutputTable())
                        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_NEVER)
                        .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                        .withCustomGcsTempLocation(ValueProvider.StaticValueProvider.of("gs://wikimedia-temp-bucket-110/temp"))
                );

        pipeline.run();
    }

}

