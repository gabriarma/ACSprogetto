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
package client;

import java.awt.EventQueue;
import java.util.concurrent.*;
import Events.*;
import guiClient.ClientGUI;

public class ClientHost {
    private static final int ERROR=1;
    private static final int EXIT=0;

    private ConcurrentLinkedQueue<Event> clientEngineToGUI=new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<Event> guiToClientEngine= new ConcurrentLinkedQueue<>();
    private boolean guiActivated;



    //Engine
    private ExecutorService clientThread;
    private ClientEngine clientEngine;

    //se viene utilizzato il terminale
    private TerminalInterface terminalInterface;
    private ExecutorService userInterfaceThread;

    //interfaccia grafica
    private ClientGUI userInterface;




    private ClientHost(boolean usingUserInterface) {
        this.guiActivated=usingUserInterface;
        clientEngine=new ClientEngine(clientEngineToGUI,guiToClientEngine);
        clientThread = Executors.newSingleThreadExecutor();

        if(guiActivated) {
            userInterface = new ClientGUI(clientEngineToGUI, guiToClientEngine);

        }else{
            terminalInterface=new TerminalInterface(clientEngineToGUI,guiToClientEngine);
            userInterfaceThread = Executors.newSingleThreadExecutor();
        }

    }








    public static void main(String[] args) {

        Future<Integer> exitCodeClient, exitCodeUserInterface;
        int exitCode;
        if (args.length < 1) {
            System.err.println("args: userinterface(true/false) ");
            return;
        }

        //INIT AND START
        try {

            ClientHost host = new ClientHost(Boolean.parseBoolean(args[0]));


            exitCodeClient = host.clientThread.submit(host.clientEngine);
            EventQueue.invokeLater(host.userInterface);


            while (true) {

                if(host.guiActivated) {
                    /**
                     * GRAPHICAL INTERFACE
                     *
                     *
                     *
                     **/
                    try {
                        //exitcode = exitCodeUserInterface.get(100, TimeUnit.MILLISECONDS);
                        switch (exitCode) {
                            case EXIT://chiudo tutto
                                host.clientThread.awaitTermination(10, TimeUnit.SECONDS);
                                return;
                            case ERROR://errore restarting...
                                EventQueue.invokeLater((host.userInterface = new ClientGUI(host.clientEngineToGUI, host.guiToClientEngine)));
                                break;
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace(System.err);
                    } catch (TimeoutException e) {/*tutto normale*/}
                }else {

                    /**
                     * TERMINAL INTERFACE
                     *
                     *
                     **/
                    //todo
                }

                /**
                 * CLIENT/ANONYMOUSCLIENT
                 *
                 *
                 *
                 **/
                try {
                    switch (exitCodeClient.get(100, TimeUnit.MILLISECONDS)) {
                        case EXIT://chiudo tutto
                            host.userInterfaceThread.awaitTermination(10, TimeUnit.SECONDS);
                            return;
                        case ERROR://errore restarting...
                            host.clientThread.submit((host.clientEngine=new ClientEngine(host.clientEngineToGUI,host.guiToClientEngine)));
                            break;
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace(System.err);
                } catch (TimeoutException e) {/*tutto normale.Questa exc viene lanciata se il thread sta ancora lavorando*/}

            }

        } catch (Exception exc) {
            exc.printStackTrace();
            return;
        }
    }
}
