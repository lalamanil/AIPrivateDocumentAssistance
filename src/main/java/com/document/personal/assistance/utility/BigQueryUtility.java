package com.document.personal.assistance.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.document.personal.assistance.constants.ApplicationConstants;
import com.document.personal.assistance.exception.PrivateDocumentException;
import com.document.personal.assistance.model.VectorSearchResultsModel;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;

public class BigQueryUtility {

	private static BigQuery bigQuery;

	private static final Logger LOGGER = Logger.getLogger(BigQueryUtility.class.getName());

	static {

		InputStream inputStream = BigQueryUtility.class.getClassLoader().getResourceAsStream("AI-ServiceAccount.json");

		if (null != inputStream) {

			try {
				GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
						.createScoped("https://www.googleapis.com/auth/cloud-platform");
				BigQueryOptions bigQueryOptions = BigQueryOptions.newBuilder().setCredentials(credentials)
						.setProjectId(ApplicationConstants.PROJECT_ID).build();
				bigQuery = bigQueryOptions.getService();
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		} else {
			LOGGER.info("InputStream for Service account is null. Please check application logs..");
		}

	}

	public static void insertRowsBatch(List<Map<String, Object>> rowData, String tableName) {
		if (null != bigQuery) {
			TableId tableId = TableId.of(ApplicationConstants.DATA_SET, tableName);
			InsertAllRequest.Builder builder = InsertAllRequest.newBuilder(tableId);
			for (Map<String, Object> row : rowData) {
				builder.addRow(row);
			}
			try {
				InsertAllResponse response = bigQuery.insertAll(builder.build());
				if (response.hasErrors()) {
					response.getInsertErrors().forEach((index, error) -> {
						System.err.println("Row at index " + index + " failed with errors:" + error);
					});

				} else {
					LOGGER.info("Inserted " + rowData.size() + " rows to " + tableId);
				}

			} catch (BigQueryException e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		} else {
			LOGGER.info("bigQuery object is null. Please check application logs");
		}

	}

	private static String getVectorQuery() {
		StringBuilder builder = new StringBuilder();
		InputStream inputStream = BigQueryUtility.class.getClassLoader().getResourceAsStream("VectorSearch.sql");
		BufferedReader br = null;
		try {
			if (null != inputStream) {
				br = new BufferedReader(new InputStreamReader(inputStream));
				String line = null;
				while ((line = br.readLine()) != null) {
					builder.append(line);
					builder.append("\n");
				}
			} else {
				LOGGER.info("InputStrean for is null. Please check application logs");
				return "";

			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			if (null != br) {
				try {
					br.close();
					LOGGER.info("Closing inputstream..");
				} catch (IOException e) {
					// TODO: handle exception
					e.printStackTrace();
				}

			}
		}

		return builder.toString();
	}

	public static List<VectorSearchResultsModel> vectorSearch(String userid, int topK, List<Double> queryVector) {

		List<VectorSearchResultsModel> vectorSearchResultsModels = new ArrayList<VectorSearchResultsModel>();
		if (null != bigQuery) {
			String vectorSearchQuery = getVectorQuery();
			if (null != vectorSearchQuery && !vectorSearchQuery.trim().isEmpty()) {
				// prepare query Configuration with parameters
				QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration.newBuilder(vectorSearchQuery)
						.addNamedParameter("userId", QueryParameterValue.string(userid))
						.addNamedParameter("topK", QueryParameterValue.int64(topK))
						.addNamedParameter("queryVector", QueryParameterValue.array(queryVector.toArray(new Double[0]),
								StandardSQLTypeName.FLOAT64))
						.build();
				// Run the Query
				try {
					TableResult tableResult = bigQuery.query(queryJobConfiguration);
					Iterable<FieldValueList> iterable = tableResult.iterateAll();
					for (FieldValueList row : iterable) {
						VectorSearchResultsModel vectorSearchResultsModel = new VectorSearchResultsModel();
						vectorSearchResultsModel.setDocumentId(row.get("doc_id").getStringValue());
						List<Double> distanceList = new ArrayList<Double>();
						List<FieldValue> distancevalues = row.get("distances").getRepeatedValue();
						if (null != distancevalues) {
							distancevalues.forEach(fv -> {
								distanceList.add(fv.getDoubleValue());
							});
						}
						vectorSearchResultsModel.setDistanceList(distanceList);
						vectorSearchResultsModel.setNumberOfChunks(row.get("numberOfChucks").getLongValue());
						vectorSearchResultsModel.setContentType(row.get("contenttype").getStringValue());
						long micros = row.get("created_at").getTimestampValue();
						Instant instant = Instant.ofEpochMilli(micros / 1000);
						LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
						vectorSearchResultsModel.setCreatedAt(localDateTime.toString());
						vectorSearchResultsModels.add(vectorSearchResultsModel);
					}

				} catch (InterruptedException e) {
					// TODO: handle exception
					LOGGER.info("if the current thread gets interrupted while waiting for the query to complete:"
							+ e.getMessage());
					e.printStackTrace();
					throw new PrivateDocumentException(e.getMessage(), e, 500);

				} catch (JobException e) {
					// TODO: handle exception
					LOGGER.info("if the job completes unsuccessfully:" + e.getMessage());
					e.printStackTrace();
					throw new PrivateDocumentException(e.getMessage(), e, 500);

				} catch (BigQueryException e) {
					// TODO: handle exception
					LOGGER.info("upon failure:" + e.getMessage());
					e.printStackTrace();
					throw new PrivateDocumentException(e.getMessage(), e, 500);
				}

			} else {
				LOGGER.info("VectorSearchQuery is either null or empty. Please check application logs");
				throw new PrivateDocumentException(
						"VectorSearchQuery is either null or empty. Please check application logs", 500);
			}
		} else {
			LOGGER.info("bigQuery object is null. Please check application logs.");
			throw new PrivateDocumentException("bigQuery object is null. Please check application logs.", 500);
		}

		return vectorSearchResultsModels;
	}

}
