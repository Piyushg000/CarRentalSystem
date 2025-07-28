package mydatabasepackage;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;

class Car {
    private String carId;
    private String brand;
    private String model;
    private double basePricePerDay;
    private boolean isAvailable;

    public Car(String carId, String brand, String model, double basePricePerDay) {
        this.carId = carId;
        this.brand = brand;
        this.model = model;
        this.basePricePerDay = basePricePerDay;
        this.isAvailable = true;
    }

    public String getCarId() {
        return carId;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public double calculatePrice(int rentalDays) {
        return basePricePerDay * rentalDays;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void rent() {
        isAvailable = false;
    }

    public void returnCar() {
        isAvailable = true;
    }
}

class Customer {
    private String customerId;
    private String name;

    public Customer(String customerId, String name) {
        this.customerId = customerId;
        this.name = name;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }
}

class Rental {
    private Car car;
    private Customer customer;
    private int days;

    public Rental(Car car, Customer customer, int days) {
        this.car = car;
        this.customer = customer;
        this.days = days;
    }

    public Car getCar() {
        return car;
    }

    public Customer getCustomer() {
        return customer;
    }

    public int getDays() {
        return days;
    }
}

class CarRentalSystem {
    public void addCar(Car car) {
        String sql = "INSERT INTO cars (car_id, brand, model, base_price_per_day) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, car.getCarId());
            stmt.setString(2, car.getBrand());
            stmt.setString(3, car.getModel());
            stmt.setDouble(4, car.calculatePrice(1)); // Using calculatePrice to get base price
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addCustomer(Customer customer) {
        String sql = "INSERT INTO customers (customer_id, name) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customer.getCustomerId());
            stmt.setString(2, customer.getName());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void rentCar(Car car, Customer customer, int days) {
        String updateCarSql = "UPDATE cars SET is_available = FALSE WHERE car_id = ?";
        String insertRentalSql = "INSERT INTO rentals (car_id, customer_id, days) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement updateStmt = conn.prepareStatement(updateCarSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertRentalSql)) {

                updateStmt.setString(1, car.getCarId());
                updateStmt.executeUpdate();

                insertStmt.setString(1, car.getCarId());
                insertStmt.setString(2, customer.getCustomerId());
                insertStmt.setInt(3, days);
                insertStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void returnCar(Car car) {
        String updateCarSql = "UPDATE cars SET is_available = TRUE WHERE car_id = ?";
        String deleteRentalSql = "DELETE FROM rentals WHERE car_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement updateStmt = conn.prepareStatement(updateCarSql);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteRentalSql)) {

                updateStmt.setString(1, car.getCarId());
                updateStmt.executeUpdate();

                deleteStmt.setString(1, car.getCarId());
                deleteStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Car> getCars() {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT * FROM cars";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Car car = new Car(
                        rs.getString("car_id"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getDouble("base_price_per_day")
                );

                if (!rs.getBoolean("is_available")) {
                    car.rent();
                }

                cars.add(car);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cars;
    }

    public List<Customer> getCustomers() {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                customers.add(new Customer(
                        rs.getString("customer_id"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return customers;
    }

    public Car getCarById(String carId) {
        String sql = "SELECT * FROM cars WHERE car_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, carId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Car car = new Car(
                        rs.getString("car_id"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getDouble("base_price_per_day")
                );

                if (!rs.getBoolean("is_available")) {
                    car.rent();
                }

                return car;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}

public class CarRentalSystemUI extends Frame {
    private CarRentalSystem rentalSystem;
    private TextArea textArea;
    private TextArea carsListArea;

    // Input fields
    private TextField customerNameField;
    private TextField carIdField;
    private TextField rentalDaysField;

    public CarRentalSystemUI(CarRentalSystem rentalSystem) {
        this.rentalSystem = rentalSystem;
        setLayout(new BorderLayout(10, 10));
        setTitle("Car Rental System");
        setSize(800, 600);

        // Initialize UI components
        createHeaderPanel();
        createLeftPanel();
        createRightPanel();

        // Add panels to frame
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createLeftPanel(), BorderLayout.WEST);
        add(createRightPanel(), BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        setVisible(true);
    }

    private Panel createHeaderPanel() {
        Panel headerPanel = new Panel();
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(new Color(240, 240, 240));

        Label title = new Label("Car Rental System");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(title);

        return headerPanel;
    }

    private Panel createLeftPanel() {
        Panel leftPanel = new Panel();
        leftPanel.setLayout(new BorderLayout(5, 10));
        leftPanel.setBackground(new Color(245, 245, 245));

        // Input Panel
        Panel inputPanel = new Panel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Customer Name
        gbc.gridx = 0; gbc.gridy = 0;
        Label nameLabel = new Label("Customer Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        customerNameField = new TextField();
        customerNameField.setPreferredSize(new Dimension(200, 30));
        inputPanel.add(customerNameField, gbc);

        // Car ID
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        Label carIdLabel = new Label("Car ID:");
        carIdLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(carIdLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        carIdField = new TextField();
        carIdField.setPreferredSize(new Dimension(100, 30));
        inputPanel.add(carIdField, gbc);

        // Rental Days
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        Label rentalDaysLabel = new Label("Rental Days:");
        rentalDaysLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(rentalDaysLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        rentalDaysField = new TextField();
        rentalDaysField.setPreferredSize(new Dimension(80, 30));
        inputPanel.add(rentalDaysField, gbc);

        leftPanel.add(inputPanel, BorderLayout.NORTH);

        // Available Cars List
        Panel carsPanel = new Panel(new BorderLayout());
        carsPanel.add(new Label("Available Cars:", Label.CENTER), BorderLayout.NORTH);

        carsListArea = new TextArea(getAvailableCarsList(), 15, 30, TextArea.SCROLLBARS_VERTICAL_ONLY);
        carsListArea.setEditable(false);
        carsListArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        carsPanel.add(carsListArea, BorderLayout.CENTER);

        leftPanel.add(carsPanel, BorderLayout.CENTER);

        return leftPanel;
    }

    private Panel createRightPanel() {
        Panel rightPanel = new Panel(new BorderLayout(5, 10));

        // Button Panel
        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new GridLayout(3, 1, 10, 10));
        buttonPanel.setBackground(new Color(230, 230, 230));

        Button rentButton = new Button("Rent a Car");
        rentButton.setFont(new Font("Arial", Font.BOLD, 14));
        rentButton.setBackground(new Color(100, 200, 100));
        rentButton.setForeground(Color.WHITE);
        rentButton.addActionListener(e -> rentCar());
        buttonPanel.add(rentButton);

        Button returnButton = new Button("Return a Car");
        returnButton.setFont(new Font("Arial", Font.BOLD, 14));
        returnButton.setBackground(new Color(200, 100, 100));
        returnButton.setForeground(Color.WHITE);
        returnButton.addActionListener(e -> returnCar());
        buttonPanel.add(returnButton);

        Button exitButton = new Button("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 14));
        exitButton.setBackground(new Color(150, 150, 150));
        exitButton.setForeground(Color.WHITE);
        exitButton.addActionListener(e -> System.exit(0));
        buttonPanel.add(exitButton);

        rightPanel.add(buttonPanel, BorderLayout.NORTH);

        // Output Panel
        Panel outputPanel = new Panel(new BorderLayout());
        outputPanel.add(new Label("Rental Information:", Label.CENTER), BorderLayout.NORTH);

        textArea = new TextArea("", 15, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputPanel.add(textArea, BorderLayout.CENTER);

        rightPanel.add(outputPanel, BorderLayout.CENTER);

        return rightPanel;
    }

    private String getAvailableCarsList() {
        StringBuilder sb = new StringBuilder();
        for (Car car : rentalSystem.getCars()) {
            if (car.isAvailable()) {
                sb.append(String.format("%-6s: %-15s %-15s $%.2f/day\n",
                        car.getCarId(),
                        car.getBrand(),
                        car.getModel(),
                        car.calculatePrice(1)));
            }
        }
        return sb.toString();
    }

    private void rentCar() {
        String customerName = customerNameField.getText().trim();
        String carId = carIdField.getText().trim();
        String daysText = rentalDaysField.getText().trim();

        if (customerName.isEmpty() || carId.isEmpty() || daysText.isEmpty()) {
            textArea.append("\nError: Please fill all fields!\n");
            return;
        }

        try {
            int rentalDays = Integer.parseInt(daysText);
            if (rentalDays <= 0) {
                textArea.append("\nError: Rental days must be positive!\n");
                return;
            }

            Customer newCustomer = new Customer("CUS" + (rentalSystem.getCustomers().size() + 1), customerName);
            rentalSystem.addCustomer(newCustomer);

            Car selectedCar = rentalSystem.getCarById(carId);

            if (selectedCar != null && selectedCar.isAvailable()) {
                double totalPrice = selectedCar.calculatePrice(rentalDays);
                textArea.append("\n=== Rental Receipt ===\n");
                textArea.append(String.format("%-15s: %s\n", "Customer", newCustomer.getName()));
                textArea.append(String.format("%-15s: %s %s\n", "Car", selectedCar.getBrand(), selectedCar.getModel()));
                textArea.append(String.format("%-15s: %d days\n", "Rental Period", rentalDays));
                textArea.append(String.format("%-15s: $%.2f\n", "Total Price", totalPrice));
                textArea.append("=====================\n");

                rentalSystem.rentCar(selectedCar, newCustomer, rentalDays);
                textArea.append("Car rented successfully!\n");

                // Clear input fields and refresh cars list
                customerNameField.setText("");
                carIdField.setText("");
                rentalDaysField.setText("");
                carsListArea.setText(getAvailableCarsList());
            } else {
                textArea.append("\nError: Car not available or invalid selection!\n");
            }
        } catch (NumberFormatException e) {
            textArea.append("\nError: Please enter valid number for rental days!\n");
        }
    }

    private void returnCar() {
        String carId = carIdField.getText().trim();
        if (carId.isEmpty()) {
            textArea.append("\nError: Please enter Car ID!\n");
            return;
        }

        Car carToReturn = rentalSystem.getCarById(carId);

        if (carToReturn != null && !carToReturn.isAvailable()) {
            rentalSystem.returnCar(carToReturn);
            textArea.append("\nCar returned successfully:\n");
            textArea.append(carToReturn.getBrand() + " " + carToReturn.getModel() + "\n");
            carIdField.setText("");
            carsListArea.setText(getAvailableCarsList());
        } else {
            textArea.append("\nError: Car ID not found or car is not rented!\n");
        }
    }

    public static void main(String[] args) {
        CarRentalSystem rentalSystem = new CarRentalSystem();

        // Initialize database with sample cars (only needed once)
        /*
        rentalSystem.addCar(new Car("C001", "Toyota", "Camry", 60.0));
        rentalSystem.addCar(new Car("C002", "Honda", "Accord", 70.0));
        rentalSystem.addCar(new Car("C003", "Mahindra", "Thar", 150.0));
        rentalSystem.addCar(new Car("C004", "Hyundai", "Creta", 90.0));
        */

        new CarRentalSystemUI(rentalSystem);
    }
}