package edd.restful;


import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edd.database.DbBookReader;
import edd.entities.Book;
import edd.exceptions.BookNotFoundException;


@Path("book")
public class BookAPI {
	
	private static Gson gson = new GsonBuilder()	//Our serializer based on Google Gson library
								.setPrettyPrinting()
								.create();
	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookById(@QueryParam(value = "id") String bookIdString) {
    	
    	System.out.println("The book id is: " + bookIdString);
    	
    	try {
    		int bookId = Integer.valueOf(bookIdString);
    		Book book = DbBookReader.getBookById(bookId);
    		String json = gson.toJson(book);
    		return Response.ok().entity(json).build(); 
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity(gson.toJson(e)).build();
		} catch (SQLException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(e)).build();
		} catch (BookNotFoundException e) {
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity(gson.toJson(e)).build();
		} 
    	
    }
}
