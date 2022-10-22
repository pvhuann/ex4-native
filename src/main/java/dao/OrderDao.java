package dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.bson.Document;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.google.gson.Gson;
import com.mongodb.reactivestreams.client.AggregatePublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;

import entity.Customer;

public class OrderDao {
	private static final Gson GSON = new Gson();
	private MongoCollection<Document> orderCol;

	public OrderDao(MongoClient mongoClient, String dbName) {
		orderCol = mongoClient.getDatabase("BikeStores").getCollection("orders");
	}

	//	db.orders.aggregate([{$group:{_id:'$customer.customer_id','number od orders':{$sum:1}}}])
	public Map<String, Integer> getOrdersByCustomerId() {

		Map<String, Integer> map = new HashMap<>();

		AggregatePublisher<Document> publisher = orderCol.aggregate(Arrays.asList(Document.parse("{$group:{_id:'$customer.customer_id','number od orders':{$sum:1}}}")));
		OrderSubscriber subscriber = new OrderSubscriber();

		publisher.subscribe(subscriber);

		List<Document> documents = subscriber.getDocuments();

		for(Document doc : documents) {
			map.put(doc.getString("_id"), doc.getInteger("number od orders"));
		}

		return map;
	}
	
//	> db.orders.aggregate([{$group:{_id:'$customer.customer_id','number od orders':{$sum:1}}}, 
//	{$lookup:{from:'customers', localField:'_id', foreignField:'_id', as:'customer'}},
//	{$unwind:'$customer'}]).pretty()
	
	
	public Map<Customer, Integer> getOrdersByCustomers() {
		
		Map<Customer, Integer> map = new HashMap<>();
		
		AggregatePublisher<Document> publisher = orderCol.aggregate(Arrays.asList(
					Document.parse("{$group:{_id:'$customer.customer_id','number od orders':{$sum:1}}}"),
					Document.parse("{$lookup:{from:'customers', localField:'_id', foreignField:'_id', as:'customer'}}"),
					Document.parse("{$unwind:'$customer'}")
				));
		
		OrderSubscriber subscriber = new OrderSubscriber();
		
		publisher.subscribe(subscriber);
		
		List<Document> documents = subscriber.getDocuments();
		for(Document doc : documents) {
			Document t = (Document) doc.get("customer");
			
//			Customer customer = new Gson().fromJson(t.toJson(), Customer.class);
			
			Customer cus = Mapper.fromDocument(t);
			Integer number = doc.getInteger("number od orders");
			
			map.put(cus, number);
		}
		
		return map;
	}
}

class OrderSubscriber implements Subscriber<Document>{

	private CountDownLatch latch;
	private List<Document> docs;
	private Subscription s;

	public OrderSubscriber() {
		latch = new CountDownLatch(1);
		docs = new ArrayList<>();
	}

	@Override
	public void onComplete() {
		latch.countDown();
	}

	@Override
	public void onError(Throwable t) {
		t.printStackTrace();
	}

	@Override
	public void onNext(Document doc) {
		docs.add(doc);
		this.s.request(1);
	}

	@Override
	public void onSubscribe(Subscription s) {
		this.s = s;
		this.s.request(1);
	}

	public List<Document> getDocuments() {

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return docs;
	}

}


