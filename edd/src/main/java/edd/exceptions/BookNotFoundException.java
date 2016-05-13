package edd.exceptions;

public class BookNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public BookNotFoundException(int id){
		super("Book with id: " + id + " wasn't found");
	}
}
