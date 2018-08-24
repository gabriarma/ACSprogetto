/*
    This file is part of ACSprogetto.

    ACSprogetto is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ACSprogetto is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ACSprogetto.  If not, see <http://www.gnu.org/licenses/>.

**/
package server;
import utility.LogFormatManager;
import utility.ResponseCode;
import utility.ServerInfo;
import utility.ServerInfoRecover;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import static java.util.Objects.requireNonNull;


public class SClient implements Callable<Integer> {
    private List<AnonymousClientExtended> clients;
    private List<ServerInfo> serverList;

    private Server myServer;
    final private LogFormatManager print;

    public SClient(List serverList,Server myServer)  {
        this(serverList, myServer, false);
        
    }

    public SClient(List serverList,Server myServer, boolean pedantic)  {
        if(serverList==null )
        {
            throw new NullPointerException("passing null argument to SClient constructor");
        }
        this.serverList=serverList;
        this.myServer=requireNonNull(myServer);
        this.print = new LogFormatManager("SCLIENT", pedantic);

    }






    public Integer call()
    {
        //INIT

        try {
            print.pedanticInfo("initializing connection with brokers");
            this.initAndConnectAnonymousClients(serverList.size());
            print.pedanticInfo("initializing accounts.");
            for (AnonymousClientExtended it:clients) {
                this.registerOnServer(it);
            }
            print.pedanticInfo("subscribing for notifications.");
            for (AnonymousClientExtended it:clients) {
                this.subscribeForNotifications(it);
            }
            print.pedanticInfo("subscribing to all topics.");
            for (AnonymousClientExtended it:clients) {
                this.subscribeToAllTopics(it);
            }

        } catch (Exception e) {
            print.error(e,"unable to create anonymous clients.");
            return 1;
        }


        System.out.println("Enter something here : ");
        try{
            BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
            String s = bufferRead.readLine();

        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        System.out.println("Closing Sclient with exitcode:0");
        return 0;
    }


    //PRIVATE METHODS

    /**
     *  Tenta di stabilire una connessione con tutti i server.
     * @param initialSize la grandezza della lista dei server
     * @throws IOException se è impossibile creare il ServerInfoRecover
     */
    private void initAndConnectAnonymousClients(int initialSize) throws IOException  {
        this.clients=new ArrayList<>(initialSize);
        Iterator it=serverList.iterator();
        int oldSize=serverList.size();
        int i=0;
        ServerInfoRecover infoServer = new ServerInfoRecover();


        while(it.hasNext()){

                it.next();
                try {
                    clients.add(new AnonymousClientExtended(this.myServer));
                    String[] a = infoServer.getServerInfo(InetAddress.getByName(((ServerInfo) it).regHost));
                    clients.get(i).setServerInfo(a[0], Integer.valueOf(a[1]), a[2]);
                    i++;
                }catch(RemoteException e){
                    print.warning(e,"unable to connect with server");
                    it.remove();//lo rimuovo dato che la connessione non è riuscita
                }

        }

        print.info("connected to "+i+"/"+oldSize+" servers.");
    }



    private void registerOnServer(AnonymousClientExtended client){
        if(client==null)throw new NullPointerException("anonymousclientextended==null");
        boolean result=client.register();
            if(result) {
                print.pedanticInfo("registration successfully completed.");
            }else{
                print.pedanticInfo("unable to register on the server.");
            }
        }


    private void subscribeForNotifications(AnonymousClientExtended client){
        if(client==null)throw new NullPointerException("anonymousclientextended==null");
        try {
            ResponseCode resp=client.getServer_stub().subscribeNewTopicNotification(client.getCookie());
            if(resp.IsOK()){
                print.pedanticInfo("successfully subscribed to notification list.");
            }else{
                print.pedanticInfo("unable to register for notification list.");
            }
        }catch(Exception exc){
            print.error(exc);
        }
    }

    private void subscribeToAllTopics(AnonymousClientExtended client){
        if(client==null)throw new NullPointerException("anonymousclientextended==null");
        boolean result;
        String[]topics;
        topics = client.getTopics();
        for (String topic : topics) {
            result = client.subscribe(topic);
            if(!result){
                print.pedanticInfo("unable to subscribe to "+topic+"on the server.");
            }else {
                myServer.addTopic(topic);
            }
        }
    }

}
