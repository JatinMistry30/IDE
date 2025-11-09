import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    // Store UI components
    private TreeView<String> treeView;
    private TreeItem<String> rootItem;
    private TextArea textArea;
    private Label editorLabel;
    private File currentFile; // Currently opened file
    private String originalContent; // Original file content to track changes
    private boolean isModified = false; // Track if file has unsaved changes
    private Stage mainStage; // Main application window
    
    // Map to link tree items with their files (since TreeItem doesn't have getUserData)
    private Map<TreeItem<String>, File> treeItemFileMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        
        // Create and show the window first
        createWindow(primaryStage);
        
        // Wait 2 seconds then show directory chooser
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                
                // Run on JavaFX thread
                Platform.runLater(() -> {
                    openDirectoryChooser(primaryStage);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Create main window with menu, file explorer, and editor
    private void createWindow(Stage stage) {
        stage.setTitle("IDE-My");
        stage.getIcons().clear();

        BorderPane root = new BorderPane();

        // Add menu bar at top
        MenuBar menuBar = createMenuBar(stage);
        root.setTop(menuBar);

        // Add split pane with file explorer and editor
        SplitPane splitPane = createSplitPane();
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());

        // Add Ctrl+S keyboard shortcut for saving
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
            () -> saveFile()
        );

        // Handle window close - check for unsaved changes
        stage.setOnCloseRequest(event -> {
            if (isModified) {
                event.consume(); // Prevent closing
                showUnsavedChangesDialog(stage);
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    // Create menu bar with all menus
    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        // File Menu
        Menu fileMenu = new Menu("File");
        MenuItem openDirItem = new MenuItem("Open Directory   Ctrl+O");
        openDirItem.setOnAction(e -> openDirectoryChooser(stage));
        
        MenuItem saveItem = new MenuItem("Save              Ctrl+S");
        saveItem.setOnAction(e -> saveFile());
        
        MenuItem closeItem = new MenuItem("Close File        Ctrl+W");
        closeItem.setOnAction(e -> closeFile());
        
        fileMenu.getItems().addAll(
                new MenuItem("New File          Ctrl+N"),
                openDirItem,
                saveItem,
                closeItem,
                new MenuItem("Exit              Alt+F4"));

        // Edit Menu
        Menu editMenu = new Menu("Edit");
        editMenu.getItems().addAll(
                new MenuItem("Undo              Ctrl+Z"),
                new MenuItem("Redo              Ctrl+Y"),
                new MenuItem("Cut               Ctrl+X"),
                new MenuItem("Copy              Ctrl+C"),
                new MenuItem("Paste             Ctrl+V"));

        // View Menu
        Menu viewMenu = new Menu("View");
        viewMenu.getItems().addAll(
                new MenuItem("Command Palette   Ctrl+Shift+P"),
                new MenuItem("Explorer          Ctrl+Shift+E"),
                new MenuItem("Terminal          Ctrl+`"));

        // Run Menu
        Menu runMenu = new Menu("Run");
        runMenu.getItems().addAll(
                new MenuItem("Run File          F5"),
                new MenuItem("Run Project       Ctrl+F5"),
                new MenuItem("Stop              Shift+F5"));

        // Help Menu
        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().addAll(
                new MenuItem("Documentation"),
                new MenuItem("About"));

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, runMenu, helpMenu);

        return menuBar;
    }

    // Create split pane layout
    private SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.25); // 25% left, 75% right

        VBox fileExplorer = createFileExplorer();
        VBox editor = createEditor();

        splitPane.getItems().addAll(fileExplorer, editor);

        return splitPane;
    }

    // Create file explorer on left side
    private VBox createFileExplorer() {
        VBox explorerBox = new VBox();
        explorerBox.getStyleClass().add("file-explorer");

        Label explorerLabel = new Label("EXPLORER");
        explorerLabel.getStyleClass().add("explorer-title");

        // Create tree view with empty root
        rootItem = new TreeItem<>("📁 No Directory Selected");
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.getStyleClass().add("tree-view");
        treeView.setShowRoot(true);

        // Handle double-click to open file
        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    openFileFromTree(selectedItem);
                }
            }
        });

        explorerBox.getChildren().addAll(explorerLabel, treeView);
        VBox.setVgrow(treeView, javafx.scene.layout.Priority.ALWAYS);

        return explorerBox;
    }

    // Create editor on right side
    private VBox createEditor() {
        VBox editorBox = new VBox();
        editorBox.getStyleClass().add("editor-area");

        // Title bar with file name and close button
        HBox titleBox = new HBox();
        titleBox.setSpacing(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getStyleClass().add("editor-title-box");
        
        editorLabel = new Label("📄 Untitled-1");
        editorLabel.getStyleClass().add("editor-title");
        
        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> closeFile());
        
        titleBox.getChildren().addAll(editorLabel, closeButton);

        // Text area for editing
        textArea = new TextArea();
        textArea.getStyleClass().add("text-editor");
        textArea.setText("// Welcome to IDE-My\n// Please select a directory to start working\n\n");
        textArea.setWrapText(false);

        // Track changes in text area
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (currentFile != null && !newValue.equals(originalContent)) {
                isModified = true;
                updateEditorTitle();
            } else if (currentFile != null && newValue.equals(originalContent)) {
                isModified = false;
                updateEditorTitle();
            }
        });

        editorBox.getChildren().addAll(titleBox, textArea);
        VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);

        return editorBox;
    }

    // Show directory chooser dialog
    private void openDirectoryChooser(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Directory");
        
        File selectedDirectory = directoryChooser.showDialog(stage);
        
        if (selectedDirectory != null) {
            loadDirectory(selectedDirectory);
        }
    }

    // Load directory into file explorer
    private void loadDirectory(File directory) {
        rootItem.getChildren().clear();
        treeItemFileMap.clear(); // Clear the map
        rootItem.setValue("📁 " + directory.getName());
        
        // Load all files and folders
        loadFilesIntoTree(directory, rootItem);
        
        textArea.setText("// Directory loaded: " + directory.getAbsolutePath() + "\n// Double-click a file to edit\n\n");
    }

    // Recursively load files and folders into tree
    private void loadFilesIntoTree(File directory, TreeItem<String> parentItem) {
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                // Choose icon based on file type
                String icon;
                if (file.isDirectory()) {
                    icon = "📁 ";
                } else {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".java")) {
                        icon = "☕ ";
                    } else if (fileName.endsWith(".css")) {
                        icon = "🎨 ";
                    } else if (fileName.endsWith(".xml") || fileName.endsWith(".fxml")) {
                        icon = "📋 ";
                    } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                        icon = "🖼️ ";
                    } else if (fileName.endsWith(".jar")) {
                        icon = "📦 ";
                    } else {
                        icon = "📄 ";
                    }
                }
                
                // Create tree item and store file reference in map
                TreeItem<String> item = new TreeItem<>(icon + file.getName());
                treeItemFileMap.put(item, file);
                parentItem.getChildren().add(item);
                
                // If folder, load its contents recursively
                if (file.isDirectory()) {
                    loadFilesIntoTree(file, item);
                }
            }
        }
    }

    // Open file when tree item is double-clicked
    private void openFileFromTree(TreeItem<String> item) {
        File file = treeItemFileMap.get(item);
        if (file != null && file.isFile()) {
            // Check for unsaved changes before opening new file
            if (isModified) {
                showUnsavedChangesDialogBeforeOpen(file);
            } else {
                openFile(file);
            }
        }
    }

    // Read and display file content
    private void openFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            textArea.setText(content);
            currentFile = file;
            originalContent = content;
            isModified = false;
            updateEditorTitle();
        } catch (IOException e) {
            showError("Error opening file: " + e.getMessage());
        }
    }

    // Save current file (Ctrl+S)
    private void saveFile() {
        if (currentFile == null) {
            showError("No file is currently open");
            return;
        }

        try {
            FileWriter writer = new FileWriter(currentFile);
            writer.write(textArea.getText());
            writer.close();
            
            // Update original content and clear modified flag
            originalContent = textArea.getText();
            isModified = false;
            updateEditorTitle();
            
            showInfo("File saved successfully!");
        } catch (IOException e) {
            showError("Error saving file: " + e.getMessage());
        }
    }

    // Close current file
    private void closeFile() {
        if (isModified) {
            showUnsavedChangesDialog(mainStage);
        } else {
            resetEditor();
        }
    }

    // Reset editor to empty state
    private void resetEditor() {
        currentFile = null;
        originalContent = null;
        isModified = false;
        textArea.setText("// Welcome to IDE-My\n// Double-click a file to edit\n\n");
        editorLabel.setText("📄 Untitled-1");
    }

    // Update title with file name and modified indicator (●)
    private void updateEditorTitle() {
        if (currentFile != null) {
            String title = "📄 " + currentFile.getName();
            if (isModified) {
                title += " ●"; // Dot shows unsaved changes
            }
            editorLabel.setText(title);
        }
    }

    // Show dialog when closing with unsaved changes
    private void showUnsavedChangesDialog(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Do you want to save changes?");
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == saveButton) {
                saveFile();
                if (stage == mainStage && stage.isShowing()) {
                    stage.close();
                } else {
                    resetEditor();
                }
            } else if (result.get() == discardButton) {
                if (stage == mainStage && stage.isShowing()) {
                    stage.close();
                } else {
                    resetEditor();
                }
            }
            // If cancel, do nothing
        }
    }

    // Show dialog when opening new file with unsaved changes
    private void showUnsavedChangesDialogBeforeOpen(File newFile) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Do you want to save changes?");
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent()) {
            if (result.get() == saveButton) {
                saveFile();
                openFile(newFile);
            } else if (result.get() == discardButton) {
                openFile(newFile);
            }
            // If cancel, stay on current file
        }
    }

    // Show error message dialog
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(mainStage);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Show success message dialog
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}