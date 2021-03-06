/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.client;

import java.io.InputStream;
import java.util.concurrent.Future;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests deserialization of a ResultSet given hardcoded Pinot results.
 *
 */
public class ResultSetGroupTest {
  @Test
  public void testDeserializeSelectionResultSet() {
    // Deserialize selection result
    ResultSetGroup resultSetGroup = getResultSet("selection.json");
    ResultSet resultSet = resultSetGroup.getResultSet(0);

    // Check length
    Assert.assertEquals(resultSetGroup.getResultSetCount(), 1, "Expected one result set for selection query");
    Assert.assertEquals(resultSet.getRowCount(), 24, "Mismatched selection query length");

    // Check that values match and are in the same order
    Assert.assertEquals(resultSet.getInt(0, 0), 84);
    Assert.assertEquals(resultSet.getLong(1, 0), 202L);
    Assert.assertEquals(resultSet.getString(2, 0), "95");
    Assert.assertEquals(resultSet.getInt(0, 78), 2014);

    // Check the columns
    Assert.assertEquals(resultSet.getColumnCount(), 79);
    Assert.assertEquals(resultSet.getColumnName(0), "ActualElapsedTime");
    Assert.assertEquals(resultSet.getColumnName(1), "AirTime");
  }

  @Test
  public void testDeserializeAggregationResultSet() throws Exception {
    // Deserialize aggregation result
    ResultSetGroup resultSetGroup = getResultSet("aggregation.json");

    // Check length and number of result groups
    Assert.assertEquals(resultSetGroup.getResultSetCount(), 1, "Result set count mismatch");
    ResultSet resultSet = resultSetGroup.getResultSet(0);
    Assert.assertEquals(resultSet.getGroupKeyLength(), 0, "Expected 0 length group key for non-group by aggregation query");
    Assert.assertEquals(resultSet.getRowCount(), 1, "Result group length mismatch");

    // Check that values match and are in the same order
    Assert.assertEquals(resultSet.getInt(0), 36542, "Mismatched int value");
    Assert.assertEquals(resultSet.getLong(0), 36542L, "Mismatched long value");
    Assert.assertEquals(resultSet.getString(0), "36542", "Mismatched String value");

    // Check the columns
    Assert.assertEquals(resultSet.getColumnCount(), 1);
    Assert.assertEquals(resultSet.getColumnName(0), "count_star");
  }

  @Test
  public void testDeserializeAggregationGroupByResultSet() {
    // Deserialize aggregation group by result
    ResultSetGroup resultSetGroup = getResultSet("aggregationGroupBy.json");

    // Check length and number of result groups
    Assert.assertEquals(resultSetGroup.getResultSetCount(), 1, "Result set count mismatch");
    ResultSet resultSet = resultSetGroup.getResultSet(0);
    Assert.assertEquals(resultSet.getGroupKeyLength(), 1, "Group key length mismatch");
    Assert.assertEquals(resultSet.getRowCount(), 10, "Result group length mismatch");

    // Check that values match and are in the same order
    Assert.assertEquals(resultSet.getGroupKeyInt(0, 0), 30194);
    Assert.assertEquals(resultSet.getGroupKeyLong(1, 0), 31057L);
    Assert.assertEquals(resultSet.getGroupKeyString(2, 0), "32467");
    Assert.assertEquals(resultSet.getInt(0), 3604);
    Assert.assertEquals(resultSet.getLong(1), 1804L);
    Assert.assertEquals(resultSet.getString(2), "1316");

    // Check the columns
    Assert.assertEquals(resultSet.getColumnCount(), 1);
    Assert.assertEquals(resultSet.getColumnName(0), "count_star");
  }

  @Test
  public void testDeserializeExceptionResultSet() {
    try {
      getResultSet("exception.json");
      Assert.fail("Execute should have thrown an exception");
    } catch (PinotClientException e) {
      // We expect an exception here
      e.printStackTrace();
    }
  }

  private ResultSetGroup getResultSet(String resourceName) {
    _dummyJsonTransport._resource = resourceName;
    Connection connection = ConnectionFactory.fromHostList("dummy");
    return connection.execute("dummy");
  }

  private DummyJsonTransport _dummyJsonTransport = new DummyJsonTransport();
  private PinotClientTransportFactory _previousTransportFactory = null;

  class DummyJsonTransport implements PinotClientTransport {
    public String _resource;

    @Override
    public BrokerResponse executeQuery(String brokerAddress, String query)
        throws PinotClientException {
      try {
        StringBuilder builder = new StringBuilder();
        InputStream stream = getClass().getResourceAsStream(_resource);
        int lastByte = stream.read();
        while (lastByte != -1) {
          builder.append((char)lastByte);
          lastByte = stream.read();
        }
        String jsonText = builder.toString();
        return BrokerResponse.fromJson(new JSONObject(jsonText));
      } catch (Exception e) {
        Assert.fail("Unexpected exception", e);
        return null;
      }
    }

    @Override
    public Future<BrokerResponse> executeQueryAsync(String brokerAddress, String query)
        throws PinotClientException {
      return null;
    }
  }

  class DummyJsonTransportFactory implements PinotClientTransportFactory {
    @Override
    public PinotClientTransport buildTransport() {
      return _dummyJsonTransport;
    }
  }

  @BeforeClass
  public void overridePinotClientTransport() {
    _previousTransportFactory = ConnectionFactory._transportFactory;
    ConnectionFactory._transportFactory = new DummyJsonTransportFactory();
  }

  @AfterClass
  public void resetPinotClientTransport() {
    ConnectionFactory._transportFactory = _previousTransportFactory;
  }
}
