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
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.reactivestreams.client.AggregatePublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;

import entity.Product;

public class ProductDao2 {
	
	private MongoCollection<Product> proCol;
	
	public ProductDao2(MongoClient mongoClient, String dbName) {
		
		PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
		CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodecProvider));
		
		proCol = mongoClient.getDatabase(dbName).getCollection("products", Product.class).withCodecRegistry(codecRegistry);
		
	}
	
	public int addProdcuts(List<Product> products) {
		Publisher<InsertManyResult> publisher = proCol.insertMany(products);
		
		return 0;
	}
	
//	 db.products.find({_id:11110})
	
	Product product = null;
	public Product getProductById(long productId) {
		CountDownLatch latch = new CountDownLatch(1);
		
		Publisher<Product> publisher = proCol.find(Filters.eq("_id", productId)).first();
		
		Subscriber<Product> subcriber = new Subscriber<Product>() {
			

			@Override
			public void onComplete() {
				latch.countDown();
			}

			@Override
			public void onError(Throwable arg0) {
				arg0.printStackTrace();
			}

			@Override
			public void onNext(Product arg0) {
				product = arg0;
			}

			@Override
			public void onSubscribe(Subscription arg0) {
				arg0.request(1);
			}
		};
		
		publisher.subscribe(subcriber);
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return product;
	}
	
//	db.products.aggregate([{$group:{_id:null, products:{$push:"$$ROOT"},max:{$max:'$price'}}},
//	{$unwind:'$products'},
//	{$match:{$expr:{$eq:["$max","$products.price"]}}},
//	{$replaceWith:'$products'}]).pretty()
	List<Product> products = new ArrayList<Product>();
	public List<Product> getProductsMax() {
		
		CountDownLatch latch = new CountDownLatch(1);
		
		 AggregatePublisher<Product> publisher = proCol.aggregate(Arrays.asList(
					Document.parse("{$group:{_id:null, products:{$push:\"$$ROOT\"},max:{$max:'$price'}}}"),
					Document.parse("{$unwind:'$products'}"),
					Document.parse("{$match:{$expr:{$eq:[\"$max\",\"$products.price\"]}}}"),
					Document.parse("{$replaceWith:'$products'}")
				));
		 
		Subscriber<Product> subscriber = new Subscriber<Product>() {
			private Subscription s;
			

			public void onComplete() {
				latch.countDown();
			}

			public void onError(Throwable arg0) {
				arg0.printStackTrace();
			}

			public void onNext(Product p) {
				products.add(p);
				this.s.request(1);
			}

			public void onSubscribe(Subscription s) {
				this.s = s;
				this.s.request(1);
			}
		}; 
		
		publisher.subscribe(subscriber);
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return products;
	}
	
	
//	db.products.aggregate([{$lookup:{from:'orders', localField:'_id', foreignField:'order_details.product_id', as:'rs'}},
//	{$match:{rs:{$size:0}}},
//	{$unset:'rs'}]).pretty()
	
	public List<Product> getProductsBad() {
		AggregatePublisher<Product> publisher = proCol.aggregate(Arrays.asList(
					Document.parse("{$lookup:{from:'orders', localField:'_id', foreignField:'order_details.product_id', as:'rs'}}"),
					Document.parse("{$match:{rs:{$size:0}}}"),
					Document.parse("{$unset:'rs'}")
				));
		
		return null;
	}
	
}
