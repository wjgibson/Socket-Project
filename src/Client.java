import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
//import java.util.Random;
import java.util.Scanner;

@SuppressWarnings("ALL")
public class Client {

    //size of the reply codes from the server
    //(reply code indicates whether the client request was accepted or rejected by server)
    private final static int SERVER_CODE_LENGTH = 1;

    public static void main(String[] args) throws IOException{

        if (args.length != 2) {
            System.err.println("Usage: java Client <server_IP> <server_port>");
            System.exit(0);
        }

        int serverPort = Integer.parseUnsignedInt(args[1]);
        String serverAddr = args[0];
        String clientMessage;
        ByteBuffer messageBuffer;

        String command;
        do{
            Scanner keyboard = new Scanner(System.in);
            System.out.println("enter a command (D, G, L, R, or Q):");
            //Commands are NOT case-sensitive.
            command = keyboard.next().toUpperCase();
            keyboard.nextLine();

            switch (command){
                case "L":
                    //List all files (ignoring directories) in the server directory
                    //(file name : file size)
                    ByteBuffer listBuffer = ByteBuffer.wrap("L".getBytes());
                    SocketChannel listChannel = SocketChannel.open();
                    listChannel.connect(new InetSocketAddress(serverAddr, serverPort));
                    //System.out.println("TCP connection established.");

                    //The random sleep is for testing purpose only!
                    // try {
                    //    Thread.sleep(new Random().nextInt(20000));
                    // }catch(InterruptedException e){;}

                    //read from the buffer into the channel
                    listChannel.write(listBuffer);

                    //before writing to buffer, clear buffer
                    //("position" set to zero, "limit" set to "capacity")
                    listBuffer.clear();

                    int bytesRead;
                    //read will return -1 if the server has closed the TCP connection
                    // (when server has finished sending)
                    if (serverCode(listChannel).equals("F")){
                        System.out.println("Server rejected the request.");
                    }else {
                        ByteBuffer data = ByteBuffer.allocate(1024);
                        while( (bytesRead = listChannel.read(data)) != -1) {
                            //before reading from buffer, flip buffer
                            //("limit" set to current position, "position" set to zero)
                            data.flip();
                            byte[] a = new byte[bytesRead];
                            //copy bytes from buffer to array
                            //(all bytes between "position" and "limit" are copied)
                            data.get(a);
                            String serverMessage = new String(a);
                            System.out.println(serverMessage);
                        }
                    }
                    listChannel.close();
                    break;

                case "D":
                    SocketChannel deleteChannel = SocketChannel.open();
                    deleteChannel.connect(new InetSocketAddress(serverAddr, serverPort));

                    System.out.println("Please enter the name of the file you wish to delete");
                    String fileToBeDeleted = keyboard.nextLine();

                    clientMessage = "D" + fileToBeDeleted;

                    messageBuffer = ByteBuffer.wrap(clientMessage.getBytes());
                    deleteChannel.write(messageBuffer);

                    deleteChannel.shutdownOutput();

                    if (serverCode(deleteChannel).equals("F")){
                        System.out.println("Server rejected the request.");
                    }
                    else{
                        System.out.println("File was deleted.");
                    }
                    deleteChannel.close();
                    break;

                case "G":
                    SocketChannel grabChannel = SocketChannel.open();
                    grabChannel.connect(new InetSocketAddress(serverAddr, serverPort));

                    System.out.println("Please enter the name of the file you wish to grab");
                    String fileToBeGrabbed = keyboard.nextLine();

                    clientMessage = "G" + fileToBeGrabbed;

                    messageBuffer = ByteBuffer.wrap(clientMessage.getBytes());
                    grabChannel.write(messageBuffer);

                    grabChannel.shutdownOutput();

                    int bytesReadG;
                    if (serverCode(grabChannel).equals("S")){
                        ByteBuffer data = ByteBuffer.allocate(1024);
                        while( grabChannel.read(data) != -1);
                        data.flip();
                        byte[] g = new byte[data.remaining()];
                        data.get(g);
                        String serverMessage = new String(g).substring(1);
                        try {
                            File grabbedFile = new File("files/g" + fileToBeGrabbed);
                            if (grabbedFile.createNewFile()) {
                                FileWriter grabbedFileWriter = new FileWriter("files/g" + fileToBeGrabbed);
                                grabbedFileWriter.write(serverMessage);
                                grabbedFileWriter.close();
                            }
                        } catch (IOException e) {
                            System.out.println("Something went wrong.");
                        }
                    }else {
                        System.out.println("Server rejected the request.");
                    }
                    grabChannel.close();
                    break;

                case "R":
                    SocketChannel renameChannel = SocketChannel.open();
                    renameChannel.connect(new InetSocketAddress(serverAddr, serverPort));

                    System.out.println("Please enter the name of the file you wish to rename");
                    String fileToBeRenamed = keyboard.nextLine();
                    System.out.println("Please enter the new name of the file");
                    String newFileName = keyboard.nextLine();

                    clientMessage = "R" + fileToBeRenamed + "&" + newFileName;

                    messageBuffer = ByteBuffer.wrap(clientMessage.getBytes());
                    renameChannel.write(messageBuffer);

                    renameChannel.shutdownOutput();

                    if (serverCode(renameChannel).equals("F")){
                        System.out.println("Server rejected the request.");
                    }
                    else{
                        System.out.println("File was renamed.");
                    }
                    renameChannel.close();
                    break;

                default:
                    if (!command.equals("Q")){
                        System.out.println("Unknown command!");
                    }
            }
        }while(!command.equals("Q"));
    }

    private static String serverCode(SocketChannel channel) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(SERVER_CODE_LENGTH);
        int bytesToRead = SERVER_CODE_LENGTH;

        //make sure we read the entire server reply
        while((bytesToRead -= channel.read(buffer)) > 0);

        //before reading from buffer, flip buffer
        buffer.flip();
        byte[] a = new byte[SERVER_CODE_LENGTH];
        //copy bytes from buffer to array
        buffer.get(a);
        String serverReplyCode = new String(a);

        //System.out.println(serverReplyCode);

        return serverReplyCode;
    }
}
