/*
 * File: CIServer.java
 * ------------------------------
 * When it is finished, this program will implement a basic
 * ecommerce network management server. Remember to update this comment!
 */

package edu.cis.Controller;

import acm.program.*;
import edu.cis.Model.*;
import edu.cis.Utils.SimpleServer;

import java.util.ArrayList;
import java.util.Objects;

public class CIServer extends ConsoleProgram implements SimpleServerListener {

    /* The internet port to listen to requests on */
    private static final int PORT = 8000;

    /* The server object. All you need to do is start it */
    private SimpleServer server = new SimpleServer(this, PORT);

    private ArrayList<CISUser> users = new ArrayList<>();

    private Menu menu = new Menu("010607");

    /**
     * Starts the server running so that when a program sends
     * a request to this server, the method requestMade is
     * called.
     */
    public void run() {
        println("Starting server on port " + PORT);
        server.start();
    }

    /**
     * When a request is sent to this server, this method is
     * called. It must return a String.
     *
     * @param req a Request object built by SimpleServer from an
     *            incoming network request by the client
     */
    public String requestMade(Request req) {
        String cmd = req.getCommand();
        println(req.toString());

        if (req.getCommand().equals(CISConstants.PING)) {
            final String PING_MSG = "Hello, internet";
            println("   => " + PING_MSG);
            return PING_MSG;
        }
        if (req.getCommand().equals(CISConstants.CREATE_USER)) {
            return createUser(req);
        } else if (req.getCommand().equals(CISConstants.ADD_MENU_ITEM)) {
            return addMenuItem(req);
        } else if (req.getCommand().equals(CISConstants.PLACE_ORDER)) {
            return placeOrder(req);
        } else if (req.getCommand().equals(CISConstants.DELETE_ORDER)) {
            return deleteOrder(req);
        } else if (req.getCommand().equals(CISConstants.GET_ORDER)) {
            return getOrder(req);
        } else if (req.getCommand().equals(CISConstants.GET_ITEM)) {
            return getItem(req);
        } else if (req.getCommand().equals(CISConstants.GET_USER)) {
            return getUser(req);
        } else if (req.getCommand().equals(CISConstants.GET_CART)) {
            return getCart(req);
        }

        return "Error: Unknown command " + cmd + ".";
    }

    public String createUser(Request req) {
        String userID = req.getParam(CISConstants.USER_ID_PARAM);
        String name = req.getParam(CISConstants.USER_NAME_PARAM);
        String yearLevel = req.getParam(CISConstants.YEAR_LEVEL_PARAM);

        if (name == null || userID == null || yearLevel == null) {
            return CISConstants.PARAM_MISSING_ERR;
        }

        CISUser user = new CISUser(userID, name, yearLevel);
        users.add(user);
        return CISConstants.SUCCESS;
    }

    public String addMenuItem(Request req) {
        String itemName = req.getParam(CISConstants.ITEM_NAME_PARAM);
        String description = req.getParam(CISConstants.DESC_PARAM);
        double price = Double.parseDouble(req.getParam(CISConstants.PRICE_PARAM));
        String itemType = req.getParam(CISConstants.ITEM_TYPE_PARAM);
        String itemID = req.getParam(CISConstants.ITEM_ID_PARAM);

        if (itemName == null || itemID == null || description == null || itemType == null) {
            return CISConstants.PARAM_MISSING_ERR;
        }

        MenuItem menuItem = new MenuItem(itemName, description, price, itemID, itemType);
        menu.getEatriumItems().add(menuItem);
        return CISConstants.SUCCESS;
    }

