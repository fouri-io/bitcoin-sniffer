package io.fouri;

import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;


import java.io.File;
import java.util.List;

/**
 * Sample App that listens to the BitCoin network and outputs Transactions as they occur in Real-time.
 * Maven Build: mvn clean package
 * Command Line Run: java -jar bitcoin-sniffer-0.1-jar-with-dependencies.jar address-to-send-back-to prod
 */
public class BitCoinSniffer
{
    public static void main( String[] args ) throws BlockStoreException {
        System.out.println( "Hello World!" );

        BriefLogFormatter.init();
        if (args.length < 2) {
            System.err.println("Usage: address-to-send-back-to [regtest|testnet]");
            return;
        }

        /* Figure out which network we should connect to. Each one gets its own set of files. */
        NetworkParameters params;
        String filePrefix;
        if (args[1].equals("testnet")) {
            params = TestNet3Params.get();
            filePrefix = "forwarding-service-testnet";
        } else if (args[1].equals("regtest")) {
            params = RegTestParams.get();
            filePrefix = "forwarding-service-regtest";
        } else {
            params = MainNetParams.get(); //-- Uncomment to allow production
            filePrefix = "forwarding-service";
            //params = TestNet3Params.get();
            //filePrefix = "forwarding-service-testnet";
        }

        /* Create Encryption Keys  -- not sure how to link to wallet yet */
        ECKey key = new ECKey();
        System.out.println("Public Address: " + key.getPubKeyHash());
        System.out.println("Private Key: " + key.getPrivateKeyEncoded(params));

        /* Create wallet and blockchain (with storage) */
        Wallet wallet = new Wallet(params, KeyChainGroup.createBasic(params));
        File file = new File(filePrefix + "_" + "my-blockchain");
        SPVBlockStore store = new SPVBlockStore(params, file);
        BlockChain chain = new BlockChain(params, wallet, store);

        System.out.println("Connecting to network and downloading chain, this could take a while...");

        /* Connect to network and start listening */
        PeerGroup peerGroup = new PeerGroup(params,chain);
        peerGroup.setUserAgent("Sample App", "1.0");
        peerGroup.addWallet(wallet);
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));


        System.out.println("Blockchain Downloaded, listening for transactions...");

        peerGroup.addOnTransactionBroadcastListener(new OnTransactionBroadcastListener() {
            @Override
            public void onTransaction(Peer peer, Transaction transaction) {
                System.out.println("New Transaction: " + transaction.getTxId());
                List<TransactionOutput> outputList = transaction.getOutputs();
                System.out.println("Transaction has " + outputList.size() + " outputs");
                for (TransactionOutput output : outputList) {
                     Coin coin = output.getValue();
                     System.out.println("Output: " + coin.toPlainString());
                }
                System.out.println("--------------------------------------------------------------------------");


            }
        });

        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
            System.out.println("You got some money! Balance: " + newBalance);
            }
        });

        peerGroup.start(); //maybe use async
        peerGroup.downloadBlockChain();

        while(true){}

    }
}
