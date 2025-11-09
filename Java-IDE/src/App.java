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
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import javax.swing.text.AbstractDocument.Content;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.input.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    // Terminal variables
    private VBox terminalBox;
    private TextFlow terminalOutput;
    private TextField terminalInput;
    private ScrollPane terminalScrollPane;
    private String currentDirectory;
    private boolean terminalVisible = false;
    private SplitPane editorTerminalSplitPane;
    private TabPane editorTabPane;
    private Map<Tab, File> tabFileMap = new HashMap<>(); // Maps tabs to files
    private Map<Tab, String> tabOriginalContentMap = new HashMap<>();
    private Map<Tab, TextArea> tabTextAreaMap = new HashMap<>();
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
                () -> saveFile());

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
        Menu terminalMenu = new Menu("Terminal");
        MenuItem terminalItem = new MenuItem("Terminal          Ctrl+`");
        terminalItem.setOnAction(e -> toggleTerimal());
        terminalMenu.getItems().addAll(
                new MenuItem("Command Palette   Ctrl+Shift+P"),
                new MenuItem("Explorer          Ctrl+Shift+E"),
                terminalItem);

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

        menuBar.getMenus().addAll(fileMenu, editMenu, terminalMenu, runMenu, helpMenu);

        return menuBar;
    }

    // Create split pane layout
    private SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.25); // 25% left, 75% right

        VBox fileExplorer = createFileExplorer();

        // Create vertical split pane for editor and terminal
        editorTerminalSplitPane = new SplitPane();
        editorTerminalSplitPane.setOrientation(Orientation.VERTICAL);
        editorTerminalSplitPane.setDividerPositions(0.7); // 70% editor, 30% terminal

        VBox editor = createEditor();
        terminalBox = createTerminal();
        terminalBox.setVisible(false);
        terminalBox.setManaged(false);

        editorTerminalSplitPane.getItems().addAll(editor, terminalBox);
        splitPane.getItems().addAll(fileExplorer, editorTerminalSplitPane); // Fixed this line!

        return splitPane;
    }

    // Create terminal panel
    private VBox createTerminal() {
        VBox terminal = new VBox();
        terminal.getStyleClass().add("terminal-area");
        terminal.setPrefHeight(250);

        // Terminal titlebar
        Label terminalLabel = new Label("TERMINAL");
        terminalLabel.getStyleClass().add("terminal-title");

        // terminal output area
        terminalOutput = new TextFlow();
        terminalOutput.getStyleClass().add("terminal-output");

        terminalScrollPane = new ScrollPane(terminalOutput);
        terminalScrollPane.getStyleClass().add("terminal-scroll");
        terminalScrollPane.setFitToWidth(true);
        terminalScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        // Terminal input field
        terminalInput = new TextField();
        terminalInput.getStyleClass().add("terminal-input");
        terminalInput.setPromptText("Enter command...");

        // Handle command execution on enter key
        terminalInput.setOnAction(e -> executeCommand(terminalInput.getText()));

        terminal.getChildren().addAll(terminalLabel, terminalScrollPane, terminalInput);
        VBox.setVgrow(terminalScrollPane, javafx.scene.layout.Priority.ALWAYS);

        return terminal;
    }

    // Toggle terminal visibility
    private void toggleTerimal() {
        terminalVisible = !terminalVisible;
        terminalBox.setVisible(terminalVisible);
        terminalBox.setManaged(terminalVisible);

        if (terminalVisible && currentDirectory != null) {
            appendToTerminal("Terminal opened at: " + currentDirectory + "\n", "green");
            terminalInput.requestFocus();
        }
    }

    // Append text to terminal with color
    private void appendToTerminal(String message, String color) {
        Text text = new Text(message);
        text.setStyle("-fx-fill: " + color + ";");
        terminalOutput.getChildren().add(text);

        // Auto scroll to bottom
        Platform.runLater(() -> terminalScrollPane.setVvalue(1.0));
    }

    // Execute terminal Command
    // Execute terminal command (uses Linux commands on all systems)
    private void executeCommand(String command) {
        if (command.trim().isEmpty()) {
            return;
        }

        // Display the command
        appendToTerminal("$ " + command + "\n", "#c9d1d9");

        // Clear input
        terminalInput.clear();

        // Handle special commands
        if (command.startsWith("cd ")) {
            handleCdCommand(command.substring(3).trim());
            return;
        }

        // Execute system command using bash/sh
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            // Set working directory
            if (currentDirectory != null) {
                processBuilder.directory(new File(currentDirectory));
            }

            // Use bash/sh for all systems (requires Git Bash, WSL, or similar on Windows)
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // For Windows, try Git Bash first, then WSL
                File gitBash = new File("C:\\Program Files\\Git\\bin\\bash.exe");
                if (gitBash.exists()) {
                    processBuilder.command(gitBash.getAbsolutePath(), "-c", command);
                } else {
                    // Try WSL
                    processBuilder.command("wsl", "bash", "-c", command);
                }
            } else {
                // Linux/Mac - use bash directly
                processBuilder.command("bash", "-c", command);
            }

            Process process = processBuilder.start();

            // Read output in separate thread
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String output = line + "\n";
                        Platform.runLater(() -> appendToTerminal(output, "#a5d6ff"));
                    }

                    // Read error output
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()));
                    while ((line = errorReader.readLine()) != null) {
                        String error = line + "\n";
                        Platform.runLater(() -> appendToTerminal(error, "#ff7b72"));
                    }

                    process.waitFor();

                } catch (Exception e) {
                    Platform.runLater(() -> appendToTerminal("Error: " + e.getMessage() + "\n", "#ff7b72"));
                }
            }).start();

        } catch (Exception e) {
            appendToTerminal("Error executing command: " + e.getMessage() + "\n", "#ff7b72");
            appendToTerminal("Note: On Windows, install Git Bash or WSL to use Linux commands\n", "#ffa657");
        }
    }

    // Handle cd (change directory) command
    private void handleCdCommand(String path) {
        File newDir;

        if (path.equals("..")) {
            // Go up one directory
            newDir = new File(currentDirectory).getParentFile();
        } else if (path.startsWith("/") || path.matches("[A-Za-z]:.*")) {
            // Absolute path
            newDir = new File(path);
        } else {
            // Relative path
            newDir = new File(currentDirectory, path);
        }

        if (newDir != null && newDir.exists() && newDir.isDirectory()) {
            currentDirectory = newDir.getAbsolutePath();
            appendToTerminal("Changed directory to: " + currentDirectory + "\n", "#7ee787");
        } else {
            appendToTerminal("Directory not found: " + path + "\n", "#ff7b72");
        }
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

        // Add Context menu for right-click
        ContextMenu contextMenu = new ContextMenu();
        MenuItem newFileItem = new MenuItem("new File");
        MenuItem newFolderItem = new MenuItem("New Folder");
        MenuItem deleteItem = new MenuItem("Delete");

        newFileItem.setOnAction(e -> createNewFile());
        newFolderItem.setOnAction(e -> createNewFolder());
        deleteItem.setOnAction(e -> deleteSelected());

        contextMenu.getItems().addAll(newFileItem, newFolderItem, new SeparatorMenuItem(), deleteItem);

        treeView.setContextMenu(contextMenu);
        return explorerBox;
    }

    // Create new file in selected directory
    private void createNewFile() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Please select a folder first");
            return;
        }

        File parentDir = getDirectoryForItem(selectedItem);
        if (parentDir == null) {
            showError("Please select a valid folder");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("newfile.txt");
        dialog.initOwner(mainStage);
        dialog.setTitle("New File");
        dialog.setHeaderText("Create New File");
        dialog.setContentText("Enter file name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(fileName -> {
            try {
                File newFile = new File(parentDir, fileName);
                if (newFile.createNewFile()) {
                    // Add to tree
                    TreeItem<String> newItem = new TreeItem<>("📄 " + fileName);
                    treeItemFileMap.put(newItem, newFile);
                    selectedItem.getChildren().add(newItem);
                    selectedItem.setExpanded(true);
                    showInfo("File created successfully!");
                } else {
                    showError("File already exists!");
                }
            } catch (IOException e) {
                showError("Error creating file: " + e.getMessage());
            }
        });
    }

    // Create new folder in selected directory
    private void createNewFolder() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Please select a folder first");
            return;
        }

        File parentDir = getDirectoryForItem(selectedItem);
        if (parentDir == null) {
            showError("Please select a valid folder");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("newfolder");
        dialog.initOwner(mainStage);
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create New Folder");
        dialog.setContentText("Enter folder name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(folderName -> {
            File newFolder = new File(parentDir, folderName);
            if (newFolder.mkdir()) {
                // Add to tree
                TreeItem<String> newItem = new TreeItem<>("📁 " + folderName);
                treeItemFileMap.put(newItem, newFolder);
                selectedItem.getChildren().add(newItem);
                selectedItem.setExpanded(true);
                showInfo("Folder created successfully!");
            } else {
                showError("Folder already exists or cannot be created!");
            }
        });
    }

    // Delete selected file or folder
    private void deleteSelected() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem == rootItem) {
            showError("Please select a file or folder to delete");
            return;
        }

        File file = treeItemFileMap.get(selectedItem);
        if (file == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete " + file.getName() + "?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (file.delete()) {
                selectedItem.getParent().getChildren().remove(selectedItem);
                treeItemFileMap.remove(selectedItem);
                showInfo("Deleted successfully!");
            } else {
                showError("Could not delete file/folder!");
            }
        }
    }

    // Get directory for tree item (file's parent or folder itself)
    private File getDirectoryForItem(TreeItem<String> item) {
        File file = treeItemFileMap.get(item);
        if (file == null) {
            return null;
        }
        return file.isDirectory() ? file : file.getParentFile();
    }

    // Create editor on right side with tabs
    private VBox createEditor() {
        VBox editorBox = new VBox();
        editorBox.getStyleClass().add("editor-area");

        // Create TabPane for multiple files
        editorTabPane = new TabPane();
        editorTabPane.getStyleClass().add("editor-tabs");
        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        // Create initial welcome tab
        Tab welcomeTab = createWelcomeTab();
        editorTabPane.getTabs().add(welcomeTab);

        // Handle tab selection change
        editorTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                updateCurrentFileFromTab(newTab);
            }
        });

        editorBox.getChildren().add(editorTabPane);
        VBox.setVgrow(editorTabPane, javafx.scene.layout.Priority.ALWAYS);

        return editorBox;
    }

    // Create Welcome tab
    private Tab createWelcomeTab() {
        Tab tab = new Tab("📄 Untitled-1");

        TextArea textArea = new TextArea();
        textArea.getStyleClass().add("text-editor");
        textArea.setText("// Welcome to IDE-My\n// Please select a directory to start working\n\n");
        textArea.setWrapText(false);

        tab.setContent(textArea);

        return tab;
    }

    // Create tab for a file
    private Tab createFileTab(File file, String content) {
        Tab tab = new Tab("📄 " + file.getName());
        tab.setClosable(true);

        TextArea textArea = new TextArea(content);
        textArea.getStyleClass().add("text-editor");
        textArea.setWrapText(false);

        // Track changes for this tab
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            String originalContent = tabOriginalContentMap.get(tab);
            if (originalContent != null && !newVal.equals(originalContent)) {
                if (!tab.getText().endsWith(" ●")) {
                    tab.setText(tab.getText() + " ●");
                }
            } else {
                tab.setText("📄 " + file.getName());
            }
        });

        // handle tab close request
        tab.setOnCloseRequest(e -> {
            if (tab.getText().endsWith(" ●")) {
                e.consume();
                handleTabClose(tab);
            } else {
                removeTabData(tab);
            }
        });

        tab.setContent(textArea);

        // Store tab data
        tabFileMap.put(tab, file);
        tabOriginalContentMap.put(tab, content);
        tabTextAreaMap.put(tab, textArea);

        return tab;
    }

    // Update current file from selectedTab
    private void updateCurrentFileFromTab(Tab tab) {
        currentFile = tabFileMap.get(tab);
        if (currentFile != null) {
            textArea = tabTextAreaMap.get(tab);
            originalContent = tabOriginalContentMap.get(tab);
            isModified = tab.getText().endsWith(" ●");
        }
    }

    // Remove tab data when closing
    private void removeTabData(Tab tab) {
        tabFileMap.remove(tab);
        tabOriginalContentMap.remove(tab);
        tabTextAreaMap.remove(tab);
    }

    // Handle Tab close with unsaved Changes
    private void handleTabClose(Tab tab) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Do you want to save changes to " + tabFileMap.get(tab).getName() + "?");
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == saveButton) {
                saveFileFromTab(tab);
                editorTabPane.getTabs().remove(tab);
                removeTabData(tab);
            } else if (result.get() == discardButton) {
                editorTabPane.getTabs().remove(tab);
                removeTabData(tab);
            }
        }

    }

    // Save file from specific tab
    private void saveFileFromTab(Tab tab) {
        File file = tabFileMap.get(tab);
        TextArea ta = tabTextAreaMap.get(tab);

        if (file != null && ta != null) {
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(ta.getText());
                writer.close();

                tabOriginalContentMap.put(tab, ta.getText());
                tab.setText("📄 " + file.getName());
                showInfo("File saved successfully!");
            } catch (IOException e) {
                showError("Error saving file: " + e.getMessage());
            }
        }
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

        textArea.setText(
                "// Directory loaded: " + directory.getAbsolutePath() + "\n// Double-click a file to edit\n\n");

        // Set current directory for terminal
        currentDirectory = directory.getAbsolutePath();
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

    // Read and display file content in new tab
    private void openFile(File file) {
        try {
            // Check if file is already open in a tab
            for (Tab tab : editorTabPane.getTabs()) {
                if (file.equals(tabFileMap.get(tab))) {
                    editorTabPane.getSelectionModel().select(tab);
                    return;
                }
            }

            // Read file content
            String content = new String(Files.readAllBytes(file.toPath()));

            // Create new tab
            Tab newTab = createFileTab(file, content);
            editorTabPane.getTabs().add(newTab);
            editorTabPane.getSelectionModel().select(newTab);

            // Update current references
            currentFile = file;
            textArea = tabTextAreaMap.get(newTab);
            originalContent = content;
            isModified = false;

        } catch (IOException e) {
            showError("Error opening file: " + e.getMessage());
        }
    }

    // Save current file (Ctrl+S)
    private void saveFile() {
        Tab currentTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null && tabFileMap.containsKey(currentTab)) {
            saveFileFromTab(currentTab);
        } else {
            showError("No file is currently open");
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