    public String placeOrder(Request req) {
        String orderID = req.getParam(CISConstants.ORDER_ID_PARAM);
        String menuItemID = req.getParam(CISConstants.ITEM_ID_PARAM);
        String userID = req.getParam(CISConstants.USER_ID_PARAM);
        String orderType = req.getParam(CISConstants.ORDER_TYPE_PARAM);

        if (orderID == null) {
            return CISConstants.ORDER_INVALID_ERR;
        }
        if (menu.getEatriumItems().isEmpty()) {
            return "Error: empty menu.";
        }

        CISUser user = new CISUser(userID, orderID, orderType);  // temp
        boolean userExists = false;
        for (CISUser u : users) {
            if (Objects.equals(u.getUserID(), userID)) {
                user = u;
                userExists = true;
                break;
            }
        }
        if (!userExists) return CISConstants.USER_INVALID_ERR;

        boolean orderExists = false;
        if (!user.getOrders().isEmpty()) {
            for (Order order : user.getOrders()) {
                if (order.getOrderID().equals(orderID)) {
                    orderExists = true;
                    break;
                }
            }
            if (orderExists) return CISConstants.DUP_ORDER_ERR;
        }

        for (CISUser u : users) {
            if (Objects.equals(u.getUserID(), userID)) continue;
            for (Order o : u.getOrders()) {
                if (Objects.equals(o.getOrderID(), orderID)) {
                    return CISConstants.ORDER_INVALID_ERR;
                }
            }
        }

        MenuItem item = new MenuItem("temp", "temp", 0.0, "temp", "temp");
        boolean itemExists = false;
        for (MenuItem m : menu.getEatriumItems()) {
            if (m.getId().equals(menuItemID)) {
                item = m;
                itemExists = true;
                break;
            }
            if (m.getId().equals(menuItemID) && m.getAmountAvailable() <= 0) {
                return CISConstants.SOLD_OUT_ERR;
            }
        }
        if (!itemExists) {
            return CISConstants.INVALID_MENU_ITEM_ERR;
        }
        if (item.getPrice() > user.getMoney()) {
            return CISConstants.USER_BROKE_ERR;
        }
        Order order = new Order(menuItemID, orderType, orderID);
        item.setAmountAvailable(item.getAmountAvailable() - 1);
        user.getOrders().add(order);
        user.setMoney(user.getMoney() - item.getPrice());

        return CISConstants.SUCCESS;
    }

    public String deleteOrder(Request req) {
        String orderID = req.getParam(CISConstants.ORDER_ID_PARAM);
        String userID = req.getParam(CISConstants.USER_ID_PARAM);

        CISUser user = new CISUser(userID, orderID, orderID);
        boolean userExists = false;
        for (CISUser u : users) {
            if (Objects.equals(u.getUserID(), userID)) {
                user = u;
                userExists = true;
                break;
            }
        }
        if (!userExists) return CISConstants.USER_INVALID_ERR;

        boolean orderExists = false;
        for (Order o : user.getOrders()) {
            if (Objects.equals(o.getOrderID(), orderID)) {
                orderExists = true;
                user.getOrders().remove(o);
                break;
            }
        }
        if (!orderExists) {
            return CISConstants.ORDER_INVALID_ERR;
        }
        return CISConstants.SUCCESS;
    }

    public String getOrder(Request req) {
        String orderID = req.getParam(CISConstants.ORDER_ID_PARAM);
        String userID = req.getParam(CISConstants.USER_ID_PARAM);

        CISUser user = new CISUser(userID, orderID, orderID);
        boolean userExists = false;
        for (CISUser u : users) {
            if (Objects.equals(u.getUserID(), userID)) {
                user = u;
                userExists = true;
                break;
            }
        }
        if (!userExists) {
            return CISConstants.USER_INVALID_ERR;
        }
        Order order = new Order("temp", "temp", "temp");
        boolean orderExists = false;
        for (Order o : user.getOrders()) {
            if (Objects.equals(o.getOrderID(), orderID)) {
                order = o;
                orderExists = true;
                break;
            }
        }
        if (!orderExists) {
            return CISConstants.ORDER_INVALID_ERR;
        }
        return order.toString();
    }

    public String getUser(Request req) {
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        CISUser user = new CISUser("temp", "temp", "temp");
        boolean userExists = false;
        for (CISUser u : users) {
            if (u.getUserID().equals(userId)) {
                user = u;
                userExists = true;
                break;
            }
        }
        if (!userExists) {
            return CISConstants.USER_INVALID_ERR;
        }
        return user.toString();
    }

    public String getItem(Request req) {
        String itemId = req.getParam(CISConstants.ITEM_ID_PARAM);
        MenuItem item = new MenuItem("temp", "temp", 0.0, "temp", "temp");
        boolean itemExists = false;
        for (MenuItem m : menu.getEatriumItems()) {
            if (m.getId().equals(itemId)) {
                item = m;
                return item.toString();
            }
        }
        return CISConstants.INVALID_MENU_ITEM_ERR;
    }

    public String getCart(Request req) {
        String userId = req.getParam(CISConstants.USER_ID_PARAM);
        CISUser user = new CISUser("temp", "temp", "temp");
        boolean userExists = false;
        for (CISUser u : users) {
            if (Objects.equals(u.getUserID(), userId)) {
                user = u;
                userExists = true;
                break;
            }
        }
        if (!userExists) {
            return CISConstants.USER_INVALID_ERR;
        }

        if (user.getOrders().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Order order : user.getOrders()) {
            sb.append(order).append(", ");
        }
        // Remove the last comma and space
        sb.setLength(sb.length() - 2);

        return "Orders: " + sb.toString();
    }

    public static void main(String[] args) {
        CIServer server = new CIServer();
        server.start(args);
    }
}