/**
 * Class for the server.
 *
 * @author Bruno Liu fc56297
 * @author Duarte Pinheiro fc54475
 * @author Rodrigo Cancelinha fc56371
 */
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class TintolmarketServer extends Thread {
    private static ArrayList<Wine> wineList = new ArrayList<Wine>();
    private static ArrayList<Client> clientList = new ArrayList<Client>();
    private static HashMap<String, ArrayList<ClientAndWines>> winesAndUsers = new HashMap<String, ArrayList<ClientAndWines>>();

    private static final String USERSFILE = "users.txt";
    private static final String USERSBALANCEFILE = "usersBalance.txt";
    private static final String USERSMESSAGEFILE = "usersMessage.txt";
    private static final String WINEFILE = "wine.txt";
    private static final String WINEANDUSERSFILE = "wineAndUsers.txt";
    private static final String SERVERIMAGES = "serverImg/";

//    private Socket socket = null;
    private Socket socket;
    public TintolmarketServer(Socket s) { socket = s; } // construtor

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Wrong input, should be: java TintolmarketServer <port> <password-cifra> <keystore> <password-keystore>");
            System.exit(0);
        }
        int port = Integer.parseInt(args[0]);
        String cifraPass = args[1];
        String keystore = args[2];
        String keystorePass = args[3];

        System.out.println("server: main");

        try {
            loadUsers();
            loadUsersBalance();
            loadBackUpMessages();
            loadWines();
            loadWineAndUsers();
            File file = new File(SERVERIMAGES);
            file.mkdir();
        } catch (Exception e) {
            System.exit(0);
        }

        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePass);
        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
        SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(port);
        while (true) {
            new TintolmarketServer(ss.accept()).start(); // uma thread por ligação
        }
//        TintolmarketServer server = new TintolmarketServer();
//        server.startServer(Integer.parseInt(args[0]));
    }

//    public void startServer(int port) {
//        ServerSocket sSoc = null;
//        try {
//            sSoc = new ServerSocket(port);
//        } catch (IOException e) {
//            System.err.println(e.getMessage());
//            System.exit(-1);
//        }
//
//        while (true) {
//            try {
//                Socket inSoc = sSoc.accept();
//                ServerThread newServerThread = new ServerThread(inSoc);
//                newServerThread.start();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
//        // sSoc.close();
//    }

    // Threads utilizadas para comunicacao com os clientes
//    class ServerThread extends Thread {



