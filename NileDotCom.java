/*
Name: Sachin Cuff
Course: CNT 4714 - Fall 2024
Assignment title: Project 1 – An Event-driven Enterprise Simulation 
Date: Sunday September 8, 2024
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;

public class NileDotCom {
    // GUI components
    private JFrame frame;
    private JTextField itemIdField, quantityField; // Input fields for item ID and quantity
    private JTextField[] cartFields; // Text fields to display items in the cart
    private JLabel itemIdPromptLabel, quantityLabel, itemDetailsLabel, itemsCountLabel, cartHeaderLabel; // Labels for guiding the user
    private JButton searchButton, addToCartButton, viewOrderButton, checkoutButton, emptyCartButton, quitButton; // Buttons for various actions
    private JTextField itemDetailsField, itemsCountField; // Fields to display item details and cart subtotal

    // Data structures for inventory and cart
    private Map<String, Item> inventory; // Map to store inventory items
    private java.util.List<Item> cart; // List to store items added to the cart
    private double subtotal = 0.0; // Running subtotal of the cart
    private int itemCount = 0; // Number of items currently in the cart
    private int currentItemNumber = 1; // Keeps track of the current item number being processed
    private static final double TAX_RATE = 0.06; // Tax rate (6%)
    private static final int MAX_CART_SIZE = 5; // Maximum number of items allowed in the cart

    public NileDotCom() {
        // Initialize inventory and cart
        inventory = new HashMap<>();
        cart = new ArrayList<>();
        
        // Load inventory data from a file
        loadInventory();

        // Set up the main frame of the GUI
        frame = new JFrame("Nile.Com - Fall 2024");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Input panel for item ID and quantity
        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        itemIdPromptLabel = new JLabel("Enter item ID for item #" + currentItemNumber + ":");
        quantityLabel = new JLabel("Enter quantity for item #" + currentItemNumber + ":");
        itemIdField = new JTextField();
        quantityField = new JTextField();
        itemDetailsLabel = new JLabel("Item #" + currentItemNumber + " Details:");
        itemsCountLabel = new JLabel("Cart subtotal for " + itemCount + " item(s):");
        itemDetailsField = new JTextField();
        itemDetailsField.setEditable(false);
        itemsCountField = new JTextField();
        itemsCountField.setEditable(false);

        // Add components to input panel
        inputPanel.add(itemIdPromptLabel);
        inputPanel.add(itemIdField);
        inputPanel.add(quantityLabel);
        inputPanel.add(quantityField);
        inputPanel.add(itemDetailsLabel);
        inputPanel.add(itemDetailsField);
        inputPanel.add(itemsCountLabel);
        inputPanel.add(itemsCountField);

        // Add input panel to the top of the frame
        frame.add(inputPanel, BorderLayout.NORTH);

        // Cart display area with 5 boxes for items and a header
        JPanel cartPanel = new JPanel(new GridLayout(6, 1)); // 5 rows for items, 1 for header
        cartHeaderLabel = new JLabel("Your Shopping Cart Currently Contains 0 Item(s)", JLabel.CENTER);
        cartHeaderLabel.setForeground(Color.RED);
        cartPanel.add(cartHeaderLabel);

        // Initialize text fields for displaying cart items
        cartFields = new JTextField[MAX_CART_SIZE];
        for (int i = 0; i < MAX_CART_SIZE; i++) {
            cartFields[i] = new JTextField();
            cartFields[i].setEditable(false);
            cartFields[i].setBackground(Color.WHITE);
            cartPanel.add(cartFields[i]);
        }

        // Add cart panel to the center of the frame
        frame.add(cartPanel, BorderLayout.CENTER);

        // Bottom panel for action buttons
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        JPanel buttonPanel = new JPanel(new GridLayout(1, 6)); // Grid layout for buttons
        searchButton = new JButton("Search For Item #" + currentItemNumber);
        addToCartButton = new JButton("Add Item #" + currentItemNumber + " To Cart");
        viewOrderButton = new JButton("View Cart");
        checkoutButton = new JButton("Check Out");
        emptyCartButton = new JButton("Empty Cart - Start A New Order");
        quitButton = new JButton("Exit (Close App)");

        // Initially disable buttons that should not be active
        addToCartButton.setEnabled(false);
        viewOrderButton.setEnabled(false);
        checkoutButton.setEnabled(false);

        // Add buttons to the button panel
        buttonPanel.add(searchButton);
        buttonPanel.add(addToCartButton);
        buttonPanel.add(viewOrderButton);
        buttonPanel.add(checkoutButton);
        buttonPanel.add(emptyCartButton);
        buttonPanel.add(quitButton);
        bottomPanel.add(buttonPanel);
        
        // Add button panel to the bottom of the frame
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Event listeners for buttons
        searchButton.addActionListener(new SearchButtonListener());
        addToCartButton.addActionListener(new AddToCartButtonListener());
        viewOrderButton.addActionListener(new ViewOrderButtonListener());
        checkoutButton.addActionListener(new CheckoutButtonListener());
        emptyCartButton.addActionListener(e -> clearOrder()); // Clear the order when empty cart button is pressed
        quitButton.addActionListener(e -> System.exit(0)); // Exit the application

        // Show the GUI
        frame.setVisible(true);
    }

    // Load inventory from a CSV file
    private void loadInventory() {
        try (BufferedReader br = new BufferedReader(new FileReader("inventory.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                String itemId = data[0].trim();
                String description = data[1].trim();
                boolean inStock = Boolean.parseBoolean(data[2].trim());
                int quantity = Integer.parseInt(data[3].trim());
                double price = Double.parseDouble(data[4].trim());
                
                // Add item to inventory
                inventory.put(itemId, new Item(itemId, description, inStock, quantity, price));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Listener for the Search button
    private class SearchButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String itemId = itemIdField.getText();
            int quantity;

            // Validate quantity input
            try {
                quantity = Integer.parseInt(quantityField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid quantity.");
                return;
            }

            // Retrieve item from inventory
            Item item = inventory.get(itemId);

            // Check if item exists and is in stock
            if (item == null) {
                JOptionPane.showMessageDialog(frame, "Item not found in inventory.");
                addToCartButton.setEnabled(false);
                return;
            }

            if (!item.isInStock()) {
                JOptionPane.showMessageDialog(frame, "Item is out of stock.");
                addToCartButton.setEnabled(false);
                return;
            }

            // Check if requested quantity is available
            if (quantity > item.getQuantity()) {
                JOptionPane.showMessageDialog(frame,
                        String.format("Insufficient stock. Only %d on hand. Please reduce the quantity.", item.getQuantity()));
                quantityField.setText("");
                return;
            }

            // Calculate discount and display item details
            double discount = calculateDiscount(quantity);
            double discountedPrice = item.getPrice() * (1 - discount);

            itemDetailsField.setText(String.format("%s %s $%.2f %d %.0f%% $%.2f",
                    item.getItemId(), item.getDescription(), item.getPrice(), quantity, discount * 100, discountedPrice));

            itemDetailsLabel.setText(String.format("Item #%d Details:", currentItemNumber));

            // Enable Add to Cart button and disable Search button
            addToCartButton.setEnabled(true);
            searchButton.setEnabled(false);
        }
    }

    // Listener for the Add to Cart button
    private class AddToCartButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Check if cart is full
            if (itemCount >= MAX_CART_SIZE) {
                JOptionPane.showMessageDialog(frame, "You cannot add more than 5 items.");
                return;
            }

            String itemId = itemIdField.getText();
            int quantity;

            // Validate quantity input
            try {
                quantity = Integer.parseInt(quantityField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter a valid quantity.");
                return;
            }

            Item item = inventory.get(itemId);

            // Add item to cart if it exists and is in stock
            if (item != null && item.isInStock()) {
                double discount = calculateDiscount(quantity);
                double discountedPrice = item.getPrice() * (1 - discount);

                // Update item quantity in inventory
                item.setQuantity(item.getQuantity() - quantity);

                // Add item to cart
                cart.add(new Item(itemId, item.getDescription(), true, quantity, discountedPrice));
                
                // Update subtotal
                subtotal += discountedPrice * quantity;

                itemCount++;
                // Update GUI for the new item
                updateGuiForNewItem();

                // Update the cart display
                updateCartDisplay();

                // Update the subtotal display and enable necessary buttons
                itemsCountField.setText(String.format("$%.2f", subtotal));
                itemsCountLabel.setText("Cart subtotal for " + itemCount + " item(s):");
                viewOrderButton.setEnabled(true);
                checkoutButton.setEnabled(true);
            }
        }
    }

    // Listener for the View Order button
    private class ViewOrderButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Display the current cart items in a dialog
            StringBuilder cartContent = new StringBuilder();
            for (int i = 0; i < cart.size(); i++) {
                Item item = cart.get(i);
                double discount = calculateDiscount(item.getQuantity());
                double discountedPrice = item.getPrice() * (1 - discount);
                cartContent.append(String.format("%d. %s \"%s\" $%.2f %d %.0f%% $%.2f\n",
                        i + 1, item.getItemId(), item.getDescription(), item.getPrice(), item.getQuantity(), discount * 100, discountedPrice * item.getQuantity()));
            }
            JOptionPane.showMessageDialog(frame, cartContent.toString(), "Nile Dot Com - Current Shopping Cart Status", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Listener for the Checkout button
    private class CheckoutButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Prepare the date and time for the invoice and transaction ID
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a z");
            String formattedDate = dateFormat.format(now);
            String transactionId = new SimpleDateFormat("yyMMddHHmmss").format(now);
    
            // Prepare the invoice details
            StringBuilder invoice = new StringBuilder();
            invoice.append("Date: ").append(formattedDate).append("\n");
            invoice.append("Number of line items: ").append(cart.size()).append("\n\n");
            invoice.append("Item#/ ID / Title / Price / Qty / Disc % / Subtotal:\n");
    
            for (int i = 0; i < cart.size(); i++) {
                Item item = cart.get(i);
                double discount = calculateDiscount(item.getQuantity());
                double discountedPrice = item.getPrice() * (1 - discount);
                invoice.append(String.format("%d. %s \"%s\" $%.2f %d %.0f%% $%.2f\n",
                        i + 1, item.getItemId(), item.getDescription(), item.getPrice(), 
                        item.getQuantity(), discount * 100, discountedPrice * item.getQuantity()));
            }
    
            // Calculate tax and total
            double tax = subtotal * TAX_RATE;
            double total = subtotal + tax;
            invoice.append("\nOrder subtotal: $").append(String.format("%.2f", subtotal));
            invoice.append("\nTax rate: ").append(String.format("%.0f%%", TAX_RATE * 100));
            invoice.append("\nTax amount: $").append(String.format("%.2f", tax));
            invoice.append("\nORDER TOTAL: $").append(String.format("%.2f", total));
            invoice.append("\n\nThanks for shopping at Nile Dot Com!");
    
            // Display the invoice in a dialog
            JOptionPane.showMessageDialog(frame, invoice.toString(), "Nile Dot Com - FINAL INVOICE", JOptionPane.INFORMATION_MESSAGE);
    
            // Save each item in the transaction log
            try (PrintWriter writer = new PrintWriter(new FileWriter("transactions.csv", true))) {
                for (Item item : cart) {
                    double discount = calculateDiscount(item.getQuantity());
                    double discountedPrice = item.getPrice() * (1 - discount);
                    writer.println(String.format("%s, %s, \"%s\", %.2f, %d, %.0f%%, $%.2f, %s",
                            transactionId, item.getItemId(), item.getDescription(),
                            item.getPrice(), item.getQuantity(), discount * 100, 
                            discountedPrice * item.getQuantity(), formattedDate));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error saving the order.");
            }
    
            // Clear the order after checkout
            clearOrder();
        }
    }

    // Clear the current order and reset the GUI
    private void clearOrder() {
        cart.clear();
        itemCount = 0;
        subtotal = 0;
        itemIdField.setText("");
        quantityField.setText("");
        itemDetailsField.setText("");
        itemIdField.setEnabled(true);
        quantityField.setEnabled(true);
        searchButton.setEnabled(true);
        addToCartButton.setEnabled(false);
        viewOrderButton.setEnabled(false);
        checkoutButton.setEnabled(false);
        currentItemNumber = 1;
        itemIdPromptLabel.setText(String.format("Enter item ID for item #%d:", currentItemNumber));
        quantityLabel.setText(String.format("Enter quantity for item #%d:", currentItemNumber));
        searchButton.setText(String.format("Search For Item #%d", currentItemNumber));
        addToCartButton.setText(String.format("Add Item #%d To Cart", currentItemNumber));
        itemsCountField.setText(""); // Clear the subtotal display
        itemsCountLabel.setText("Cart subtotal for 0 item(s):"); // Reset subtotal label
        cartHeaderLabel.setText("Your Shopping Cart Currently Contains 0 Item(s)"); // Reset cart header
        
        // Clear the cart display fields
        for (JTextField cartField : cartFields) {
            cartField.setText("");
        }
    }

    // Calculate the discount based on the quantity of items
    private double calculateDiscount(int quantity) {
        if (quantity >= 15) {
            return 0.20;
        } else if (quantity >= 10) {
            return 0.15;
        } else if (quantity >= 5) {
            return 0.10;
        } else {
            return 0.00;
        }
    }

    // Update the GUI for the next item entry
    private void updateGuiForNewItem() {
        currentItemNumber++;
        itemIdPromptLabel.setText("Enter item ID for item #" + currentItemNumber + ":");
        quantityLabel.setText("Enter quantity for item #" + currentItemNumber + ":");
        searchButton.setText("Search For Item #" + currentItemNumber);
        addToCartButton.setText("Add Item #" + currentItemNumber + " To Cart");
        itemIdField.setText("");
        quantityField.setText("");
        addToCartButton.setEnabled(false);
        searchButton.setEnabled(true);

        // Disable inputs and buttons if the cart is full
        if (itemCount >= MAX_CART_SIZE) {
            itemIdField.setEnabled(false);
            quantityField.setEnabled(false);
            searchButton.setEnabled(false);
            addToCartButton.setEnabled(false);
        }
    }

    // Update the cart display with the items in the cart
    private void updateCartDisplay() {
        cartHeaderLabel.setText("Your Shopping Cart Currently Contains " + itemCount + " Item(s)");
        for (int i = 0; i < MAX_CART_SIZE; i++) {
            if (i < cart.size()) {
                Item item = cart.get(i);
                double discount = calculateDiscount(item.getQuantity());
                double discountedPrice = item.getPrice() * (1 - discount);
                cartFields[i].setText(String.format("Item %d – SKU: %s, Desc: \"%s\", Price Ea. $%.2f, Qty: %d, Total: $%.2f",
                        i + 1, item.getItemId(), item.getDescription(), item.getPrice(), item.getQuantity(), discountedPrice * item.getQuantity()));
            } else {
                cartFields[i].setText("");
            }
        }
    }

    public static void main(String[] args) {
        new NileDotCom();
    }
}

// Class representing an item in the inventory and cart
class Item {
    private String itemId;
    private String description;
    private boolean inStock;
    private int quantity;
    private double price;

    public Item(String itemId, String description, boolean inStock, int quantity, double price) {
        this.itemId = itemId;
        this.description = description;
        this.inStock = inStock;
        this.quantity = quantity;
        this.price = price;
    }

    public String getItemId() { return itemId;}

    public String getDescription() {return description;}

    public boolean isInStock() {return inStock;}

    public int getQuantity() {return quantity;}

    public void setQuantity(int quantity) {this.quantity = quantity;}

    public double getPrice() {return price;}
}
