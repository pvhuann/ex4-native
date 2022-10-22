package app;

import com.mongodb.reactivestreams.client.MongoClient;

import dao.OrderDao;
import dao.ProductDao;
import db.DBConnection;

public class App {
	private static final String DB_NAME = "BikeStores";

	public static void main(String[] args) {
		
		MongoClient mongoClient = DBConnection.getInstance().getMongoClient();
		ProductDao productDao = new ProductDao(mongoClient , DB_NAME);
		OrderDao orderDao = new OrderDao(mongoClient, DB_NAME);
		orderDao.getOrdersByCustomers();
		
		orderDao.getOrdersByCustomers().entrySet().forEach(entry -> {
			System.out.println(entry.getKey());
			System.out.println("Nmber of orders: " + entry.getValue());
		});
		
		
//		orderDao.getOrdersByCustomerId()
//		.entrySet()
//		.forEach(entry -> {
//			System.out.println("Customer ID: " + entry.getKey());
//			System.out.println("Number of Orders: " + entry.getValue());
//			System.out.println("==========");
//		});
		
//		productDao.getProductsMax().forEach(p -> System.out.println(p));
//		Product product = productDao.getProductById(15l);
//		System.out.println(product);
	}
}