//    ServerThread(Socket inSoc) {
//        socket = inSoc;
//        System.out.println("thread do server para cada cliente");
//    }

    public void run() {

        try {

            ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

            //obter userID
            String userID = (String) inStream.readObject();   //VERIFICAR SE USERID JA ESTA REGISTADO OU NAO DEPOIS FAZER IF PARA ISSO

            //Gerar nonce e enviar
            SecureRandom random = new SecureRandom();
            byte[] nonce = new byte[8];
            random.nextBytes(nonce);
            long nonceLong = (long) ((nonce[0] & 0xFF) << 56)
                    | (long) ((nonce[1] & 0xFF) << 48)
                    | (long) ((nonce[2] & 0xFF) << 40)
                    | (long) ((nonce[3] & 0xFF) << 32)
                    | (long) ((nonce[4] & 0xFF) << 24)
                    | (long) ((nonce[5] & 0xFF) << 16)
                    | (long) ((nonce[6] & 0xFF) << 8)
                    | (long) (nonce[7] & 0xFF);
            outStream.writeObject(nonceLong); //Envia o nonce

            String nonceBytesRecebido = (String) inStream.readObject(); //O nonce recebido
            byte[] assinaturaCliente = (byte[]) inStream.readObject(); //Assinatura do utilizador
            Certificate certRecebido = (Certificate) inStream.readObject(); //Certificado
            Client currentUser = null;

            if((currentUser = existUser(userID)) != null){

                //obter a publickey atraves da publuc key em bytes guardado no cliente
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(currentUser.getPublicKey());
                PublicKey publicKey = keyFactory.generatePublic(keySpec);

                Signature verifier = Signature.getInstance("SHA256withRSA");
                verifier.initVerify(publicKey);

                verifier.update(nonceBytesRecebido.getBytes());
                if (verifier.verify(assinaturaCliente)) {
                    System.out.println("Cliente antigo conectado com sucesso!");
                    outStream.writeObject(true);
                } else {
                    outStream.writeObject(false);
                }
            } else {

                if (nonceLong != Long.parseLong(nonceBytesRecebido)) {   //nonce diferente
                    outStream.writeObject(false);
                } else {
                    PublicKey publicKey = certRecebido.getPublicKey();
                    Signature verifier = Signature.getInstance("SHA256withRSA");
                    verifier.initVerify(publicKey);
                    verifier.update(nonceBytesRecebido.getBytes());


                    if (verifier.verify(assinaturaCliente)) { //se o utilizador for valido, verificacao da assinatura
                        currentUser = new Client(userID, publicKey.getEncoded());
                        clientList.add(currentUser);
                        saveUsers();
                        saveUsersBalance();
                        System.out.println("Cliente conectado com sucesso!");
                        outStream.writeObject(true);
                    } else {    //nonce ou assinatura invalida, enviar erro a informar o cliente
                        outStream.writeObject(false);
                    }
                }
            }

            boolean loop = true;
            String info;
            while (loop) {
                info = (String) inStream.readObject();
                String wine;
                String seller;
                Double value;
                int quantity;
                boolean exists;
                switch (info) {
                    case "a":
                    case "add":
                        boolean fileExist = inStream.readBoolean();
                        if (fileExist) {
                            wine = (String) inStream.readObject();
                            String image = (String) inStream.readObject();
                            // ----------------------------------------------------
                            exists = false;
                            for (Wine wineAux : wineList) {
                                if (wineAux.getWineName().equalsIgnoreCase(wine)) {
                                    System.out.println("The wine is already in our database");
                                    exists = true;
                                }
                            }
                            outStream.writeObject(exists);

                            if (!exists) {
                                long fileSize = inStream.readLong();
                                add(wine, image, fileSize, inStream);
                                outStream.writeObject(exists);
                                saveWines();
                            }
                        } else {
                            System.out.println("Problem on the client side");
                        }

                        break;
                    case "s":
                    case "sell":
                        wine = (String) inStream.readObject();
                        value = inStream.readDouble();
                        quantity = (int) inStream.readObject();
                        // ----------------------------------------------------
                        exists = sell(wine, value, quantity, currentUser);
                        outStream.writeObject(exists);
                        saveWineAndUsers();
                        saveUsersBalance();
                        break;
                    case "v":
                    case "view":
                        wine = (String) inStream.readObject();
                        // ----------------------------------------------------
                        String viewString = view(wine);
                        outStream.writeObject(viewString);

                        exists = false;
                        int i = 0;
                        while (i < wineList.size() && exists == false) {
                            if (wineList.get(i).getWineName().equalsIgnoreCase(wine)) {
                                exists = true;
                            }
                            i++;
                        }
                        if (!exists) {
                            outStream.writeObject(exists);
                            break;
                        }
                        i--;
                        File file = new File(SERVERIMAGES + wineList.get(i).getImage());
                        if (!file.exists()) {
                            exists = false;
                            System.out.println("The " + wineList.get(i).getImage() + " does not exists!");
                        }
                        outStream.writeObject(exists);

                        if (exists) {
                            outStream.writeObject(wineList.get(i).getImage());
                            int bytesread = 0;
                            FileInputStream fis = new FileInputStream(file);
                            long fileSize = file.length();
                            outStream.writeLong(fileSize);
                            long bytesReceived = 0;
                            byte[] buffer = new byte[1024];
                            while (bytesReceived < fileSize) {
                                bytesread = fis.read(buffer);
                                if (bytesread == -1) {
                                    break;
                                }
                                outStream.write(buffer, 0, bytesread);
                                bytesReceived += bytesread;
                            }
                            outStream.flush();
                            fis.close();
                        }

                        break;
                    case "b":
                    case "buy":
                        wine = (String) inStream.readObject();
                        seller = (String) inStream.readObject();
                        quantity = (int) inStream.readObject();
                        // ----------------------------------------------------
                        String buyString = buy(wine, seller, quantity, currentUser);
                        outStream.writeObject(buyString);
                        saveWineAndUsers();
                        saveUsersBalance();
                        break;
                    case "w":
                    case "wallet":
                        for (Client client : clientList) {
                            if (client.getClientId().equals(currentUser.getClientId())) {
                                currentUser = client;
                            }
                        }
                        Double balance = wallet(currentUser);
                        outStream.writeObject(balance);
                        break;
                    case "c":
                    case "classify":
                        wine = (String) inStream.readObject();
                        int star = (int) inStream.readObject();
                        // ----------------------------------------------------
                        exists = classify(wine, star);
                        outStream.writeObject(exists);
                        saveWines();
                        break;
                    case "t":
                    case "talk":
                        String receiver = (String) inStream.readObject();
                        String message = (String) inStream.readObject();
                        // ----------------------------------------------------
                        exists = talk(receiver, message, currentUser);
                        outStream.writeObject(exists);
                        saveBackUpMessages();
                        break;
                    case "r":
                    case "read":
                        for (Client client : clientList) {
                            if (client.getClientId().equals(currentUser.getClientId())) {
                                currentUser = client;
                            }
                        }
                        String messages = currentUser.readMessages();
                        outStream.writeObject(messages);
                        saveBackUpMessages();
                        break;
                    case "e":
                    case "exit":
                        loop = false;
                        System.out.println("Client disconnected.");
                        outStream.close();
                        inStream.close();
                        socket.close();
                        break;
                    default:
                        System.out.println("Wrong input, try again");
                        break;
                }
            }
        } catch (SocketException e) {
            System.err.println("Client disconnected");
        } catch (EOFException e) {
            System.err.println("EOF error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found error: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }

    }
//}

    // The interface functions

    /**
     * Function that adds a wine and it's image.
     *
     * @param wine     The wine's name
     * @param image    The wine's image
     * @param fileSize The image size
     * @param inStream The inStream used to read the data
     * @requires The wine is not already in the wineList.
     */
    public void add(String wine, String image, long fileSize, ObjectInputStream inStream) {

        Wine newWine = new Wine(wine, image);
        wineList.add(newWine);

        try {
            FileOutputStream fos = new FileOutputStream(SERVERIMAGES + image);
            byte[] buffer = new byte[1024];
            int bytesread = 0;
            long bytesReceived = 0;

            while (bytesReceived < fileSize) {
                bytesread = inStream.read(buffer);
                if (bytesread == -1) {
                    break;
                }
                fos.write(buffer, 0, bytesread);
                bytesReceived += bytesread;
            }

            fos.flush();
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The functions that is responsable to announce a certain sell from a certain
     * currentClient.
     *
     * @param wine        The wine's name.
     * @param value       The seller ID.
     * @param quantity    The amount that the user is going to buy.
     * @param currentUser The current user.
     * @return
     */
    public boolean sell(String wine, Double value, int quantity, Client currentUser) {
        boolean existsWine = false;
        for (int i = 0; i < wineList.size(); i++) {
            if (wineList.get(i).getWineName().equalsIgnoreCase(wine)) {
                existsWine = true;
            }
        }
        if (!existsWine) {
            System.out.println("This wine doesn't exists");
            return false;
        }

        if (winesAndUsers.containsKey(wine)) {
            boolean userAlreadyHasAnOffer = false;
            for (int j = 0; j < winesAndUsers.get(wine).size(); j++) {
                if (winesAndUsers.get(wine).get(j).getUserID().equals(currentUser.getClientId())) {
                    winesAndUsers.get(wine).get(j).addAmount(quantity);
                    winesAndUsers.get(wine).get(j).updatePrice(value);
                    userAlreadyHasAnOffer = true;
                }
            }
            if (!userAlreadyHasAnOffer) {
                ArrayList<ClientAndWines> clientAndWinesList = winesAndUsers.get(wine);
                clientAndWinesList.add(new ClientAndWines(currentUser.getClientId(), value, quantity));
            }
        } else {
            ArrayList<ClientAndWines> clientAndWinesList = new ArrayList<>();
            clientAndWinesList.add(new ClientAndWines(currentUser.getClientId(), value, quantity));
            winesAndUsers.put(wine, clientAndWinesList);
        }
        return true;
    }

    /**
     * Returns all the information about a certain wine.
     *
     * @param wine The wine's name
     * @return All the information about the wine
     */
    public String view(String wine) {
        Boolean exists = false;
        if (wineList.size() < 1)
            return ("The wine the user inserted doesn's exist");

        Wine currentWine = wineList.get(0);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < wineList.size(); i++) {
            if (wineList.get(i).getWineName().equalsIgnoreCase(wine)) {
                exists = true;
                currentWine = wineList.get(i);
            }
        }
        if (exists == false) {
            sb.append("The wine the user inserted doesn's exist");
            return sb.toString();
        }

        sb.append("The " + wine + " wine image is: " + currentWine.getImage() + "\n");
        sb.append("The average classification is: " + currentWine.getAverageClassification() + "\n");
        if (winesAndUsers.get(wine) == null) {
            sb.append("This wine doesn't has a seller yet.\n");
            return sb.toString();
        }
        for (int i = 0; i < winesAndUsers.get(wine).size(); i++) {
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("Seller: " + winesAndUsers.get(wine).get(i).getUserID() + "\n");
            sb.append("Wine's price: " + winesAndUsers.get(wine).get(i).getPrice() + "\n");
            sb.append("Current stock: " + winesAndUsers.get(wine).get(i).getAmount() + " Units.\n");
        }
        return sb.toString();
    }

    /**
     * The user executes a buy from a certain seller.
     *
     * @param wine          The wine's name.
     * @param seller        The seller ID.
     * @param quantity      The amount that the user is going to buy.
     * @param currentClient The current user.
     * @return True if the operation executes with no problem, false otherwise.
     */
    public String buy(String wine, String seller, int quantity, Client currentClient) {
        Boolean exists = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wineList.size(); i++) {
            if (wineList.get(i).getWineName().equalsIgnoreCase(wine)) {
                exists = true;
            }
        }
        if (exists == false) {
            sb.append("The wine doesn't exist");
            return sb.toString();
        }

        Integer sellerPointer = -1;
        for (int j = 0; j < clientList.size(); j++) {
            if (clientList.get(j).getClientId().equalsIgnoreCase(seller)) {
                sellerPointer = j;
            }
        }
        if (sellerPointer == -1) {
            sb.append("That seller doesn't exist");
            return sb.toString();
        }

        ArrayList<ClientAndWines> actualClientAndWines = winesAndUsers.get(wine);
        // Integer actualClientAndWinesPointer = 0;
        if (actualClientAndWines == null) {
            sb.append("This wine doesn't have unit to sell");
            return sb.toString();
        }

        ClientAndWines actualClientAndWine = winesAndUsers.get(wine).get(0);

        for (int k = 0; k < actualClientAndWines.size(); k++) {
            if (actualClientAndWines.get(k).getUserID().equalsIgnoreCase(seller)) {
                // actualClientAndWinesPointer = k;
                actualClientAndWine = actualClientAndWines.get(k);
            }
        }
        if (actualClientAndWine.getAmount() < quantity) {
            sb.append("The seller doesn't has enough amount of bottles of wine to execute the purchase");
            return sb.toString();
        }
        Double totalAPagar = quantity * actualClientAndWine.getPrice();
        if (totalAPagar > currentClient.getBalance()) {
            sb.append("The user don't have enough money execute the purchase");
            return sb.toString();
        }

        currentClient.pay(totalAPagar);
        clientList.get(sellerPointer).receive(totalAPagar);
        actualClientAndWine.takeAmount(quantity);
        sb.append("The purchase have been executed with no problem");
        return sb.toString();

    }

    /**
     * Returns the current user's balance.
     *
     * @param currentUser The current user.
     * @return The current user's balance.
     */
    public Double wallet(Client currentUser) {
        return currentUser.getBalance();
    }

    /**
     * Function that adds a star to the wine.
     *
     * @param wine  The wine's name.
     * @param stars The wine's star given.
     * @return True if the wine exists, false otherwise.
     */
    public boolean classify(String wine, int stars) {
        for (Wine wineAux : wineList) {
            if (wineAux.getWineName().equals(wine)) {
                wineAux.addStar(stars - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Function that adds a message to a certain user's box message.
     *
     * @param user    The user that is going to recieve the message.
     * @param message The message.
     * @param sender  The sender of the message.
     * @return True if the user exists, false otherwise.
     */
    public boolean talk(String user, String message, Client sender) {
        boolean exists = false;
        for (Client client : clientList) {
            if (client.getClientId().equals(user)) {
                client.addMessages(sender.getClientId(), message);
                exists = true;
            }
        }
        return exists;
    }

    /**
     * Reads all the currentUser's message.
     *
     * @param currentUser The current user.
     * @return The user's messages.
     */
    public String read(Client currentUser) {
        return currentUser.readMessages();
    }

    /**
     * Check if it is a new user.
     *
     * @param user User Id
     * @return true if the user is new, false otherwise.
     */
    private boolean newUser(String user) {
        for (Client client : clientList) {
            if (client.getClientId().equalsIgnoreCase(user)) {
                return false;
            }
        }
        return true;
    }

    //ACHO QUE ESTA FUNCAO JA NAO é NECESSARIA
//    /**
//     * Check if the password is correct.
//     *
//     * @param user   The user ID.
//     * @param passwd The user's password.
//     * @return True if the password corresponds to the user password, false
//     *         otherwise.
//     */
//    private boolean autentification(String user, String passwd) {
//        for (Client client : clientList) {
//            if (client.getClientId().equalsIgnoreCase(user) && client.getPassword().equalsIgnoreCase(passwd)) {
//                return true;
//            }
//        }
//        return false;
//    }

    private Client existUser(String userId) {
        for (Client client : clientList) {
            if (client.getClientId().equalsIgnoreCase(userId) ) {
                return client;
            }
        }
        return null;
    }

    // Functions responsables on saving and loading the data into txt files
    // for userID:chavePublica
    private static synchronized void saveUsers() {
        try {
            StringBuilder sb = new StringBuilder();
            for (Client clientAux : clientList) {
//                sb.append(clientAux.getClientId() + ":" + clientAux.getPassword() + "\n");
                sb.append(clientAux.getClientId() + ":" + Base64.getEncoder().encodeToString(clientAux.getPublicKey()) + "\n");
            }
            PrintWriter w = new PrintWriter(new File(USERSFILE));
            w.append(sb.toString());
            w.close();

        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + USERSFILE);
            System.exit(0);
        }

    }

    private static void loadUsers() {
        try {
            File file = new File(USERSFILE);
            file.createNewFile();
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String aux = sc.nextLine();
                String[] userAndPass = aux.split(":");
                clientList.add(new Client(userAndPass[0], Base64.getDecoder().decode(userAndPass[1].getBytes())));

            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + USERSFILE);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // for user:balance
    private static synchronized void saveUsersBalance() {
        try {
            StringBuilder sb = new StringBuilder();
            for (Client clientAux : clientList) {
                sb.append(clientAux.getClientId() + ":" + clientAux.getBalance() + "\n");

            }
            PrintWriter w = new PrintWriter(new File(USERSBALANCEFILE));
            w.append(sb.toString());
            w.close();

        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + USERSBALANCEFILE);
            System.exit(0);
        }

    }

    private static void loadUsersBalance() {
        try {
            File file = new File(USERSBALANCEFILE);
            file.createNewFile();
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String aux = sc.nextLine();
                String[] userAndBalance = aux.split(":");
                for (Client clientAux : clientList) {
                    if (clientAux.getClientId().equals(aux)) {
                        clientAux.setBalance(Double.parseDouble(userAndBalance[1]));
                    }
                }
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + USERSBALANCEFILE);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // for Users:Messages
    private static synchronized void saveBackUpMessages() {
        try {
            StringBuilder sb = new StringBuilder();
            for (Client clientAux : clientList) {
                sb.append(clientAux.getClientId() + "\n");
                Map<String, ArrayList<String>> mapAux = clientAux.getBoxMessage();
                for (Map.Entry<String, ArrayList<String>> set : mapAux.entrySet()) {
                    for (String message : set.getValue()) {
                        sb.append(set.getKey() + ":" + message + "\n");
                    }
                }
            }
            PrintWriter w = new PrintWriter(new File(USERSMESSAGEFILE));
            w.append(sb.toString());
            w.close();

        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + USERSMESSAGEFILE);
            System.exit(0);
        }

    }

    private static void loadBackUpMessages() {
        try {
            File file = new File(USERSMESSAGEFILE);
            file.createNewFile();
            Scanner sc = new Scanner(file);
            Client clientAux = null;
            while (sc.hasNextLine()) {
                String aux = sc.nextLine();

                if (aux.contains(":")) {
                    String[] senderAndMessage = aux.split(":");
                    StringBuilder messageAux = new StringBuilder();
                    for (int i = 1; i < senderAndMessage.length; i++) {
                        messageAux.append(senderAndMessage[i]);
                    }
                    clientAux.addMessages(senderAndMessage[0], messageAux.toString());
                } else {
                    for (Client client : clientList) {
                        if (client.getClientId().equals(aux)) {
                            clientAux = client;
                        }
                    }
                }
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + USERSMESSAGEFILE);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // for wines

    private static synchronized void saveWines() {
        try {
            StringBuilder sb = new StringBuilder();
            for (Wine wineAux : wineList) {
                sb.append(wineAux.getWineName() + ":" + wineAux.getImage() + ":");
                int[] classiAux = wineAux.getClassifications();
                sb.append(classiAux[0] + "," + classiAux[1] + "," + classiAux[2] + "," + classiAux[3] + ","
                        + classiAux[4] + "\n");
            }
            PrintWriter w = new PrintWriter(new File(WINEFILE));
            w.append(sb.toString());
            w.close();

        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + WINEFILE);
            System.exit(0);
        }

    }

    private static void loadWines() {
        try {
            File file = new File(WINEFILE);
            file.createNewFile();
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String aux = sc.nextLine();
                String[] wineInfo = aux.split(":");
                Wine wineAux = new Wine(wineInfo[0], wineInfo[1]);
                String[] classiInfo = wineInfo[2].split(",");
                int[] classiAux = new int[5];
                for (int i = 0; i < classiInfo.length; i++) {
                    classiAux[i] = Integer.parseInt(classiInfo[i]);
                }
                wineAux.setClassifications(classiAux);
                wineList.add(wineAux);
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + WINEFILE);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // for wineAndUsers
    private static synchronized void saveWineAndUsers() {
        try {
            StringBuilder sb = new StringBuilder();
            for (HashMap.Entry<String, ArrayList<ClientAndWines>> set : winesAndUsers.entrySet()) {
                ArrayList<ClientAndWines> setAux = set.getValue();
                for (ClientAndWines clientAndWines : setAux) {
                    sb.append(set.getKey() + ":" + clientAndWines.getUserID() + ":" + clientAndWines.getPrice() + ":"
                            + clientAndWines.getAmount() + "\n");
                }

            }
            PrintWriter w = new PrintWriter(new File(WINEANDUSERSFILE));
            w.append(sb.toString());
            w.close();

        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + WINEANDUSERSFILE);
            System.exit(0);
        }

    }

    private static void loadWineAndUsers() {
        try {
            File file = new File(WINEANDUSERSFILE);
            file.createNewFile();
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String aux = sc.nextLine();
                String[] wineInfo = aux.split(":");
                if (winesAndUsers.containsKey(wineInfo[0])) {
                    ArrayList<ClientAndWines> clientAndWinesList = winesAndUsers.get(wineInfo[0]);
                    clientAndWinesList.add(new ClientAndWines(wineInfo[1], Double.parseDouble(wineInfo[2]),
                            Integer.parseInt(wineInfo[3])));
                } else {
                    ArrayList<ClientAndWines> clientAndWinesList = new ArrayList<>();
                    clientAndWinesList.add(new ClientAndWines(wineInfo[1], Double.parseDouble(wineInfo[2]),
                            Integer.parseInt(wineInfo[3])));
                    winesAndUsers.put(wineInfo[0], clientAndWinesList);
                }
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.err.println("The following file was not found: " + WINEANDUSERSFILE);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
