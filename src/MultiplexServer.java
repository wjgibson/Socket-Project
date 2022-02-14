import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexServer {

    private static final int CLIENT_CODE_LENGTH = 1;

    public static void main(String[] args) throws IOException{

        //open a selector
        Selector monitor = Selector.open();

        ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
        welcomeChannel.socket().bind(new InetSocketAddress(2000));

        //configure the serverSocketChannel to be non-blocking
        //(selector only works with non-blocking channels.)
        welcomeChannel.configureBlocking(false);

        //register the channel and event to be monitored
        //this causes a "selection key" object to be created for this channel
        welcomeChannel.register(monitor, SelectionKey.OP_ACCEPT);

        while (true) {
            // select() is a blocking call (so there is NO busy waiting here)
            // It returns only after at least one channel is selected,
            // or the current thread is interrupted
            int readyChannels = monitor.select();

            //select() returns the number of keys, possibly zero
            if (readyChannels == 0) {
                continue;
            }

            // elements in this set are the keys that are ready
            // i.e., a registered event has happened in each of those keys
            Set<SelectionKey> readyKeys = monitor.selectedKeys();

            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();
                iterator.remove();

                if (key.isAcceptable()) {
                    // OS received a new connection request from some new client
                    ServerSocketChannel wChannel = (ServerSocketChannel) key.channel();
                    SocketChannel serveChannel = wChannel.accept();

                    //create the dedicated socket channel to serve the new client
                    serveChannel.configureBlocking(false);

                    //register the dedicated socket channel for reading
                    serveChannel.register(monitor, SelectionKey.OP_READ);
                }

                else if (key.isReadable()) {
                    //OS received one or more packets from one or more clients
                    SocketChannel serveChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(CLIENT_CODE_LENGTH);
                    int bytesToRead = CLIENT_CODE_LENGTH;

                    //make sure we read the entire server reply
                    while((bytesToRead -= serveChannel.read(buffer)) > 0);

                    byte[] a = new byte[CLIENT_CODE_LENGTH];
                    buffer.flip();
                    buffer.get(a);
                    String request = new String(a);
                    System.out.println("Request from client: " + request);

                    switch(request){
                        case "L":
                            //send reply code to indicate request was accepted
                            sendReplyCode(serveChannel, "S");

                            File[] filesList = new File(".").listFiles();
                            StringBuilder allFiles = new StringBuilder();
                            if (filesList != null){
                                for (File f : filesList){
                                    //ignore directories
                                    if (!f.isDirectory()) {
                                        allFiles.append(f.getName());
                                        allFiles.append(" : ");
                                        allFiles.append(f.length());
                                        allFiles.append("\n");
                                    }
                                }
                            }

                            ByteBuffer data = ByteBuffer.wrap(allFiles.toString().getBytes());
                            serveChannel.write(data);

                            break;

                        case "D":

                            ByteBuffer deleteData = ByteBuffer.allocate(1024);
                            int bytesRead = serveChannel.read(deleteData);

                            byte[] b = new byte[1024];
                            deleteData.flip();
                            deleteData.get(b);
                            String filename = new String(b);
                            System.out.println("filename " + filename);
                            //Delete file
                            break;

                        case "G":
                            //Send file to client
                            break;

                        case "R":
                            //Rename file
                            break;

                        default:
                            System.out.println("Unknown command!");
                            //send reply code to indicate request was rejected.
                            sendReplyCode(serveChannel, "F");
                    }
                    //note that calling close() will automatically
                    // deregister the channel with the selector
                    serveChannel.close();
                }
            }
        }
    }

    private static void sendReplyCode (SocketChannel channel, String code) throws IOException{
        ByteBuffer data = ByteBuffer.wrap(code.getBytes());
        channel.write(data);
    }
}


