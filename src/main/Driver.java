package main;

import exceptions.DuplicateObjectException;
import exceptions.InvalidCredentialsException;
import exceptions.InvalidIdentifierException;
import objects.Alert;
import objects.Stock;

import java.sql.*;
import java.util.ArrayList;

public class Driver {
    private Connection connection;

    /******* Initialization *******/

    public Driver() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://localhost/stokieDatabase?autoReconnect=true&useSSL=false", "stokieDatabaseRemote", Passwords.driverPassword);
    }

    public void close() throws SQLException {
        connection.close();
    }


    /******* Device Flow *******/

    public ArrayList<String> getDeviceTokens(String userID) throws SQLException, ClassNotFoundException, InvalidIdentifierException {
        PreparedStatement ps = connection.prepareStatement("select deviceToken from Device where userID=?");
        ps.setString(1, userID);
        ResultSet rs = ps.executeQuery();

        ArrayList<String> deviceTokens = new ArrayList<>();
        while(rs.next()) deviceTokens.add(rs.getString("deviceToken"));
        if(deviceTokens.size() == 0) throw new InvalidIdentifierException("User ID");

        return deviceTokens;
    }

    public void removeDeviceToken(String deviceToken) throws SQLException, ClassNotFoundException {
        PreparedStatement ps = connection.prepareStatement("delete from Device where deviceToken=?");
        ps.setString(1, deviceToken);

        ps.executeUpdate();
    }

    public void removeDeviceToken(String userID, String deviceToken) throws SQLException, ClassNotFoundException {
        PreparedStatement ps = connection.prepareStatement("delete from Device where userID=? and deviceToken=?");
        ps.setString(1, userID);
        ps.setString(2, deviceToken);

        ps.executeUpdate();
    }

    public String getUserIDForDeviceToken(String deviceToken) throws SQLException, ClassNotFoundException, InvalidIdentifierException {
        PreparedStatement ps = connection.prepareStatement("select userID from Device where deviceToken=?");
        ps.setString(1, deviceToken);
        ResultSet rs = ps.executeQuery();

        String userID = "";
        while(rs.next()) userID = rs.getString("userID");
        if(userID.equals("")) throw new InvalidIdentifierException("Device Token");

        return userID;
    }

    /******* Stock Flow *******/

    public void validateStock(Stock stock) throws SQLException, ClassNotFoundException, InvalidIdentifierException {
        PreparedStatement stockExistsPS = connection.prepareStatement("select * from Stock where symbol=?");
        stockExistsPS.setString(1, stock.getSymbol());
        ResultSet stockExistsRS = stockExistsPS.executeQuery();

        int count = 0;
        while(stockExistsRS.next()) count++;
        if(count == 0) throw new InvalidIdentifierException("StockID");
    }

    public boolean symbolExists(String symbol) throws SQLException, ClassNotFoundException {
        PreparedStatement stockExistsPS = connection.prepareStatement("select * from Stock where symbol=?");
        stockExistsPS.setString(1, symbol);
        ResultSet stockExistsRS = stockExistsPS.executeQuery();

        while(stockExistsRS.next()) return true;
        return false;
    }

    public void createStock(Stock stock) throws SQLException, ClassNotFoundException, DuplicateObjectException {
        PreparedStatement duplicateStockPS = connection.prepareStatement("select * from Stock where symbol=?");
        duplicateStockPS.setString(1, stock.getSymbol());
        ResultSet duplicateStockRS = duplicateStockPS.executeQuery();

        while(duplicateStockRS.next()) throw new DuplicateObjectException("Stock");

        PreparedStatement ps = connection.prepareStatement("insert into Stock (symbol, company, lastPrice) values (?,?,?)");
        ps.setString(1, stock.getSymbol());
        ps.setString(2, stock.getCompany());
        ps.setDouble(3, stock.getLastPrice());

        ps.executeUpdate();
    }

    public void updateStock(Stock stock) throws SQLException, ClassNotFoundException, InvalidIdentifierException {
        validateStock(stock);

        PreparedStatement ps = connection.prepareStatement("update Stock set company=?, lastPrice=? where symbol=?");
        ps.setString(1, stock.getCompany());
        ps.setDouble(2, stock.getLastPrice());
        ps.setString(3, stock.getSymbol());

        ps.executeUpdate();
    }

    public void deleteStock(Stock stock) throws SQLException, ClassNotFoundException, InvalidIdentifierException {
        validateStock(stock);

        PreparedStatement ps = connection.prepareStatement("delete from Stock where symbol=?");
        ps.setString(1, stock.getSymbol());

        ps.executeUpdate();
    }

    public ArrayList<Stock> getAllStocks() throws SQLException, ClassNotFoundException {
        PreparedStatement ps = connection.prepareStatement("select * from Stock");
        ResultSet rs = ps.executeQuery();

        ArrayList<Stock> stocks = new ArrayList<Stock>();

        while(rs.next()) {
            String symbol = rs.getString("symbol");
            String company = rs.getString("company");
            double lastPrice = rs.getDouble("lastPrice");

            stocks.add(new Stock(symbol, company, lastPrice));
        }

        return stocks;
    }

    public Stock getStock(String symbol) throws SQLException, ClassNotFoundException, InvalidIdentifierException {
        PreparedStatement ps = connection.prepareStatement("select * from Stock where symbol=?");
        ps.setString(1, symbol);
        ResultSet rs = ps.executeQuery();

        Stock stock = null;

        while(rs.next()) {
            String company = rs.getString("company");
            double lastPrice = rs.getDouble("lastPrice");

            stock = new Stock(symbol, company, lastPrice);
        }

        if(stock == null) throw new InvalidIdentifierException("Stock Symbol");

        return stock;
    }


    /******* Alert Flow *******/

    public ArrayList<Alert> getAllAlerts() throws SQLException, ClassNotFoundException {
        PreparedStatement ps = connection.prepareStatement("select * from Alert");
        ResultSet rs = ps.executeQuery();

        ArrayList<Alert> alerts = new ArrayList<>();

        while(rs.next()) {
            String alertID = rs.getString("alertID");
            String symbol = rs.getString("symbol");
            String userID = rs.getString("userID");
            double price = rs.getDouble("price");
            boolean overPrice = rs.getBoolean("overPrice");
            boolean executed = rs.getBoolean("executed");

            alerts.add(new Alert(alertID, symbol, userID, price, overPrice, executed));
        }

        return alerts;
    }

    public void setAlertExecuted(Alert alert, boolean executed) throws SQLException, ClassNotFoundException, InvalidIdentifierException {
        PreparedStatement verificationPS = connection.prepareStatement("select symbol from Alert where alertID=?");
        verificationPS.setString(1, alert.getAlertID());
        ResultSet verificationRS = verificationPS.executeQuery();

        int count = 0;
        while(verificationRS.next()) count++;
        if(count == 0) throw new InvalidIdentifierException("Alert ID");

        PreparedStatement ps = connection.prepareStatement("update Alert set executed=? where alertID=?");
        ps.setBoolean(1, executed);
        ps.setString(2, alert.getAlertID());

        ps.executeUpdate();
    }
}
