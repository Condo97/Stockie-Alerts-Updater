package main;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.TokenUtil;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;
import com.turo.pushy.apns.util.concurrent.PushNotificationResponseListener;
import io.netty.util.concurrent.Future;
import objects.Alert;
import objects.Stock;
import yahoofinance.YahooFinance;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    Toolkit toolkit;
    Timer timer;
    ApnsClient apnsClient;
    ArrayList<PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>> pushNotificationFutures;

    public static void main(String[] args) {
        new Main(Integer.parseInt(args[0]));
    }

    public Main(int delay) {
        toolkit = Toolkit.getDefaultToolkit();
        pushNotificationFutures = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Closing...");

                final Future<Void> closeFuture = apnsClient.close();

                try {
                    closeFuture.await();
                } catch(Exception e) {
                    System.out.println("Could not properly close futures.");
                    e.printStackTrace();
                }

                System.out.println("Enjoy your day!");
            }
        });

        try {
            apnsClient = new ApnsClientBuilder().setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST).setClientCredentials(new File("APNSCertificates/aps_development.p12"), "Pumkin97?!").build();
        } catch(Exception e) {
            e.printStackTrace();
        }

        timer = new Timer();
        timer.scheduleAtFixedRate(new CheckStocks(), 0, delay * 1000);
    }

    class CheckStocks extends TimerTask {
        public void run() {
            try {
                Driver dr = new Driver();

                //Update Stocks
                ArrayList<Stock> stocks = dr.getAllStocks();
                for(Stock stock: stocks) {
                    yahoofinance.Stock yahoofinanceStock = YahooFinance.get(stock.getSymbol());
                    if(yahoofinanceStock == null) dr.deleteStock(stock);

                    stock.setLastPrice(yahoofinanceStock.getQuote().getPrice().doubleValue());
                    stock.setCompany(yahoofinanceStock.getName());

                    dr.updateStock(stock);
                }

                //Send Alerts
                ArrayList<Alert> alerts = dr.getAllAlerts();
                for(Alert alert: alerts) {
                    Stock stock = dr.getStock(alert.getSymbol());

                    if(!alert.isExecuted() && ((alert.isOverPrice() && stock.getLastPrice() > alert.getPrice()) || (!alert.isOverPrice() && alert.getPrice() > stock.getLastPrice()))) {
                        ArrayList<String> deviceTokens = dr.getDeviceTokens(alert.getUserID());

                        for(String deviceToken: deviceTokens) {
                            final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();

                            String payloadString = "" + stock.getCompany() + "(" + stock.getSymbol() + ") has gone " + (alert.isOverPrice() ? "over" : "under") + " " + alert.getPrice() + " and is currently at: " + stock.getLastPrice();

                            payloadBuilder.setAlertBody(payloadString);

                            final String payload = payloadBuilder.buildWithDefaultMaximumLength();
                            final String token = TokenUtil.sanitizeTokenString(deviceToken);

                            final SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, "com.acapplications.stockie", payload);

                            final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture = apnsClient.sendNotification(pushNotification);

                            sendNotificationFuture.addListener(new PushNotificationResponseListener<SimpleApnsPushNotification>() {
                                @Override
                                public void operationComplete(PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> simpleApnsPushNotificationPushNotificationResponsePushNotificationFuture) throws Exception {
                                    if(simpleApnsPushNotificationPushNotificationResponsePushNotificationFuture.isSuccess()) {
                                        dr.setAlertExecuted(alert, true);
                                    }
                                }
                            });

                            pushNotificationFutures.add(sendNotificationFuture);
                        }
                    }
                }

                //Verify Alerts
                for(PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture: pushNotificationFutures) {
                    final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = sendNotificationFuture.get();

                    if(!pushNotificationResponse.isAccepted()) {
                        System.out.println("Push notification rejected by APNs gateway. Here's why: " + pushNotificationResponse.getRejectionReason());

                        if(pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
                            System.out.println("The token is invalid as of: " + pushNotificationResponse.getTokenInvalidationTimestamp() + "\nRemoving...");
                            dr.removeDeviceToken(pushNotificationResponse.getPushNotification().getToken());
                        }
                    }
                }

                dr.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
