import java.security.PrivateKey;
import java.sql.*;

public class LibDatabase {

    private static Connection con;
    private static boolean hasData = false;
    private static LibDatabase database;

    // Only for testing!
    public static void main(String[] args) {
        LibDatabase test = new LibDatabase();
        try {
            test.createBooksTable();
            test.insertBookIntoBookTable("book", "publisher");

            test.createUserTable();
            test.insertUserIntoUserTable("UserName", "password123");

            test.createLibraryBranchesTable();
            test.insertLibraryBranchIntoLibraryBranchTable("Branch Name", "Street 5");


            test.createBorrowTable();
            test.insertBorrowIntoBorrowTable("1","1","1", "01.01.2000");
            System.out.println("test add books");


            //print all books.
            System.out.println("\nPrint Books in table");
            ResultSet res =  test.getAllBooksFromBooksTable();
            while(res.next()){
                System.out.println("title: " + res.getString("title"));
                System.out.println("publisher: " + res.getString("publisher"));
                System.out.println(); //make some space
            }



        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private LibDatabase(){
        getConnection();
    }


    private void createUserDB(){

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


    public ResultSet displayUsers() throws SQLException, ClassNotFoundException {
        if (con == null){
            getConnection();
        }

        Statement state = con.createStatement();
        ResultSet res = state.executeQuery("SELECT name FROM user");
        return res;
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
        Statement state = con.createStatement();
        state.execute("CREATE TABLE IF NOT EXISTS user (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT," +
                "password TEXT " +
                ");");
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

    //todo: Java doc
    private synchronized ResultSet getAllBooksFromBooksTable() throws SQLException {

        Statement state = con.createStatement();
        ResultSet res = state.executeQuery("SELECT title, publisher FROM books");

        return res;
    }

    //todo: Java doc
    private void createLibraryBranchesTable() throws SQLException {
        Statement state = con.createStatement();
        state.execute("CREATE TABLE IF NOT EXISTS books (" +
                "book_id INTEGER PRIMARY KEY," +
                "title varchar(255) NOT NULL DEFAULT ''," +
                "publisher varchar(255) NOT NULL DEFAULT ''" +
                ");");
    }

    //todo: java Doc, Guards
    private synchronized void insertLibraryBranchIntoLibraryBranchTable(String name, String address) throws SQLException {

        PreparedStatement prep = con.prepareStatement("INSERT INTO books values(?,?,?);");
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


    private synchronized void insertBorrowIntoBorrowTable(
            String borrowerId, String bookId, String branchId, String due_date) throws SQLException {

        //check for duplicate entry before trying to insert
        if (findBorrowByIds(borrowerId, bookId, branchId) == null){
            PreparedStatement prep = con.prepareStatement("INSERT INTO book_loans values(?,?,?,?);");
            prep.setString(1,borrowerId);
            prep.setString(2,bookId);
            prep.setString(3,branchId);
            prep.setString(4,branchId);
            prep.execute();
        }
    }

    private synchronized String[] findBorrowByIds(String borrowerId, String bookId, String branchId) throws SQLException {
        String[] res = null;
        Statement state = con.createStatement();
        ResultSet resultSet = state.executeQuery("SELECT borrowers_id, book_id, branch_id, due_date FROM book_loans");

        if (resultSet.next()){
            res = new String[3];
            res[0] = resultSet.getString(1);
            res[1] = resultSet.getString(2);
            res[2] = resultSet.getString(3);
        }
        return res;
    }



    //todo: remove this!
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
