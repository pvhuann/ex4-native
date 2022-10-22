package dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.AggregatePublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;

import entity.Product;

public class ProductDao {
	
	private MongoCollection<Product> proCol;
	
	public ProductDao(MongoClient mongoClient, String dbName) {
		
		PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
		CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodecProvider));
		
		proCol = mongoClient.getDatabase(dbName).getCollection("products", Product.class).withCodecRegistry(codecRegistry);
		
	}
	
		
//	 db.products.find({_id:11110})
	public Product getProductById(long productId) {
		
		Publisher<Product> publisher = proCol.find(Filters.eq("_id", productId)).first();
		
		ProductSubscriber subscriber = new ProductSubscriber();
		
		publisher.subscribe(subscriber);
		
		List<Product> products = subscriber.getProducts();
		
		return products.size() > 0 ? products.get(0) : null;
	}
	
//	db.products.aggregate([{$group:{_id:null, products:{$push:"$$ROOT"},max:{$max:'$price'}}},
//	{$unwind:'$products'},
//	{$match:{$expr:{$eq:["$max","$products.price"]}}},
//	{$replaceWith:'$products'}]).pretty()
	public List<Product> getProductsMax() {
		
		 AggregatePublisher<Product> publisher = proCol.aggregate(Arrays.asList(
					Document.parse("{$group:{_id:null, products:{$push:\"$$ROOT\"},max:{$max:'$price'}}}"),
					Document.parse("{$unwind:'$products'}"),
					Document.parse("{$match:{$expr:{$eq:[\"$max\",\"$products.price\"]}}}"),
					Document.parse("{$replaceWith:'$products'}")
				));
		 
		ProductSubscriber subscriber = new ProductSubscriber();
		
		publisher.subscribe(subscriber);
		
		return subscriber.getProducts();
		
	}
	
}

class ProductSubscriber implements Subscriber<Product>{
	
	private CountDownLatch latch;
	private List<Product> products;
	private Subscription s;
	
	public ProductSubscriber() {
		latch = new CountDownLatch(1);
		products = new ArrayList<>();
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
	public void onNext(Product p) {
		products.add(p);
		this.s.request(1);
	}

	@Override
	public void onSubscribe(Subscription s) {
		this.s = s;
		this.s.request(1);
	}
	
	public List<Product> getProducts() {
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return products;
	}
	
}
