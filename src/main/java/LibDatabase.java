import java.sql.*;

public class LibDatabase {

    private static Connection con;
    private static boolean hasData = false;


    public static void main(String[] args) {
        LibDatabase test = new LibDatabase();
        ResultSet rs;
        try {
            rs = test.displayUsers();
            while (rs.next()){
                System.out.println("First name: " + rs.getString("fname"));
                System.out.println("Last name: " + rs.getString("lname") + "\n\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }

    public LibDatabase(){

    }


    /**
     * returns a list of users based on a serach input form user.
     * @param searchTerm The search query from user
     * @return
     */
    public String getUsers(String searchTerm){
        //Todo: sanitize user input!!!!

        String res = "";


        return res;
    }


    public ResultSet displayUsers() throws SQLException, ClassNotFoundException {
        if (con == null){
            getConnection();
        }

        Statement state = con.createStatement();
        ResultSet res = state.executeQuery("SELECT fname, lname FROM user");
        return res;
    }


    private void getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:SQLiteTest1.db");
        initialize();
    }

    private void initialize() throws SQLException {
        if(!hasData) {
            hasData = true;

            Statement state = con.createStatement();
            ResultSet res = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='user'");

            if (!res.next()){
                System.out.println("Building the User table");

                //create new user table
                Statement state2 = con.createStatement();
                state2.execute("CREATE TABLE user (id INTEGER PRIMARY KEY," +
                        "fName TEXT," +
                        "lName TEXT);");

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

    public void addUser(String firstName, String lastName) throws SQLException, ClassNotFoundException {
        if (con == null){
            getConnection();
        }

        PreparedStatement prep = con.prepareStatement("INSERT INTO user values(?,?,?);");
        prep.setString(2, firstName);
        prep.setString(3, lastName);
        prep.execute();

    }


}
