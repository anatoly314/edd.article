package edd.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edd.database.providers.ConnectionProvider;
import edd.entities.Book;
import edd.exceptions.BookNotFoundException;

public class DbBookReader {
	public static Book getBookById(int id) throws SQLException, BookNotFoundException{
		Book book = null;
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		
		String sql = "SELECT * FROM BOOKS WHERE ID = ?";
		
		try {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		
		return book;
	}
	
}
