package client;
/**
 * Class for the server users.
 * 
 * @author Bruno Liu fc56297
 * @author Duarte Pinheiro fc54475
 * @author Rodrigo Cancelinha fc56371
 */

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Tintolmarket {
    private static final String CLIENTIMAGES = "clientImg/";

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println(
                    "Wrong input, should be: Tintolmarket <serverAddress> <truststore> <keystore> <password-keystore> <userID>");
            System.exit(0);
        }

        String[] address = args[0].split(":");
        String truststore = args[1];
        String keystore = args[2];
        String keystorePass = args[3];
        String userID = args[4];

        SSLSocket s;
        ObjectInputStream in;
        ObjectOutputStream out;
        Boolean fromServer = null;
        File file = new File(CLIENTIMAGES);
        file.mkdir();
        try {

            System.setProperty("javax.net.ssl.trustStore", truststore);
            System.setProperty("javax.net.ssl.trustStorePassword", keystorePass);
            SocketFactory sf = SSLSocketFactory.getDefault( );
            s = (SSLSocket) sf.createSocket("localhost", Integer.parseInt(address[1]));

            in = new ObjectInputStream(s.getInputStream());
            out = new ObjectOutputStream(s.getOutputStream());

            out.writeObject(userID);
            long nonce = (long) in.readObject();
            String nonceInString = String.valueOf(nonce);

            String alias = userID;
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream fis = new java.io.FileInputStream(keystore);
            char[] password = keystorePass.toCharArray();
            ks.load(fis, password);

            Certificate cert = ks.getCertificate(alias);
            PrivateKey pk = (PrivateKey) ks.getKey(alias, password);
            PublicKey pubK = cert.getPublicKey( );

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(pk);

            sig.update(nonceInString.getBytes());

            out.writeObject(nonceInString); //Nonce a enviar
            out.writeObject(sig.sign());    //A assinatura
            out.writeObject(cert);    //O certificado

            fromServer = (Boolean) in.readObject();

            if (fromServer) {
                System.out.println("Login com sucesso!");
                cases(in, out);
            } else {
                System.out.println("Nonce ou assinatura invalida.");
                System.out.println("Foi desconectado.");
                System.exit(0);
            }

            in.close();
            out.close();
            s.close();
        } catch (NoSuchElementException e) {
            System.err.println("Disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Function that is responsable for dealing each case
     * 
     * @param in  The inputStream
     * @param out The outputStream
     */
    public static void cases(
            ObjectInputStream in,
            ObjectOutputStream out) {
        boolean loop = true;
        Scanner sc = new Scanner(System.in);
        String[] info;

        while (loop) {
            System.out.println(
                    "\n------------------------------------\"\"\"\"\"\"\"\"------------------------------------");

            System.out.println(
                    "Insert which comand you want to execute from the commands down below. " +
                            "You can also just write the first letter of the command before you insert the arguments");
            System.out.println("List of commands:");
            System.out.println(
                    "\tadd <wine> <image>\n\tsell <wine> <value> <quantity>\n\tview <wine>\n\tbuy <wine> <seller> <quantity>"
                            +
                            "\n\twallet\n\tclassify <wine> <stars>\n\ttalk <user> <message>\n\tread\n\texit");
            info = sc.nextLine().split(" ");
            info[0].toLowerCase();
            boolean check;

            try {
                switch (info[0]) {
                    case "a":
                    case "add":
                        if (info.length < 3) {
                            System.err.println(
                                    "Invalid arguments for add command. Usage: add <wine> <image>");
                            break;
                        }
                        out.writeObject(info[0]);
                        File file = new File(CLIENTIMAGES + info[2]);
                        if (file.exists()) {
                            out.writeBoolean(true);

                            out.writeObject(info[1]);
                            out.writeObject(info[2]);

                            check = (Boolean) in.readObject();

                            if (check) {
                                System.out.println("That wine was already inserted");
                            } else {
                                int bytesread = 0;
                                FileInputStream fis = new FileInputStream(file);
                                long fileSize = file.length();
                                out.writeLong(fileSize);
                                long bytesReceived = 0;
                                byte[] buffer = new byte[1024];
                                while (bytesReceived < fileSize) {
                                    bytesread = fis.read(buffer);
                                    if (bytesread == -1) {
                                        break;
                                    }
                                    out.write(buffer, 0, bytesread);
                                    bytesReceived += bytesread;
                                }
                                out.flush();
                                System.out.println("The operation was successfully processed");
                                fis.close();
                            }
                        } else {
                            out.writeBoolean(false);
                            System.out.println("The Image that you inserted doesn't exist");
                        }

                        break;
                    case "s":
                    case "sell":
                        if (info.length < 4) {
                            System.err.println(
                                    "Invalid arguments for sell command. Usage: sell <wine> <value> <quantity>");
                            break;
                        } else if (Double.parseDouble(info[2]) < 0 || Integer.parseInt(info[3]) < 0) {
                            System.err.println(
                                    "The <value> an the <quantity> has to be bigger than 0");
                            break;
                        }
                        out.writeObject(info[0]);
                        out.writeObject(info[1]);
                        out.writeDouble(
                                Math.round(Double.parseDouble(info[2]) * 100) / 100); // CATCH DE ERRO?
                        out.writeObject(Integer.parseInt(info[3]));
                        // -------------------------------------------------------------
                        check = (Boolean) in.readObject();
                        if (check) {
                            System.out.println("The operation was successfully processed");
                        } else {
                            System.out.println("The wine doesn't exists");
                        }

                        break;
                    case "v":
                    case "view":
                        if (info.length < 2) {
                            System.err.println(
                                    "Invalid arguments for view command. Usage: view <wine>");
                            break;
                        }
                        out.writeObject(info[0]);
                        out.writeObject(info[1]);
                        // -------------------------------------------------------------
                        String viewString = (String) in.readObject();
                        System.out.println(viewString);
                        check = (Boolean) in.readObject();

                        if (check) {
                            String image = (String) in.readObject();
                            long fileSize = in.readLong();
                            FileOutputStream fos = new FileOutputStream(CLIENTIMAGES + image);
                            byte[] buffer = new byte[1024];
                            int bytesread = 0;
                            long bytesReceived = 0;

                            while (bytesReceived < fileSize) {
                                bytesread = in.read(buffer);
                                if (bytesread == -1) {
                                    break;
                                }
                                fos.write(buffer, 0, bytesread);
                                bytesReceived += bytesread;
                            }

                            fos.flush();
                            fos.close();
                            seeImage(image);
                        }

                        break;
                    case "b":
                    case "buy":
                        if (info.length < 4) {
                            System.err.println(
                                    "Invalid arguments for buy command. Usage: buy <wine> <seller> <quantity>");
                            break;
                        }
                        out.writeObject(info[0]);
                        out.writeObject(info[1]);
                        out.writeObject(info[2]);
                        out.writeObject(Integer.parseInt(info[3])); // CATCH DE ERRO?

                        // -------------------------------------------------------------
                        String buyString = (String) in.readObject();
                        System.out.println(buyString);

                        break;
                    case "w":
                    case "wallet":
                        out.writeObject(info[0]);
                        // -------------------------------------------------------------
                        Double balance = (Double) in.readObject();
                        System.out.println("Your current balance is: " + balance);
                        break;
                    case "c":
                    case "classify":
                        if (info.length < 3 ||
                                5 < Integer.parseInt(info[2]) ||
                                1 > Integer.parseInt(info[2])) {
                            System.err.println(
                                    "Invalid arguments for classify command. The stars must be a number between 1 and 5. Usage: classify <wine> <stars>");
                            break;
                        }
                        out.writeObject(info[0]);
                        out.writeObject(info[1]);
                        out.writeObject(Integer.parseInt(info[2]));
                        // -------------------------------------------------------------
                        check = (Boolean) in.readObject();
                        if (check) {
                            System.out.println("The operation was successfully processed");
                        } else {
                            System.out.println("The wine doesn't exists");
                        }

                        break;
                    case "t":
                    case "talk":
                        if (info.length < 3) {
                            System.err.println(
                                    "Invalid arguments for talk command. Usage: talk <user> <message>");
                            break;
                        }
                        out.writeObject(info[0]);
                        out.writeObject(info[1]);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < info.length; i++) {
                            sb.append(info[i] + " ");
                        }
                        out.writeObject(sb.toString());
                        // -------------------------------------------------------------
                        check = (Boolean) in.readObject();
                        if (check) {
                            System.out.println("The operation was successfully processed");
                        } else {
                            System.out.println("The user doesn't exists");
                        }

                        break;
                    case "r":
                    case "read":
                        out.writeObject(info[0]);
                        // -------------------------------------------------------------
                        String messages = (String) in.readObject();
                        if (messages.equals("")) {
                            System.out.println("You have no messages in your message box");
                        } else {
                            System.out.println(messages);
                        }

                        break;
                    case "e":
                    case "exit":
                        out.writeObject(info[0]);
                        loop = false;
                        out.close();
                        in.close();
                        break;
                    default:
                        System.out.println("Wrong input, try again");
                        break;
                }
            } catch (NoSuchElementException e) {
                System.err.println("Disconnected");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        sc.close();

    }

    private static void seeImage(String image) {
        JFrame f = new JFrame("Wine image");
        ImageIcon icon = new ImageIcon(CLIENTIMAGES + image);
        f.add(new JLabel(icon));
        f.pack();
        f.setVisible(true);
    }
}
