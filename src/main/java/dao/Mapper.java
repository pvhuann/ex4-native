package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import entity.Address;
import entity.Customer;
import entity.Phone;

public class Mapper {

	public static Customer fromDocument(Document doc) {
		
		Document addDoc = (Document) doc.get("address");
//		System.out.println(addDoc);
		Address address = new Address(addDoc.getString("city"), 
				addDoc.getString("state"), 
				addDoc.getString("street"), 
				addDoc.getInteger("zop_code"));
		
		List<Document> docs = (List<Document>) doc.get("phones");
		List<Phone> phones = null;
		
		
		if(docs != null) {
			phones = new ArrayList<>();
			for(Document temp : docs) {
				phones.add(new Phone(temp.getString("number"), temp.getString("type")));
			}
		}
		Customer customer = new Customer(doc.getString("_id"), 
				doc.getString("first_name"), 
				doc.getString("last_name"), address, doc.getDate("registration_date"), 
				doc.getString("email"));
		
		if(phones != null)
			customer.setPhones(phones);
		
		return customer;
	}
	
	
	
	
	

}
