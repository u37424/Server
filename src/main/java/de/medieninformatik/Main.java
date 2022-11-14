package de.medieninformatik;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static int PORT;
    private static ExecutorService pool;
    private static LinkedList<ObjectOutputStream> clients = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        PORT = 6000;
        pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server an Port " + PORT + " gestartet.");

            while (true) {
                final Socket socket = serverSocket.accept();
                pool.execute(() -> {
                    InetAddress other = socket.getInetAddress();

                    //Verbinden
                    System.out.println("Verbunden mit " + other.getCanonicalHostName()
                            + " (" + other.getHostAddress() + ")");

                    //Streams
                    ObjectOutputStream oos = null;
                    ObjectInputStream is;
                    try {
                        oos = new ObjectOutputStream(socket.getOutputStream());
                        oos.flush();
                        is = new ObjectInputStream(socket.getInputStream());

                        //Anmelden
                        clients.add(oos);

                        //Annehmen und weiterleiten
                        do {
                            String text = (String) is.readObject();
                            if (text == null) {
                                socket.close();
                                System.err.println("User disconnected from your channel.");
                                if (disconnect(oos))
                                    System.err.println("Client " + other.getCanonicalHostName() + " abgemeldet.");
                                break;
                            }
                            System.out.println(text);
                            for (ObjectOutputStream o : clients) {
                                if (!o.equals(oos)) o.writeObject(text);
                            }
                        } while (true);


                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("User disconnected from your Channel.");
                        if (disconnect(oos))
                            System.err.println("Client " + other.getCanonicalHostName() + " abgemeldet.");
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean disconnect(ObjectOutputStream oos) {
        for (int i = 0; i < clients.size(); i++) {
            ObjectOutputStream o = clients.get(i);
            if (oos.equals(o)) {
                clients.remove(i);
                return true;
            }
        }
        return false;
    }
}
