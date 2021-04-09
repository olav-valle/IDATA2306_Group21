package HTTPServer.Services;



import HTTPServer.HTTPServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class UploadUserImage {
    public static void main(String[] args) throws IOException {
        //1.define a method, get the image path;
        File path = getPath();
        System.out.println(path);
        //2.define a method, verify the image exist in the lib folder.

        //3.if it exist, remind user. upload fail.
        if(isExists(path.getName())){
            System.out.println("Image exists, fail to upload!");
        }else{
            //System.out.println("Uploading...");
            uploadImageFile(path);
            //FileInputStream fis = new FileInputStream(path);
        }
        //4. if it does not exist, upload the image and show upload succeed.

    }

    //1.define a method, get the image path;
    public static File getPath() {
        //1. remind the user to upload the image path and receive.

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("Please enter the path of upload file");
            String path = sc.nextLine();
            //2. judge the image whether it is : .jpg, .png or .bmp

            //3. if it is not, remind: you are not uploading right format.
            if (!path.endsWith(".jpg") && !path.endsWith(".png") && !path.endsWith(".bmp")) {
                System.out.println("You are not uploading a picture, try again!");
                System.out.println("Only JPG PNG and BMP files are accepted!");
                //don't forget:
                continue;
            }

            //4. if it is, judge whether the path exist, whether it is a file.

            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return file;
            } else {
                // 5. if it is not, remind: path not valid, upload again!.
                System.out.println("Path does not exist!");
            }

        }
        //6. if it is, return.

        // 7. because do not know how many times the user will be correct,
        // use while to improve.
    }
    //2.define a method, verify the image exist in the lib folder.
    public static Boolean isExists(String path){
        // .png
        //1. package lib folder as a File objec
        File file = new File("lib");
        //2.get all the file and folder name array.
        String[] names = file.list();
        //3. iterate the array and compare with the path
        for (String name : names) {
            if (name.equals(path)){
                //if they are the same:
                return true;
            }
        }

        return false;
    }
    //Uploading image
    public static void uploadImageFile(File path) throws IOException {
        //source   ->  destination
        //path\\1.jpg  -> despath\\1.jpg
        FileInputStream fis = new FileInputStream(path);
        FileOutputStream fos = new FileOutputStream("lib/"+path.getName());
        int len;
        while((len = fis.read())!=-1){
            fos.write(len);
        }
        fis.close();
        fos.close();
        System.out.println("Upload completed!");

    }
}
