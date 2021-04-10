
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

class ORMClient extends Thread {
    static int ASSIGNMENT_NUM = 1;
    Socket s = new Socket(InetAddress.getByName("localhost"), 8082);
    //Socket s = new Socket(InetAddress.getByName("158.38.101.105"), 80);
    PrintWriter pw = new PrintWriter(s.getOutputStream());

    String response;
    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));

    int number;
    ORMClient() throws IOException {
        number = ASSIGNMENT_NUM++;

    }


    @Override
    public void run() {
        //request first line formation.
        try {
            pw.print("POST /");
            pw.print("ORM-Lib");

            pw.print(" HTTP/1.1\r\n");
            //request headers formation.
            pw.print("Host: localhost\r\n\r\n");
            //request body formation.

            if (number < 6) {
                pw.print(currentThread().getName() + " with ");
                pw.print("QUESTION" + number + "\n");
                //You can use here to print the sending message.
//                System.out.println(currentThread().getName() +"  "+ number);
                switch (number) {
                    case 1:

                        String title = "title";
                        String publisher = "publisher";
                        //String publisher = "pub'); DROP TABLE books;--";

                        pw.print("insertBook" + "\n");
                        pw.print("title=" + title + "\n");
                        pw.print("publisher=" + publisher + "\n");
                        break;

                    case 2:
                        String id = "title";
                        String newName = "publisher";
                        String newPassword = "newPassword";

                        pw.print("updateUser" + "\n");
                        pw.print("id=" + id + "\n");
                        pw.print("newName=" + newName + "\n");
                        pw.print("newPassword=" + newPassword + "\n");
                        break;
                    case 3:
                        pw.print("updateUser\n");

                        String borrowers_id = "1";
                        String book_id = "1";
                        String branch_id = "1";

                        pw.print("borrowersId=" + borrowers_id + "\n");
                        pw.print("bookId=" + book_id + "\n");
                        pw.print("branchId=" + branch_id + "\n");
                        break;
                    case 4:
                        pw.print("findBorrowedBooksByTitle\n");

                        String User_Id;

                                pw.print("User_Id="+ "\n");

                        break;
                    case 5:
                        //todo: implement
                        break;
                    case 6:
                        //todo: implement
                        break;
                    default:
                        System.out.println("Not a valid: " + number);
                }
            }

            pw.flush();

            while ((response = br.readLine()) != null) System.out.println(currentThread().getName() + " " + response);


        }catch(Exception e){
            e.printStackTrace();
        }finally{
            pw.close();
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


}

public class ORMTestClient {

    public static void main(String[] args) throws IOException {


        ORMClient ormClient1 = new ORMClient();
        ORMClient ormClient2 = new ORMClient();
        ORMClient ormClient3 = new ORMClient();
        ORMClient ormClient4 = new ORMClient();
        ORMClient ormClient5 = new ORMClient();

        ormClient1.setName("Client 1 - ");
        ormClient2.setName("Client 2 - ");
        ormClient3.setName("Client 3 - ");
        ormClient4.setName("Client 4 - ");
        ormClient5.setName("Client 5 - ");

        ormClient1.start();
        ormClient2.start();
        ormClient3.start();
        ormClient4.start();
        ormClient5.start();

    }
}
