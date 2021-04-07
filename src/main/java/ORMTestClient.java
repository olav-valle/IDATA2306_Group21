package AD2021.ORM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

class ORMClient extends Thread {
    static int ASSIGNMENT_NUM = 1;
    Socket s = new Socket(InetAddress.getByName("localhost"), 8081);
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
                pw.print("QUESTION" + number + "\r\n");
                //You can use here to print the sending message.
//                System.out.println(currentThread().getName() +"  "+ number);
            }

            pw.flush();

            while ((response = br.readLine()) != null) System.out.println(response);


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
