package com.linkedin.pinot.query.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.linkedin.pinot.index.query.FilterQuery;
import com.linkedin.pinot.index.segment.IndexSegment;
import com.linkedin.pinot.query.aggregation.AggregationResult;
import com.linkedin.pinot.query.aggregation.CombineLevel;
import com.linkedin.pinot.query.aggregation.CombineReduceService;
import com.linkedin.pinot.query.request.Query;
import com.linkedin.pinot.query.utils.IndexSegmentUtils;
import com.linkedin.pinot.server.utils.NamedThreadFactory;


public class TestSinglePlanExecutor {
  private static ExecutorService _globalExecutorService;

  @BeforeClass
  public static void setup() {
    //_globalExecutorService = Executors.newFixedThreadPool(20, new NamedThreadFactory("test-plan-executor-global"));
    _globalExecutorService = Executors.newCachedThreadPool(new NamedThreadFactory("test-plan-executor-global"));

  }

  @Test
  public void testCountQuery() {
    int numDocsPerSegment = 20000001;
    int numJobs = 4;
    int numSegmentsPerJob = 1;
    Query query = getCountQuery();
    List<List<IndexSegment>> indexSegmentsList = new ArrayList<List<IndexSegment>>();
    new ArrayList<IndexSegment>();

    for (int i = 0; i < numJobs; ++i) {
      indexSegmentsList.add(new ArrayList<IndexSegment>());
      for (int j = 0; j < numSegmentsPerJob; ++j) {
        indexSegmentsList.get(i).add(IndexSegmentUtils.getIndexSegmentWithAscendingOrderValues(numDocsPerSegment));
      }
    }
    // long startTime = System.currentTimeMillis();
    long endTime = 0;
    List<Future<List<List<AggregationResult>>>> producerJobs = new ArrayList<Future<List<List<AggregationResult>>>>();
    for (int i = 0; i < numJobs; ++i) {
      producerJobs.add(_globalExecutorService.submit(new SingleThreadMultiSegmentsWorker(i, indexSegmentsList.get(i),
          query)));
    }
    List<List<AggregationResult>> instanceResults = new ArrayList<List<AggregationResult>>();
    for (int i = 0; i < query.getAggregationJSONArray().length(); ++i) {
      instanceResults.add(new ArrayList<AggregationResult>());
    }
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numJobs; ++i) {
      try {
        List<List<AggregationResult>> segmentResults = producerJobs.get(i).get(100000, TimeUnit.MILLISECONDS);
        for (int j = 0; j < segmentResults.size(); ++j) {
          instanceResults.get(j).addAll(segmentResults.get(j));
        }
        endTime = System.currentTimeMillis();
        System.out.println("Time used : " + (endTime - startTime));
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (TimeoutException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    endTime = System.currentTimeMillis();
    CombineReduceService.combine(query.getAggregationFunction(), instanceResults, CombineLevel.INSTANCE);

    System.out.println("Time used : " + (endTime - startTime));
    for (int j = 0; j < instanceResults.size(); ++j) {
      System.out.println(instanceResults.get(j).get(0).toString());
    }
  }

  @Test
  public void testSumQuery() {
    int numDocsPerSegment = 20000001;
    int numJobs = 1;
    int numSegmentsPerJob = 8;

    Query query = getSumQuery();
    List<List<IndexSegment>> indexSegmentsList = new ArrayList<List<IndexSegment>>();
    new ArrayList<IndexSegment>();

    for (int i = 0; i < numJobs; ++i) {
      indexSegmentsList.add(new ArrayList<IndexSegment>());
      for (int j = 0; j < numSegmentsPerJob; ++j) {
        indexSegmentsList.get(i).add(IndexSegmentUtils.getIndexSegmentWithAscendingOrderValues(numDocsPerSegment));
      }
    }
    long startTime = System.currentTimeMillis();
    long endTime = 0;
    List<Future<List<List<AggregationResult>>>> producerJobs = new ArrayList<Future<List<List<AggregationResult>>>>();
    for (int i = 0; i < numJobs; ++i) {
      producerJobs.add(_globalExecutorService.submit(new SingleThreadMultiSegmentsWorker(i, indexSegmentsList.get(i),
          query)));
    }
    List<List<AggregationResult>> instanceResults = new ArrayList<List<AggregationResult>>();
    for (int i = 0; i < query.getAggregationJSONArray().length(); ++i) {
      instanceResults.add(new ArrayList<AggregationResult>());
    }

    for (int i = 0; i < numJobs; ++i) {
      try {
        List<List<AggregationResult>> segmentResults = producerJobs.get(i).get(100000, TimeUnit.MILLISECONDS);
        for (int j = 0; j < segmentResults.size(); ++j) {
          instanceResults.get(j).addAll(segmentResults.get(j));
        }
        endTime = System.currentTimeMillis();
        System.out.println("Time used : " + (endTime - startTime));
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (TimeoutException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    endTime = System.currentTimeMillis();
    CombineReduceService.combine(query.getAggregationFunction(), instanceResults, CombineLevel.INSTANCE);

    System.out.println("Total Time used : " + (endTime - startTime));
    for (int j = 0; j < instanceResults.size(); ++j) {
      System.out.println(instanceResults.get(j).get(0).toString());
    }
  }

  private Query getCountQuery() {
    Query query = new Query();
    JSONArray aggregationJsonArray = getCountAggregationJsonArray();
    query.setAggregations(aggregationJsonArray);
    FilterQuery filterQuery = getFilterQuery();
    query.setFilterQuery(filterQuery);
    return query;
  }

  private Query getSumQuery() {
    Query query = new Query();
    JSONArray aggregationJsonArray = getSumAggregationJsonArray();
    query.setAggregations(aggregationJsonArray);
    FilterQuery filterQuery = getFilterQuery();
    query.setFilterQuery(filterQuery);
    return query;
  }

  private FilterQuery getFilterQuery() {
    FilterQuery filterQuery = new FilterQuery();
    return filterQuery;
  }

  private JSONArray getCountAggregationJsonArray() {
    JSONArray aggregationJsonArray = new JSONArray();
    JSONObject paramsJsonObject = new JSONObject();
    paramsJsonObject.put("column", "met");
    JSONObject functionJsonObject = new JSONObject();

    functionJsonObject.put("function", "count");
    functionJsonObject.put("params", paramsJsonObject);

    aggregationJsonArray.put(functionJsonObject);
    return aggregationJsonArray;
  }

  private JSONArray getSumAggregationJsonArray() {
    JSONArray aggregationJsonArray = new JSONArray();
    JSONObject paramsJsonObject = new JSONObject();
    paramsJsonObject.put("column", "met");
    JSONObject functionJsonObject = new JSONObject();

    functionJsonObject.put("function", "sum");
    functionJsonObject.put("params", paramsJsonObject);

    aggregationJsonArray.put(functionJsonObject);
    return aggregationJsonArray;
  }

}