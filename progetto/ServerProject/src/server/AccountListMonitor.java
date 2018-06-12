/**
 * This file is part of ACSprogetto.
 * <p>
 * ACSprogetto is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * ACSprogetto is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with ACSprogetto.  If not, see <http://www.gnu.org/licenses/>.
 **/

package server;

import interfaces.ClientInterface;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;

import customException.*;
import utility.Account;

import javax.management.RuntimeErrorException;

/**
 * Implementazione con multiple reader single writer lock
 */
public class AccountListMonitor implements AccountCollectionInterface {

    final private int MAXACCOUNTNUMBERDEFAULT = 300;
    final private int MAXACCOUNTNUMBER;

    private Account[] accountList;
    private int length = 0;
    private int lastFreeposition = -1;//funziona come una cache

    private ReentrantReadWriteLock listLock = new ReentrantReadWriteLock();

    private ConcurrentHashMap<Long, Pair> h;


    /*****************************************************************************************************/
    //COSTRUTTORI
    public AccountListMonitor(int maxAccountNumber) throws IllegalArgumentException {
        if (maxAccountNumber <= 0) {
            throw new IllegalArgumentException("maxaccountnumber<=0");
        }
        this.MAXACCOUNTNUMBER = maxAccountNumber;
        this.accountList = new Account[MAXACCOUNTNUMBER];
    }

    public AccountListMonitor() {

        this.MAXACCOUNTNUMBER = this.MAXACCOUNTNUMBERDEFAULT;
        this.accountList = new Account[MAXACCOUNTNUMBER];
    }


    /*****************************************************************************************************/
    //METODI MODIFICATORI
    public int addAccount(Account account) throws NullPointerException, MaxNumberAccountReached, AccountMonitorRuntimeException {
        int posizione;
        if (account == null) {
            throw new NullPointerException("account==null");
        }
        if (this.getNumberOfAccount() >= MAXACCOUNTNUMBER) {
            throw new MaxNumberAccountReached();
        }


        this.listLock.writeLock().lock();
        try {

            if (lastFreeposition != -1) {//cache funzionante
                accountList[lastFreeposition] = account;
                account.setAccountId(lastFreeposition);
                posizione = lastFreeposition;//System.err.println(" cache!");
                lastFreeposition = -1;
                return posizione;
            } else {
                for (int i = 0; i < this.MAXACCOUNTNUMBER; i++) {
                    if (accountList[i] == null) {
                        accountList[i] = account;
                        account.setAccountId(i);
                        return i;
                    }
                }
            }
        } finally {
            this.length++;
            listLock.writeLock().unlock();
        }
        throw new AccountMonitorRuntimeException("ERRORE:addAccount");//Non dovrebbe mai essere sollevata :D speremmu!
    }

    public Account addAccount(Account account, int accountId) {
        testRange(accountId);

        Account prev;
        this.listLock.writeLock().lock();
        try {
            prev = accountList[accountId];

            //gestione length
            if (prev != null && account == null) {
                this.length--;
            } else if (prev == null && account != null) {
                this.length++;
            }
            if (account == null) {
                lastFreeposition = accountId;
            } else {
                account.setAccountId(accountId);
            }
            accountList[accountId] = account;
        } finally {
            this.listLock.writeLock().unlock();
        }
        return prev;
    }

    public Account removeAccount(int accountId) {
        testRange(accountId);

        Account toRemove;
        this.listLock.writeLock().lock();
        try {
            toRemove = accountList[accountId];
            if (toRemove != null) {
                this.length--;
                accountList[accountId] = null;
            }
        } finally {
            this.listLock.writeLock().unlock();
        }
        return toRemove;
    }


    /*****************************************************************************************************/
    //METODI GETTER
    public Account getAccountCopy(int accountId) {
        testRange(accountId);

        Account snapShot;
        Account curr;


        this.listLock.readLock().lock();
        try {
            curr = accountList[accountId];
            if (curr == null) {
                return null;
            }
            snapShot = new
                    Account(curr.getUsername(), curr.getPassword(), curr.getStub(), curr.getPublicKey(), curr.getAccountId());
            return snapShot;
        } finally {
            listLock.readLock().unlock();
        }
    }



    public String getPublicKey(int accountId) {
        testRange(accountId);

        listLock.readLock().lock();
        try {
            return accountList[accountId].getPublicKey();
        } finally {
            this.listLock.readLock().unlock();
        }
    }

    public byte[] getPassword(int accountId) {
        testRange(accountId);

        listLock.readLock().lock();
        try {
            return accountList[accountId].getPassword();
        } finally {
            this.listLock.readLock().unlock();
        }
    }

    public String getUsername(int accountId) {
        testRange(accountId);

        listLock.readLock().lock();
        try {
            return accountList[accountId].getUsername();

        } finally {
            this.listLock.readLock().unlock();
        }
    }

    public ClientInterface getStub(int accountId) {
        testRange(accountId);

        listLock.readLock().lock();
        try {
            return accountList[accountId].getStub();

        } finally {
            this.listLock.readLock().unlock();
        }
    }



    public int getNumberOfAccount() {
        int l;
        listLock.readLock().lock();
        l = this.length;
        listLock.readLock().unlock();
        return l;
    }

    public int getMAXACCOUNTNUMBER() {
        return this.MAXACCOUNTNUMBER;
    }


    /*****************************************************************************************************/
    //METODI SETTER
    public String setPublicKey(String clientPublicKey, int accountId) {
        testRange(accountId);

        listLock.writeLock().lock();
        try {
            String prev = accountList[accountId].getPublicKey();
            accountList[accountId].setPublicKey(clientPublicKey);
            return prev;
        } finally {
            this.listLock.writeLock().unlock();
        }
    }

    public byte[] setPassword(String plainPassword, int accountId) throws NoSuchAlgorithmException {
        testRange(accountId);

        listLock.writeLock().lock();
        try {
            byte[] prev = accountList[accountId].getPassword();
            accountList[accountId].encryptAndSetPassword(plainPassword);
            return prev;
        } finally {
            this.listLock.writeLock().unlock();
        }
    }

    public String setUsername(String username, int accountId) {
        testRange(accountId);

        listLock.writeLock().lock();
        try {
            String prev = accountList[accountId].getUsername();
            accountList[accountId].setUsername(username);
            return prev;
        } finally {
            this.listLock.writeLock().unlock();
        }
    }

    public ClientInterface setStub(ClientInterface clientStub, int accountId) {
        testRange(accountId);

        listLock.writeLock().lock();
        try {
            ClientInterface prev = accountList[accountId].getStub();
            accountList[accountId].setStub(clientStub);
            return prev;
        } finally {
            this.listLock.writeLock().unlock();
        }
    }


    /*****************************************************************************************************/
    //METODI PRIVATI
    private void testRange(int n) {
        if (n >= this.MAXACCOUNTNUMBER || n < 0) {
            throw new IllegalArgumentException("accountId>MAXACCOUNTNUMBER || accountId<0");
        }
    }


}
