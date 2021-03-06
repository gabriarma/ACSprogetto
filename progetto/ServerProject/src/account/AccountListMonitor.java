/*
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

package account;

import customException.MaxNumberAccountReached;
import interfaces.ClientInterface;
import utility.Account;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/* *
 * Implementazione con multiple reader single writer lock
 * Il testing è effettuato nella classe accountMonitorTest_2 nel pkg
 *  ServerProject/src/test/accountMonitorTest_2
 */
public class AccountListMonitor implements AccountCollectionInterface {

    final private int MAXACCOUNTNUMBERDEFAULT = 300;
    final private int MAXACCOUNTNUMBER;

    private Account[] accountList;
    private int length = 0;
    private int lastFreeposition = -1;//funziona come una cache

    private ReentrantReadWriteLock listLock = new ReentrantReadWriteLock();

    /* ****************************************************************************************************/
    //COSTRUTTORI
    public AccountListMonitor(int maxAccountNumber) throws IllegalArgumentException {
        if (maxAccountNumber <= 0) {
            throw new IllegalArgumentException("Class: AccountListMonitor - Error: maxAccountNumber <= 0");
        }
        this.MAXACCOUNTNUMBER = maxAccountNumber;
        this.accountList = new Account[MAXACCOUNTNUMBER];
    }

    public AccountListMonitor() {
        this.MAXACCOUNTNUMBER = this.MAXACCOUNTNUMBERDEFAULT;
        this.accountList = new Account[MAXACCOUNTNUMBER];
    }


    /* ****************************************************************************************************/
    //METODI MODIFICATORI
    public int addAccount(Account account) throws NullPointerException, MaxNumberAccountReached {
        int posizione;
        if (account == null) {
            throw new NullPointerException("account==null");
        }
        if (this.getNumberOfAccount() >= MAXACCOUNTNUMBER) {
            throw new MaxNumberAccountReached();
        }


        listLock.writeLock().lock();
        try {

            if (lastFreeposition != -1) {//cache funzionante
                accountList[lastFreeposition] = account;
                account.setAccountId(lastFreeposition);
                posizione = lastFreeposition;//System.err.println(" cache!");
                lastFreeposition = -1;
                this.length++;
                return posizione;
            } else {
                for (int i = 0; i < this.MAXACCOUNTNUMBER; i++) {
                    if (accountList[i] == null) {
                        accountList[i] = account;
                        account.setAccountId(i);
                        this.length++;
                        return i;
                    }
                }
                throw new MaxNumberAccountReached();
            }
        } finally {
            listLock.writeLock().unlock();
        }
    }

    public Account addAccount(Account account, int accountId) {
        testRange(accountId);
        if(account==null)throw new NullPointerException();
        Account prev;
        account.setAccountId(accountId);
        listLock.writeLock().lock();
        try {
            prev = accountList[accountId];
            accountList[accountId] = account;
            if(prev==null){
                this.length++;
            }
        } finally {
            this.listLock.writeLock().unlock();
        }
        return prev;
    }


    public Account removeAccount(int accountId) {
        testRange(accountId);

        Account toRemove;
        listLock.writeLock().lock();
        try {
            toRemove = accountList[accountId];
            if (toRemove != null) {
                this.length--;
                accountList[accountId] = null;
            }
            lastFreeposition = accountId;
        } finally {
            this.listLock.writeLock().unlock();
        }
        return toRemove;
    }

    public int putIfAbsentEmailUsername(Account account) throws NullPointerException, MaxNumberAccountReached, IllegalArgumentException {

        if(account==null)throw new IllegalArgumentException("account==null");
        if (this.getNumberOfAccount() >= MAXACCOUNTNUMBER) {
            throw new MaxNumberAccountReached();
        }
        String email=account.getEmail();
        String username=account.getUsername();
        if(email==null||username==null)throw new NullPointerException("email or username==null");

        this.listLock.writeLock().lock();
        try {
            for (int i = 0; i < this.MAXACCOUNTNUMBER; i++) {
                if (accountList[i] != null) {
                    if (email.equalsIgnoreCase(accountList[i].getEmail())) {
                        return -1;
                    }
                    if(username.equals(accountList[i].getUsername())){
                        return -2;
                    }
                }
            }//se finisce il for vuol dire che non sono presenti account con quella email o quella password
            return this.addAccount(account);
        }finally{
            listLock.writeLock().unlock();
        }
    }

    public int putIfAbsentUsername(Account account) throws NullPointerException, MaxNumberAccountReached, IllegalArgumentException {

        if(account==null)throw new IllegalArgumentException("account==null");
        if (this.getNumberOfAccount() >= MAXACCOUNTNUMBER) {
            throw new MaxNumberAccountReached();
        }
        String username=account.getUsername();
        if(username==null)throw new NullPointerException("username==null");

        this.listLock.writeLock().lock();
        try {
            for (int i = 0; i < this.MAXACCOUNTNUMBER; i++) {
                if (accountList[i] != null) {
                    if(username.equals(accountList[i].getUsername())){
                        return -2;
                    }
                }
            }
            return this.addAccount(account);
        }finally{
            listLock.writeLock().unlock();
        }
    }

