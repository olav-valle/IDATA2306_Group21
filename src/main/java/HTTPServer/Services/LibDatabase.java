package HTTPServer.Services;

import java.sql.*;

public class LibDatabase {

    private static Connection con;
    private static boolean hasData = false;
    private static LibDatabase database;

    // Only for testing!
    public static void main(String[] args) {
        LibDatabase test = new LibDatabase();
        try {
            // clear all tables.
            con.createStatement().execute("DROP TABLE IF EXISTS books;");
            con.createStatement().execute("DROP TABLE IF EXISTS user;");
            con.createStatement().execute("DROP TABLE IF EXISTS library_branches;");
            con.createStatement().execute("DROP TABLE IF EXISTS book_loans;");
            con.createStatement().execute("DROP TABLE IF EXISTS branch_inventory;");

            test.createBooksTable();
            test.createBorrowTable();
            test.createLibraryBranchesTable();
            test.createUserTable();

            //task I
            System.out.println("Task 1");
            test.insertBookIntoBookTable("How to Horse", "Mr. Horse");

            //add filler data to tables
            for (int i = 1; i < 11; i++){
                test.insertBookIntoBookTable("book" + i, "publisher" + i);

                test.insertUserIntoUserTable("UserName" + i, "password123");
            }

            test.createLibraryBranchesTable();
            test.insertLibraryBranchIntoLibraryBranchTable("Branch A", "Street A");
            test.insertLibraryBranchIntoLibraryBranchTable("Branch B", "Street B");


            // user , book , branch , due date.
            test.createBorrowTable();
            test.insertBorrowIntoBorrowTable("1","1","1", "01.01.2000");
            test.insertBorrowIntoBorrowTable("2","2","1", "01.01.2000");
            test.insertBorrowIntoBorrowTable("3","3","2", "01.01.2000");
            test.insertBorrowIntoBorrowTable("2","4","2", "01.01.2000");
            test.insertBorrowIntoBorrowTable("2","4","1", "01.01.2000");
            test.insertBorrowIntoBorrowTable("2","3","2", "01.01.2000");
            test.insertBorrowIntoBorrowTable("1","2","2", "01.01.2000");
            //Changed last borrow from branch 1 to branch 2,
            // since branch 1 book 2 is loaned by borrower 2 (second line).
            System.out.println("test add books");

            //Task III
            System.out.println("Task 3");

            test.removeBorrowByIds("1", "1", "1");

            //print all books.
            System.out.println("\nPrint Books in table");
            ResultSet res =  test.getAllBooksFromBooksTable();
            while(res.next()){
                System.out.println("book_Id: " + res.getString(1));
                System.out.println("title: " + res.getString("title"));
                System.out.println("publisher: " + res.getString("publisher"));
                System.out.println(); //make some space
            }

            //task II
            System.out.println("Task 2");

            test.updateUser("1", "newUserName", "newStrongerPassword456!");

            System.out.println("\n\nUsers:");
            res = test.getAllUsers();
            while (res.next()){
                System.out.println("User_Id: " + res.getString(1));
                System.out.println("User Name: " + res.getString(2));
                System.out.println("User Password: " + res.getString(3));
                System.out.println();
            }

            //Task IV
            System.out.println("Task 4");

            res = test.findBorrowedBooksByTitle("book1");
            while (res.next()){
                System.out.println("title: " + res.getString("title"));
                System.out.println("Name of borrower: " + res.getString("name"));
                System.out.println("Branch id: " + res.getString("branch_id"));
                //todo: get branch name? Collides with borrower name in res object..?
            }

            System.out.println(); // make some space
            // task V
            System.out.println("Task 5");

            res = test.findBorrowedBooksByDueDateFromBranchName("Branch A","01.01.2000");
            while (res.next()){
                System.out.println();
                System.out.println("Branch: " + res.getString("branch"));
                System.out.println("Name: " + res.getString("name"));
            }

            // task VI

            System.out.println(); // make some space
            System.out.println("Task 6");

            res = test.getNumberOfBorrowsPerBranch();
            while (res.next()){
                System.out.print(res.getString("name") + ": ");
                System.out.println(res.getString("books_amount"));
            }




        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private LibDatabase(){
        getConnection();
        try {
            createBooksTable();
            createBorrowTable();
            createLibraryBranchesTable();
            createUserTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Singleton pattern.
     * There can only be one data base
     *
     * @return returns a singleton of this object
     */
    public static LibDatabase getDatabase(){
        if (database == null){
            database = new LibDatabase();
        }
        return database;
    }


    private void getConnection(){
        //drivers
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //database
        try {
            con = DriverManager.getConnection("jdbc:sqlite:SQLiteTest1.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    /* * * * * * * * *
     *  User Table.  *
     * * * * * * * * */

    /**
     * creates a DB of users.
     * a user has: ID, UserName, Password
     *
     * @throws SQLException
     */
    private synchronized void createUserTable() throws SQLException {
        if (con == null){ getConnection(); }

        Statement state = con.createStatement();
        state.execute("CREATE TABLE IF NOT EXISTS user (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "password TEXT " +
                ");");
    }

    /**
     *
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public ResultSet getAllUsers() throws SQLException {
        if (con == null){ getConnection(); }

        Statement state = con.createStatement();
        ResultSet res = state.executeQuery("SELECT * FROM user");
        return res;
    }

    public synchronized void updateUser(String id, String userName, String password) throws SQLException {
        PreparedStatement statement = con.prepareStatement("UPDATE user SET " +
                "name = ?," +
                "password= ?" +
                "Where id = ?");

        if (userName != null){
            statement.setString(1, userName);
        }
        if (password != null){
            statement.setString(2, password);
        }
        statement.setString(3, id);
        statement.executeUpdate();
    }

    /**
     *  create a new User and insert it in the database
     *  The class DB_AddUserBuilder can be used do this.
     *  name and password can not be null or empty string.
     *
     *  Ps. No sanitized is preformed here.
     *
     * @param userName User name
     * @param password user Password
     * @throws SQLException
     */
         public boolean insertUserIntoUserTable(String userName, String password) throws SQLException {
        //guards
        if (userName == null || userName.equals("")) {
            return false;
        }
        if (password == null || password.equals("")){
            return false;
        }

        //get connection
        if (con == null){
            getConnection();
        }

        PreparedStatement prep = con.prepareStatement("INSERT INTO user values(?,?,?);");
        prep.setString(2, userName);
        prep.setString(3, password);
        prep.execute();
        System.out.println("added user: " + userName + ":" + password);
        return true;
    }




    /* * * * * * * * *
     *  Books Table. *
     * * * * * * * * */

    /**
     * create books table
     * @throws SQLException
     */
    private synchronized void createBooksTable() throws SQLException {
        Statement state = con.createStatement();
        state.execute("CREATE TABLE IF NOT EXISTS books (" +
                "book_id INTEGER PRIMARY KEY," +
                "title varchar(255) NOT NULL DEFAULT ''," +
                "publisher varchar(255) NOT NULL DEFAULT ''" +
                ");");
    }

    /**
     *
     * @param title
     * @param publisher
     * @throws SQLException
     */
    private synchronized void insertBookIntoBookTable(String title, String publisher) throws SQLException {
        PreparedStatement prep = con.prepareStatement("INSERT INTO books values(?,?,?);");
        prep.setString(2,title);
        prep.setString(3,publisher);
        prep.execute();
    }

    public synchronized boolean insertBook(String title, String publisher) {
        boolean success = false;
        if ( !title.trim().isBlank() && !publisher.trim().isBlank()){
            try {
                insertBookIntoBookTable(title, publisher);
                success = true;
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return  success;
    }

    //todo: Java doc
    private synchronized ResultSet getAllBooksFromBooksTable() throws SQLException {

        Statement state = con.createStatement();
        ResultSet res = state.executeQuery("SELECT * FROM books");

        return res;
    }

    //todo: Java doc
    private void createLibraryBranchesTable() throws SQLException {
        Statement state = con.createStatement();
        state.execute("CREATE TABLE IF NOT EXISTS library_branches (" +
                "branch_id INTEGER PRIMARY KEY," +
                "name varchar(255) NOT NULL DEFAULT ''," +
                "address varchar(255) NOT NULL DEFAULT ''" +
                ");");
    }

    //todo: java Doc, Guards
    public synchronized void insertLibraryBranchIntoLibraryBranchTable(String name, String address) throws SQLException {

        PreparedStatement prep = con.prepareStatement("INSERT INTO library_branches values(?,?,?);");
        prep.setString(2,name);
        prep.setString(3,address);
        prep.execute();
    }





    //Borrows
    private synchronized void createBorrowTable() throws SQLException {
        Statement state = con.createStatement();
        state.execute("CREATE TABLE IF NOT EXISTS book_loans (" +
                "borrowers_id INTEGER NOT NULL," +
                "book_id INTEGER NOT NULL," +
                "branch_id INTEGER NOT NULL," +
                "due_date varchar(255) NOT NULL DEFAULT ''," +
                "UNIQUE (borrowers_id, book_id, branch_id)," +
                "PRIMARY KEY (borrowers_id, book_id, branch_id)" +
                ");");
    }


    public synchronized void insertBorrowIntoBorrowTable(
            String borrowerId, String bookId, String branchId, String due_date) throws SQLException {

        //check for duplicate entry before trying to insert
        if (findBorrowByIds(borrowerId, bookId, branchId) == null){
            PreparedStatement prep = con.prepareStatement("INSERT INTO book_loans values(?,?,?,?);");
            prep.setString(1,borrowerId);
            prep.setString(2,bookId);
            prep.setString(3,branchId);
            prep.setString(4,due_date);
            prep.execute();
        }
    }

    public synchronized void removeBorrowByIds(String borrowerId, String bookId, String branchId) throws SQLException {

        if (findBorrowByIds(borrowerId, bookId, branchId) == null){
            PreparedStatement prep = con.prepareStatement("DELETE FROM book_loans " +
                    "WHERE borrowers_id = ? " +
                    "AND book_id = ? " +
                    "AND branch_id = ?;");

            prep.setString(1,borrowerId);
            prep.setString(2,bookId);
            prep.setString(3,branchId);
            prep.executeUpdate();
        }
    }

    public synchronized String[] findBorrowByIds(String borrowerId, String bookId, String branchId) throws SQLException {
        String[] res = null;
        PreparedStatement prep = con.prepareStatement("SELECT * FROM book_loans " +
                "WHERE borrowers_id = ?" +
                "AND book_id = ?" +
                "AND branch_id = ?");
        prep.setString(1,borrowerId);
        prep.setString(2,bookId);
        prep.setString(3,branchId);

        ResultSet resultSet = prep.executeQuery();

        if (resultSet.next()){
            res = new String[4];
            res[0] = resultSet.getString(1);
            res[1] = resultSet.getString(2);
            res[2] = resultSet.getString(3);
            res[3] = resultSet.getString(4);
        }
        return res;
    }

    private synchronized ResultSet getAllBorrows() throws SQLException {
        if (con == null){ getConnection(); }

        Statement state = con.createStatement();
        ResultSet res = state.executeQuery("SELECT * FROM book_loans");

        return res;
    }


    public synchronized ResultSet getNumberOfBorrowsPerBranch() throws SQLException {
        if (con == null){ getConnection(); }

        Statement state = con.createStatement();
        ResultSet res = state.executeQuery("SELECT " +
                "COUNT(bl.book_id) AS books_amount, lb.name AS name " +
                "FROM book_loans bl " +
                "JOIN (SELECT * FROM library_branches lb) AS lb  ON lb.branch_id = bl.branch_id " +
                "GROUP BY bl.branch_id;");

        return res;
    }



    /**
     *
     * @param title
     * @return
     * @throws SQLException
     */
    public synchronized ResultSet findBorrowedBooksByTitle(String title) throws SQLException {
        PreparedStatement sqlQuery = con.prepareStatement("SELECT * FROM book_loans bl " +
                "JOIN (SELECT * FROM user u) AS u ON u.id = bl.borrowers_id " +
                "JOIN (SELECT * FROM books b) AS b ON b.book_id = bl.book_id " +
                "JOIN (SELECT * FROM library_branches lb) AS lb  ON lb.branch_id = bl.branch_id " +
                "WHERE b.title = ?;");
        sqlQuery.setString(1, title);
        ResultSet res = sqlQuery.executeQuery();

        return res;
    }


    public synchronized ResultSet findBorrowedBooksByDueDateFromBranchName(String branch, String dueDate) throws SQLException {
        PreparedStatement sqlQuery = con.prepareStatement("SELECT lb.name AS branch, u.name AS name FROM book_loans bl " +
                "JOIN (SELECT * FROM user u) AS u ON u.id = bl.borrowers_id " +
                "JOIN (SELECT * FROM books b) AS b ON b.book_id = bl.book_id " +
                "JOIN (SELECT * FROM library_branches lb) AS lb  ON lb.branch_id = bl.branch_id " +
                "WHERE lb.name = ? ");
        sqlQuery.setString(1, branch);
        //sqlQuery.setString(2, dueDate);
        ResultSet res = sqlQuery.executeQuery();

        return res;
    }


    /**
     * Drops all tables and adds filler data for testing.
     * @throws SQLException
     */
    private void initialize() throws SQLException {


        if(!hasData) {
            hasData = true;

            Statement state = con.createStatement();
            ResultSet res = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='user'");

            if (!res.next()){
                System.out.println("Building the User table");

                //create new user table, PS Do not store username and password in the same DB!!!!!!!!!!1!!1
                Statement state2 = con.createStatement();
                state2.execute("CREATE TABLE user (" +
                        "id INTEGER PRIMARY KEY," +
                        "name TEXT," +
                        "password TEXT " +
                        ");");

                //insert data
                PreparedStatement prep = con.prepareStatement("INSERT INTO user values(?,?,?);");
                prep.setString(2, "Jon ");
                prep.setString(3, "Jensen");
                prep.execute();

                PreparedStatement prep2 = con.prepareStatement("INSERT INTO user values(?,?,?);");
                prep2.setString(2, "Bob");
                prep2.setString(3, "Bakke");
                prep2.execute();
            }
        }
    }


}
