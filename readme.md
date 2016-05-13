# Exception driven development


>Programming credo I lived by, is that: the line of code is the most elegant, has by far the least bugs, is the easiest to write, is the easiest to someone else to understand your code, that line of code you have never write.
> -- <cite>[Paul Hegarty][5]</cite>

### 0. Motivation

I think every programmer asks himself sooner or later when to catch exception, when to throw exception and when return null and when return exception. After more then two years in JavaEE development I have gained some good practices which I'd like to share with others. 

I will base my example on a simple WebService which responsible for getting book by provided id of type `int` from database. 
What our requirements from this WebService? 

1. If book found it will return [HTTP code 200][6] and Json representation of the book
2. <a name="error404">If it book isn't exists it will return 404 error.</a> Why? See explanation here: [When to use HTTP status code 404 in an API][1]
3. If it didn't due to error (database error, id contains wrong characters, etc) it will return an appropriate error message

So we have requirements now, lets try to implement this with less lines of code while trying to keep it simple, readable, maintainable and expandable.


### 1. Skeleton
We should start from something, sometimes it's most difficult part of a process, so let make it simple. The method below receives parameter, prints it and returns [HTTP code 200][6], everything ok.

```Java
@Path("book")
public class BookAPI {
	
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBookById(@QueryParam(value = "id") String bookIdString) {
    	
    	System.out.println("The book id is: " + bookIdString);
    	
        return Response.ok().build();
    }
}
```

### 2. Parsing String to Integer
We will use the following line of code to parse string to integer:

```
int bookId = Integer.valueOf(bookIdString);
```

But what will happen if we get unparsable string for example something like `123a`? 
In such situation [static Integer valueOf(String s)](https://docs.oracle.com/javase/7/docs/api/java/lang/Integer.html#valueOf(java.lang.String)) will throw an [NumberFormatException][2] which we need to catch and return meaningful [HTTP response][3].

>Actually the gold rule is following: no exception should be ignored. The following lines of code are inappropriate:
>
>```
>try{
>  ...
>  //code which can throw an exception
>  ...
>}catch{Exception e}{
>    //who cares?
>}
>``` 

So we will handle it as follow: 

```
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response getBookById(@QueryParam(value = "id") String bookIdString) {
  	System.out.println("The book id is: " + bookIdString);
    try {
    	int bookId = Integer.valueOf(bookIdString);
	} catch (NumberFormatException e) {
		e.printStackTrace();
		return Response.status(Status.BAD_REQUEST).build();
	}
    return Response.ok().build();
}
```

Now if [NumberFormatException][2] will be thrown we will print error to IDE console and return [HTTP error code 400][9] which meaning:
>The request could not be understood by the server due to malformed syntax. The client SHOULD NOT repeat the request without modifications.

If everything is ok our WebService will return [HTTP code 200][6] which meaning:
>The request has succeeded. The information returned with the response is dependent on the method used in the request.

### 3. Method to retrieve book by id from database
Lets write code which will do actual work, return book from database.

```
public class DbBookReader {
	public static Book getBookById(int id) throws SQLException, BookNotFoundException{
		Book book = null;
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		
		String sql = "SELECT * FROM BOOKS WHERE ID = ?";
		
		try {
    		//ConnectionProvider is helper class to get Connections
  			con = ConnectionProvider.getConnection(); 
  			
			ps = con.prepareStatement(sql);
			ps.setInt(1, id);
			rs = ps.executeQuery();
			
			while(rs.next()){
				book = new Book();
				book.setId(id);
				book.setTitle(rs.getString("TITLE"));
				book.setAuthorName(rs.getString("AUTHOR_NAME"));
			}
			
			if(book == null){
				throw new BookNotFoundException(id);
			}
			
		} catch (SQLException e) {
  			e.printStackTrace();
			throw e;
		}
		
		return book;
	}
}
```

What do happen here? First of all we don't return `null` if book with such `id` doesn't exist, but throwing `BookNotFoundException`

```
public class BookNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public BookNotFoundException(int id){
		super("Book with id: " + id + " wasn't found");
	}
}
```

Why throwing an exception and not returning `null`? Actually it's matter of convention, in this method I specify an `id` of book which I expect does exist, if it doesn't it's error and `BookNotFoundException` should be thrown. If we just perform search and return for example `Set` of books which can contain multiple books or can be empty we won't be throwing exception, but returning empty `Set`, for example like this:

```
public static List<Book> getBooksById(int id) throws SQLException{
		...
} 
```

Another reason is behavior consistency across different part of our program. If my object doesn't exists [our WebService will return 404](#error404) not 200 and empty object, so we'll expect that method which used by WebService will return exception too, not `null` object.

Next interesting thing is catching `SQLException` and then immediately throwing it back, why do we need it? We catching exception ASAP to make our debugging life easier, you won't want to move back and force across your code to see which exception were thrown and why was it thrown, for more details see this answer: [Exceptions: Why throw early? Why catch late?][4]. And we rethrowing it to let our WebService know that something went wrong, you'll see how it work in final implementation of our WebService.

### 4.WebService - Final Implementation

And this is our result:

```
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
```
We have added `Gson` object which serialize our `Book` object to Json String:

```
private static Gson gson = new GsonBuilder()	
								.setPrettyPrinting()
								.create();
```

In try block we're trying to perform our job:

```
int bookId = Integer.valueOf(bookIdString);
Book book = DbBookReader.getBookById(bookId);
String json = gson.toJson(book);
return Response.ok().entity(json).build();
```

If everything will work as expected we will get `Book` object, convert it to Json and then return response [200][6] with our json as entity, and our client we will see:

```
{
  "id": 5,
  "title": "Atlas Shrugged",
  "authorName": "Ayn Rand"
}
```

If something went wrong one of our `catch` clauses will be activated and will handle it, returning one of the following HTTP errors codes: [400][9], [404][7] or [500][8].


### 5. Conclusion

- Our Webservice **easy to read** and/or **maintain**, it has linear workflow without multiple `if/else`. 
- It's **expandable**. If we need to handle another error we just add `catch` clause, without changing general workflow. 
- We not only returning appropriate HTTP codes we also serialize and return Exception's message. It can help debugging process, make application more user friendly or just do nothing it's wholly up to client's implementation.


[1]: http://programmers.stackexchange.com/questions/203492/when-to-use-http-status-code-404-in-an-api
[2]: https://docs.oracle.com/javase/7/docs/api/java/lang/NumberFormatException.html
[3]: https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html
[4]: http://programmers.stackexchange.com/a/231059/161072
[5]: https://www.youtube.com/watch?v=oHcToNCn4K8
[6]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Response_codes#200
[7]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Response_codes#404
[8]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Response_codes#500
[9]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Response_codes#400
