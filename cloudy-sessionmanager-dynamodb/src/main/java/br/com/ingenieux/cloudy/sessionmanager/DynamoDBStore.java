package br.com.ingenieux.cloudy.sessionmanager;

import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.management.ObjectName;

import org.apache.catalina.Session;
import org.apache.catalina.session.StoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.DeleteItemRequest;
import com.amazonaws.services.dynamodb.model.DeleteItemResult;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;

/**
 * The workhorse and backbone of the cloud session manager. This <b>Store</b>
 * implementation manages a dynamic list of references to session objects that
 * exist on various nodes within the cloud.
 * 
 */
public class DynamoDBStore extends StoreBase {

	/**
	 * Info on this implementation of a <b>Store</b>.
	 */
	static final String info = "DynamoDBStore/1.0";
	/**
	 * Name of this implementation.
	 */
	static final String name = "DynamoDBStore";

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected boolean DEBUG = log.isDebugEnabled();

	/**
	 * <b>ObjectName</b> we'll register ourself under in JMX so we can interact
	 * directly with the store.
	 */
	protected ObjectName objectName;

	protected AmazonDynamoDB _dynamoDB;

	protected String table;

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	protected String region;

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public DynamoDBStore() {
	}
	
	protected AmazonDynamoDB createDynamoDB() {
		ClientConfiguration clientConfig = new ClientConfiguration();

		clientConfig.setUserAgent(info);

		AmazonDynamoDBClient client = new AmazonDynamoDBClient(new BeanstalkerCredentialsProviderChain(), clientConfig);

		if (null != region) {
			client.setEndpoint(format("https://dynamodb.%s.amazonaws.com", region));
		}

		return client;
	}

	@Override
	public int getSize() throws IOException {
		AmazonDynamoDB dynamoDB = getClient();
		int result = 0;

		try {
			ScanRequest scanRequest = new ScanRequest(table).withAttributesToGet("id").withCount(true);
			boolean done = false;
			do {
				ScanResult scanResult = dynamoDB.scan(scanRequest);

				result += scanResult.getCount();
				done = (null != scanResult.getLastEvaluatedKey());

				scanRequest.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
			} while (!done);
		} catch (Exception exc) {
			if (log.isWarnEnabled())
				log.warn("getSize()", exc);

			throw new IOException(exc);
		}

		return result;
	}

	@Override
	public String[] keys() throws IOException {
		AmazonDynamoDB dynamoDB = getClient();
		List<String> result = new ArrayList<String>();

		try {
			ScanRequest scanRequest = new ScanRequest(table).withAttributesToGet("id");
			boolean done = false;
			do {
				ScanResult scanResult = dynamoDB.scan(scanRequest);

				for (Map<String, AttributeValue> item : scanResult.getItems())
					result.add(item.get("id").getS());

				done = (null != scanResult.getLastEvaluatedKey());

				scanRequest.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
			} while (!done);
		} catch (Exception exc) {
			if (log.isWarnEnabled())
				log.warn("keys()", exc);

			throw new IOException(exc);
		}

		return result.toArray(new String[result.size()]);
	}

	@Override
	public Session load(String id) throws ClassNotFoundException, IOException {
		AmazonDynamoDB dynamoDB = getClient();
		
		try {
			GetItemResult item = dynamoDB.getItem(new GetItemRequest(table, new Key(new AttributeValue().withS(id))));

			if (null != item) {
				DynamoDBSession session = new DynamoDBSession(manager);

				for (Map.Entry<String, AttributeValue> entry : item.getItem().entrySet()) {
					String key = entry.getKey();
					AttributeValue value = entry.getValue();

					if ("id".equals(key)) {
						session.setId(value.getS());
					} else {
						session.setAttribute(key, value.getS());
					}
				}

				return session;
			}

			return null;
		} catch (Exception exc) {
			if (log.isWarnEnabled())
				log.warn("load(id='{}')", id, exc);
			
			throw new IOException(exc);
		}
	}

	@Override
	public void remove(String id) throws IOException {
		AmazonDynamoDB dynamoDB = getClient();
		
		try {
			DeleteItemResult result = dynamoDB.deleteItem(new DeleteItemRequest(table, new Key(new AttributeValue().withS(id))));

		} catch (Exception exc) {
			if (log.isWarnEnabled())
				log.warn("remove(id='{}')", id, exc);
			
			throw new IOException(exc);
		}
	}

	@Override
	public void clear() throws IOException {
	}

	@Override
	public void save(Session session) throws IOException {
		AmazonDynamoDB dynamoDB = getClient();
		
		try {
			PutItemRequest request = new PutItemRequest().withTableName(table);
			
			Map<String, AttributeValue> itemMap = new TreeMap<String, AttributeValue>();
			
			itemMap.put("id", new AttributeValue().withS(session.getId()));
			
			Enumeration<String> attributeNames = ((DynamoDBSession) session).getAttributeNames();
			
			while (attributeNames.hasMoreElements()) {
				String key = attributeNames.nextElement();
				Object value = ((DynamoDBSession) session).getAttribute(key);
				
				itemMap.put(key, new AttributeValue().withS("" + value));
			}
			
			request.withItem(itemMap);
			
			dynamoDB.putItem(request);
			
		} catch (Exception exc) {
			if (log.isWarnEnabled())
				log.warn("save(id='{}')", session.getId(), exc);
			
			throw new IOException(exc);
		}
	}

	private AmazonDynamoDB getClient() {
		if (null != this._dynamoDB)
			return _dynamoDB;
		
		_dynamoDB = createDynamoDB();

		return _dynamoDB;
	}

	public boolean isValidSession(String id) {
		// TODO Auto-generated method stub
		return false;
	}
}