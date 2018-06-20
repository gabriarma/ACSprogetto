package client;

import interfaces.ClientInterface;
import interfaces.ServerInterface;
import utility.Message;
import utility.ResponseCode;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static utility.ResponseCode.Codici.R220;

public class AnonymousClient implements ClientInterface {



    /******************/
    /* client fields */
    private String username;
    private ClientInterface skeleton;//my stub
    private String cookie;
    private String myPrivateKey;
    private String myPublicKey;
    private boolean pedantic  = true;

    private String[] topicsSubscribed;               //topic a cui si è iscritti

    /******************/
    /* server fields */
    private String serverName;                      //the name for the remote reference to look up
    private String brokerPublicKey;                 //broker's public key
    private ServerInterface server_stub;            //broker's stub, se è null allora non si è connessi ad alcun server
    private String[] topicOnServer;                 //topic che gestisce il server

    /* remote registry fields */
    private String registryHost;                    //host for the remote registry
    private int registryPort = 1099;                //port on which the registry accepts requests




    // ************************************************************************************************************
    //CONSTRUCTOR

    /**
     * Anonymous user's constructor
     * @param username          il mio username
     * @param my_private_key    la mia chiave privata
     * @param my_public_key     la mia chiave pubblica
     */
    public AnonymousClient(String username, String my_public_key, String my_private_key)throws RemoteException
    {
        if(username==null || my_public_key==null || my_private_key==null)
            throw new NullPointerException();
        this.username     = username;
        this.myPublicKey  = my_public_key;
        this.myPrivateKey = my_private_key;
        this.skeleton     = (ClientInterface) UnicastRemoteObject.exportObject(this,0);
    }





    // *************************************************************************************************************
    //API


    /**
     *Il client si registra sul server su cui si era connesso con il metodo connect() e viene settato il cookie
     * @return true se registrazione andata a buon fine, false altrimenti
    **/
    public boolean register(){
        try {
            ResponseCode responseCode = server_stub.anonymousRegister(this.skeleton, this.myPublicKey);
            return registered(responseCode);
        }catch (RemoteException e){
            errorStamp(e, "Unable to reach the server.");
            return false;
        }
    }


    /**
     * Si connette al server specificato dalla stringa broker e dalla porta regPort facendo il lookup
     * sul registry dell'host, utilizato per vedere se il server esiste ed è attivo nel caso recuperarne lo stub
     * @param regHost l'indirizzo della macchina su cui risiede il registry
     * @param regPort porta su cui connettersi al registry
     * @param server  il nome con cui il server ha fatto la bind del suo stub sul registry
     * @return true se andata a buon fine,false altrimenti
     */
    public ServerInterface connect(String regHost, String server, Integer regPort)
    {
        try {
            Registry r = LocateRegistry.getRegistry(regHost, regPort);
            ServerInterface server_stub = (ServerInterface) r.lookup(server);
            ResponseCode rc = server_stub.connect();
            if(rc.IsOK())
                this.brokerPublicKey = rc.getMessaggioInfo();
            return server_stub;
        }catch (RemoteException |NotBoundException exc){
            return null;
        }
    }


    /**
     * Si iscrive al topic passato come argomento
     * @param topic a cui ci si vuole iscrivere
     */
    public void subscribe(String topic)
    {


    }


    /**
     * Si disconnette al server a cui si è connessi. Nonostante il server ci invii un messaggio di risposta questo
     * viene ignorato perchè al client non importa.
     * @return true se ci si è disconnesi con successo, false altrimenti
     */
    public boolean disconnect(){
        try {
            ResponseCode response=server_stub.disconnect(cookie);
            return true;

        }catch(RemoteException exc){
            System.err.println(exc.getClass().getSimpleName());
            return false;
        }finally {
            //Il fatto che lo stub sia null significa che non si è connessi da alcun server
            this.server_stub=null;
        }

    }





    //metodi non ancora utilizzati ma che penso possano servire più tardi
    public void setServerInfo(String regHost, String serverName){
        if(regHost==null || regHost.isEmpty() || serverName==null || serverName.isEmpty()){
            throw new IllegalArgumentException("Invalid argument format of regHost or serverName");
        }
        this.registryHost = regHost;
        this.serverName   = serverName;

    }

    public void setServerInfo(String regHost, int regPort, String serverName) throws IllegalArgumentException{
        if(regHost==null || regHost.isEmpty() || serverName==null || serverName.isEmpty()){
            throw new IllegalArgumentException("Invalid argument format of regHost or serverName");
        }
        if(regPort>1024 && regPort<=65535)  //Se la porta passata è valida impostala come porta del server
            this.registryPort = regPort;
        else
            this.registryPort = 1099;
        setServerInfo(regHost, serverName);
        this.server_stub = connect(regHost, serverName, regPort);
        if(server_stub!=null){      //connesione al server avvenuta con successo
            infoStamp("Successful connection to the server.");
        }else {
            infoStamp("Unable to reach the server.");
        }
    }




    // *************************************************************************************************************
    //REMOTE METHOD

    @Override
    public ResponseCode notify(Message m) {
        ResponseCode rc;
        if(m==null) {
            rc=new ResponseCode(ResponseCode.Codici.R500, ResponseCode.TipoClasse.CLIENT,
                    "(-) NOT OK Il server ha ricevuto un messaggio vuoto");
            pedanticInfo("Receved new message\n"+m.toString());
            return rc;
        }
        rc=new ResponseCode(ResponseCode.Codici.R200, ResponseCode.TipoClasse.CLIENT,
                "(+) OK il server ha ricevuto il messaggio");
        return rc;
    }

    @Override
    public void isAlive() {
    }




    // *************************************************************************************************************
    //PRIVATE METHOD
    /*

     */
    private boolean registered(ResponseCode response){
        if(response == null || !response.getCodice().equals(ResponseCode.Codici.R100)) {     //Registrazione fallita
            errorStamp(response, "Server registration failed");
            return false;
        }

        //Registrazione avvenuta con successo
        this.cookie = response.getMessaggioInfo();
        infoStamp("Successfully registered on server "+serverName+".");
        return true;
    }




































    //METODI UTILIZZATI PER LA GESTIONE DELL'OUTPUT DEL CLIENT

    protected void errorStamp(Exception e){
        System.out.flush();
        System.err.println("[ANONYMOUS_CLIENT-ERROR]");
        System.err.println("\tException type: "    + e.getClass().getSimpleName());
        System.err.println("\tException message: " + e.getMessage());
        e.printStackTrace();
    }

    protected void errorStamp(ResponseCode r, String msg){
        System.out.flush();
        System.err.println("[ANONYMOUS_CLIENT-ERROR]: "      + msg);
        System.err.println("\tServer error code: "    + r.getCodice());
        System.err.println("\tServer error message: " + r.getMessaggioInfo());
    }

    protected void errorStamp(Exception e, String msg){
        System.out.flush();
        System.err.println("[ANONYMOUS_CLIENT-ERROR]: "      + msg);
        System.err.println("\tException type: "    + e.getClass().getSimpleName());
        System.err.println("\tException message: " + e.getMessage());
        e.printStackTrace();
    }

    protected void warningStamp(Exception e, String msg){
        System.out.flush();
        System.err.println("[ANONYMOUS_CLIENT-WARNING]: "    + msg);
        System.err.println("\tException type: "    + e.getClass().getSimpleName());
        System.err.println("\tException message: " + e.getMessage());
    }

    protected void infoStamp(String msg){
        System.out.println("[CLIENT-INFO]: " + msg);
    }

    protected void pedanticInfo(String msg){
        if(pedantic){
            infoStamp(msg);
        }
    }

}
