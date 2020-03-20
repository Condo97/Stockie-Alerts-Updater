package objects;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Random;

public class Watchlist {
    private String watchlistID, name;
    private ArrayList<Stock> stocks;

    public Watchlist(String name) {
        this.name = name;

        stocks = new ArrayList<Stock>();

        Random rd = new Random();
        byte[] b = new byte[255];
        rd.nextBytes(b);

        watchlistID = DatatypeConverter.printBase64Binary(b);
    }

    public Watchlist(String watchlistID, String name, ArrayList<Stock> stocks) {
        this.watchlistID = watchlistID;
        this.stocks = stocks;
        this.name = name;
    }

    public String getWatchlistID() {
        return watchlistID;
    }

    public void setWatchlistID(String watchlistID) {
        this.watchlistID = watchlistID;
    }

    public ArrayList<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(ArrayList<Stock> stocks) {
        this.stocks = stocks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