    public Account removeAccountCheckEmail(int accountId,String email){
        testRange(accountId);
        if(email==null)throw new IllegalArgumentException();

        Account toRemove;
        listLock.writeLock().lock();
        try {
            toRemove = accountList[accountId];
            if (toRemove != null) {
                if(email.equalsIgnoreCase(toRemove.getEmail())) {
                    this.length--;
                    accountList[accountId] = null;
                    lastFreeposition = accountId;
                }else{
                    toRemove=null;
                }
            }
        } finally {
            this.listLock.writeLock().unlock();
        }
        return toRemove;
    }

    /* ****************************************************************************************************/
    //METODI GETTER


    public Account isMember(String email,String username) throws IllegalArgumentException {
        if(email==null&&username==null){throw new IllegalArgumentException("email==null AND username==null");}
        String[] coppia;

        listLock.readLock().lock();
        try {
            for (int i = 0; i < this.MAXACCOUNTNUMBER; i++) {
                coppia = this.getEmailAndUsername(i);

                if ((email!=null&&email.equalsIgnoreCase(coppia[0])) || (username!=null&&username.equals(coppia[1]))) { //GLI AND e gli OR  sono cortocircuitati
                    return this.getAccountCopy(i);
                }
            }
            return null;
        }finally{
            listLock.readLock().unlock();
        }
    }



    public Account getAccountCopy(int accountId) {
        testRange(accountId);

        Account snapShot;

        this.listLock.readLock().lock();
        try {

            if (accountList[accountId] == null) {
                return null;
            }
            snapShot = accountList[accountId].copy();
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
            return Arrays.copyOf(accountList[accountId].getPassword(),accountList[accountId].getPassword().length);//snapshot;
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
            return  accountList[accountId].getStub();
        } finally {
            this.listLock.readLock().unlock();
        }
    }

    public String getEmail(int accountId){
        testRange(accountId);

        listLock.readLock().lock();
        try{
            return accountList[accountId].getEmail();
        }finally{
            listLock.readLock().unlock();
        }
    }

    @Override
    public String[] getTopicSubscribed(int accountId) {
        testRange(accountId);
        listLock.readLock().lock();
        try {
            return accountList[accountId].getTopicSubscribed();
        } finally {
            listLock.readLock().unlock();
        }
    }

    public Account getAccountCopyUsername(String username){
        if(username==null)throw new IllegalArgumentException("username==null");
        listLock.readLock().lock();
        try {
            for (int i = 0; i < this.MAXACCOUNTNUMBER; i++) {
                if(accountList[i]!=null){
                    if(username.equals(accountList[i].getUsername())){
                        return accountList[i].copy();
                    }
                }
            }
            return null;
        }finally{
            listLock.readLock().unlock();
        }

    }

    public Account getAccountCopyEmail(String email){
        if(email==null){throw new IllegalArgumentException("email==null");}
        listLock.readLock().lock();
        try {
            for (int i = 0; i < this.MAXACCOUNTNUMBER; i++) {
                if(accountList[i]!=null){
                    if(email.equalsIgnoreCase(accountList[i].getEmail())){
                        return accountList[i].copy();
                    }
                }
            }
            return null;
        }finally{
            listLock.readLock().unlock();
        }

    }


    public int getNumberOfAccount() {
        int l;
        listLock.readLock().lock();
        l = this.length;
        listLock.readLock().unlock();
        return l;
    }

    public int getMAXACCOUNTNUMBER() { return this.MAXACCOUNTNUMBER; }
    public int getMAXACCOUNTNUMBERDEFAULT(){return this.MAXACCOUNTNUMBERDEFAULT;}

    /* ****************************************************************************************************/
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
        return setStub(clientStub, accountId, false);
    }

    public ClientInterface setStub(ClientInterface clientStub, int accountId, boolean force) {
        testRange(accountId);

        listLock.writeLock().lock();
        try {
            ClientInterface prev = accountList[accountId].getStub();
            if (prev == null || force)
                accountList[accountId].setStub(clientStub);
            return prev;
        } finally {
            this.listLock.writeLock().unlock();
        }
    }

    public String setEmail(String email,int accountId){
        testRange(accountId);
        listLock.writeLock().lock();
        try{
            String oldEmail=accountList[accountId].getEmail();
            accountList[accountId].setEmail(email);
            return oldEmail;
        }finally{
            listLock.writeLock().unlock();
        }
    }

    @Override
    public boolean addTopic(String topicName, int accountId) throws NullPointerException {
        testRange(accountId);
        listLock.writeLock().lock();
        try {
            return accountList[accountId].addTopic(topicName);
        } finally {
            listLock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeTopic(String topicName, int accountId) throws NullPointerException {
        testRange(accountId);
        listLock.writeLock().lock();
        try {
            return accountList[accountId].removeTopic(topicName);
        } finally {
            listLock.writeLock().unlock();
        }
    }

    /* ****************************************************************************************************/
    //METODI PRIVATI
    private void testRange(int n) {
        if (n >= this.MAXACCOUNTNUMBER || n < 0) {
            throw new IllegalArgumentException("accountId>MAXACCOUNTNUMBER || accountId<0");
        }
    }

    private String[] getEmailAndUsername(int accountId){
        String[] coppia=new String[2];
        testRange(accountId);

        listLock.readLock().lock();//reentrant lock
        try{
            try {
                coppia[0] = this.getEmail(accountId);
            }catch (NullPointerException exc){
                coppia[0]=null;
            }
            try{
                coppia[1]= this.getUsername(accountId);
            }catch (NullPointerException exc){
                coppia[1]=null;
            }
        } finally{
            listLock.readLock().unlock();
        }
        return coppia;
    }





}

