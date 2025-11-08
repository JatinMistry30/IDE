import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TextArea;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        createWindow("IDE-My");
    }

    private void createWindow(String labelText) {
        Stage stage = new Stage();
        stage.setTitle("IDE-My");
        stage.getIcons().clear();

        BorderPane root = new BorderPane();

        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        SplitPane splitPane = createSplitPane();
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(
                new MenuItem("New File          Ctrl+N"),
                new MenuItem("Open File         Ctrl+O"),
                new MenuItem("Save              Ctrl+S"),
                new MenuItem("Save As           Ctrl+Shift+S"),
                new MenuItem("Exit              Alt+F4"));

        Menu editMenu = new Menu("Edit");
        editMenu.getItems().addAll(
                new MenuItem("Undo              Ctrl+Z"),
                new MenuItem("Redo              Ctrl+Y"),
                new MenuItem("Cut               Ctrl+X"),
                new MenuItem("Copy              Ctrl+C"),
                new MenuItem("Paste             Ctrl+V"));

        Menu viewMenu = new Menu("View");
        viewMenu.getItems().addAll(
                new MenuItem("Command Palette   Ctrl+Shift+P"),
                new MenuItem("Explorer          Ctrl+Shift+E"),
                new MenuItem("Terminal          Ctrl+`"));

        Menu runMenu = new Menu("Run");
        runMenu.getItems().addAll(
                new MenuItem("Run File          F5"),
                new MenuItem("Run Project       Ctrl+F5"),
                new MenuItem("Stop              Shift+F5"));

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().addAll(
                new MenuItem("Documentation"),
                new MenuItem("About"));

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, runMenu, helpMenu);

        return menuBar;
    }

    private SplitPane createSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.25);

        VBox fileExplorer = createFileExplorer();
        VBox editor = createEditor();

        splitPane.getItems().addAll(fileExplorer, editor);

        return splitPane;
    }

    private VBox createFileExplorer() {
        VBox explorerBox = new VBox();
        explorerBox.getStyleClass().add("file-explorer");

        Label explorerLabel = new Label("EXPLORER");
        explorerLabel.getStyleClass().add("explorer-title");

        TreeItem<String> rootItem = new TreeItem<>("📁 Project");
        rootItem.setExpanded(true);

        TreeItem<String> srcFolder = new TreeItem<>("📁 src");
        srcFolder.getChildren().addAll(
                new TreeItem<>("📄 Main.java"),
                new TreeItem<>("📄 App.java"),
                new TreeItem<>("📄 Utils.java"));
        srcFolder.setExpanded(true);

        TreeItem<String> resFolder = new TreeItem<>("📁 resources");
        resFolder.getChildren().addAll(
                new TreeItem<>("🎨 dark-theme.css"),
                new TreeItem<>("🖼️ icon.png"));

        TreeItem<String> libFolder = new TreeItem<>("📁 lib");
        libFolder.getChildren().addAll(
                new TreeItem<>("📦 library.jar"));

        rootItem.getChildren().addAll(srcFolder, resFolder, libFolder);

        TreeView<String> treeView = new TreeView<>(rootItem);
        treeView.getStyleClass().add("tree-view");
        treeView.setShowRoot(true);

        explorerBox.getChildren().addAll(explorerLabel, treeView);
        VBox.setVgrow(treeView, javafx.scene.layout.Priority.ALWAYS);

        return explorerBox;
    }

    private VBox createEditor() {
        VBox editorBox = new VBox();
        editorBox.getStyleClass().add("editor-area");

        Label editorLabel = new Label("📄 Untitled-1");
        editorLabel.getStyleClass().add("editor-title");

        TextArea textArea = new TextArea();
        textArea.getStyleClass().add("text-editor");
        textArea.setText("// Welcome to IDE-My\n\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}");
        textArea.setWrapText(false);

        editorBox.getChildren().addAll(editorLabel, textArea);
        VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);

        return editorBox;
    }

    public static void main(String[] args) {
        launch(args);
    }
}