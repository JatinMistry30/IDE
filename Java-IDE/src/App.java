import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
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
import java.util.HashMap;
import java.util.Map;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class App extends Application {

    // UI components
    private TreeView<String> treeView;
    private TreeItem<String> rootItem;
    private TextArea textArea;
    private File currentFile;
    private String originalContent;
    private boolean isModified = false;
    private Stage mainStage;

    // Terminal components
    private VBox terminalBox;
    private TextFlow terminalOutput;
    private TextField terminalInput;
    private ScrollPane terminalScrollPane;
    private String currentDirectory;
    private boolean terminalVisible = false;
    private SplitPane editorTerminalSplitPane;
    private TabPane editorTabPane;
    
    // Shell process variables
    private Process shellProcess;
    private PrintWriter shellWriter;
    private Thread outputReaderThread;
    private Thread errorReaderThread;
    private boolean isShellStarting = false;
    
    // Tab management maps
    private Map<Tab, File> tabFileMap = new HashMap<>();
    private Map<Tab, String> tabOriginalContentMap = new HashMap<>();
    private Map<Tab, TextArea> tabTextAreaMap = new HashMap<>();
    private Map<TreeItem<String>, File> treeItemFileMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        createWindow(primaryStage);

        // Show directory chooser after 2 seconds
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> openDirectoryChooser(primaryStage));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Create main window with all components
    private void createWindow(Stage stage) {
        stage.setTitle("IDE-My - Simple Java IDE");

        BorderPane root = new BorderPane();
        MenuBar menuBar = createMenuBar(stage);
        root.setTop(menuBar);

        SplitPane splitPane = createSplitPane();
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1400, 900);
        
        // Load CSS if available
        try {
            scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }

        addKeyboardShortcuts(scene);

        // Handle window close
        stage.setOnCloseRequest(event -> {
            if (hasUnsavedChanges()) {
                event.consume();
                showUnsavedChangesDialogOnExit(stage);
            } else {
                stopShellProcess();
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    // Add all keyboard shortcuts
    private void addKeyboardShortcuts(Scene scene) {
        // Ctrl+S - Save
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                this::saveFile);

        // Ctrl+W - Close tab
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN),
                this::closeCurrentTab);

        // Ctrl+N - New file
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                this::createNewFile);

        // Ctrl+O - Open directory
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN),
                () -> openDirectoryChooser(mainStage));

        // Ctrl+` - Toggle terminal
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.BACK_QUOTE, KeyCombination.CONTROL_DOWN),
                this::toggleTerminal);

        // F5 - Run file
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F5),
                this::runCurrentFile);
    }

    // Create menu bar
    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        // File menu
        Menu fileMenu = new Menu("File");
        
        MenuItem newFileItem = new MenuItem("New File          Ctrl+N");
        newFileItem.setOnAction(e -> createNewFile());
        
        MenuItem openDirItem = new MenuItem("Open Directory   Ctrl+O");
        openDirItem.setOnAction(e -> openDirectoryChooser(stage));

        MenuItem saveItem = new MenuItem("Save              Ctrl+S");
        saveItem.setOnAction(e -> saveFile());

        MenuItem closeItem = new MenuItem("Close File        Ctrl+W");
        closeItem.setOnAction(e -> closeCurrentTab());

        MenuItem exitItem = new MenuItem("Exit              Alt+F4");
        exitItem.setOnAction(e -> {
            if (hasUnsavedChanges()) {
                showUnsavedChangesDialogOnExit(stage);
            } else {
                stopShellProcess();
                stage.close();
            }
        });

        fileMenu.getItems().addAll(newFileItem, openDirItem, saveItem, closeItem, new SeparatorMenuItem(), exitItem);

        // Edit menu
        Menu editMenu = new Menu("Edit");
        
        MenuItem undoItem = new MenuItem("Undo              Ctrl+Z");
        undoItem.setOnAction(e -> {
            Tab tab = editorTabPane.getSelectionModel().getSelectedItem();
            if (tab != null) {
                TextArea ta = tabTextAreaMap.get(tab);
                if (ta != null) ta.undo();
            }
        });
        
        MenuItem redoItem = new MenuItem("Redo              Ctrl+Y");
        redoItem.setOnAction(e -> {
            Tab tab = editorTabPane.getSelectionModel().getSelectedItem();
            if (tab != null) {
                TextArea ta = tabTextAreaMap.get(tab);
                if (ta != null) ta.redo();
            }
        });
        
        editMenu.getItems().addAll(undoItem, redoItem);

        // Terminal menu
        Menu terminalMenu = new Menu("Terminal");
        MenuItem terminalItem = new MenuItem("Toggle Terminal   Ctrl+`");
        terminalItem.setOnAction(e -> toggleTerminal());
        terminalMenu.getItems().add(terminalItem);

        // Run menu
        Menu runMenu = new Menu("Run");
        MenuItem runFileItem = new MenuItem("Run File          F5");
        runFileItem.setOnAction(e -> runCurrentFile());
        runMenu.getItems().add(runFileItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, terminalMenu, runMenu, helpMenu);

        return menuBar;
    }

    // Create main split pane layout
    private SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.25);

        VBox fileExplorer = createFileExplorer();

        editorTerminalSplitPane = new SplitPane();
        editorTerminalSplitPane.setOrientation(Orientation.VERTICAL);
        editorTerminalSplitPane.setDividerPositions(0.7);

        VBox editor = createEditor();
        terminalBox = createTerminal();
        terminalBox.setVisible(false);
        terminalBox.setManaged(false);

        editorTerminalSplitPane.getItems().addAll(editor, terminalBox);
        splitPane.getItems().addAll(fileExplorer, editorTerminalSplitPane);

        return splitPane;
    }

    // Create terminal panel
    private VBox createTerminal() {
        VBox terminal = new VBox();
        terminal.getStyleClass().add("terminal-area");
        terminal.setPrefHeight(250);
        terminal.setStyle("-fx-background-color: #0d1117;");

        // Terminal header with buttons
        HBox terminalHeader = new HBox(10);
        terminalHeader.setStyle("-fx-padding: 5px; -fx-background-color: #161b22;");
        
        Label terminalLabel = new Label("TERMINAL");
        terminalLabel.setStyle("-fx-text-fill: #7ee787; -fx-font-weight: bold; -fx-font-size: 12px;");
        
        Button stopButton = new Button("Stop (Ctrl+C)");
        stopButton.setStyle("-fx-background-color: #da3633; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3px 8px;");
        stopButton.setOnAction(e -> stopCurrentProcess());
        
        Button clearButton = new Button("Clear");
        clearButton.setStyle("-fx-background-color: #238636; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3px 8px;");
        clearButton.setOnAction(e -> clearTerminal());
        
        Button restartButton = new Button("Restart");
        restartButton.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3px 8px;");
        restartButton.setOnAction(e -> {
            stopShellProcess();
            startInteractiveShell();
        });
        
        terminalHeader.getChildren().addAll(terminalLabel, stopButton, clearButton, restartButton);

        // Terminal input field
        terminalInput = new TextField();
        terminalInput.setStyle(
            "-fx-background-color: #0d1117; " +
            "-fx-text-fill: #c9d1d9; " +
            "-fx-font-family: 'Consolas', 'Courier New', monospace; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 10px; " +
            "-fx-border-color: transparent; " +
            "-fx-prompt-text-fill: #6e7681;"
        );
        terminalInput.setPromptText("Type command and press Enter...");

        // Terminal output area
        terminalOutput = new TextFlow();
        terminalOutput.setStyle("-fx-background-color: #0d1117; -fx-padding: 10px;");

        terminalScrollPane = new ScrollPane(terminalOutput);
        terminalScrollPane.setStyle("-fx-background-color: #0d1117; -fx-background: #0d1117;");
        terminalScrollPane.setFitToWidth(true);
        terminalScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        terminalScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Handle Enter key
        terminalInput.setOnAction(e -> {
            String command = terminalInput.getText();
            if (!command.trim().isEmpty()) {
                appendToTerminal("$ " + command + "\n", "#7ee787");
                sendCommandToShell(command);
                terminalInput.clear();
            }
        });

        // Auto-focus input when terminal clicked
        terminalScrollPane.setOnMouseClicked(e -> terminalInput.requestFocus());
        terminalOutput.setOnMouseClicked(e -> terminalInput.requestFocus());

        terminal.getChildren().addAll(terminalHeader, terminalScrollPane, terminalInput);
        VBox.setVgrow(terminalScrollPane, javafx.scene.layout.Priority.ALWAYS);

        return terminal;
    }

    // Toggle terminal visibility
    private void toggleTerminal() {
        terminalVisible = !terminalVisible;
        terminalBox.setVisible(terminalVisible);
        terminalBox.setManaged(terminalVisible);

        if (terminalVisible) {
            if (shellProcess == null || !shellProcess.isAlive()) {
                startInteractiveShell();
            }
            terminalInput.requestFocus();
        }
    }

    // Start interactive shell session
    private void startInteractiveShell() {
        // Prevent multiple starts
        if (isShellStarting) {
            appendToTerminal("⚠️  Shell is already starting...\n", "#ffa657");
            return;
        }
        
        isShellStarting = true;
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            // Set working directory
            if (currentDirectory != null) {
                processBuilder.directory(new File(currentDirectory));
            }

            // Detect OS and choose shell
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Use cmd.exe for Windows
                processBuilder.command("cmd.exe");
                appendToTerminal("====================================\n", "#7ee787");
                appendToTerminal("Terminal Started (Windows CMD)\n", "#7ee787");
                appendToTerminal("====================================\n", "#7ee787");
                appendToTerminal("\n💡 For better experience:\n", "#ffa657");
                appendToTerminal("1. Install Git Bash: https://git-scm.com/downloads\n", "#a5d6ff");
                appendToTerminal("2. After install, restart IDE\n", "#a5d6ff");
                appendToTerminal("3. You'll get Linux commands (ls, rm, grep, etc.)\n\n", "#a5d6ff");
                
                // Check for WSL
                File wsl = new File("C:\\Windows\\System32\\wsl.exe");
                if (!wsl.exists()) {
                    appendToTerminal("📌 Want WSL? Run in PowerShell (Admin):\n", "#ffa657");
                    appendToTerminal("   wsl --install\n", "#a5d6ff");
                    appendToTerminal("   Then restart your PC\n\n", "#a5d6ff");
                }
                
                // Check for Git Bash
                File gitBash = new File("C:\\Program Files\\Git\\bin\\bash.exe");
                if (gitBash.exists()) {
                    appendToTerminal("✅ Git Bash detected! Restarting terminal will use it.\n\n", "#7ee787");
                }
            } else {
                // Use bash for Linux/Mac
                processBuilder.command("bash", "-i");
                appendToTerminal("🐧 Starting Bash...\n", "#7ee787");
            }

            processBuilder.redirectErrorStream(false);
            
            // Start process
            shellProcess = processBuilder.start();

            // Check if started successfully
            Thread.sleep(300);
            if (!shellProcess.isAlive()) {
                appendToTerminal("❌ Failed to start shell\n", "#ff7b72");
                shellProcess = null;
                isShellStarting = false;
                return;
            }

            // Get output streams
            OutputStream shellInput = shellProcess.getOutputStream();
            shellWriter = new PrintWriter(shellInput, true);

            // Start output reader thread
            outputReaderThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(shellProcess.getInputStream()));
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String output = line + "\n";
                        // Remove ANSI escape codes
                        String cleanOutput = output.replaceAll("\u001B\\[[;\\d]*m", "");
                        Platform.runLater(() -> appendToTerminal(cleanOutput, "#a5d6ff"));
                    }
                } catch (IOException e) {
                    // Stream closed
                }
            });
            outputReaderThread.setDaemon(true);
            outputReaderThread.start();

            // Start error reader thread
            errorReaderThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(shellProcess.getErrorStream()));
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String error = line + "\n";
                        // Remove ANSI escape codes
                        String cleanError = error.replaceAll("\u001B\\[[;\\d]*m", "");
                        Platform.runLater(() -> appendToTerminal(cleanError, "#ff7b72"));
                    }
                } catch (IOException e) {
                    // Stream closed
                }
            });
            errorReaderThread.setDaemon(true);
            errorReaderThread.start();

            // Change to current directory for cmd.exe
            if (os.contains("win") && currentDirectory != null) {
                Thread.sleep(500);
                // Change drive
                String drive = currentDirectory.substring(0, 2);
                shellWriter.println(drive);
                shellWriter.flush();
                Thread.sleep(200);
                // Change directory
                shellWriter.println("cd \"" + currentDirectory + "\"");
                shellWriter.flush();
            }

            appendToTerminal("✓ Terminal ready at: " + currentDirectory + "\n", "#7ee787");
            appendToTerminal("You can now type commands like:\n", "#c9d1d9");
            appendToTerminal("  dir          - list files\n", "#c9d1d9");
            appendToTerminal("  cd foldername - change directory\n", "#c9d1d9");
            appendToTerminal("  npm start    - run Node.js app\n", "#c9d1d9");
            appendToTerminal("  node file.js - run JavaScript\n", "#c9d1d9");
            appendToTerminal("  javac File.java - compile Java\n\n", "#c9d1d9");
            
            isShellStarting = false;

        } catch (Exception e) {
            appendToTerminal("❌ Error: " + e.getMessage() + "\n", "#ff7b72");
            e.printStackTrace();
            shellProcess = null;
            isShellStarting = false;
        }
    }

    // Send command to shell
    private void sendCommandToShell(String command) {
        if (command.trim().isEmpty()) {
            return;
        }

        // Check if shell is running
        if (shellProcess == null || !shellProcess.isAlive()) {
            appendToTerminal("⚠️  Shell not running.\n", "#ffa657");
            appendToTerminal("💡 Click 'Restart' button or toggle terminal\n", "#ffa657");
            return;
        }

        // Handle special commands
        if (command.trim().equals("clear") || command.trim().equals("cls")) {
            clearTerminal();
            return;
        }

        if (command.trim().equals("exit")) {
            stopShellProcess();
            appendToTerminal("✓ Shell stopped. Click 'Restart' to start again.\n", "#7ee787");
            return;
        }

        // Send command to shell
        try {
            shellWriter.println(command);
            shellWriter.flush();
        } catch (Exception e) {
            appendToTerminal("❌ Error: " + e.getMessage() + "\n", "#ff7b72");
        }
    }

    // Stop current process (Ctrl+C)
    private void stopCurrentProcess() {
        if (shellProcess != null && shellProcess.isAlive()) {
            try {
                // Send Ctrl+C
                shellWriter.write(3);
                shellWriter.flush();
                
                appendToTerminal("\n^C (Process interrupted)\n", "#ffa657");
            } catch (Exception e) {
                shellProcess.destroyForcibly();
                appendToTerminal("Process stopped\n", "#ff7b72");
                
                // Restart shell
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        Platform.runLater(this::startInteractiveShell);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        } else {
            appendToTerminal("No process running\n", "#ffa657");
        }
    }

    // Stop shell completely
    private void stopShellProcess() {
        if (shellProcess != null && shellProcess.isAlive()) {
            try {
                shellWriter.println("exit");
                shellWriter.flush();
                Thread.sleep(500);
                
                if (shellProcess.isAlive()) {
                    shellProcess.destroyForcibly();
                }
            } catch (Exception e) {
                shellProcess.destroyForcibly();
            }
        }
        shellProcess = null;
        isShellStarting = false;
    }

    // Clear terminal output
    private void clearTerminal() {
        terminalOutput.getChildren().clear();
        appendToTerminal("Terminal cleared\n", "#7ee787");
    }

    // Append text to terminal
    private void appendToTerminal(String message, String color) {
        Text text = new Text(message);
        text.setStyle("-fx-fill: " + color + "; -fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px;");
        terminalOutput.getChildren().add(text);

        Platform.runLater(() -> terminalScrollPane.setVvalue(1.0));
    }

    // Create file explorer
    private VBox createFileExplorer() {
        VBox explorerBox = new VBox();
        explorerBox.getStyleClass().add("file-explorer");
        explorerBox.setStyle("-fx-background-color: #1f2121; -fx-padding: 5px;");

        Label explorerLabel = new Label("EXPLORER");
        explorerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px;");

        rootItem = new TreeItem<>("📁 No Directory Selected");
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.getStyleClass().add("tree-view");
        treeView.setShowRoot(true);

        // Double-click to open file
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

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem newFileItem = new MenuItem("New File");
        MenuItem newFolderItem = new MenuItem("New Folder");
        MenuItem deleteItem = new MenuItem("Delete");
        MenuItem refreshItem = new MenuItem("Refresh");

        newFileItem.setOnAction(e -> createNewFile());
        newFolderItem.setOnAction(e -> createNewFolder());
        deleteItem.setOnAction(e -> deleteSelected());
        refreshItem.setOnAction(e -> refreshFileExplorer());

        contextMenu.getItems().addAll(newFileItem, newFolderItem, new SeparatorMenuItem(), deleteItem, refreshItem);

        treeView.setContextMenu(contextMenu);
        return explorerBox;
    }

    // Refresh file explorer
    private void refreshFileExplorer() {
        if (currentDirectory != null) {
            File dir = new File(currentDirectory);
            if (dir.exists() && dir.isDirectory()) {
                loadDirectory(dir);
                showInfo("Refreshed!");
            }
        }
    }

    // Create new file
    private void createNewFile() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        
        File parentDir;
        if (selectedItem == null || selectedItem == rootItem) {
            if (currentDirectory == null) {
                showError("Please open a directory first");
                return;
            }
            parentDir = new File(currentDirectory);
        } else {
            parentDir = getDirectoryForItem(selectedItem);
        }

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
                    refreshFileExplorer();
                    showInfo("File created!");
                    openFile(newFile);
                } else {
                    showError("File already exists!");
                }
            } catch (IOException e) {
                showError("Error: " + e.getMessage());
            }
        });
    }

    // Create new folder
    private void createNewFolder() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        
        File parentDir;
        if (selectedItem == null || selectedItem == rootItem) {
            if (currentDirectory == null) {
                showError("Please open a directory first");
                return;
            }
            parentDir = new File(currentDirectory);
        } else {
            parentDir = getDirectoryForItem(selectedItem);
        }

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
                refreshFileExplorer();
                showInfo("Folder created!");
            } else {
                showError("Folder already exists!");
            }
        });
    }

    // Delete selected file or folder
    private void deleteSelected() {
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem == rootItem) {
            showError("Please select a file or folder");
            return;
        }

        File file = treeItemFileMap.get(selectedItem);
        if (file == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete " + file.getName() + "?");
        alert.setContentText("This cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteFileRecursively(file)) {
                selectedItem.getParent().getChildren().remove(selectedItem);
                treeItemFileMap.remove(selectedItem);
                showInfo("Deleted!");
                closeTabForFile(file);
            } else {
                showError("Could not delete!");
            }
        }
    }

    // Delete file or folder recursively
    private boolean deleteFileRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteFileRecursively(child)) return false;
                }
            }
        }
        return file.delete();
    }

    // Close tab for deleted file
    private void closeTabForFile(File file) {
        Tab tabToClose = null;
        for (Map.Entry<Tab, File> entry : tabFileMap.entrySet()) {
            if (entry.getValue().equals(file)) {
                tabToClose = entry.getKey();
                break;
            }
        }
        if (tabToClose != null) {
            editorTabPane.getTabs().remove(tabToClose);
            removeTabData(tabToClose);
        }
    }

    // Get directory for tree item
    private File getDirectoryForItem(TreeItem<String> item) {
        File file = treeItemFileMap.get(item);
        if (file == null) {
            if (item == rootItem && currentDirectory != null) {
                return new File(currentDirectory);
            }
            return null;
        }
        return file.isDirectory() ? file : file.getParentFile();
    }

    // Create editor with tabs
    private VBox createEditor() {
        VBox editorBox = new VBox();
        editorBox.getStyleClass().add("editor-area");

        editorTabPane = new TabPane();
        editorTabPane.getStyleClass().add("editor-tabs");
        editorTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        Tab welcomeTab = createWelcomeTab();
        editorTabPane.getTabs().add(welcomeTab);
        
        textArea = (TextArea) welcomeTab.getContent();

        // Update current file when tab changes
        editorTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                updateCurrentFileFromTab(newTab);
            }
        });

        editorBox.getChildren().add(editorTabPane);
        VBox.setVgrow(editorTabPane, javafx.scene.layout.Priority.ALWAYS);

        return editorBox;
    }

    // Create welcome tab
    private Tab createWelcomeTab() {
        Tab tab = new Tab("📄 Welcome");
        tab.setClosable(true);

        TextArea textArea = new TextArea();
        textArea.getStyleClass().add("text-editor");
        textArea.setText("// Welcome to IDE-My\n// Open directory (Ctrl+O) to start\n// Press Ctrl+` for terminal\n// Press F5 to run file\n\n");
        textArea.setWrapText(false);

        tab.setContent(textArea);
        return tab;
    }

    // Create tab for file
    private Tab createFileTab(File file, String content) {
        String icon = getFileIcon(file);
        
        Tab tab = new Tab(icon + " " + file.getName());
        tab.setClosable(true);

        TextArea textArea = new TextArea(content);
        textArea.getStyleClass().add("text-editor");
        textArea.setWrapText(false);

        // Track changes
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            String originalContent = tabOriginalContentMap.get(tab);
            if (originalContent != null && !newVal.equals(originalContent)) {
                if (!tab.getText().endsWith(" ●")) {
                    tab.setText(tab.getText() + " ●");
                }
            } else {
                tab.setText(icon + " " + file.getName());
            }
        });

        // Handle tab close
        tab.setOnCloseRequest(e -> {
            if (tab.getText().endsWith(" ●")) {
                e.consume();
                handleTabClose(tab);
            } else {
                removeTabData(tab);
            }
        });

        tab.setContent(textArea);

        tabFileMap.put(tab, file);
        tabOriginalContentMap.put(tab, content);
        tabTextAreaMap.put(tab, textArea);

        return tab;
    }

    // Get file icon based on extension
    private String getFileIcon(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".java")) return "☕";
        if (fileName.endsWith(".py")) return "🐍";
        if (fileName.endsWith(".js")) return "📜";
        if (fileName.endsWith(".html")) return "🌐";
        if (fileName.endsWith(".css")) return "🎨";
        if (fileName.endsWith(".json")) return "📋";
        if (fileName.endsWith(".txt")) return "📝";
        if (fileName.endsWith(".md")) return "📄";
        return "📄";
    }

    // Update current file from selected tab
    private void updateCurrentFileFromTab(Tab tab) {
        currentFile = tabFileMap.get(tab);
        textArea = tabTextAreaMap.get(tab);
        if (currentFile != null) {
            originalContent = tabOriginalContentMap.get(tab);
            isModified = tab.getText().endsWith(" ●");
        } else {
            originalContent = null;
            isModified = false;
        }
    }

    // Remove tab data
    private void removeTabData(Tab tab) {
        tabFileMap.remove(tab);
        tabOriginalContentMap.remove(tab);
        tabTextAreaMap.remove(tab);
    }

    // Handle tab close with unsaved changes
    private void handleTabClose(Tab tab) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Unsaved Changes");
        File file = tabFileMap.get(tab);
        alert.setHeaderText("Save changes to " + (file != null ? file.getName() : "this file") + "?");

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

    // Save file from tab
    private void saveFileFromTab(Tab tab) {
        File file = tabFileMap.get(tab);
        TextArea ta = tabTextAreaMap.get(tab);

        if (file != null && ta != null) {
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(ta.getText());
                writer.close();

                tabOriginalContentMap.put(tab, ta.getText());
                String icon = getFileIcon(file);
                tab.setText(icon + " " + file.getName());
                showInfo("Saved!");
            } catch (IOException e) {
                showError("Error: " + e.getMessage());
            }
        }
    }

    // Show directory chooser
    private void openDirectoryChooser(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Project Directory");

        String userHome = System.getProperty("user.home");
        directoryChooser.setInitialDirectory(new File(userHome));

        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            loadDirectory(selectedDirectory);
        }
    }

    // Load directory into file explorer
    private void loadDirectory(File directory) {
        rootItem.getChildren().clear();
        treeItemFileMap.clear();
        rootItem.setValue("📁 " + directory.getName());

        loadFilesIntoTree(directory, rootItem);

        currentDirectory = directory.getAbsolutePath();

        // Restart terminal if visible
        if (terminalVisible && shellProcess != null) {
            stopShellProcess();
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(this::startInteractiveShell);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        showInfo("Directory loaded!");
    }

    // Load files recursively into tree
    private void loadFilesIntoTree(File directory, TreeItem<String> parentItem) {
        File[] files = directory.listFiles();

        if (files != null) {
            // Sort: folders first, then files
            java.util.Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            for (File file : files) {
                if (file.isHidden()) continue;

                String icon = file.isDirectory() ? "📁 " : getFileIcon(file) + " ";

                TreeItem<String> item = new TreeItem<>(icon + file.getName());
                treeItemFileMap.put(item, file);
                parentItem.getChildren().add(item);

                if (file.isDirectory()) {
                    loadFilesIntoTree(file, item);
                }
            }
        }
    }

    // Open file from tree
    private void openFileFromTree(TreeItem<String> item) {
        File file = treeItemFileMap.get(item);
        if (file != null && file.isFile()) {
            openFile(file);
        } else if (file != null && file.isDirectory()) {
            item.setExpanded(!item.isExpanded());
        }
    }

    // Open file in editor
    private void openFile(File file) {
        try {
            // Check if already open
            for (Tab tab : editorTabPane.getTabs()) {
                if (file.equals(tabFileMap.get(tab))) {
                    editorTabPane.getSelectionModel().select(tab);
                    return;
                }
            }

            String content = new String(Files.readAllBytes(file.toPath()));

            Tab newTab = createFileTab(file, content);
            editorTabPane.getTabs().add(newTab);
            editorTabPane.getSelectionModel().select(newTab);

            currentFile = file;
            textArea = tabTextAreaMap.get(newTab);
            originalContent = content;
            isModified = false;

        } catch (IOException e) {
            showError("Error: " + e.getMessage());
        }
    }

    // Save current file
    private void saveFile() {
        Tab currentTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null && tabFileMap.containsKey(currentTab)) {
            saveFileFromTab(currentTab);
        } else {
            showError("No file open");
        }
    }

    // Close current tab
    private void closeCurrentTab() {
        Tab currentTab = editorTabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            if (currentTab.getText().endsWith(" ●")) {
                handleTabClose(currentTab);
            } else {
                editorTabPane.getTabs().remove(currentTab);
                removeTabData(currentTab);
            }
        }
    }

    // Run current file
    private void runCurrentFile() {
        Tab currentTab = editorTabPane.getSelectionModel().getSelectedItem();
        File file = tabFileMap.get(currentTab);
        
        if (file == null) {
            showError("No file open");
            return;
        }

        if (!terminalVisible) {
            toggleTerminal();
        }

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                
                Platform.runLater(() -> {
                    String fileName = file.getName().toLowerCase();
                    String command;

                    if (fileName.endsWith(".java")) {
                        if (currentTab.getText().endsWith(" ●")) {
                            saveFileFromTab(currentTab);
                        }
                        String className = fileName.substring(0, fileName.length() - 5);
                        command = "javac " + fileName + " && java " + className;
                    } else if (fileName.endsWith(".py")) {
                        command = "python " + fileName;
                    } else if (fileName.endsWith(".js")) {
                        command = "node " + fileName;
                    } else if (fileName.endsWith(".html")) {
                        try {
                            java.awt.Desktop.getDesktop().browse(file.toURI());
                            appendToTerminal("Opening in browser...\n", "#7ee787");
                        } catch (Exception e) {
                            showError("Cannot open: " + e.getMessage());
                        }
                        return;
                    } else {
                        showError("Cannot run this file type");
                        return;
                    }

                    File parentDir = file.getParentFile();
                    if (parentDir != null) {
                        sendCommandToShell("cd \"" + parentDir.getAbsolutePath() + "\"");
                        
                        new Thread(() -> {
                            try {
                                Thread.sleep(300);
                                String finalCommand = command;
                                Platform.runLater(() -> {
                                    appendToTerminal("\n=== Running " + fileName + " ===\n", "#7ee787");
                                    sendCommandToShell(finalCommand);
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Check if any tabs have unsaved changes
    private boolean hasUnsavedChanges() {
        for (Tab tab : editorTabPane.getTabs()) {
            if (tab.getText().endsWith(" ●")) {
                return true;
            }
        }
        return false;
    }

    // Show dialog on exit with unsaved changes
    private void showUnsavedChangesDialogOnExit(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        alert.setContentText("Save all before exiting?");

        ButtonType saveAllButton = new ButtonType("Save All");
        ButtonType discardButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveAllButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == saveAllButton) {
                for (Tab tab : editorTabPane.getTabs()) {
                    if (tab.getText().endsWith(" ●")) {
                        saveFileFromTab(tab);
                    }
                }
                stopShellProcess();
                stage.close();
            } else if (result.get() == discardButton) {
                stopShellProcess();
                stage.close();
            }
        }
    }

    // Show about dialog
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(mainStage);
        alert.setTitle("About IDE-My");
        alert.setHeaderText("IDE-My - Simple Java IDE");
        alert.setContentText("Features:\n" +
                "• Multiple file editor\n" +
                "• Interactive terminal\n" +
                "• Run Java, Python, JavaScript, HTML\n" +
                "• File operations\n\n" +
                "Shortcuts:\n" +
                "Ctrl+O - Open Directory\n" +
                "Ctrl+N - New File\n" +
                "Ctrl+S - Save File\n" +
                "Ctrl+W - Close Tab\n" +
                "Ctrl+` - Toggle Terminal\n" +
                "F5 - Run File\n\n" +
                "Install Git Bash for better terminal:\n" +
                "https://git-scm.com/downloads");
        alert.showAndWait();
    }

    // Show error dialog
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(mainStage);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Show info dialog
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(mainStage);
        alert.setTitle("Info");
        alert.setContentText(message);
        
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(() -> alert.close());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
